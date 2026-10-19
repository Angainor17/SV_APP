package su.sv.news.presentation.debug

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import su.sv.commonui.theme.CustomColorAttribute
import su.sv.commonui.theme.CustomThemeColors
import su.sv.managers.theme.CustomColorsRepository
import timber.log.Timber
import javax.inject.Inject

private const val TAG = "ThemeEditorVM"

/**
 * Состояние экрана редактирования темы.
 */
data class ThemeEditorState(
    val themeMode: String = "LIGHT",
    val customColors: CustomThemeColors = CustomThemeColors.emptyLight(),
    val isLoading: Boolean = true,
)

/**
 * ViewModel для экрана редактирования темы.
 */
@HiltViewModel
class ThemeEditorViewModel @Inject constructor(
    private val customColorsRepository: CustomColorsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ThemeEditorState())
    val state: StateFlow<ThemeEditorState> = _state.asStateFlow()

    // Callback для перезапуска Activity
    var onRestartApp: (() -> Unit)? = null

    init {
        loadCustomColors()
    }

    /**
     * Загрузить кастомные цвета для указанного режима темы.
     */
    fun loadCustomColors() {
        viewModelScope.launch {
            val themeMode = _state.value.themeMode
            Timber.tag(TAG).d("Loading custom colors for $themeMode")

            _state.update { it.copy(isLoading = true) }

            val colors = customColorsRepository.getCustomColors(themeMode).first()
            Timber.tag(TAG).d("Loaded colors: ${colors?.colors?.size ?: 0}")

            _state.update {
                it.copy(
                    customColors = colors ?: CustomThemeColors(themeMode = themeMode),
                    isLoading = false
                )
            }
        }
    }

    /**
     * Переключить режим темы.
     */
    fun setThemeMode(mode: String) {
        if (_state.value.themeMode == mode) return

        Timber.tag(TAG).d("Switching theme mode to $mode")

        _state.update { it.copy(themeMode = mode) }
        loadCustomColors()
    }

    /**
     * Установить цвет для атрибута.
     */
    fun setColor(attribute: CustomColorAttribute, color: Color) {
        val currentColors = _state.value.customColors
        val updatedColors = currentColors.setColor(attribute.attributeName, color)

        Timber.tag(TAG).d("Setting color for ${attribute.attributeName}: ${color.value}")

        _state.update { it.copy(customColors = updatedColors) }
    }

    /**
     * Удалить кастомный цвет для атрибута (вернуть к исходному).
     */
    fun removeColor(attribute: CustomColorAttribute) {
        val currentColors = _state.value.customColors
        val updatedColors = currentColors.removeColor(attribute.attributeName)

        Timber.tag(TAG).d("Removing custom color for ${attribute.attributeName}")

        _state.update { it.copy(customColors = updatedColors) }
    }

    /**
     * Применить изменения - сохранить и перезапустить приложение.
     */
    fun applyChanges() {
        viewModelScope.launch {
            val themeMode = _state.value.themeMode
            val colors = _state.value.customColors

            Timber.tag(TAG).d("Applying changes for $themeMode: ${colors.colors.size} colors")

            customColorsRepository.saveCustomColors(colors)

            // Перезапуск приложения
            onRestartApp?.invoke()
        }
    }

    /**
     * Сбросить все кастомные цвета для текущего режима темы.
     */
    fun resetToDefault() {
        viewModelScope.launch {
            val themeMode = _state.value.themeMode

            Timber.tag(TAG).d("Resetting colors for $themeMode")

            customColorsRepository.clearCustomColors(themeMode)

            _state.update {
                it.copy(customColors = CustomThemeColors(themeMode = themeMode))
            }
        }
    }
}