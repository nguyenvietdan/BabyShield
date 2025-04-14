package com.monkey.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface BabyShieldDataSource {

    val isLocked: StateFlow<Boolean>

    suspend fun save(key: String, value: Unit)

    companion object {
        const val DATA_NAME = "com.monkey.babyshield.data.prefs"
        const val KEY_IS_LOCKED = "is_locked"
    }
}