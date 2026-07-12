package com.hiresplayer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hiresplayer.core.database.entity.BookmarkEntity
import com.hiresplayer.core.database.relation.BookmarkWithTrack
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query(
        """
        SELECT bookmarks.id AS id, bookmarks.trackId AS trackId, bookmarks.title AS title, bookmarks.positionMs AS positionMs,
               bookmarks.createdAtMs AS createdAtMs, tracks.title AS trackTitle
        FROM bookmarks
        INNER JOIN tracks ON tracks.id = bookmarks.trackId
        ORDER BY bookmarks.createdAtMs DESC
        """
    )
    fun observeBookmarks(): Flow<List<BookmarkWithTrack>>

    @Insert
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmark(bookmarkId: Long)
}
