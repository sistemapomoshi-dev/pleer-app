package com.hiresplayer.player

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class SeekIntervalController @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences("player_settings", Context.MODE_PRIVATE)
    private val _intervalMs = MutableStateFlow(
        SeekIntervalPolicy.normalize(preferences.getLong(KEY_SEEK_INTERVAL_MS, SeekIntervalPolicy.defaultIntervalMs))
    )

    val intervalMs: StateFlow<Long> = _intervalMs.asStateFlow()

    fun setIntervalMs(value: Long) {
        val normalized = SeekIntervalPolicy.normalize(value)
        preferences.edit().putLong(KEY_SEEK_INTERVAL_MS, normalized).apply()
        _intervalMs.value = normalized
    }

    fun forwardDeltaMs(): Long = SeekIntervalPolicy.forwardDelta(intervalMs.value)

    fun backwardDeltaMs(): Long = SeekIntervalPolicy.backwardDelta(intervalMs.value)

    private companion object {
        const val KEY_SEEK_INTERVAL_MS = "seek_interval_ms"
    }
}
