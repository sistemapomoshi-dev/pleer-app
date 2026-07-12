package com.hiresplayer.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hiresplayer.core.database.dao.BookmarkDao
import com.hiresplayer.core.database.dao.PlaylistDao
import com.hiresplayer.core.database.dao.TrackDao
import com.hiresplayer.core.database.entity.BookmarkEntity
import com.hiresplayer.core.database.entity.PlaylistEntity
import com.hiresplayer.core.database.entity.PlaylistTrackEntity
import com.hiresplayer.core.database.entity.TrackEntity
import com.hiresplayer.core.database.entity.TrackFtsEntity

@Database(
    entities = [
        TrackEntity::class,
        TrackFtsEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        BookmarkEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun bookmarkDao(): BookmarkDao
}
