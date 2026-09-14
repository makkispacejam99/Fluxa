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
import kotlin.math.sqrt
import kotlin.math.tanh

@OptIn(UnstableApi::class)
class NormalizeAudioProcessor : BaseAudioProcessor() {

    companion object {
        @Volatile
        var enabled = false
        private const val TARGET_RMS = 0.19f
        private const val MIN_GAIN = 0.4f
        private const val MAX_GAIN = 3.5f
        private const val SILENCE_RMS = 0.004f
    }

    private var encoding = C.ENCODING_PCM_16BIT
    private var bytesPerSample = 2
    private var channels = 1
    private var currentGain = 1f

    override fun onConfigure(inputFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        encoding = inputFormat.encoding
        bytesPerSample = if (inputFormat.encoding == C.ENCODING_PCM_FLOAT) 4 else 2
        channels = (inputFormat.bytesPerFrame / bytesPerSample).coerceAtLeast(1)
        return inputFormat
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        currentGain = 1f
    }

    override fun onReset() {
        currentGain = 1f
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val output = replaceOutputBuffer(inputBuffer.remaining())
        if (enabled) {
            val frames = inputBuffer.remaining() / inputAudioFormat.bytesPerFrame
            val gain = smoothedGain(measureRms(inputBuffer))
            var frame = 0
            while (inputBuffer.hasRemaining() && frame < frames) {
                repeat(channels) {
                    if (encoding == C.ENCODING_PCM_FLOAT) {
                        output.putFloat(limited(inputBuffer.float * gain))
                    } else {
                        val sample = inputBuffer.short.toFloat() / 32768f
                        val value = (limited(sample * gain) * 32767f).toInt().coerceIn(-32768, 32767)
                        output.putShort(value.toShort())
                    }
                }
                frame++
            }
        } else {
            output.put(inputBuffer)
        }
        output.flip()
    }

    private fun measureRms(inputBuffer: ByteBuffer): Float {
        val duplicate = inputBuffer.duplicate()
        var sum = 0.0
        var count = 0
        if (encoding == C.ENCODING_PCM_FLOAT) {
            while (duplicate.hasRemaining()) {
                val sample = duplicate.float
                sum += (sample * sample).toDouble()
                count++
            }
        } else {
            while (duplicate.hasRemaining()) {
                val sample = duplicate.short.toFloat() / 32768f
                sum += (sample * sample).toDouble()
                count++
            }
        }
        return if (count == 0) 0f else sqrt((sum / count).toFloat())
    }

    private fun smoothedGain(rms: Float): Float {
        if (rms < SILENCE_RMS) return currentGain
        val target = (TARGET_RMS / rms).coerceIn(MIN_GAIN, MAX_GAIN)
        val alpha = if (target > currentGain) 0.22f else 0.06f
        currentGain += (target - currentGain) * alpha
        return currentGain
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