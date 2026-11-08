package com.arborecence.core.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arborecence.core.domain.model.AccessibilitySettings
import com.arborecence.core.domain.model.HighlightColor
import com.arborecence.core.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.ds by preferencesDataStore("settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(@ApplicationContext private val context: Context) : SettingsRepository {
    private val reduceMotion = booleanPreferencesKey("reduce_motion")
    private val reduceTransparency = booleanPreferencesKey("reduce_transparency")
    private val fontScale = floatPreferencesKey("font_scale")
    private val highContrast = booleanPreferencesKey("high_contrast")
    override fun accessibility(): Flow<AccessibilitySettings> = context.ds.data.map {
        AccessibilitySettings(it[reduceMotion] ?: false, it[reduceTransparency] ?: false, it[fontScale] ?: 1f, it[highContrast] ?: false)
    }
    override suspend fun setAccessibility(s: AccessibilitySettings) { context.ds.edit {
        it[reduceMotion] = s.reduceMotion; it[reduceTransparency] = s.reduceTransparency; it[fontScale] = s.fontScale; it[highContrast] = s.highContrast
    } }
    private fun meaningKey(c: HighlightColor) = stringPreferencesKey("meaning_${c.key}")
    override fun highlightMeaning(color: HighlightColor): Flow<String> = context.ds.data.map { it[meaningKey(color)] ?: color.defaultMeaning }
    override suspend fun setHighlightMeaning(color: HighlightColor, meaning: String) { context.ds.edit { it[meaningKey(color)] = meaning } }
}
