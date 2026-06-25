package su.sv.commonui.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import su.sv.commonui.R
import su.sv.commonui.theme.ThemeMode

/**
 * Стандартный тулбар приложения
 *
 * @param title заголовок
 * @param modifier модификатор
 * @param navigationIcon иконка навигации (кнопка назад по умолчанию)
 * @param onNavigationClick обработчик клика на навигационную иконку
 * @param scrollBehavior поведение скролла тулбара
 * @param actions дополнительные действия в правой части тулбара
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppToolbar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    onNavigationClick: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier.fillMaxWidth(),
        navigationIcon = {
            if (navigationIcon != null) {
                navigationIcon()
            } else if (onNavigationClick != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_navigate_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        scrollBehavior = scrollBehavior,
        windowInsets = WindowInsets.statusBars
    )
}

/**
 * Тулбар с кнопкой назад
 *
 * @param title заголовок
 * @param onBackClick обработчик клика на кнопку назад
 * @param modifier модификатор
 * @param actions дополнительные действия
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppToolbarWithBack(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    AppToolbar(
        title = title,
        modifier = modifier,
        onNavigationClick = onBackClick,
        actions = actions
    )
}

/**
 * Тулбар с переключателем темы
 *
 * @param title заголовок
 * @param currentThemeMode текущий режим темы
 * @param onThemeToggle обработчик переключения темы
 * @param modifier модификатор
 * @param navigationIcon иконка навигации
 * @param onNavigationClick обработчик клика на навигационную иконку
 * @param additionalActions дополнительные действия перед переключателем темы
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppToolbarWithThemeToggle(
    title: String,
    currentThemeMode: ThemeMode,
    onThemeToggle: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    onNavigationClick: (() -> Unit)? = null,
    additionalActions: @Composable RowScope.() -> Unit = {}
) {
    AppToolbar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        onNavigationClick = onNavigationClick
    ) {
        additionalActions()
        ThemeToggleIcon(
            currentMode = currentThemeMode,
            onToggle = onThemeToggle
        )
    }
}

/**
 * Простой тулбар без действий
 *
 * @param title заголовок
 * @param modifier модификатор
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppToolbarSimple(
    title: String,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier.fillMaxWidth(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        windowInsets = WindowInsets.statusBars
    )
}
