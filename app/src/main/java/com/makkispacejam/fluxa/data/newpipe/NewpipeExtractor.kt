@file:Suppress("SameParameterValue")

package com.makkispacejam.fluxa.data.newpipe

import android.util.Log
import com.google.gson.JsonParser
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
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class YouTubeDownloader : Downloader() {

    companion object {
        val overrideClient = ThreadLocal<String>()

        private const val FIREFOX_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0"
        private const val CHROME_GFE_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 gzip(gfe)"
        private val CLIENT_NAME_REGEX = Regex("\"clientName\"\\s*:\\s*\"([A-Za-z_]+)\"")

        private const val CONSENT_PROBE_URL =
            "https://www.youtube.com/results?search_query=&ucbcb=1"
        private const val FALLBACK_CLIENT_VERSION = "2.20260120.01.00"
        private const val SWJS_REQUEST_URL = "https://www.youtube.com/sw.js"
        private const val SWJS_VERSION_URL = "https://www.youtube.com/sw.js_data"
        private val INJECT_VERSION_REGEX_1 = Regex("""INNERTUBE_CONTEXT_CLIENT_VERSION":"[\d.]+"""")
        private val INJECT_VERSION_REGEX_2 = Regex("""innertube_context_client_version":"[\d.]+"""")
        private val CONSENT_FORM_ACTION = Regex(
            """<form[^>]*?\baction\s*=\s*["']([^"']*consent\.youtube\.com[^"']*)["']""",
            RegexOption.IGNORE_CASE
        )
        private val CONSENT_INPUT_NAME_VALUE = Regex(
            """<input[^>]*?\bname\s*=\s*["']([^"']+)["'][^>]*?\bvalue\s*=\s*["']([^"']*)["']""",
            RegexOption.IGNORE_CASE
        )
        private val CONSENT_INPUT_VALUE_NAME = Regex(
            """<input[^>]*?\bvalue\s*=\s*["']([^"']*)["'][^>]*?\bname\s*=\s*["']([^"']+)["']""",
            RegexOption.IGNORE_CASE
        )

        @Volatile
        var visitorData: String? = null

        @Volatile
        private var realSocsCookie: String? = null
        private val consentResolved = AtomicBoolean(false)
        private val consentLock = Any()
        private val consentCookies = HashMap<String, MutableList<Cookie>>()

        @Volatile
        private var realClientVersion: String? = null
        private val clientVersionResolved = AtomicBoolean(false)
        private val clientVersionLock = Any()

        private val consentClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .cookieJar(object : CookieJar {
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    synchronized(consentCookies) {
                        consentCookies[url.host] = cookies.toMutableList()
                    }
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    synchronized(consentCookies) {
                        return consentCookies[url.host] ?: emptyList()
                    }
                }
            })
            .build()

        fun fetchVisitorData(): Boolean {
            ensureYouTubeConsent()
            YoutubeParsingHelper.setConsentAccepted(true)
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

        private fun ensureYouTubeConsent() {
            if (consentResolved.get()) return
            synchronized(consentLock) {
                if (consentResolved.get()) return
                consentResolved.set(true)
                try {
                    if (testConsentWith("SOCS=CAI")) {
                        realSocsCookie = "CAI"
                        Log.d("FluxaDownloader", "consentimiento YouTube OK: SOCS=CAI")
                        return
                    }
                    val wallHtml = consentGet(CONSENT_PROBE_URL, null)
                    if (!isConsentWall(wallHtml)) {
                        Log.d("FluxaDownloader", "sin pared de consentimiento de YouTube")
                        return
                    }
                    val socs = runConsentSaveFlow(wallHtml)
                    if (!socs.isNullOrEmpty()) {
                        realSocsCookie = socs
                        Log.d("FluxaDownloader", "consentimiento YouTube OK via consent.youtube.com/save")
                    } else {
                        realSocsCookie = "CAI"
                        Log.d("FluxaDownloader", "consentimiento YouTube OK: fallback SOCS=CAI")
                    }
                } catch (e: Exception) {
                    realSocsCookie = "CAI"
                    Log.w("FluxaDownloader", "no se pudo resolver consentimiento; usando SOCS=CAI", e)
                }
            }
        }

        private fun testConsentWith(cookies: String): Boolean {
            val html = consentGet(CONSENT_PROBE_URL, cookies)
            return !isConsentWall(html)
        }

        private fun consentGet(url: String, cookies: String?): String {
            val builder = Request.Builder()
                .url(url)
                .header("User-Agent", FIREFOX_UA)
                .header("Accept-Language", "es-ES,es;q=0.9")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Upgrade-Insecure-Requests", "1")
            if (cookies != null) {
                builder.header("Cookie", cookies)
            }
            consentClient.newCall(builder.build()).execute().use { resp ->
                return resp.body.string()
            }
        }

        private fun isConsentWall(html: String): Boolean =
            html.contains("WIZ_global_data") && html.contains("_gd") && !html.contains("ytInitialData")

        private fun runConsentSaveFlow(consentHtml: String): String? {
            val action = CONSENT_FORM_ACTION.find(consentHtml)?.groupValues?.get(1) ?: return null
            val fields = extractConsentFormFields(consentHtml)
            fields["v"] = "y"
            val body = fields.entries.joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
            }
            val builder = Request.Builder()
                .url(action)
                .header("User-Agent", FIREFOX_UA)
                .header("Accept-Language", "es-ES,es;q=0.9")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Origin", "https://www.youtube.com")
                .header("Referer", CONSENT_PROBE_URL)
                .post(body.toRequestBody())
            val heldCookies = consentCookies["www.youtube.com"] ?: consentCookies["youtube.com"]
            if (!heldCookies.isNullOrEmpty()) {
                builder.header("Cookie", heldCookies.joinToString("; ") { "${it.name}=${it.value}" })
            }
            consentClient.newCall(builder.build()).execute().use { resp ->
                if (resp.code >= 400) {
                    Log.w("FluxaDownloader", "consent save devolvio ${resp.code}")
                }
                resp.body.close()
            }
            return findSocsCookie()
        }

        private fun extractConsentFormFields(html: String): LinkedHashMap<String, String> {
            val fields = LinkedHashMap<String, String>()
            val start = html.indexOf("<form")
            val end = if (start >= 0) html.indexOf("</form>", start) else -1
            val scope = if (start in 0..<end) html.substring(start, end) else html
            (CONSENT_INPUT_NAME_VALUE.findAll(scope).map { it.groupValues[1] to decodeEntities(it.groupValues[2]) } +
                CONSENT_INPUT_VALUE_NAME.findAll(scope).map { it.groupValues[2] to decodeEntities(it.groupValues[1]) })
                .forEach { (k, v) -> fields.putIfAbsent(k, v) }
            return fields
        }

        private fun decodeEntities(s: String): String =
            s.replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ")

        private fun findSocsCookie(): String? {
            synchronized(consentCookies) {
                consentCookies.values.forEach { list ->
                    list.firstOrNull { c ->
                        c.name.equals("SOCS", ignoreCase = true) &&
                            c.value.isNotBlank() && c.value != "CAE="
                    }?.let { return it.value }
                }
            }
            return null
        }

        private fun resolveClientVersion(): String? {
            if (clientVersionResolved.get()) return realClientVersion
            synchronized(clientVersionLock) {
                if (!clientVersionResolved.get()) {
                    realClientVersion = fetchClientVersionFromSwJsData()
                    clientVersionResolved.set(true)
                }
            }
            return realClientVersion
        }

        private fun fetchClientVersionFromSwJsData(): String? = try {
            val request = Request.Builder()
                .url(SWJS_VERSION_URL)
                .header("User-Agent", FIREFOX_UA)
                .header("Cookie", "SOCS=${realSocsCookie ?: "CAI"}")
                .build()
            consentClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    null
                } else {
                    var body = resp.body.string()
                    if (body.startsWith(")]}'")) body = body.substring(4)
                    JsonParser.parseString(body).asJsonArray
                        .get(0).asJsonArray
                        .get(2).asJsonArray
                        .get(0).asJsonArray
                        .get(0).asJsonArray
                        .get(16).asString
                }
            }
        } catch (e: Exception) {
            Log.w("FluxaDownloader", "no se pudo obtener clientVersion de sw.js_data", e)
            null
        }

        private fun alreadyHasClientVersion(body: String): Boolean =
            INJECT_VERSION_REGEX_1.containsMatchIn(body) || INJECT_VERSION_REGEX_2.containsMatchIn(body)

        private fun buildCookieHeader(headers: Map<String, List<String>>): String {
            val sock = realSocsCookie ?: return ""
            val parts = StringBuilder()
            var hasSocs = false
            headersLoop@ for ((name, values) in headers) {
                if (!name.equals("Cookie", ignoreCase = true)) continue@headersLoop
                for (value in values) {
                    for (tokenRaw in value.split(";")) {
                        val token = tokenRaw.trim()
                        if (token.startsWith("SOCS=", ignoreCase = true)) {
                            if (!hasSocs) {
                                if (parts.isNotEmpty()) parts.append("; ")
                                parts.append("SOCS=").append(sock)
                                hasSocs = true
                            }
                        } else if (token.isNotEmpty()) {
                            if (parts.isNotEmpty()) parts.append("; ")
                            parts.append(token)
                        }
                    }
                }
            }
            if (!hasSocs) {
                if (parts.isNotEmpty()) parts.append("; ")
                parts.append("SOCS=").append(sock)
            }
            return parts.toString()
        }

        private fun buildSyntheticResultsPage(): String {
            val version = resolveClientVersion() ?: FALLBACK_CLIENT_VERSION
            return "<!DOCTYPE html><html lang=\"es\"><head><script>window[\"ytInitialData\"]=" +
                "{\"responseContext\":{\"serviceTrackingParams\":[{\"service\":\"CSI\"," +
                "\"params\":[{\"key\":\"cver\",\"value\":\"$version\"}]}]}};" +
                "</script></head><body></body></html>"
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

        val isYoutube = url.contains("youtube.com") ||
            url.contains("youtubei.googleapis.com") ||
            url.contains("googlevideo.com") ||
            url.contains("consent.youtube.com")
        if (isYoutube) {
            ensureYouTubeConsent()
        }

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

        val cookieOverride = if (isYoutube) buildCookieHeader(headers) else null
        headers@ for (entry in headers) {
            if (cookieOverride != null && entry.key.equals("Cookie", ignoreCase = true)) continue@headers
            entry.value.forEach { value ->
                okHttpRequestBuilder.addHeader(entry.key, value)
            }
        }
        if (cookieOverride != null) {
            okHttpRequestBuilder.addHeader("Cookie", cookieOverride)
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
        var responseBody = okHttpResponse.body.use { it.string() }
        if (isYoutube && url.contains("/results") && url.contains("ucbcb") && isConsentWall(responseBody)) {
            Log.w("FluxaDownloader", "probe con pared de consentimiento; usando pagina sintetica con clientVersion")
            responseBody = buildSyntheticResultsPage()
        }
        if (isYoutube && url.startsWith(SWJS_REQUEST_URL) && !alreadyHasClientVersion(responseBody)) {
            val version = resolveClientVersion() ?: FALLBACK_CLIENT_VERSION
            responseBody += "\nINNERTUBE_CONTEXT_CLIENT_VERSION\":\"$version\";\n"
            Log.d("FluxaDownloader", "clientVersion inyectado en sw.js: $version")
        }
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
