package com.hiresplayer.cloud

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.hiresplayer.cloud.yandex.YandexDiskProvider
import com.hiresplayer.cloud.yandex.YandexOAuthManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
class CloudRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val yandexDiskProvider: YandexDiskProvider,
    private val yandexOAuthManager: YandexOAuthManager,
    private val cloudSyncManager: CloudSyncManager
) {
    val authState: StateFlow<CloudAuthState> = yandexOAuthManager.authState

    fun buildAuthUrl(): String = yandexOAuthManager.buildAuthUrl()

    suspend fun listYandex(path: String): List<CloudFile> = yandexDiskProvider.list(path)

    suspend fun getYandexDownloadUrl(path: String): String = yandexDiskProvider.getDownloadUrl(path)
    suspend fun deleteYandex(path: String) = yandexDiskProvider.delete(path)

    suspend fun syncFolder(path: String): Int = cloudSyncManager.syncFolder(path)

    fun enqueueDownload(file: CloudFile) {
        val request = OneTimeWorkRequestBuilder<CloudDownloadWorker>()
            .setInputData(
                workDataOf(
                    CloudDownloadWorker.KEY_PROVIDER to yandexDiskProvider.id,
                    CloudDownloadWorker.KEY_PATH to file.path,
                    CloudDownloadWorker.KEY_NAME to file.name
                )
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "cloud-download-${file.path}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    suspend fun signOut() {
        yandexDiskProvider.signOut()
    }
}
