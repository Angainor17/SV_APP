package su.sv.commonui.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import su.sv.commonui.R
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.theme.ThemeMode

/**
 * Иконка переключения темы
 *
 * Отображает текущий режим темы и позволяет переключаться между ними.
 * Порядок переключения: Light -> Dark -> Light
 *
 * @param currentMode текущий режим темы
 * @param onToggle обработчик переключения (передаёт новый режим)
 * @param modifier модификатор
 */
@Composable
fun ThemeToggleIcon(
    currentMode: ThemeMode,
    onToggle: (ThemeMode) -> Unit,
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

    IconButton(
        onClick = { onToggle(currentMode.next()) },
        modifier = modifier
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
