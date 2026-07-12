package com.hiresplayer.core.database.relation

data class BookmarkWithTrack(
    val id: Long,
    val trackId: String,
    val trackTitle: String,
    val title: String,
    val positionMs: Long,
    val createdAtMs: Long
)
