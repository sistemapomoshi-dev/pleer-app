package com.hiresplayer.core.database.entity

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = TrackEntity::class)
@Entity(tableName = "tracks_fts")
data class TrackFtsEntity(
    val title: String,
    val artist: String,
    val album: String
)
