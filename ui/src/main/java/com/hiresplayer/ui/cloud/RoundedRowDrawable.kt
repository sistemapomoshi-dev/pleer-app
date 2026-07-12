package com.hiresplayer.ui.cloud

import android.graphics.Color
import android.graphics.drawable.GradientDrawable

internal class RoundedRowDrawable : GradientDrawable() {
    init {
        cornerRadius = 28f
        setColor(Color.WHITE)
    }
}
