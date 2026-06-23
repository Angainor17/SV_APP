package su.sv.books.catalog.presentation.downloaded.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import su.sv.books.catalog.presentation.downloaded.model.UiDownloadedBook
import kotlin.math.roundToInt

/**
 * Список скачанных книг с поддержкой свайпа для удаления.
 *
 * Особенности реализации:
 * - Максимальное смещение свайпа ограничено 30% ширины экрана
 * - Красный фон отображается на всю ширину карточки
 * - После отмены удаления свайп продолжает работать
 * - Подсказка свайпа для первого элемента
 */
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
            val isFirstItem = books.indexOf(book) == 0
            val shouldShowHint = showSwipeHint && isFirstItem && !hintAnimationPlayed

            SwipeableBookItem(
                book = book,
                onReadClick = { onReadClick(book) },
                onDeleteRequest = { onDeleteRequest(book) },
                showHint = shouldShowHint,
                onHintShown = {
                    hintAnimationPlayed = true
                    onSwipeHintShown()
                },
                resetKey = resetKey,
            )
        }
    }
}

/**
 * Элемент книги с поддержкой свайпа
 */
@Composable
private fun SwipeableBookItem(
    book: UiDownloadedBook,
    onReadClick: () -> Unit,
    onDeleteRequest: () -> Unit,
    showHint: Boolean,
    onHintShown: () -> Unit,
    resetKey: Any?,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Максимальное смещение - 30% от ширины экрана
    val maxSwipeDistance = with(density) {
        (configuration.screenWidthDp.dp.toPx() * 0.3f)
    }

    // Порог срабатывания - 25% от ширины (должен быть меньше максимального)
    val triggerDistance = with(density) {
        (configuration.screenWidthDp.dp.toPx() * 0.25f)
    }

    // Состояние свайпа - сбрасывается при изменении resetKey или book.id
    var offsetX by remember(book.id, resetKey) { mutableFloatStateOf(0f) }
    var isDismissed by remember(book.id, resetKey) { mutableStateOf(false) }

    // Анимация подсказки для первого элемента
    if (showHint) {
        LaunchedEffect(Unit) {
            delay(300)
            // Показываем небольшое смещение влево дважды
            repeat(2) {
                offsetX = -triggerDistance * 0.8f
                delay(400)
                offsetX = 0f
                delay(200)
            }
            onHintShown()
        }
    }

    // Анимация возвращения или dismissal
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isDismissed) -maxSwipeDistance else offsetX,
        animationSpec = SpringSpec(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "swipe_animation"
    )

    // Обработка dismissal - вызываем onDeleteRequest после анимации
    LaunchedEffect(isDismissed) {
        if (isDismissed) {
            delay(200) // Небольшая задержка для анимации
            onDeleteRequest()
            // Сбрасываем состояние после вызова onDeleteRequest
            // (диалог покажется и пользователь может отменить)
            isDismissed = false
            offsetX = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        // Фон удаления (красный на всю ширину)
        DeleteSwipeBackground(
            modifier = Modifier.fillMaxSize()
        )

        // Карточка книги со смещением
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .fillMaxWidth()
                .pointerInput(book.id, resetKey, maxSwipeDistance, triggerDistance) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < -triggerDistance) {
                                // Достгли порога - триггерим удаление
                                isDismissed = true
                            } else {
                                // Не достигли порога - возвращаем обратно
                                offsetX = 0f
                            }
                        },
                        onDragCancel = {
                            // При отмене возвращаем на место
                            offsetX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            // Ограничиваем свайп: только влево и не дальше maxSwipeDistance
                            val newOffset = offsetX + dragAmount
                            offsetX = newOffset.coerceIn(-maxSwipeDistance, 0f)
                        }
                    )
                }
        ) {
            DownloadedBookItem(
                book = book,
                onReadClick = onReadClick,
            )
        }
    }
}
