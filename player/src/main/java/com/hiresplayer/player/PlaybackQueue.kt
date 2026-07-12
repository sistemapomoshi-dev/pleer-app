package com.hiresplayer.player

import com.hiresplayer.core.model.AudioTrack
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class PlaybackQueue @Inject constructor() {
    private val _tracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val tracks: StateFlow<List<AudioTrack>> = _tracks.asStateFlow()

    fun replace(track: AudioTrack) { _tracks.value = listOf(track) }
    fun sync(items: List<AudioTrack>) { _tracks.value = items }
    fun playNext(currentIndex: Int, track: AudioTrack): Int {
        val target = (currentIndex + 1).coerceIn(0, _tracks.value.size)
        _tracks.value = _tracks.value.toMutableList().apply { add(target, track) }
        return target
    }
    fun playLater(track: AudioTrack): Int {
        _tracks.value = _tracks.value + track
        return _tracks.value.lastIndex
    }
}
