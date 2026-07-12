package com.hiresplayer.player

import android.os.Handler
import android.os.Looper
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

data class SleepTimerState(
    val mode: SleepTimerMode = SleepTimerMode.Off,
    val endsAtMs: Long? = null,
    val remainingMs: Long = 0L
)

sealed interface SleepTimerMode {
    data object Off : SleepTimerMode
    data class Duration(val durationMs: Long) : SleepTimerMode
    data object EndOfTrack : SleepTimerMode
}

@Singleton
class SleepTimerController @Inject constructor(
    private val audioPlayer: AudioPlayer
) {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(SleepTimerState())
    private var stopRunnable: Runnable? = null

    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    init {
        scope.launch {
            while (isActive) {
                val current = _state.value
                val remaining = current.endsAtMs?.let { (it - System.currentTimeMillis()).coerceAtLeast(0L) } ?: 0L
                _state.value = current.copy(remainingMs = remaining)
                delay(1_000)
            }
        }
        scope.launch {
            audioPlayer.playbackState.collect { playback ->
                if (_state.value.mode == SleepTimerMode.EndOfTrack &&
                    playback.durationMs > 0 &&
                    playback.positionMs >= playback.durationMs - 750
                ) {
                    audioPlayer.stop()
                    _state.value = SleepTimerState()
                }
            }
        }
    }

    fun start(durationMs: Long) {
        cancelScheduledStop()
        val endsAtMs = System.currentTimeMillis() + durationMs
        val runnable = Runnable {
            audioPlayer.stop()
            _state.value = SleepTimerState()
        }
        stopRunnable = runnable
        handler.postDelayed(runnable, durationMs)
        _state.value = SleepTimerState(
            mode = SleepTimerMode.Duration(durationMs),
            endsAtMs = endsAtMs,
            remainingMs = durationMs
        )
    }

    fun stopAtEndOfTrack() {
        cancelScheduledStop()
        _state.value = SleepTimerState(mode = SleepTimerMode.EndOfTrack)
    }

    fun cancel() {
        cancelScheduledStop()
        _state.value = SleepTimerState()
    }

    private fun cancelScheduledStop() {
        stopRunnable?.let(handler::removeCallbacks)
        stopRunnable = null
    }
}
