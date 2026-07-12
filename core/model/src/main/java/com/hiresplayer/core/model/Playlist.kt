package com.hiresplayer.core.model

data class Playlist(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val totalDurationMs: Long = 0L,
    val createdAtMs: Long
)

data class PlaylistTrack(
    val playlistId: Long,
    val track: AudioTrack,
    val sortOrder: Int
)
