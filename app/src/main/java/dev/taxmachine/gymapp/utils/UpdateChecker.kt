package dev.taxmachine.gymapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import dev.taxmachine.gymapp.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val GITHUB_API_BASE = "https://api.github.com/repos/TaxMachine/GymApp"
    private const val LATEST_RELEASE_URL = "$GITHUB_API_BASE/releases/latest"
    private const val TAGS_URL = "$GITHUB_API_BASE/tags"
    private const val CHANNEL_ID = "app_updates"
    private const val NOTIFICATION_ID = 1001
    private const val TAG = "UpdateChecker"

    data class UpdateInfo(
        val latestTagName: String,
        val latestCommitHash: String,
        val htmlUrl: String,
        val isUpdateAvailable: Boolean
    )

    fun checkForUpdates(context: Context, onResult: ((UpdateInfo?) -> Unit)? = null) {
        val currentCommitHash = BuildConfig.GIT_COMMIT_HASH
        Logger.i(TAG, "Current Commit Hash: $currentCommitHash")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Get the latest release to find the tag name
                val releaseResponse = fetchUrl(LATEST_RELEASE_URL)
                if (releaseResponse == null) {
                    onResult?.invoke(null)
                    return@launch
                }
                val releaseJson = JSONObject(releaseResponse)
                val latestTagName = releaseJson.getString("tag_name")
                val htmlUrl = releaseJson.getString("html_url")

                // 2. Get the tags to find the commit SHA for that tag
                val tagsResponse = fetchUrl(TAGS_URL)
                if (tagsResponse == null) {
                    onResult?.invoke(null)
                    return@launch
                }
                val tagsArray = JSONArray(tagsResponse)
                var latestCommitHash: String? = null

                for (i in 0 until tagsArray.length()) {
                    val tagObj = tagsArray.getJSONObject(i)
                    if (tagObj.getString("name") == latestTagName) {
                        latestCommitHash = tagObj.getJSONObject("commit").getString("sha")
                        break
                    }
                }

                if (latestCommitHash == null) {
                    Logger.w(TAG, "Could not find commit hash for tag: $latestTagName")
                    onResult?.invoke(null)
                    return@launch
                }

                Logger.i(TAG, "Latest Release Commit Hash: $latestCommitHash")

                // 3. Compare hashes
                val isUpdateAvailable = !(latestCommitHash.startsWith(currentCommitHash) || currentCommitHash.startsWith(latestCommitHash))
                
                if (isUpdateAvailable && onResult == null) {
                    showUpdateNotification(context, latestTagName, htmlUrl)
                }

                onResult?.invoke(UpdateInfo(latestTagName, latestCommitHash, htmlUrl, isUpdateAvailable))

            } catch (e: Exception) {
                Logger.e(TAG, "Error checking for updates", e)
                onResult?.invoke(null)
            }
        }
    }

    private fun fetchUrl(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                Logger.w(TAG, "API returned ${connection.responseCode} for $urlString")
                null
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to fetch $urlString", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun showUpdateNotification(context: Context, version: String, url: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "App Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for new app versions"
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("New Version Available")
            .setContentText("A new release ($version) is available on GitHub.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        Logger.i(TAG, "Update notification shown for version $version")
    }
}
