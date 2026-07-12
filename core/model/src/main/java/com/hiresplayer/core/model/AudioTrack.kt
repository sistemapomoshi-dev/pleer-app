package com.hiresplayer.core.model

import android.net.Uri

data class AudioTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri,
    val artworkUri: Uri? = null,
    val source: TrackSource = TrackSource.Local
)

enum class TrackSource {
    Local,
    Cloud
}
