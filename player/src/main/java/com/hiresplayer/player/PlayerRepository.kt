package com.hiresplayer.player

import com.hiresplayer.core.model.AudioTrack
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepository @Inject constructor(
    private val audioPlayer: AudioPlayer,
    private val seekIntervalController: SeekIntervalController,
    private val equalizerController: EqualizerManager,
    private val sleepTimerController: SleepTimerController
) {
    val playbackState = audioPlayer.playbackState
    val seekIntervalMs = seekIntervalController.intervalMs
    val equalizerState = equalizerController.state
    val sleepTimerState = sleepTimerController.state
    val audioSessionId = audioPlayer.audioSessionId
    val playbackSpeed = audioPlayer.playbackSpeed

    fun play(track: AudioTrack) = audioPlayer.play(track)
    fun playNext(track: AudioTrack) = audioPlayer.playNext(track)
    fun playLater(track: AudioTrack) = audioPlayer.playLater(track)
    fun skipToNext() = audioPlayer.skipToNext()
    fun skipToPrevious() = audioPlayer.skipToPrevious()
    fun setPlaybackSpeed(speed: Float) = audioPlayer.setPlaybackSpeed(speed)

    fun togglePlayPause() = audioPlayer.togglePlayPause()

    fun seekTo(positionMs: Long) = audioPlayer.seekTo(positionMs)

    fun seekForward() = audioPlayer.seekBy(seekIntervalController.forwardDeltaMs())

    fun seekBackward() = audioPlayer.seekBy(seekIntervalController.backwardDeltaMs())

    fun setSeekIntervalMs(value: Long) = seekIntervalController.setIntervalMs(value)

    fun attachEqualizer(audioSessionId: Int) = equalizerController.attachToAudioSession(audioSessionId)

    fun setEqualizerEnabled(enabled: Boolean) = equalizerController.setEnabled(enabled)

    fun setEqualizerBandLevel(band: Short, level: Short) = equalizerController.setBandLevel(band.toInt(), level)

    fun setBassBoost(strength: Short) = equalizerController.setPreamp(strength / 100f)

    fun applyPreset(preset: EqualizerPreset) = preset.bandLevels.forEachIndexed { i, level -> equalizerController.setBandLevel(i, level) }

    fun startSleepTimer(durationMs: Long) = sleepTimerController.start(durationMs)

    fun stopAtEndOfTrack() = sleepTimerController.stopAtEndOfTrack()

    fun cancelSleepTimer() = sleepTimerController.cancel()
}
