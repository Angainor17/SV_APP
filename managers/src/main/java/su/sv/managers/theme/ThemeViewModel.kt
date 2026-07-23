package su.sv.managers.theme

import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import su.sv.commonarchitecture.di.module.DispatcherProvider
import su.sv.commonui.theme.ThemeConfig
import su.sv.commonui.theme.ThemeMode
import su.sv.commonui.theme.ThemeRepository
import javax.inject.Inject

/**
 * ViewModel для управления темой приложения
 *
 * Используется на верхнем уровне приложения для предоставления
 * состояния темы всем дочерним экранам.
 *
 * Все IO операции (DataStore) выполняются на IO dispatcher.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
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
            initialValue = ThemeMode.SYSTEM
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
            // DataStore - IO операция
            withContext(dispatcherProvider.io) {
                val currentConfig = themeConfig.value
                val currentIsDark = currentConfig.themeMode.isDarkTheme(
                    isSystemDark = isSystemDark()
                )
                val newMode = currentConfig.themeMode.next(currentIsDark)
                themeRepository.setThemeMode(newMode)
            }
        }
    }

    /**
     * Определить, является ли системная тема тёмной
     */
    private fun isSystemDark(): Boolean {
        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return uiMode == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Установить режим темы
     */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            // DataStore - IO операция
            withContext(dispatcherProvider.io) {
                themeRepository.setThemeMode(mode)
            }
        }
    }

    /**
     * Установить использование динамических цветов
     */
    fun setUseDynamicColors(use: Boolean) {
        viewModelScope.launch {
            // DataStore - IO операция
            withContext(dispatcherProvider.io) {
                themeRepository.setUseDynamicColors(use)
            }
        }
    }
}
