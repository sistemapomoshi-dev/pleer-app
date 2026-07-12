package com.hiresplayer.player

object SeekIntervalPolicy {
    val allowedIntervalsMs = listOf(5_000L, 10_000L, 15_000L, 30_000L)
    const val defaultIntervalMs = 10_000L

    fun normalize(value: Long): Long =
        value.takeIf { it in allowedIntervalsMs } ?: defaultIntervalMs

    fun forwardDelta(value: Long): Long = normalize(value)

    fun backwardDelta(value: Long): Long = -normalize(value)
}
