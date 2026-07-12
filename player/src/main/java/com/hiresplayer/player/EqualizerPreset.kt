package com.hiresplayer.player

data class EqualizerPreset(
    val name: String,
    val bandLevels: List<Short>
)

data class EqualizerState(
    val isEnabled: Boolean = false,
    val bandCount: Int = 0,
    val lowerLevel: Short = -1500,
    val upperLevel: Short = 1500,
    val bandLevels: List<Short> = emptyList(),
    val preampDb: Float = 0f,
    val bassBoostStrength: Short = 0,
    val presets: List<EqualizerPreset> = emptyList()
)
