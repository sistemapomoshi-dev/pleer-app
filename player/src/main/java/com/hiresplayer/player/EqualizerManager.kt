package com.hiresplayer.player

import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Ten-band UI adapter over the frequency bands exposed by Android's audio effect. */
@Singleton
class EqualizerManager @Inject constructor() {
    companion object { val FREQUENCIES = intArrayOf(32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000) }
    private var effect: Equalizer? = null
    private var preamp: LoudnessEnhancer? = null
    private var levels = MutableList(10) { 0.toShort() }
    private val _state = MutableStateFlow(EqualizerState(bandCount = 10, lowerLevel = -1200, upperLevel = 1200, bandLevels = levels))
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun attachToAudioSession(id: Int) {
        if (id == 0) return
        release()
        runCatching {
            effect = Equalizer(0, id)
            preamp = LoudnessEnhancer(id)
            effect?.enabled = _state.value.isEnabled
            preamp?.enabled = _state.value.isEnabled
            levels.forEachIndexed(::setBandLevel)
            setPreamp(_state.value.preampDb)
        }
    }

    fun setEnabled(value: Boolean) {
        effect?.enabled = value; preamp?.enabled = value
        _state.value = _state.value.copy(isEnabled = value)
    }

    fun setBandLevel(band: Int, level: Short) {
        if (band !in levels.indices) return
        val value = level.coerceIn((-1200).toShort(), 1200.toShort())
        levels[band] = value
        effect?.let { eq ->
            val physical = eq.getBand((FREQUENCIES[band] * 1000).toInt())
            val range = eq.bandLevelRange
            eq.setBandLevel(physical, value.coerceIn(range[0], range[1]))
        }
        _state.value = _state.value.copy(bandLevels = levels.toList())
    }

    fun setPreamp(db: Float) {
        val value = db.coerceIn(-12f, 12f)
        // LoudnessEnhancer only boosts; negative preamp remains represented in state.
        preamp?.setTargetGain((value.coerceAtLeast(0f) * 100).toInt())
        _state.value = _state.value.copy(preampDb = value)
    }

    fun release() { effect?.release(); preamp?.release(); effect = null; preamp = null }
}
