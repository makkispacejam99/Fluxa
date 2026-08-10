package com.makkispacejam.fluxa.video.source

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.okhttp.OkHttpDataSource

/* Sistema de chunks, ayuda a prevenir parones en la
reproducción cargando el video a trozos de manera progresiva */

@UnstableApi
class ChunkedReconnectDataSource(
    private val upstreamFactory: OkHttpDataSource.Factory,
    private val chunkSizeBytes: Long = CHUNK_SIZE_DEFAULT
) : DataSource {

    private var current: OkHttpDataSource? = null
    private var listener: TransferListener? = null

    private lateinit var baseDataSpec: DataSpec
    private var absolutePosition: Long = 0L
    private var resourceLength: Long = C.LENGTH_UNSET.toLong()
    private var bytesRemainingForRequest: Long = C.LENGTH_UNSET.toLong()

    override fun addTransferListener(transferListener: TransferListener) {
        listener = transferListener
        current?.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        baseDataSpec = dataSpec
        absolutePosition = dataSpec.position
        bytesRemainingForRequest = dataSpec.length
        resourceLength = C.LENGTH_UNSET.toLong()

        val firstChunkLength = openNextChunk()

        return when {
            dataSpec.length != C.LENGTH_UNSET.toLong() -> dataSpec.length
            resourceLength != C.LENGTH_UNSET.toLong() -> resourceLength - dataSpec.position
            else -> firstChunkLength
        }
    }

    private fun openNextChunk(): Long {
        current?.let { runCatching { it.close() } }

        val chunkLength = if (bytesRemainingForRequest == C.LENGTH_UNSET.toLong())
            chunkSizeBytes
        else
            minOf(chunkSizeBytes, bytesRemainingForRequest)

        val chunkSpec = baseDataSpec.buildUpon()
            .setPosition(absolutePosition)
            .setLength(if (chunkLength <= 0L) C.LENGTH_UNSET.toLong() else chunkLength)
            .build()

        val ds = upstreamFactory.createDataSource()
        listener?.let { ds.addTransferListener(it) }
        val opened = ds.open(chunkSpec)
        current = ds

        if (resourceLength == C.LENGTH_UNSET.toLong()) {
            val contentRange = ds.responseHeaders["Content-Range"]?.firstOrNull()
            contentRange?.substringAfterLast('/')?.toLongOrNull()?.let { resourceLength = it }
        }

        Log.d("FluxaChunkedDS", "🔄 Chunk abierto en byte $absolutePosition (size=$chunkLength)")
        return opened
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (bytesRemainingForRequest == 0L) return C.RESULT_END_OF_INPUT
        val ds = current ?: return C.RESULT_END_OF_INPUT

        val bytesRead = ds.read(buffer, offset, length)

        if (bytesRead == C.RESULT_END_OF_INPUT) {
            val reachedEndOfResource = resourceLength != C.LENGTH_UNSET.toLong() &&
                    absolutePosition >= resourceLength
            if (reachedEndOfResource || bytesRemainingForRequest == 0L) {
                return C.RESULT_END_OF_INPUT
            }
            openNextChunk()
            return read(buffer, offset, length)
        }

        absolutePosition += bytesRead
        if (bytesRemainingForRequest != C.LENGTH_UNSET.toLong()) {
            bytesRemainingForRequest -= bytesRead
        }
        return bytesRead
    }

    override fun getUri(): Uri? = current?.uri

    override fun close() {
        current?.let { runCatching { it.close() } }
        current = null
    }

    companion object {
        const val CHUNK_SIZE_DEFAULT = 4L * 1024 * 1024
    }

    class Factory(
        private val upstreamFactory: OkHttpDataSource.Factory,
        private val chunkSizeBytes: Long = CHUNK_SIZE_DEFAULT
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource =
            ChunkedReconnectDataSource(upstreamFactory, chunkSizeBytes)
    }
}