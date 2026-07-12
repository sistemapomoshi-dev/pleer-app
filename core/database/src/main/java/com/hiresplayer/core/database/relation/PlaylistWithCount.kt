package com.hiresplayer.core.database.relation

data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val createdAtMs: Long,
    val trackCount: Int,
    val totalDurationMs: Long
)
