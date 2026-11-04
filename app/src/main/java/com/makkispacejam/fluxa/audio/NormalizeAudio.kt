package com.makkispacejam.fluxa.audio

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.math.tanh

@OptIn(UnstableApi::class)
class NormalizeAudioProcessor : BaseAudioProcessor() {

    companion object {
        @Volatile
        var enabled = false
        private const val TARGET_RMS = 0.19f
        private const val MIN_GAIN = 0.3f
        private const val MAX_GAIN = 3.0f
        private const val SILENCE_RMS = 0.002f

        private const val RMS_WINDOW_SAMPLES = 512
        private const val ATTACK_GAIN = 0.008f
        private const val RELEASE_GAIN = 0.00006f
        private const val ENV_FOLLOW_UP = 0.25f
        private const val ENV_FOLLOW_DOWN = 0.05f

        private const val LOW_SHELF_FREQ = 200f
        private const val HIGH_SHELF_FREQ = 4500f
        private const val BASS_BOOST = 0.28f
        private const val AIR_BOOST = 0.08f
    }

    private var encoding = C.ENCODING_PCM_16BIT
    private var bytesPerSample = 2
    private var channels = 1
    private var sampleRate = 44100
    private var currentGain = 1f
    private var targetGain = 1f
    private var envelope = 0f
    private var rmsSquareSum = 0.0
    private var rmsSampleCount = 0
    private var lpLow = FloatArray(1)
    private var lpHigh = FloatArray(1)
    private var bLow = 1f - exp(-2f * PI.toFloat() * LOW_SHELF_FREQ / sampleRate)
    private var bHigh = 1f - exp(-2f * PI.toFloat() * HIGH_SHELF_FREQ / sampleRate)

    override fun onConfigure(inputFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        encoding = inputFormat.encoding
        bytesPerSample = if (inputFormat.encoding == C.ENCODING_PCM_FLOAT) 4 else 2
        channels = (inputFormat.bytesPerFrame / bytesPerSample).coerceAtLeast(1)
        sampleRate = inputFormat.sampleRate.takeIf { it > 0 } ?: 44100
        bLow = 1f - exp(-2f * PI.toFloat() * LOW_SHELF_FREQ / sampleRate)
        bHigh = 1f - exp(-2f * PI.toFloat() * HIGH_SHELF_FREQ / sampleRate)
        lpLow = FloatArray(channels)
        lpHigh = FloatArray(channels)
        return inputFormat
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        currentGain = 1f
        targetGain = 1f
        envelope = 0f
        rmsSquareSum = 0.0
        rmsSampleCount = 0
        lpLow.fill(0f)
        lpHigh.fill(0f)
    }

    override fun onReset() {
        currentGain = 1f
        targetGain = 1f
        envelope = 0f
        rmsSquareSum = 0.0
        rmsSampleCount = 0
        lpLow.fill(0f)
        lpHigh.fill(0f)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val output = replaceOutputBuffer(inputBuffer.remaining())
        if (enabled) {
            var channelIndex = 0
            while (inputBuffer.hasRemaining()) {
                val sample = if (encoding == C.ENCODING_PCM_FLOAT) {
                    inputBuffer.float
                } else {
                    inputBuffer.short.toFloat() / 32768f
                }
                updateEnvelope(sample)
                currentGain = if (targetGain > currentGain) {
                    currentGain + (targetGain - currentGain) * RELEASE_GAIN
                } else {
                    currentGain + (targetGain - currentGain) * ATTACK_GAIN
                }
                val value = limited(toneShaped(sample * currentGain, channelIndex % channels))
                if (encoding == C.ENCODING_PCM_FLOAT) {
                    output.putFloat(value)
                } else {
                    output.putShort((value * 32767f).toInt().coerceIn(-32768, 32767).toShort())
                }
                channelIndex++
            }
        } else {
            output.put(inputBuffer)
        }
        output.flip()
    }

    private fun toneShaped(input: Float, channel: Int): Float {
        val filteredLow = lpLow[channel] + bLow * (input - lpLow[channel])
        val filteredHigh = lpHigh[channel] + bHigh * (input - lpHigh[channel])
        lpLow[channel] = filteredLow
        lpHigh[channel] = filteredHigh
        return input + BASS_BOOST * filteredLow + AIR_BOOST * (input - filteredHigh)
    }

    private fun updateEnvelope(sample: Float) {
        rmsSquareSum += (sample * sample).toDouble()
        rmsSampleCount++
        if (rmsSampleCount < RMS_WINDOW_SAMPLES) return

        val rms = sqrt((rmsSquareSum / rmsSampleCount).toFloat())
        envelope = if (rms > envelope) {
            envelope + (rms - envelope) * ENV_FOLLOW_UP
        } else {
            envelope + (rms - envelope) * ENV_FOLLOW_DOWN
        }
        rmsSquareSum = 0.0
        rmsSampleCount = 0

        targetGain = if (envelope < SILENCE_RMS) {
            currentGain
        } else {
            (TARGET_RMS / envelope).coerceIn(MIN_GAIN, MAX_GAIN)
        }
    }

    private fun limited(value: Float): Float =
        if (value > 1f || value < -1f) tanh(value) else value
}

object NormalizeAudio {

    fun setEnabled(value: Boolean) {
        NormalizeAudioProcessor.enabled = value
    }

    @OptIn(UnstableApi::class)
    fun renderersFactory(context: Context): DefaultRenderersFactory =
        @OptIn(UnstableApi::class)
        object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioOutputPlaybackParameters: Boolean
            ): AudioSink =
                DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParameters)
                    .setAudioProcessors(arrayOf(NormalizeAudioProcessor()))
                    .build()
        }
}