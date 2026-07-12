package com.hiresplayer.library

object BookmarkRules {
    fun normalizePosition(positionMs: Long, durationMs: Long? = null): Long {
        val lowerBounded = positionMs.coerceAtLeast(0L)
        return durationMs?.takeIf { it > 0 }?.let { lowerBounded.coerceAtMost(it) } ?: lowerBounded
    }

    fun isUsefulBookmark(positionMs: Long): Boolean =
        normalizePosition(positionMs) >= MIN_BOOKMARK_POSITION_MS

    private const val MIN_BOOKMARK_POSITION_MS = 1_000L
}
