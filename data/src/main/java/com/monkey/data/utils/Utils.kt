package com.monkey.data.utils

import android.graphics.Point

object Utils {

    fun Point.toPosition(): String = "${this.x}$POSITION_SEPARATOR${this.y}"
    const val POSITION_SEPARATOR = "x"
}