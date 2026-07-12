package com.hiresplayer.core.database.relation

data class PlaylistTrackWithTrack(
    val playlistId: Long,
    val trackId: String,
    val sortOrder: Int,
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val artworkUri: String?,
    val source: String,
    val updatedAtMs: Long
)
