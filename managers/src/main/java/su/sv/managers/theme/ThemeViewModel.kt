package su.sv.managers.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import su.sv.commonui.theme.ThemeConfig
import su.sv.commonui.theme.ThemeMode
import su.sv.commonui.theme.ThemeRepository
import javax.inject.Inject

/**
 * ViewModel для управления темой приложения
 *
 * Используется на верхнем уровне приложения для предоставления
 * состояния темы всем дочерним экранам.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    /**
     * Текущая конфигурация темы
     */
    val themeConfig: StateFlow<ThemeConfig> = themeRepository.themeConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeConfig.Default
        )

    /**
     * Текущий режим темы
     */
    val themeMode: StateFlow<ThemeMode> = themeRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.LIGHT
        )

    /**
     * Использование динамических цветов
     */
    val useDynamicColors: StateFlow<Boolean> = themeRepository.useDynamicColors
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Переключить режим темы
     */
    fun toggleTheme() {
        viewModelScope.launch {
            val currentMode = themeConfig.value.themeMode
            themeRepository.setThemeMode(currentMode.next())
        }
    }

    /**
     * Установить режим темы
     */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeRepository.setThemeMode(mode)
        }
    }

    /**
     * Установить использование динамических цветов
     */
    fun setUseDynamicColors(use: Boolean) {
        viewModelScope.launch {
            themeRepository.setUseDynamicColors(use)
        }
    }
}
