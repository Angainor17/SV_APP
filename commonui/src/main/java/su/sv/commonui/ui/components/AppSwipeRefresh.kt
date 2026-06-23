package su.sv.commonui.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Компонент Pull-to-Refresh
 *
 * Обёртка над Material3 PullToRefreshBox с настройками темы.
 *
 * @param isRefreshing состояние обновления
 * @param onRefresh обработчик обновления
 * @param modifier модификатор
 * @param content содержимое
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSwipeRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val state = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Полноэкранный Pull-to-Refresh
 *
 * Заполняет весь доступный размер.
 *
 * @param isRefreshing состояние обновления
 * @param onRefresh обработчик обновления
 * @param modifier модификатор
 * @param content содержимое
 */
@Composable
fun AppSwipeRefreshFullScreen(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AppSwipeRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
        content = content
    )
}
