package su.sv.managers.theme

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import su.sv.commonui.theme.CustomThemeColors
import javax.inject.Inject
import javax.inject.Singleton

private val Context.customColorsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "custom_colors_preferences"
)

private const val TAG = "CustomColorsRepo"

/**
 * Реализация CustomColorsRepository на основе DataStore.
 *
 * Хранит кастомные цвета как JSON в DataStore Preferences.
 */
@Singleton
class CustomColorsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson = Gson()
) : CustomColorsRepository {

    private object PreferencesKeys {
        val CUSTOM_COLORS_LIGHT = stringPreferencesKey("custom_colors_light")
        val CUSTOM_COLORS_DARK = stringPreferencesKey("custom_colors_dark")
    }

    override fun getCustomColors(themeMode: String): Flow<CustomThemeColors?> {
        val key = when (themeMode) {
            "LIGHT" -> PreferencesKeys.CUSTOM_COLORS_LIGHT
            "DARK" -> PreferencesKeys.CUSTOM_COLORS_DARK
            else -> return kotlinx.coroutines.flow.flowOf(null)
        }

        return context.customColorsDataStore.data.map { preferences ->
            val jsonString = preferences[key]
            if (jsonString.isNullOrEmpty()) {
                null
            } else {
                try {
                    gson.fromJson(jsonString, CustomThemeColors::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Error decoding custom colors for $themeMode", e)
                    null
                }
            }
        }
    }

    override suspend fun saveCustomColors(colors: CustomThemeColors) {
        val key = when (colors.themeMode) {
            "LIGHT" -> PreferencesKeys.CUSTOM_COLORS_LIGHT
            "DARK" -> PreferencesKeys.CUSTOM_COLORS_DARK
            else -> {
                Log.w(TAG, "Unknown theme mode: ${colors.themeMode}")
                return
            }
        }

        val jsonString = gson.toJson(colors)
        Log.d(TAG, "Saving custom colors for ${colors.themeMode}: ${colors.colors.size} colors")

        context.customColorsDataStore.edit { preferences ->
            preferences[key] = jsonString
        }
    }

    override suspend fun clearCustomColors(themeMode: String) {
        val key = when (themeMode) {
            "LIGHT" -> PreferencesKeys.CUSTOM_COLORS_LIGHT
            "DARK" -> PreferencesKeys.CUSTOM_COLORS_DARK
            else -> {
                Log.w(TAG, "Unknown theme mode: $themeMode")
                return
            }
        }

        Log.d(TAG, "Clearing custom colors for $themeMode")

        context.customColorsDataStore.edit { preferences ->
            preferences.remove(key)
        }
    }

    override suspend fun clearAll() {
        Log.d(TAG, "Clearing all custom colors")

        context.customColorsDataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.CUSTOM_COLORS_LIGHT)
            preferences.remove(PreferencesKeys.CUSTOM_COLORS_DARK)
        }
    }
}