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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import su.sv.commonui.theme.ThemeConfig
import su.sv.commonui.theme.ThemeMode
import su.sv.commonui.theme.ThemeRepository
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences"
)

/**
 * Реализация [ThemeRepository] на основе одного DataStore.
 *
 * Единственный источник правды — DataStore. Для синхронного применения темы до первого
 * кадра приложение должно знать режим сразу, поэтому в [init] значение читается один раз
 * через `runBlocking` и кэшируется в памяти. Это осознанное исключение: однократное чтение
 * крошечного файла на старте, до создания первой Activity, чтобы не мигать системной темой.
 * Повторные чтения не нужны — после первого [setThemeMode] кэш обновляется синхронно.
 */
@Singleton
class ThemeRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ThemeRepository {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
    }

    companion object {
        @Volatile
        private var cachedThemeMode: ThemeMode? = null

        /**
         * Начальный режим темы для стартового значения StateFlow. Значение уже считано
         * синхронно в [ThemeRepositoryImpl.init] до создания ViewModel.
         */
        fun getInitialThemeMode(): ThemeMode = cachedThemeMode ?: ThemeMode.SYSTEM
    }

    init {
        if (cachedThemeMode == null) {
            cachedThemeMode = runCatching {
                runBlocking {
                    context.themeDataStore.data
                        .first()
                        .let { preferences ->
                            val raw = preferences[PreferencesKeys.THEME_MODE]
                                ?: ThemeMode.SYSTEM.name
                            runCatching { ThemeMode.valueOf(raw) }
                                .getOrDefault(ThemeMode.SYSTEM)
                        }
                }
            }.getOrDefault(ThemeMode.SYSTEM)
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
        context.themeDataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
        // Кэш обновляем синхронно — для следующего холодного старта значение уже известно.
        cachedThemeMode = mode
    }

    override suspend fun setUseDynamicColors(use: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_DYNAMIC_COLORS] = use
        }
    }
}
