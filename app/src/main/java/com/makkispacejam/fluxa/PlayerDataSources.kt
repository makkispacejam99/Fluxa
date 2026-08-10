package com.makkispacejam.fluxa

import android.content.Context
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import com.makkispacejam.fluxa.video.source.ChunkedReconnectDataSource
import okhttp3.OkHttpClient

@androidx.media3.common.util.UnstableApi
object PlayerDataSources {

    fun buildHttpDataSourceFactory(): OkHttpDataSource.Factory {
        val dispatcher = okhttp3.Dispatcher().apply {
            maxRequests = 100
            maxRequestsPerHost = 10
        }
        val client = OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .connectionPool(okhttp3.ConnectionPool(50, 5, java.util.concurrent.TimeUnit.MINUTES))
            .build()

        return OkHttpDataSource.Factory(client)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14; Pixel 9a) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.6167.164 Mobile Safari/537.36")
            .setDefaultRequestProperties(mapOf(
                "Origin" to "https://www.youtube.com",
                "Referer" to "https://www.youtube.com/",
                "Accept" to "*/*",
                "Accept-Language" to "es-419,es;q=0.9"
            ))
    }

    fun buildSubtitleDataSourceFactory(): OkHttpDataSource.Factory {
        val client = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        return OkHttpDataSource.Factory(client)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14; Pixel 9a) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.6167.164 Mobile Safari/537.36")
            .setDefaultRequestProperties(mapOf(
                "Origin" to "https://www.youtube.com",
                "Referer" to "https://www.youtube.com/"
            ))
    }

    fun buildChunkedDataSourceFactory(httpFactory: OkHttpDataSource.Factory): ChunkedReconnectDataSource.Factory {
        return ChunkedReconnectDataSource.Factory(httpFactory)
    }

    fun buildMediaSourceFactory(subtitleFactory: OkHttpDataSource.Factory, context: Context): DefaultMediaSourceFactory {
        return DefaultMediaSourceFactory(context)
            .setDataSourceFactory(subtitleFactory)
            .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(7))
    }
}
