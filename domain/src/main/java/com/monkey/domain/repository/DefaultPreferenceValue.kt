package com.monkey.domain.repository

import android.graphics.Point

interface DefaultPreferenceValue {

    val isLocked: Boolean
    val edgeMargin: Int
    val alphaValue: Int
    val iconSize: Int
    val iconColor: Int
    val position: Point
}