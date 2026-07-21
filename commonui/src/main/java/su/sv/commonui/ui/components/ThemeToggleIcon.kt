package su.sv.commonui.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import su.sv.commonui.R
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.theme.ThemeMode

/**
 * Иконка переключения темы
 *
 * Отображает текущий режим темы и позволяет переключаться между ними.
 * Порядок переключения: Light -> Dark -> Light
 *
 * При долгом нажатии (>5 сек) открывает редактор темы (для отладки).
 *
 * @param currentMode текущий режим темы
 * @param onToggle обработчик переключения (передаёт новый режим)
 * @param onLongPress обработчик долгого нажатия (открывает редактор темы)
 * @param modifier модификатор
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThemeToggleIcon(
    currentMode: ThemeMode,
    onToggle: (ThemeMode) -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dimensions = LocalAppDimensions.current

    val iconRes = when (currentMode) {
        ThemeMode.LIGHT -> R.drawable.ic_theme_dark   // При светлой теме показываем луну (переключит на тёмную)
        ThemeMode.DARK -> R.drawable.ic_theme_light   // При тёмной теме показываем солнце (переключит на светлую)
    }

    val contentDescription = when (currentMode) {
        ThemeMode.LIGHT -> stringResource(R.string.theme_mode_dark)   // Переключить на тёмную
        ThemeMode.DARK -> stringResource(R.string.theme_mode_light)   // Переключить на светлую
    }

    // Отслеживание долгого нажатия
    val interactionSource = remember { MutableInteractionSource() }
    var isPressed by remember { mutableStateOf(false) }
    var hasTriggeredLongPress by remember { mutableStateOf(false) }

    // Обработка долгого нажатия через корутину
    LaunchedEffect(isPressed) {
        if (isPressed && !hasTriggeredLongPress) {
            delay(5000) // 5 секунд
            onLongPress()
            hasTriggeredLongPress = true
        }
    }

    // Сброс флага при отпускании
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isPressed = true
                    hasTriggeredLongPress = false
                }
                is PressInteraction.Release -> {
                    isPressed = false
                }
                is PressInteraction.Cancel -> {
                    isPressed = false
                    hasTriggeredLongPress = false
                }
            }
        }
    }

    Box(
        modifier = modifier
            .size(48.dp) // Стандартный размер IconButton
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 24.dp),
                onClick = {
                    if (!hasTriggeredLongPress) {
                        onToggle(currentMode.next())
                    }
                },
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(iconRes),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(dimensions.iconSizeToolbar)
        )
    }
}

/**
 * Иконка режима темы (без клика)
 * Показывает иконку следующего режима при переключении
 *
 * @param mode режим темы
 * @param modifier модификатор
 */
@Composable
fun ThemeModeIcon(
    mode: ThemeMode,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalAppDimensions.current

    // Показываем иконку противоположного режима
    val iconRes = when (mode) {
        ThemeMode.LIGHT -> R.drawable.ic_theme_dark   // При светлой теме показываем луну
        ThemeMode.DARK -> R.drawable.ic_theme_light   // При тёмной теме показываем солнце
    }

    val contentDescription = when (mode) {
        ThemeMode.LIGHT -> stringResource(R.string.theme_mode_dark)
        ThemeMode.DARK -> stringResource(R.string.theme_mode_light)
    }

    Icon(
        imageVector = ImageVector.vectorResource(iconRes),
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.size(dimensions.iconSizeToolbar)
    )
}
