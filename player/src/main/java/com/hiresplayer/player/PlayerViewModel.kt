package com.hiresplayer.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiresplayer.core.model.AudioTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerRepository: PlayerRepository
) : ViewModel() {
    val playbackState = playerRepository.playbackState
    val seekIntervalMs = playerRepository.seekIntervalMs
    val equalizerState = playerRepository.equalizerState
    val sleepTimerState = playerRepository.sleepTimerState
    val playbackSpeed = playerRepository.playbackSpeed

    init {
        viewModelScope.launch {
            playerRepository.audioSessionId
                .filter { it != 0 }
                .collect { playerRepository.attachEqualizer(it) }
        }
    }

    fun play(track: AudioTrack) = playerRepository.play(track)
    fun playNext(track: AudioTrack) = playerRepository.playNext(track)
    fun playLater(track: AudioTrack) = playerRepository.playLater(track)
    fun skipToNext() = playerRepository.skipToNext()
    fun skipToPrevious() = playerRepository.skipToPrevious()
    fun setPlaybackSpeed(speed: Float) = playerRepository.setPlaybackSpeed(speed)

    fun togglePlayPause() = playerRepository.togglePlayPause()

    fun seekTo(positionMs: Long) = playerRepository.seekTo(positionMs)

    fun seekForward() = playerRepository.seekForward()

    fun seekBackward() = playerRepository.seekBackward()

    fun setSeekIntervalMs(value: Long) = playerRepository.setSeekIntervalMs(value)

    fun setEqualizerEnabled(enabled: Boolean) = playerRepository.setEqualizerEnabled(enabled)

    fun setEqualizerBandLevel(band: Short, level: Short) = playerRepository.setEqualizerBandLevel(band, level)

    fun setBassBoost(strength: Short) = playerRepository.setBassBoost(strength)

    fun applyPreset(preset: EqualizerPreset) = playerRepository.applyPreset(preset)

    fun startSleepTimer(durationMs: Long) = playerRepository.startSleepTimer(durationMs)

    fun stopAtEndOfTrack() = playerRepository.stopAtEndOfTrack()

    fun cancelSleepTimer() = playerRepository.cancelSleepTimer()
}
