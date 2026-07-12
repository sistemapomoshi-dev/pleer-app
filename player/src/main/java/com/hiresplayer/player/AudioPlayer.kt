package com.hiresplayer.player

import com.hiresplayer.core.model.AudioTrack
import com.hiresplayer.core.model.PlaybackState
import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {
    val playbackState: StateFlow<PlaybackState>
    val audioSessionId: StateFlow<Int>
    val playbackSpeed: StateFlow<Float>

    fun play(track: AudioTrack)
    fun playNext(track: AudioTrack)
    fun playLater(track: AudioTrack)
    fun skipToNext()
    fun skipToPrevious()
    fun setPlaybackSpeed(speed: Float)
    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun seekBy(deltaMs: Long)
    fun stop()
    fun release()
}
