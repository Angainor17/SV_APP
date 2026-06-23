package su.sv.commonui.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import su.sv.commonui.R

/**
 * Полноэкранный индикатор загрузки
 */
@Composable
fun FullScreenLoading(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AppLoadingIndicator()
    }
}

/**
 * Полноэкранный экран ошибки с кнопкой повтора
 *
 * @param onRetry обработчик нажатия кнопки повтора
 * @param message сообщение об ошибке (опционально)
 * @param modifier модификатор
 */
@Composable
fun FullScreenError(
    onRetry: () -> Unit,
    message: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = message ?: stringResource(R.string.common_error_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        AppButton(
            text = stringResource(R.string.common_retry),
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp),
            style = ButtonStyle.OUTLINED
        )
    }
}

/**
 * Полноэкранный экран пустого состояния
 *
 * @param title заголовок
 * @param description описание (опционально)
 * @param icon иконка (опционально)
 * @param action действие (кнопка) (опционально)
 * @param modifier модификатор
 */
@Composable
fun FullScreenEmpty(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    action: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(
                    top = 8.dp,
                    start = 32.dp,
                    end = 32.dp
                )
            )
        }

        action?.invoke()
    }
}

/**
 * Стандартный экран "Нет результатов поиска"
 */
@Composable
fun FullScreenNoSearchResults(
    query: String? = null,
    modifier: Modifier = Modifier
) {
    val description = query?.let {
        "По запросу \"$it\" ничего не найдено"
    }

    FullScreenEmpty(
        title = "Ничего не найдено",
        description = description,
        icon = Icons.Rounded.Search,
        modifier = modifier
    )
}

/**
 * Стандартный экран "Нет данных"
 */
@Composable
fun FullScreenNoData(
    message: String = "Нет данных для отображения",
    modifier: Modifier = Modifier
) {
    FullScreenEmpty(
        title = message,
        icon = Icons.Rounded.Info,
        modifier = modifier
    )
}

/**
 * Inline-ошибка (для использования внутри контента)
 */
@Composable
fun InlineError(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        onRetry?.let {
            AppButton(
                text = stringResource(R.string.common_retry),
                onClick = it,
                modifier = Modifier.padding(top = 8.dp),
                style = ButtonStyle.TEXT
            )
        }
    }
}

/**
 * Inline-индикатор загрузки (для использования внутри контента)
 */
@Composable
fun InlineLoading(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AppLoadingIndicator()
    }
}
