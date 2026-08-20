/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.util

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class GitHubAsset(
    val name: String,
    val size: Long,
    val downloadUrl: String,
    val downloadCount: Int
)

data class GitHubReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val publishedAt: String,
    val apkAsset: GitHubAsset?,
    val isUpdateAvailable: Boolean,
    val currentVersion: String,
    val latestVersion: String
)

object GitHubUpdateChecker {

    private const val GITHUB_API_LATEST_RELEASE = "https://api.github.com/repos/agupta07505/AttendSmartly/releases/latest"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdates(currentVersion: String): Result<GitHubReleaseInfo?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_API_LATEST_RELEASE)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "AttendSmartly-Android/$currentVersion")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("GitHub API error: HTTP ${response.code}"))
                }

                val bodyString = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response body from GitHub API"))

                val json = JsonParser.parseString(bodyString).asJsonObject

                val tagName = json.get("tag_name")?.asString ?: ""
                val name = json.get("name")?.asString ?: tagName
                val body = json.get("body")?.asString ?: ""
                val htmlUrl = json.get("html_url")?.asString ?: "https://github.com/agupta07505/AttendSmartly/releases"
                val publishedAt = json.get("published_at")?.asString ?: ""

                var apkAsset: GitHubAsset? = null
                val assetsArray = json.getAsJsonArray("assets")
                if (assetsArray != null) {
                    for (element in assetsArray) {
                        val assetObj = element.asJsonObject
                        val assetName = assetObj.get("name")?.asString ?: ""
                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                            val downloadUrl = assetObj.get("browser_download_url")?.asString ?: ""
                            val size = assetObj.get("size")?.asLong ?: 0L
                            val downloadCount = assetObj.get("download_count")?.asInt ?: 0
                            apkAsset = GitHubAsset(
                                name = assetName,
                                size = size,
                                downloadUrl = downloadUrl,
                                downloadCount = downloadCount
                            )
                            break
                        }
                    }
                }

                val cleanLatest = tagName.removePrefix("v").trim()
                val cleanCurrent = currentVersion.removePrefix("v").trim()

                val isNewer = isVersionNewer(remote = cleanLatest, current = cleanCurrent)

                val info = GitHubReleaseInfo(
                    tagName = tagName,
                    name = name,
                    body = body,
                    htmlUrl = htmlUrl,
                    publishedAt = publishedAt,
                    apkAsset = apkAsset,
                    isUpdateAvailable = isNewer,
                    currentVersion = cleanCurrent,
                    latestVersion = cleanLatest
                )

                Result.success(info)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Compares semantic versions like 1.2 vs 1.1.0 or 1.2.1 vs 1.2
     * Returns true if remote is strictly greater than current
     */
    fun isVersionNewer(remote: String, current: String): Boolean {
        if (remote.isBlank() || current.isBlank()) return false
        if (remote.equals(current, ignoreCase = true)) return false

        val remoteParts = remote.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
