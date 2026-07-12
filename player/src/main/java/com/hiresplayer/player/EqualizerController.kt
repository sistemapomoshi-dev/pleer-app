package com.hiresplayer.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class EqualizerController @Inject constructor() {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private val _state = MutableStateFlow(EqualizerState(presets = defaultPresets(5)))

    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId == 0) return
        release()
        runCatching {
            val eq = Equalizer(0, audioSessionId)
            val boost = BassBoost(0, audioSessionId)
            equalizer = eq
            bassBoost = boost
            eq.enabled = _state.value.isEnabled
            boost.enabled = _state.value.isEnabled
            publishState(eq, boost)
        }
    }

    fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        _state.value = _state.value.copy(isEnabled = enabled)
    }

    fun setBandLevel(band: Short, level: Short) {
        val eq = equalizer ?: return
        val range = eq.bandLevelRange
        val clamped = level.coerceIn(range[0], range[1])
        eq.setBandLevel(band, clamped)
        publishState(eq, bassBoost)
    }

    fun setBassBoost(strength: Short) {
        val boost = bassBoost ?: return
        boost.setStrength(strength.coerceIn(0, 1000).toShort())
        publishState(equalizer, boost)
    }

    fun applyPreset(preset: EqualizerPreset) {
        val eq = equalizer ?: return
        preset.bandLevels.take(eq.numberOfBands.toInt()).forEachIndexed { index, level ->
            setBandLevel(index.toShort(), level)
        }
    }

    fun release() {
        equalizer?.release()
        bassBoost?.release()
        equalizer = null
        bassBoost = null
    }

    private fun publishState(eq: Equalizer?, boost: BassBoost?) {
        if (eq == null) return
        val bandLevels = (0 until eq.numberOfBands).map { band ->
            eq.getBandLevel(band.toShort())
        }
        _state.value = EqualizerState(
            isEnabled = eq.enabled,
            bandCount = eq.numberOfBands.toInt(),
            lowerLevel = eq.bandLevelRange[0],
            upperLevel = eq.bandLevelRange[1],
            bandLevels = bandLevels,
            bassBoostStrength = boost?.roundedStrength ?: 0,
            presets = defaultPresets(eq.numberOfBands.toInt())
        )
    }

    private fun defaultPresets(bands: Int): List<EqualizerPreset> {
        fun levels(vararg values: Short): List<Short> {
            val source = values.take(bands)
            return source + List((bands - source.size).coerceAtLeast(0)) { 0.toShort() }
        }
        return listOf(
            EqualizerPreset("Ровно", levels(0, 0, 0, 0, 0)),
            EqualizerPreset("Бас", levels(600, 350, 0, -150, -250)),
            EqualizerPreset("Вокал", levels(-150, 0, 350, 250, -100)),
            EqualizerPreset("Воздух", levels(-250, -100, 0, 300, 550))
        )
    }
}
