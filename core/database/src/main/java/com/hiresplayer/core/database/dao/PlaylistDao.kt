package com.hiresplayer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.hiresplayer.core.database.entity.PlaylistEntity
import com.hiresplayer.core.database.entity.PlaylistTrackEntity
import com.hiresplayer.core.database.relation.PlaylistTrackWithTrack
import com.hiresplayer.core.database.relation.PlaylistWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query(
        """
        SELECT playlists.id, playlists.name, playlists.createdAtMs, COUNT(playlist_tracks.trackId) AS trackCount,
               COALESCE(SUM(tracks.durationMs), 0) AS totalDurationMs
        FROM playlists
        LEFT JOIN playlist_tracks ON playlists.id = playlist_tracks.playlistId
        LEFT JOIN tracks ON tracks.id = playlist_tracks.trackId
        GROUP BY playlists.id
        ORDER BY playlists.createdAtMs DESC
        """
    )
    fun observePlaylists(): Flow<List<PlaylistWithCount>>

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("SELECT id FROM playlists WHERE name = :name ORDER BY id LIMIT 1")
    suspend fun findPlaylistIdByName(name: String): Long?

    @Query("INSERT OR IGNORE INTO playlist_tracks (playlistId, trackId, sortOrder) SELECT :playlistId, id, rowid FROM tracks")
    suspend fun addAllTracks(playlistId: Long)

    @Transaction
    suspend fun ensureMySongsPlaylist(): Long {
        val id = findPlaylistIdByName("Мои песни") ?: insertPlaylist(PlaylistEntity(name = "Мои песни", createdAtMs = 0L))
        addAllTracks(id)
        return id
    }

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun nextSortOrder(playlistId: Long): Int

    @Upsert
    suspend fun upsertPlaylistTrack(entity: PlaylistTrackEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrack(playlistId: Long, trackId: String)

    @Query("UPDATE playlist_tracks SET sortOrder = :sortOrder WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun updateTrackOrder(playlistId: Long, trackId: String, sortOrder: Int)

    @Transaction
    @Query(
        """
        SELECT playlist_tracks.playlistId, playlist_tracks.trackId, playlist_tracks.sortOrder,
               tracks.id AS id, tracks.title AS title, tracks.artist AS artist, tracks.album AS album,
               tracks.durationMs AS durationMs, tracks.uri AS uri, tracks.artworkUri AS artworkUri,
               tracks.source AS source, tracks.updatedAtMs AS updatedAtMs
        FROM playlist_tracks
        INNER JOIN tracks ON tracks.id = playlist_tracks.trackId
        WHERE playlist_tracks.playlistId = :playlistId
        ORDER BY playlist_tracks.sortOrder
        """
    )
    fun observePlaylistTracks(playlistId: Long): Flow<List<PlaylistTrackWithTrack>>

    @Transaction
    suspend fun addTrackToPlaylist(playlistId: Long, trackId: String) {
        upsertPlaylistTrack(
            PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = trackId,
                sortOrder = nextSortOrder(playlistId)
            )
        )
    }
}
