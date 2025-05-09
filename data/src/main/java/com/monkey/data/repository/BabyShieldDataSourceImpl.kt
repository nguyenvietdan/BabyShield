package com.monkey.data.repository

import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.monkey.domain.repository.BabyShieldDataSource
import com.monkey.domain.repository.BabyShieldDataSource.Companion.DATA_NAME
import com.monkey.domain.repository.BabyShieldDataSource.Companion.KEY_ALPHA
import com.monkey.domain.repository.BabyShieldDataSource.Companion.KEY_EDGE_MARGIN
import com.monkey.domain.repository.BabyShieldDataSource.Companion.KEY_ICON_COLOR
import com.monkey.domain.repository.BabyShieldDataSource.Companion.KEY_ICON_SIZE
import com.monkey.domain.repository.BabyShieldDataSource.Companion.KEY_IS_LOCKED
import com.monkey.domain.repository.BabyShieldDataSource.Companion.KEY_POSITION_Y
import com.monkey.domain.repository.DefaultPreferenceValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class BabyShieldDataSourceImpl @Inject constructor(
    private val context: Context,
    private val defaultValue: DefaultPreferenceValue
) : BabyShieldDataSource {

    private val TAG = "BabyShieldDataSourceImpl"

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATA_NAME)

    private val _isLocked = IS_LOCKED.createFlow(defaultValue.isLocked)
    override val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _edgeMargin = EDGE_MARGIN.createFlow(defaultValue.edgeMargin)
    override val edgeMargin: StateFlow<Int> = _edgeMargin.asStateFlow()

    private val _alpha = ALPHA.createFlow(defaultValue.alphaValue)
    override val alpha: StateFlow<Int> = _alpha.asStateFlow()

    private val _iconSize = ICON_SIZE.createFlow(defaultValue.iconSize)
    override val iconSize: StateFlow<Int> = _iconSize.asStateFlow()

    private val _iconColor = ICON_COLOR.createFlow(defaultValue.iconColor.toLong())
    override val iconColor: StateFlow<Long> = _iconColor.asStateFlow()

    private val _positionY = POSITION_Y.createFlow(defaultValue.position.y)
    override val positionY: StateFlow<Int> = _positionY.asStateFlow()

    override suspend fun save(key: String, value: Any) {
        context.dataStore.edit { preferences ->
            Log.i(TAG, "save: preference $key, $value")
            when (key) {
                KEY_IS_LOCKED -> {
                    preferences[IS_LOCKED] = value as Boolean
                    _isLocked.value = value
                }

                KEY_EDGE_MARGIN -> {
                    preferences[EDGE_MARGIN] = value as Int
                    _edgeMargin.value = value
                }

                KEY_ALPHA -> {
                    preferences[ALPHA] = value as Int
                    _alpha.value = value
                }

                KEY_ICON_SIZE -> {
                    preferences[ICON_SIZE] = value as Int
                    _iconSize.value = value
                }

                KEY_ICON_COLOR -> {
                    preferences[ICON_COLOR] = value as Long
                    _iconColor.value = value
                }

                KEY_POSITION_Y -> {
                    preferences[POSITION_Y] = value as Int
                    _positionY.value = value
                }

                else -> Log.i(TAG, "save: not support key $key")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <T, reified R> Preferences.Key<T>.default(default: R): R = runBlocking {
        val value = context.dataStore.data.catch { exception ->
            if (exception is CorruptionException || exception is IOException) {
                Log.w(TAG, "Data store exception $exception")
                context.filesDir.listFiles()?.find {
                    it.name == "datastore"
                }?.let { dataStoreFolder ->
                    dataStoreFolder.listFiles()?.find { it.name.contains(DATA_NAME) }?.let {
                        Log.w(TAG, "data store file delete ${it.name}")
                        it.delete()
                    }
                }
                emit(emptyPreferences())
            } else throw exception
        }.map {
            Log.i(TAG, "[default] preference load: ${this@default} = ${it[this@default]}, $default")
            if (it[this@default] == null) {
                context.dataStore.edit { preferences ->
                    preferences[this@default] = default as T
                }
            }
            return@map it[this@default] ?: default
        }.first()
        if (value is R) value else default
    }

    private inline fun <reified T> Preferences.Key<T>.createFlow(default: T): MutableStateFlow<T> {
        return MutableStateFlow(this.default(default))
    }

    companion object {
        private val IS_LOCKED = booleanPreferencesKey(KEY_IS_LOCKED)
        private val EDGE_MARGIN = intPreferencesKey(KEY_EDGE_MARGIN)
        private val ALPHA = intPreferencesKey(KEY_ALPHA)
        private val ICON_SIZE = intPreferencesKey(KEY_ICON_SIZE)
        private val ICON_COLOR = longPreferencesKey(KEY_ICON_COLOR)
        private val POSITION_Y = intPreferencesKey(KEY_POSITION_Y)
    }
}