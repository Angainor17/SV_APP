package su.sv.books.catalog.presentation.downloaded.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import su.sv.books.catalog.presentation.downloaded.model.UiDownloadedBook

/**
 * Список скачанных книг с поддержкой свайпа для удаления
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadedBooksList(
    books: List<UiDownloadedBook>,
    onReadClick: (UiDownloadedBook) -> Unit,
    onDeleteRequest: (UiDownloadedBook) -> Unit,
    showSwipeHint: Boolean = false,
    onSwipeHintShown: () -> Unit = {},
    resetKey: Any? = null,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var hintAnimationPlayed by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    // Максимальное смещение свайпа - 30% от ширины экрана (примерно 120dp)
    val maxSwipeDistance = with(density) { 120.dp.toPx() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = books,
            key = { it.id }
        ) { book ->
            val updatedOnDeleteRequest by rememberUpdatedState(onDeleteRequest)
            val updatedOnSwipeHintShown by rememberUpdatedState(onSwipeHintShown)

            var currentOffset by remember { mutableFloatStateOf(0f) }

            val dismissState = rememberSwipeToDismissBoxState(
                initialValue = SwipeToDismissBoxValue.Settled,
                confirmValueChange = { dismissValue ->
                    if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                        updatedOnDeleteRequest(book)
                        true
                    } else {
                        false
                    }
                },
                positionalThreshold = { totalDistance ->
                    // Свайп срабатывает при смещении более 30%
                    totalDistance * 0.3f
                },
            )

            // Сбрасываем состояние при изменении resetKey
            LaunchedEffect(resetKey) {
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }

            // Анимация подсказки для первой книги
            val isFirstItem = books.indexOf(book) == 0
            val shouldShowHint = showSwipeHint && isFirstItem && !hintAnimationPlayed

            if (shouldShowHint) {
                LaunchedEffect(Unit) {
                    // Показываем небольшое смещение влево (фон немного выглядывает)
                    dismissState.snapTo(SwipeToDismissBoxValue.EndToStart)
                    kotlinx.coroutines.delay(400)
                    // Возвращаем обратно
                    dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                    kotlinx.coroutines.delay(200)
                    // Повторяем для наглядности
                    dismissState.snapTo(SwipeToDismissBoxValue.EndToStart)
                    kotlinx.coroutines.delay(400)
                    dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                    hintAnimationPlayed = true
                    updatedOnSwipeHintShown()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                // Фон с иконкой удаления (бледно-красный)
                DeleteSwipeBackground(
                    modifier = Modifier.fillMaxSize()
                )

                // Карточка книги поверх фона
                SwipeToDismissBox(
                    state = dismissState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = true,
                    backgroundContent = {
                        // Пустой фон, так как реальный фон под карточкой
                        Box(modifier = Modifier.fillMaxSize())
                    },
                    content = {
                        DownloadedBookItem(
                            book = book,
                            onReadClick = { onReadClick(book) },
                        )
                    }
                )
            }
        }
    }
}
