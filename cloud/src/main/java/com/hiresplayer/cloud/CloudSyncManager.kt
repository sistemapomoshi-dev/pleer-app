package com.hiresplayer.cloud

import com.hiresplayer.core.database.dao.TrackDao
import com.hiresplayer.core.database.dao.PlaylistDao
import com.hiresplayer.core.database.entity.TrackEntity
import com.hiresplayer.cloud.yandex.YandexDiskProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncManager @Inject constructor(
    private val yandexDiskProvider: YandexDiskProvider,
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao
) {
    suspend fun syncFolder(path: String): Int {
        val rootPath = path.ifBlank { "disk:/" }
        val tracks = collectAudioTracks(rootPath)
        trackDao.upsertTracks(tracks)

        val sourceIds = tracks.map { it.id }
        val pathPrefix = rootPath.takeIf { it == "disk:/" } ?: "$rootPath/"
        if (sourceIds.isEmpty()) {
            trackDao.deleteCloudTracksUnder(pathPrefix)
        } else {
            trackDao.deleteMissingCloudTracksUnder(pathPrefix, sourceIds)
        }
        playlistDao.ensureMySongsPlaylist()
        return tracks.size
    }

    // Рекурсивно обходим выбранную папку и превращаем аудиофайлы Яндекс.Диска в записи библиотеки.
    private suspend fun collectAudioTracks(path: String): List<TrackEntity> {
        val files = yandexDiskProvider.list(path)
        return buildList {
            files.forEach { file ->
                when {
                    file.type == CloudFileType.Directory -> addAll(collectAudioTracks(file.path))
                    file.type == CloudFileType.File && file.isSupportedAudio() -> {
                        add(file.toTrackEntity(yandexDiskProvider.getDownloadUrl(file.path)))
                    }
                }
            }
        }
    }

    private fun CloudFile.toTrackEntity(downloadUrl: String): TrackEntity =
        TrackEntity(
            id = path,
            title = name.substringBeforeLast("."),
            artist = yandexDiskProvider.title,
            album = parentFolderName(path),
            durationMs = 0L,
            uri = downloadUrl,
            artworkUri = null,
            source = yandexDiskProvider.id,
            updatedAtMs = System.currentTimeMillis()
        )

    private fun parentFolderName(path: String): String =
        path.removePrefix("disk:/")
            .substringBeforeLast("/", missingDelimiterValue = "Облако")
            .substringAfterLast("/")
            .ifBlank { "Облако" }

    companion object {
        private val supportedAudioExtensions = setOf("flac", "mp3", "wav", "aac", "m4a", "ogg", "opus", "alac", "aiff", "dsf", "dff")

        fun CloudFile.isSupportedAudio(): Boolean =
            name.substringAfterLast(".", missingDelimiterValue = "")
                .lowercase() in supportedAudioExtensions
    }
}
