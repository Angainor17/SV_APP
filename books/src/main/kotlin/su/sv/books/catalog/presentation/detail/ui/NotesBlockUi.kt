package su.sv.books.catalog.presentation.detail.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import su.sv.books.R
import su.sv.books.catalog.presentation.detail.model.UiNoteWithContext
import su.sv.books.catalog.presentation.detail.model.UiNotesBlockState
import su.sv.commonui.theme.SVAPPTheme

/**
 * Блок заметок на карточке книги
 *
 * Показывает до 5 заметок с контекстом
 * Адаптивный: разные layout для смартфона и планшета
 */
@Composable
fun NotesBlockUi(
    state: UiNotesBlockState,
    onNoteClick: (UiNoteWithContext) -> Unit,
    onAllNotesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = state !is UiNotesBlockState.Hidden,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        when (state) {
            is UiNotesBlockState.Loading -> {
                NotesBlockLoading()
            }

            is UiNotesBlockState.Content -> {
                NotesBlockContent(
                    notes = state.notes,
                    totalCount = state.totalCount,
                    hasMore = state.hasMore,
                    onNoteClick = onNoteClick,
                    onAllNotesClick = onAllNotesClick,
                )
            }

            is UiNotesBlockState.Hidden -> {
                // Не показываем ничего
            }
        }
    }
}

@Composable
private fun NotesBlockLoading(
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = stringResource(R.string.notes_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NotesBlockContent(
    notes: List<UiNoteWithContext>,
    totalCount: Int,
    hasMore: Boolean,
    onNoteClick: (UiNoteWithContext) -> Unit,
    onAllNotesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Заголовок секции
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Левая часть: иконка + заголовок
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.notes_title_with_count, totalCount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            // Правая часть: кнопка "Все заметки"
            if (hasMore) {
                TextButton(onClick = onAllNotesClick) {
                    Text(
                        text = stringResource(R.string.notes_all_notes),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Список заметок
        notes.forEach { note ->
            NoteWithContextItem(
                note = note,
                onClick = { onNoteClick(note) },
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

/**
 * Карточка отдельной заметки с контекстом
 */
@Composable
fun NoteWithContextItem(
    note: UiNoteWithContext,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Текст с контекстом
            val annotatedText = buildAnnotatedString {
                // Текст предложения до заметки (блёклый)
                if (!note.sentenceBefore.isNullOrBlank()) {
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        append(note.sentenceBefore)
                        append(" ")
                    }
                }

                // Текст заметки (выделенный жирным)
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    append(note.text)
                }

                // Текст предложения после заметки (блёклый)
                if (!note.sentenceAfter.isNullOrBlank()) {
                    append(" ")
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        append(note.sentenceAfter)
                    }
                }
            }

            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Номер страницы справа внизу
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = stringResource(R.string.bookmarks_page, note.page),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ============================================
// Preview
// ============================================

@Preview(showBackground = true)
@Composable
private fun NotesBlockLoadingPreview() {
    SVAPPTheme {
        NotesBlockUi(
            state = UiNotesBlockState.Loading,
            onNoteClick = {},
            onAllNotesClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotesBlockContentPreview() {
    SVAPPTheme {
        val notes = listOf(
            UiNoteWithContext(
                id = "1",
                text = "Важная мысль из книги",
                sentenceBefore = "Перед этим было сказано, что",
                sentenceAfter = "и это означает важный вывод.",
                page = 42,
                createdAt = System.currentTimeMillis(),
                startParagraph = 10,
                startElement = 0,
                startChar = 0,
                endParagraph = 10,
                endElement = 5,
                endChar = 0,
            ),
            UiNoteWithContext(
                id = "2",
                text = "Ещё одна заметка",
                sentenceBefore = null,
                sentenceAfter = "продолжение текста",
                page = 87,
                createdAt = System.currentTimeMillis(),
                startParagraph = 20,
                startElement = 0,
                startChar = 0,
                endParagraph = 20,
                endElement = 3,
                endChar = 0,
            ),
        )

        NotesBlockUi(
            state = UiNotesBlockState.Content(
                notes = notes,
                totalCount = 5,
                hasMore = true,
            ),
            onNoteClick = {},
            onAllNotesClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteWithContextItemPreview() {
    SVAPPTheme {
        NoteWithContextItem(
            note = UiNoteWithContext(
                id = "1",
                text = "Важная мысль из книги",
                sentenceBefore = "Перед этим было сказано, что",
                sentenceAfter = "и это означает важный вывод.",
                page = 42,
                createdAt = System.currentTimeMillis(),
                startParagraph = 10,
                startElement = 0,
                startChar = 0,
                endParagraph = 10,
                endElement = 5,
                endChar = 0,
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}