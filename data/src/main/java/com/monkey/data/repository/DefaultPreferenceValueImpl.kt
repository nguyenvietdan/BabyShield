package com.monkey.data.repository

import com.monkey.data.local.DefaultValue
import com.monkey.domain.repository.DefaultPreferenceValue

class DefaultPreferenceValueImpl: DefaultPreferenceValue {

    override val isLocked: Boolean
        get() = DefaultValue.IS_LOCKED
    override val edgeMargin: Int
        get() = DefaultValue.EDGE_MARGIN
    override val alphaValue: Int
        get() = DefaultValue.ALPHA
    override val iconSize: Int
        get() = DefaultValue.ICON_SIZE
}