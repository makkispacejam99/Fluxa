package com.makkispacejam.fluxa.data.newpipe

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NewPipeRequest
import org.schabi.newpipe.extractor.downloader.Response as NewPipeResponse
import java.io.IOException
import java.util.concurrent.TimeUnit

class YouTubeDownloader : Downloader() {

    companion object {
        val overrideClient = ThreadLocal<String>()
    }

    private val cookieStore = HashMap<String, MutableList<Cookie>>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore[url.host] = cookies.toMutableList()
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host] ?: emptyList()
            }
        })
        .build()

    @Throws(IOException::class)
    override fun execute(request: NewPipeRequest): NewPipeResponse {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val data = request.dataToSend()

        val currentOverride = overrideClient.get()

        val finalData = if (currentOverride != null && url.contains("youtubei/v1/player") && data != null) {
            try {
                String(data, Charsets.UTF_8)
                    .replace("\"clientName\":\"ANDROID\"", "\"clientName\":\"$currentOverride\"")
                    .replace("\"clientName\": \"ANDROID\"", "\"clientName\": \"$currentOverride\"")
                    .toByteArray(Charsets.UTF_8)
            } catch (_: Exception) { data }
        } else data

        val body = if (httpMethod == "POST" || httpMethod == "PUT") {
            finalData?.toRequestBody() ?: "".toRequestBody()
        } else {
            null
        }

        val okHttpRequestBuilder = Request.Builder()
            .url(url)
            .method(httpMethod, body)

        val hasUserAgent = headers.keys.any { it.equals("User-Agent", ignoreCase = true) }
        if (!hasUserAgent) {
            val ua = when (currentOverride) {
                "WEB" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36"
                "IOS" -> "com.google.ios.youtube/19.45.32 (iPhone16,2; U; CPU iOS 18_0 like Mac OS X; es_ES)"
                "WEB_REMIX" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36 gzip(gfe)"
                else -> "com.google.android.youtube/19.05.36 (Linux; U; Android 14; es_ES) Mozilla/5.0"
            }
            okHttpRequestBuilder.header("User-Agent", ua)
        }

        val hasAcceptLanguage = headers.keys.any { it.equals("Accept-Language", ignoreCase = true) }
        if (!hasAcceptLanguage) {
            okHttpRequestBuilder.header("Accept-Language", "es-419,es;q=0.9")
            val clientHeader = when (currentOverride) {
                "IOS" -> "5"
                "WEB" -> "2"
                "WEB_REMIX" -> "67"
                "ANDROID_MUSIC" -> "21"
                else -> "1"
            }
            okHttpRequestBuilder.header("X-YouTube-Client-Name", clientHeader)
        }

        headers.forEach { (name, values) ->
            values.forEach { value ->
                okHttpRequestBuilder.addHeader(name, value)
            }
        }

        val okHttpRequest = okHttpRequestBuilder.build()
        val response = client.newCall(okHttpRequest).execute()

        return NewPipeResponse(
            response.code,
            response.message,
            response.headers.toMultimap(),
            response.body.use { it.string() },
            url
        )
    }
}