package su.sv.managers.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import su.sv.commonui.theme.ThemeConfig
import su.sv.commonui.theme.ThemeMode
import su.sv.commonui.theme.ThemeRepository
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences"
)

/**
 * Реализация ThemeRepository на основе DataStore
 */
@Singleton
class ThemeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ThemeRepository {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
    }

    override val themeMode: Flow<ThemeMode> = context.themeDataStore.data
        .map { preferences ->
            val modeString = preferences[PreferencesKeys.THEME_MODE]
                ?: ThemeMode.LIGHT.name
            runCatching { ThemeMode.valueOf(modeString) }
                .getOrDefault(ThemeMode.LIGHT)
        }

    override val useDynamicColors: Flow<Boolean> = context.themeDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.USE_DYNAMIC_COLORS] ?: false
        }

    override val themeConfig: Flow<ThemeConfig> = context.themeDataStore.data
        .map { preferences ->
            val modeString = preferences[PreferencesKeys.THEME_MODE]
                ?: ThemeMode.LIGHT.name
            val mode = runCatching { ThemeMode.valueOf(modeString) }
                .getOrDefault(ThemeMode.LIGHT)
            val dynamicColors = preferences[PreferencesKeys.USE_DYNAMIC_COLORS] ?: false

            ThemeConfig(
                themeMode = mode,
                useDynamicColors = dynamicColors
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    override suspend fun setUseDynamicColors(use: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_DYNAMIC_COLORS] = use
        }
    }
}
