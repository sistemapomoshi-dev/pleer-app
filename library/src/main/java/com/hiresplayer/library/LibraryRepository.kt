package com.hiresplayer.library

import com.hiresplayer.core.database.dao.BookmarkDao
import com.hiresplayer.core.database.dao.PlaylistDao
import com.hiresplayer.core.database.dao.TrackDao
import com.hiresplayer.core.database.entity.BookmarkEntity
import com.hiresplayer.core.database.entity.PlaylistEntity
import com.hiresplayer.core.database.toEntity
import com.hiresplayer.core.database.toModel
import com.hiresplayer.core.model.AudioTrack
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class LibraryRepository @Inject constructor(
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
    private val bookmarkDao: BookmarkDao
) {
    val tracks: Flow<List<AudioTrack>> = trackDao.observeTracks().map { items ->
        items.map { it.toModel() }
    }

    val playlists = playlistDao.observePlaylists().map { items ->
        items.map { it.toModel() }
    }

    val bookmarks = bookmarkDao.observeBookmarks().map { items ->
        items.map { it.toModel() }
    }

    suspend fun importTracks(tracks: List<AudioTrack>) {
        // Локальный сканер остаётся источником, а Room становится главным кэшем библиотеки.
        trackDao.upsertTracks(tracks.map { it.toEntity() })
        playlistDao.ensureMySongsPlaylist()
    }

    suspend fun ensureMySongsPlaylist() = playlistDao.ensureMySongsPlaylist()

    suspend fun updateTrack(track: AudioTrack) = trackDao.upsertTracks(listOf(track.toEntity()))
    suspend fun deleteTrack(trackId: String) = trackDao.deleteTrack(trackId)

    fun searchTracks(query: String): Flow<List<AudioTrack>> {
        val safeQuery = query.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { "$it*" }
        return if (safeQuery.isBlank()) {
            tracks
        } else {
            trackDao.searchTracks(safeQuery).map { items -> items.map { it.toModel() } }
        }
    }

    suspend fun createPlaylist(name: String): Long =
        playlistDao.insertPlaylist(
            PlaylistEntity(
                name = name.trim().ifBlank { "Новый плейлист" },
                createdAtMs = System.currentTimeMillis()
            )
        )

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: String) {
        playlistDao.addTrackToPlaylist(playlistId, trackId)
    }

    suspend fun deletePlaylist(playlistId: Long) {
        if (playlistDao.findPlaylistIdByName("Мои песни") == playlistId) return
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addBookmark(trackId: String, title: String, positionMs: Long) {
        val normalizedPosition = BookmarkRules.normalizePosition(positionMs)
        bookmarkDao.insertBookmark(
            BookmarkEntity(
                trackId = trackId,
                title = title.trim().ifBlank { "Закладка" },
                positionMs = normalizedPosition,
                createdAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteBookmark(bookmarkId: Long) {
        bookmarkDao.deleteBookmark(bookmarkId)
    }
}
