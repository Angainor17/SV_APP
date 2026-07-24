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
 * Реализация ThemeRepository на основе DataStore и SharedPreferences
 *
 * SharedPreferences используется для синхронного чтения темы при запуске приложения.
 * Это необходимо для применения темы до создания Activity через AppCompatDelegate.
 */
@Singleton
class ThemeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ThemeRepository {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
    }

    companion object {
        private const val SHARED_PREFS_NAME = "theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode_sync"

        /**
         * Синхронное чтение режима темы из SharedPreferences
         * Используется при запуске приложения до создания Activity
         */
        fun getThemeModeSync(context: Context): ThemeMode {
            val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            val modeString = prefs.getString(KEY_THEME_MODE, null)
            return runCatching { ThemeMode.valueOf(modeString ?: ThemeMode.SYSTEM.name) }
                .getOrDefault(ThemeMode.SYSTEM)
        }

        /**
         * Синхронное сохранение режима темы в SharedPreferences
         */
        private fun saveThemeModeSync(context: Context, mode: ThemeMode) {
            val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        }
    }

    override val themeMode: Flow<ThemeMode> = context.themeDataStore.data
        .map { preferences ->
            val modeString = preferences[PreferencesKeys.THEME_MODE]
                ?: ThemeMode.SYSTEM.name
            runCatching { ThemeMode.valueOf(modeString) }
                .getOrDefault(ThemeMode.SYSTEM)
        }

    override val useDynamicColors: Flow<Boolean> = context.themeDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.USE_DYNAMIC_COLORS] ?: false
        }

    override val themeConfig: Flow<ThemeConfig> = context.themeDataStore.data
        .map { preferences ->
            val modeString = preferences[PreferencesKeys.THEME_MODE]
                ?: ThemeMode.SYSTEM.name
            val mode = runCatching { ThemeMode.valueOf(modeString) }
                .getOrDefault(ThemeMode.SYSTEM)
            val dynamicColors = preferences[PreferencesKeys.USE_DYNAMIC_COLORS] ?: false

            ThemeConfig(
                themeMode = mode,
                useDynamicColors = dynamicColors
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        // Сохраняем в DataStore (асинхронно для Flow)
        context.themeDataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
        // Сохраняем в SharedPreferences (синхронно для быстрого запуска)
        saveThemeModeSync(context, mode)
    }

    override suspend fun setUseDynamicColors(use: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_DYNAMIC_COLORS] = use
        }
    }
}
