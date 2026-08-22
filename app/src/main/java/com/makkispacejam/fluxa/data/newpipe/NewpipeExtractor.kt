package com.makkispacejam.fluxa.data.newpipe

import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response as OkHttpResponse
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.services.youtube.InnertubeClientRequestInfo
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.downloader.Request as NewPipeRequest
import org.schabi.newpipe.extractor.downloader.Response as NewPipeResponse
import java.io.IOException
import java.util.concurrent.TimeUnit

class YouTubeDownloader : Downloader() {

    companion object {
        val overrideClient = ThreadLocal<String>()

        private const val FIREFOX_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0"
        private const val CHROME_GFE_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 gzip(gfe)"
        private val CLIENT_NAME_REGEX = Regex("\"clientName\"\\s*:\\s*\"([A-Za-z_]+)\"")

        @Volatile
        var visitorData: String? = null

        fun fetchVisitorData(): Boolean {
            return try {
                val vd = YoutubeParsingHelper.getVisitorDataFromInnertube(
                    InnertubeClientRequestInfo.ofAndroidClient(),
                    NewPipe.getPreferredLocalization(),
                    NewPipe.getPreferredContentCountry(),
                    YoutubeParsingHelper.getClientInfoHeaders(),
                    YoutubeParsingHelper.YOUTUBEI_V1_URL,
                    YoutubeParsingHelper.getClientVersion(),
                    false
                )
                if (vd.isNotEmpty()) {
                    visitorData = vd
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.w("FluxaDownloader", "no se pudo obtener visitorData de YouTube", e)
                false
            }
        }
    }

    private val cookieStore = HashMap<String, MutableList<Cookie>>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
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

        var bodyString: String? = null
        val finalData = if (data != null && url.contains("youtubei/v1/")) {
            try {
                var bodyStr = String(data, Charsets.UTF_8)
                bodyString = bodyStr
                if (currentOverride != null && url.contains("youtubei/v1/player")) {
                    bodyStr = bodyStr
                        .replace("\"clientName\":\"ANDROID\"", "\"clientName\":\"$currentOverride\"")
                        .replace("\"clientName\": \"ANDROID\"", "\"clientName\": \"$currentOverride\"")
                }
                val vd = visitorData
                if (vd != null && !bodyStr.contains("\"visitorData\":\"")) {
                    bodyStr = bodyStr
                        .replace("\"visitorData\":null", "\"visitorData\":\"$vd\"")
                        .replace("\"visitorData\": null", "\"visitorData\": \"$vd\"")
                }
                bodyStr.toByteArray(Charsets.UTF_8)
            } catch (e: Exception) {
                Log.e("FluxaDownloader", "fallo al modificar el cuerpo de la peticion: $url", e)
                data
            }
        } else data

        val body = if (httpMethod == "POST" || httpMethod == "PUT") {
            finalData?.toRequestBody() ?: "".toRequestBody()
        } else {
            null
        }

        val okHttpRequestBuilder = Request.Builder()
            .url(url)
            .method(httpMethod, body)

        headers.forEach { (name, values) ->
            values.forEach { value ->
                okHttpRequestBuilder.addHeader(name, value)
            }
        }

        val hasUserAgent = headers.keys.any { it.equals("User-Agent", ignoreCase = true) }
        if (!hasUserAgent) {
            okHttpRequestBuilder.header("User-Agent", resolveUserAgent(currentOverride, bodyString))
        }

        val hasAcceptLanguage = headers.keys.any { it.equals("Accept-Language", ignoreCase = true) }
        if (!hasAcceptLanguage) {
            okHttpRequestBuilder.header("Accept-Language", buildAcceptLanguage())
        }

        val clientName = when (currentOverride ?: "") {
            "WEB" -> "1"
            "IOS" -> "5"
            "WEB_REMIX" -> "67"
            else -> null
        }
        if (clientName != null) {
            okHttpRequestBuilder.header("X-YouTube-Client-Name", clientName)
        }

        var okHttpResponse: OkHttpResponse
        val okHttpRequest = okHttpRequestBuilder.build()
        try {
            okHttpResponse = client.newCall(okHttpRequest).execute()
        } catch (_: IOException) {
            val retryRequest = okHttpRequest.newBuilder()
                .header("User-Agent", alternateUserAgent(currentOverride, bodyString))
                .build()
            okHttpResponse = client.newCall(retryRequest).execute()
        }
        if (okHttpResponse.code == 429 || okHttpResponse.code == 403) {
            okHttpResponse.close()
            val retryRequest = okHttpRequest.newBuilder()
                .header("User-Agent", alternateUserAgent(currentOverride, bodyString))
                .build()
            okHttpResponse = client.newCall(retryRequest).execute()
        }

        val status = okHttpResponse.code
        val responseBody = okHttpResponse.body.use { it.string() }
        if (status >= 400 || responseBody.contains("recaptcha") || responseBody.contains("captcha") ||
            responseBody.contains("robot") || responseBody.contains("unusual traffic") ||
            responseBody.contains("sign in to confirm") || responseBody.contains("loginRequired") ||
            responseBody.contains("ERROR")) {
            Log.e("FluxaDownloader", "posible bloqueo de YouTube: $httpMethod $url -> $status :: ${responseBody.take(300)}")
        } else {
            Log.d("FluxaDownloader", "OK: $httpMethod $url -> $status")
        }

        return NewPipeResponse(
            status,
            okHttpResponse.message,
            okHttpResponse.headers.toMultimap(),
            responseBody,
            url
        )
    }

    private fun buildAcceptLanguage(): String {
        val localization = NewPipe.getPreferredLocalization()
        val lang = localization.languageCode
        val country = localization.countryCode
        return if (lang.isEmpty()) {
            "es-ES,es;q=0.9"
        } else if (country.isEmpty()) {
            lang
        } else {
            "$lang-$country,$lang;q=0.9"
        }
    }

    private fun resolveUserAgent(override: String?, body: String?): String {
        val client = CLIENT_NAME_REGEX.find(body ?: "")?.groupValues?.get(1)
        return when {
            override == "IOS" || client == "IOS" -> YoutubeParsingHelper.getIosUserAgent(NewPipe.getPreferredLocalization())
            override == "WEB_REMIX" || client == "WEB_REMIX" -> CHROME_GFE_UA
            override == "WEB" || client == "WEB" || client == "WEB_EMBEDDED_PLAYER" -> FIREFOX_UA
            else -> YoutubeParsingHelper.getAndroidUserAgent(NewPipe.getPreferredLocalization())
        }
    }

    private fun alternateUserAgent(override: String?, body: String?): String {
        val client = CLIENT_NAME_REGEX.find(body ?: "")?.groupValues?.get(1)
        return when {
            override == "IOS" || override == "WEB" || override == "WEB_REMIX" ||
                client == "IOS" || client == "WEB" || client == "WEB_REMIX" ->
                YoutubeParsingHelper.getAndroidUserAgent(NewPipe.getPreferredLocalization())
            else -> FIREFOX_UA
        }
    }
}
