package com.monkey.babyshield.di

import android.content.Context
import com.monkey.data.repository.BabyShieldDataSourceImpl
import com.monkey.data.repository.DefaultPreferenceValueImpl
import com.monkey.domain.repository.BabyShieldDataSource
import com.monkey.domain.repository.DefaultPreferenceValue
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {

    @Provides
    @Singleton
    fun provideDefaultPreferenceValue(): DefaultPreferenceValue = DefaultPreferenceValueImpl()

    @Provides
    @Singleton
    fun provideBabyShieldDataSource(
        @ApplicationContext context: Context,
        defaultPreferenceValue: DefaultPreferenceValue
    ): BabyShieldDataSource = BabyShieldDataSourceImpl(context, defaultPreferenceValue)
}