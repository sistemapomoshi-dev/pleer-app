package com.hiresplayer.core.model

sealed interface PlayerCommand {
    data class PlayTrack(val track: AudioTrack) : PlayerCommand
    data object PlayPause : PlayerCommand
    data class SeekTo(val positionMs: Long) : PlayerCommand
    data object SeekForward : PlayerCommand
    data object SeekBackward : PlayerCommand
    data object Stop : PlayerCommand
}
