package com.monkey.babyshield.di

import com.monkey.data.repository.BabyShieldDataSourceImpl
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BabyShieldManagerEntryPoint {
    fun getBabyShieldDataSource(): BabyShieldDataSourceImpl
}