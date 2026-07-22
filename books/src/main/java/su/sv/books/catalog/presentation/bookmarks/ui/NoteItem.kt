package su.sv.books.catalog.presentation.bookmarks.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.delay
import su.sv.books.R
import su.sv.books.catalog.presentation.bookmarks.model.UiBookmarkNote
import su.sv.commonui.theme.SVAPPTheme
import java.io.File
import kotlin.math.roundToInt

/**
 * Элемент заметки с поддержкой свайпа для удаления
 *
 * @param note Данные заметки
 * @param showBookInfo Показывать информацию о книге
 * @param onClick Callback при нажатии на "Перейти" (открыть книгу)
 * @param onBookClick Callback при нажатии на "К книге" (если книга удалена)
 * @param onDeleteRequest Callback при запросе удаления
 * @param onShareClick Callback при нажатии на кнопку "Поделиться"
 */
@Composable
fun NoteItem(
    note: UiBookmarkNote,
    showBookInfo: Boolean,
    onClick: () -> Unit,
    onBookClick: () -> Unit,
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
            delay(200)
            onDeleteRequest()
            isDismissed = false
            offsetX = 0f
        }
    }

    // Внешний контейнер с отступами
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // Красный фон удаления (под карточкой)
        Surface(
            modifier = Modifier.matchParentSize(),
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
                hasBookFile = note.bookFileUri != null,
                onClick = onClick,
                onBookClick = onBookClick,
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
    hasBookFile: Boolean,
    onClick: () -> Unit,
    onBookClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // Название книги (если нужно показать)
        if (showBookInfo) {
            NoteBookInfo(
                bookTitle = note.bookTitle,
                bookCoverUrl = note.bookCoverUrl,
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

        // Нижняя строка: кнопка "Перейти" или "К книге", "Поделиться" справа
        NoteItemActions(
            hasBookFile = hasBookFile,
            onClick = onClick,
            onBookClick = onBookClick,
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
    bookCoverUrl: String,
    page: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Обложка книги
        BookCoverSmall(
            coverUrl = bookCoverUrl,
            modifier = Modifier
                .size(32.dp, 48.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        // Название книги
        Text(
            text = bookTitle,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Страница
        Text(
            text = stringResource(R.string.bookmarks_page, page),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Маленькая обложка книги для заметки
 */
@Composable
private fun BookCoverSmall(
    coverUrl: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Преобразуем путь в Uri если это локальный файл
    val imageModel = remember(coverUrl) {
        if (coverUrl.isNotBlank()) {
            // Если это локальный путь к файлу, преобразуем в Uri
            if (coverUrl.startsWith("/") || coverUrl.startsWith("file://")) {
                try {
                    File(coverUrl).toUri()
                } catch (e: Exception) {
                    coverUrl
                }
            } else {
                coverUrl
            }
        } else {
            null
        }
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageModel)
            .build(),
        placeholder = painterResource(R.drawable.ic_book_placeholder),
        error = painterResource(R.drawable.ic_book_placeholder),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

/**
 * Кнопки действий в заметке
 */
@Composable
private fun NoteItemActions(
    hasBookFile: Boolean,
    onClick: () -> Unit,
    onBookClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Показываем "Перейти" если книга есть, "К книге" если книга удалена
        if (hasBookFile) {
            OutlinedButton(onClick = onClick) {
                Text(stringResource(R.string.bookmarks_go_to))
            }
        } else {
            OutlinedButton(onClick = onBookClick) {
                Text(stringResource(R.string.bookmarks_go_to_book))
            }
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
                bookFileUri = null,
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
            onBookClick = {},
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
                bookFileUri = null,
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
            onBookClick = {},
            onDeleteRequest = {},
            onShareClick = {}
        )
    }
}

//endregion
