package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AngleUnit
import com.example.domain.model.AppSettings
import com.example.domain.model.DecimalSeparator
import com.example.domain.model.FractionDisplayPreference
import com.example.domain.model.GroupingStyle
import com.example.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "calc_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DECIMAL_PRECISION = intPreferencesKey("decimal_precision")
        val DECIMAL_SEPARATOR = stringPreferencesKey("decimal_separator")
        val GROUPING_STYLE = stringPreferencesKey("grouping_style")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val FRACTION_DISPLAY = stringPreferencesKey("fraction_display")
        val ANGLE_UNIT = stringPreferencesKey("angle_unit")
        val CURRENCY_FROM = stringPreferencesKey("currency_from")
        val CURRENCY_TO = stringPreferencesKey("currency_to")
        val EXTRA_CURRENCIES = stringSetPreferencesKey("extra_currencies")
        val FAVORITE_PHYSICS_IDS = stringSetPreferencesKey("favorite_physics_ids")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.DARK,
            decimalPrecision = (prefs[Keys.DECIMAL_PRECISION] ?: 8).coerceIn(2, 12),
            decimalSeparator = prefs[Keys.DECIMAL_SEPARATOR]?.let {
                runCatching { DecimalSeparator.valueOf(it) }.getOrNull()
            } ?: DecimalSeparator.COMMA,
            groupingStyle = prefs[Keys.GROUPING_STYLE]?.let {
                runCatching { GroupingStyle.valueOf(it) }.getOrNull()
            } ?: GroupingStyle.SPACE,
            hapticEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: false,
            fractionDisplayPreference = prefs[Keys.FRACTION_DISPLAY]?.let {
                runCatching { FractionDisplayPreference.valueOf(it) }.getOrNull()
            } ?: FractionDisplayPreference.MIXED_FIRST
        )
    }

    val angleUnitFlow: Flow<AngleUnit> = context.dataStore.data.map { prefs ->
        prefs[Keys.ANGLE_UNIT]?.let { runCatching { AngleUnit.valueOf(it) }.getOrNull() }
            ?: AngleUnit.DEG
    }

    val currencySelectionFlow: Flow<Triple<String, String, Set<String>>> = context.dataStore.data.map { prefs ->
        Triple(
            prefs[Keys.CURRENCY_FROM] ?: "USD",
            prefs[Keys.CURRENCY_TO] ?: "UAH",
            prefs[Keys.EXTRA_CURRENCIES] ?: emptySet()
        )
    }

    val favoritePhysicsIdsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.FAVORITE_PHYSICS_IDS] ?: emptySet()
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDecimalPrecision(precision: Int) {
        context.dataStore.edit { it[Keys.DECIMAL_PRECISION] = precision.coerceIn(2, 12) }
    }

    suspend fun setDecimalSeparator(separator: DecimalSeparator) {
        context.dataStore.edit { it[Keys.DECIMAL_SEPARATOR] = separator.name }
    }

    suspend fun setGroupingStyle(style: GroupingStyle) {
        context.dataStore.edit { it[Keys.GROUPING_STYLE] = style.name }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_ENABLED] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setFractionDisplayPreference(pref: FractionDisplayPreference) {
        context.dataStore.edit { it[Keys.FRACTION_DISPLAY] = pref.name }
    }

    suspend fun setAngleUnit(unit: AngleUnit) {
        context.dataStore.edit { it[Keys.ANGLE_UNIT] = unit.name }
    }

    suspend fun setCurrencyPair(from: String, to: String) {
        context.dataStore.edit {
            it[Keys.CURRENCY_FROM] = from
            it[Keys.CURRENCY_TO] = to
        }
    }

    suspend fun addExtraCurrency(code: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.EXTRA_CURRENCIES] ?: emptySet()
            prefs[Keys.EXTRA_CURRENCIES] = current + code.uppercase()
        }
    }

    suspend fun toggleFavoritePhysics(id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_PHYSICS_IDS] ?: emptySet()
            prefs[Keys.FAVORITE_PHYSICS_IDS] = if (id in current) current - id else current + id
        }
    }
}
