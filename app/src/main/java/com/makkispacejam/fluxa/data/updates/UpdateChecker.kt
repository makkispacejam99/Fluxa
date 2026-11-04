package com.makkispacejam.fluxa.data.updates

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val version: String,
    val tag: String,
    val url: String
)

sealed interface UpdateCheckResult {
    data class Available(val info: UpdateInfo) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data object Failed : UpdateCheckResult
}

object UpdateChecker {
    private const val RELEASES_URL = "https://api.github.com/repos/makkispacejam99/Fluxa/releases/latest"
    private const val FALLBACK_URL = "https://github.com/makkispacejam99/Fluxa/releases/latest"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    suspend fun check(context: Context): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(RELEASES_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Fluxa-Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching UpdateCheckResult.Failed
                val body = response.body.string()
                val json = JSONObject(body)
                val tag = json.optString("tag_name").takeIf { it.isNotBlank() } ?: return@runCatching UpdateCheckResult.Failed
                val url = json.optString("html_url").takeIf { it.isNotBlank() } ?: FALLBACK_URL
                val remote = normalizeVersion(tag)
                val installed = normalizeVersion(installedVersion(context))
                if (remote.isEmpty() || installed.isEmpty()) return@runCatching UpdateCheckResult.Failed
                if (compareVersions(remote, installed) > 0) {
                    UpdateCheckResult.Available(UpdateInfo(remote, tag, url))
                } else {
                    UpdateCheckResult.UpToDate
                }
            }
        }.getOrDefault(UpdateCheckResult.Failed)
    }

    private fun installedVersion(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (_: Exception) {
        ""
    }

    private fun normalizeVersion(raw: String): String {
        val cleaned = raw.trim().removePrefix("v").removePrefix("V")
        return Regex("""\d+(?:\.\d+)*""").find(cleaned)?.value ?: ""
    }

    private fun compareVersions(remote: String, installed: String): Int {
        val remoteParts = remote.split('.').map { it.toIntOrNull() ?: 0 }
        val installedParts = installed.split('.').map { it.toIntOrNull() ?: 0 }
        val size = maxOf(remoteParts.size, installedParts.size)
        for (index in 0 until size) {
            val remoteValue = remoteParts.getOrElse(index) { 0 }
            val installedValue = installedParts.getOrElse(index) { 0 }
            if (remoteValue != installedValue) return remoteValue.compareTo(installedValue)
        }
        return 0
    }
}
