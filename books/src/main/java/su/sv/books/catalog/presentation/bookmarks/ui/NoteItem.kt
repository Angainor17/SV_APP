package su.sv.books.catalog.presentation.bookmarks.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import su.sv.books.R
import su.sv.books.catalog.presentation.bookmarks.model.UiBookmarkNote
import su.sv.commonui.theme.SVAPPTheme
import kotlin.math.roundToInt

/**
 * Элемент заметки с поддержкой свайпа для удаления
 *
 * @param note Данные заметки
 * @param showBookInfo Показывать информацию о книге
 * @param onClick Callback при нажатии на заметку
 * @param onDeleteRequest Callback при запросе удаления
 * @param onShareClick Callback при нажатии на кнопку "Поделиться"
 */
@Composable
fun NoteItem(
    note: UiBookmarkNote,
    showBookInfo: Boolean,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit,
    onShareClick: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Максимальное смещение - 30% от ширины экрана
    val maxSwipeDistance = with(density) {
        (configuration.screenWidthDp.dp.toPx() * 0.3f)
    }

    // Порог срабатывания - 25% от ширины
    val triggerDistance = with(density) {
        (configuration.screenWidthDp.dp.toPx() * 0.25f)
    }

    var offsetX by remember(note.id) { mutableFloatStateOf(0f) }
    var isDismissed by remember(note.id) { mutableStateOf(false) }

    // Анимация
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isDismissed) -maxSwipeDistance else offsetX,
        animationSpec = SpringSpec(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "swipe_animation"
    )

    // Обработка dismissal
    LaunchedEffect(isDismissed) {
        if (isDismissed) {
            kotlinx.coroutines.delay(200)
            onDeleteRequest()
            isDismissed = false
            offsetX = 0f
        }
    }

    // Внешний контейнер с отступами
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // Красный фон удаления (под карточкой)
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Карточка заметки (поверх красного фона)
        Surface(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .fillMaxWidth()
                .pointerInput(note.id, maxSwipeDistance, triggerDistance) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < -triggerDistance) {
                                isDismissed = true
                            } else {
                                offsetX = 0f
                            }
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            val newOffset = offsetX + dragAmount
                            offsetX = newOffset.coerceIn(-maxSwipeDistance, 0f)
                        }
                    )
                },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            NoteItemContent(
                note = note,
                showBookInfo = showBookInfo,
                onClick = onClick,
                onShareClick = onShareClick
            )
        }
    }
}

/**
 * Контент элемента заметки
 */
@Composable
private fun NoteItemContent(
    note: UiBookmarkNote,
    showBookInfo: Boolean,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // Название книги (если нужно показать)
        if (showBookInfo) {
            NoteBookInfo(
                bookTitle = note.bookTitle,
                page = note.page
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Текст заметки
        Text(
            text = note.text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f, fill = false))

        // Нижняя строка: кнопка "Перейти" слева, "Поделиться" справа
        NoteItemActions(
            onClick = onClick,
            onShareClick = onShareClick
        )
    }
}

/**
 * Информация о книге в заметке
 */
@Composable
private fun NoteBookInfo(
    bookTitle: String,
    page: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = bookTitle,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(R.string.bookmarks_page, page),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Кнопки действий в заметке
 */
@Composable
private fun NoteItemActions(
    onClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onClick) {
            Text(stringResource(R.string.bookmarks_go_to))
        }

        IconButton(onClick = onShareClick) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = stringResource(R.string.bookmarks_share),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

//region Previews

@Preview(showBackground = true)
@Composable
private fun NoteItemPreview() {
    SVAPPTheme {
        NoteItem(
            note = UiBookmarkNote(
                id = "1",
                bookId = "book1",
                bookTitle = "Название книги",
                bookAuthor = "Автор",
                bookCoverUrl = "",
                text = "Это пример текста заметки, который может быть достаточно длинным и занимать несколько строк.",
                name = null,
                page = 42,
                createdAt = System.currentTimeMillis(),
                startParagraph = 0,
                startElement = 0,
                startChar = 0,
                endParagraph = 0,
                endElement = 0,
                endChar = 0
            ),
            showBookInfo = true,
            onClick = {},
            onDeleteRequest = {},
            onShareClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteItemWithoutBookInfoPreview() {
    SVAPPTheme {
        NoteItem(
            note = UiBookmarkNote(
                id = "1",
                bookId = "book1",
                bookTitle = "Название книги",
                bookAuthor = "Автор",
                bookCoverUrl = "",
                text = "Короткая заметка",
                name = null,
                page = 15,
                createdAt = System.currentTimeMillis(),
                startParagraph = 0,
                startElement = 0,
                startChar = 0,
                endParagraph = 0,
                endElement = 0,
                endChar = 0
            ),
            showBookInfo = false,
            onClick = {},
            onDeleteRequest = {},
            onShareClick = {}
        )
    }
}

//endregion
