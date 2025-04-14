package com.monkey.data.repository

import com.monkey.data.local.DefaultValue
import com.monkey.domain.repository.DefaultPreferenceValue

class DefaultPreferenceValueImpl: DefaultPreferenceValue {

    override val isLocked: Boolean
        get() = DefaultValue.IS_LOCKED
}