package com.hiresplayer.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.hiresplayer.core.model.AudioTrack
import com.hiresplayer.core.model.PlaybackState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
class Media3AudioPlayer @Inject constructor(
    @ApplicationContext context: Context,
    private val playbackQueue: PlaybackQueue
) : AudioPlayer {
    private val player = ExoPlayer.Builder(context).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _playbackState = MutableStateFlow(PlaybackState())
    private val _audioSessionId = MutableStateFlow(0)
    private val _playbackSpeed = MutableStateFlow(1f)
    private var currentTrack: AudioTrack? = null

    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    override val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()
    override val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    init {
        player.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    publishState()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    updateCurrentTrack()
                    publishState()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    updateCurrentTrack()
                    publishState()
                }

                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    _audioSessionId.value = audioSessionId
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    _playbackState.value = _playbackState.value.copy(errorMessage = error.localizedMessage)
                }
            }
        )

        scope.launch {
            while (isActive) {
                publishState()
                delay(500)
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun play(track: AudioTrack) {
        // Создаём MediaItem из локального Uri и передаём его в Media3 для воспроизведения.
        currentTrack = track
        val mediaItem = MediaItem.Builder()
            .setUri(track.uri)
            .setMediaId(track.id)
            .setTag(track)
            .build()
        player.setMediaItem(mediaItem)
        playbackQueue.replace(track)
        player.prepare()
        player.play()
        publishState()
    }

    override fun playNext(track: AudioTrack) {
        val index = playbackQueue.playNext(player.currentMediaItemIndex, track)
        player.addMediaItem(index, track.toMediaItem())
    }

    override fun playLater(track: AudioTrack) {
        playbackQueue.playLater(track)
        player.addMediaItem(track.toMediaItem())
    }

    override fun skipToNext() { if (player.hasNextMediaItem()) player.seekToNextMediaItem() }
    override fun skipToPrevious() { if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem() else player.seekTo(0) }
    override fun setPlaybackSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(.25f, 2f)
        player.playbackParameters = PlaybackParameters(safeSpeed)
        _playbackSpeed.value = safeSpeed
    }

    override fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        publishState()
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L)))
        publishState()
    }

    override fun seekBy(deltaMs: Long) {
        seekTo(player.currentPosition + deltaMs)
    }

    override fun stop() {
        player.stop()
        publishState()
    }

    override fun release() {
        player.release()
    }

    private fun publishState() {
        // Отдаём UI компактное состояние плеера через StateFlow без прямого доступа к ExoPlayer.
        _playbackState.value = PlaybackState(
            currentTrack = currentTrack,
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = when {
                player.duration > 0 -> player.duration
                currentTrack != null -> currentTrack?.durationMs ?: 0L
                else -> 0L
            },
            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L)
        )
    }

    private fun updateCurrentTrack() {
        currentTrack = player.currentMediaItem?.localConfiguration?.tag as? AudioTrack ?: currentTrack
    }

    private fun AudioTrack.toMediaItem() = MediaItem.Builder().setUri(uri).setMediaId(id).setTag(this).build()
}
