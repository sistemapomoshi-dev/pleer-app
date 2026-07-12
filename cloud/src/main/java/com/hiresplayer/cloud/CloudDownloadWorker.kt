package com.hiresplayer.cloud

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.hiresplayer.cloud.yandex.YandexDiskProvider
import com.hiresplayer.cloud.yandex.YandexOAuthManager
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CloudDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    private val yandexDiskProvider = YandexDiskProvider(YandexOAuthManager(context))

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val path = inputData.getString(KEY_PATH) ?: return@withContext Result.failure()
        val name = inputData.getString(KEY_NAME) ?: path.substringAfterLast("/")
        runCatching {
            val downloadUrl = yandexDiskProvider.getDownloadUrl(path)
            val connection = URL(downloadUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            val targetDir = File(applicationContext.filesDir, "offline-cache").apply { mkdirs() }
            val targetFile = File(targetDir, name.sanitizeFileName())
            connection.inputStream.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Result.success(Data.Builder().putString(KEY_LOCAL_PATH, targetFile.absolutePath).build())
        }.getOrElse {
            Result.failure(Data.Builder().putString(KEY_ERROR, it.localizedMessage).build())
        }
    }

    private fun String.sanitizeFileName(): String =
        replace(Regex("[\\\\/:*?\"<>|]"), "_")

    companion object {
        const val KEY_PROVIDER = "provider"
        const val KEY_PATH = "path"
        const val KEY_NAME = "name"
        const val KEY_LOCAL_PATH = "local_path"
        const val KEY_ERROR = "error"
    }
}
