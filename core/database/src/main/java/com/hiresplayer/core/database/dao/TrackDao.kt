package com.hiresplayer.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hiresplayer.core.database.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE")
    fun observeTracks(): Flow<List<TrackEntity>>

    @Query(
        """
        SELECT tracks.* FROM tracks
        JOIN tracks_fts ON tracks_fts.rowid = tracks.rowid
        WHERE tracks_fts MATCH :query
        ORDER BY tracks.title COLLATE NOCASE
        """
    )
    fun searchTracks(query: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :trackId LIMIT 1")
    suspend fun findById(trackId: String): TrackEntity?

    @Upsert
    suspend fun upsertTracks(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrack(trackId: String)

    @Query("DELETE FROM tracks WHERE source = 'yandex_disk' AND id LIKE :pathPrefix || '%' AND id NOT IN (:actualIds)")
    suspend fun deleteMissingCloudTracksUnder(pathPrefix: String, actualIds: List<String>)

    @Query("DELETE FROM tracks WHERE source = 'yandex_disk' AND id LIKE :pathPrefix || '%'")
    suspend fun deleteCloudTracksUnder(pathPrefix: String)
}
