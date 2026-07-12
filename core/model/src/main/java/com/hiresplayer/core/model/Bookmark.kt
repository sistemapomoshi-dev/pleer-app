package com.hiresplayer.core.model

data class Bookmark(
    val id: Long,
    val trackId: String,
    val trackTitle: String,
    val title: String,
    val positionMs: Long,
    val createdAtMs: Long
)
