package su.sv.books.catalog.presentation.bookmarks.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.axet.bookreader.screens.BookmarkPosition
import com.github.axet.bookreader.screens.ReaderScreen
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import com.github.terrakok.modo.stack.forward
import su.sv.books.R
import su.sv.books.catalog.presentation.bookmarks.model.NotesViewMode
import su.sv.books.catalog.presentation.bookmarks.model.UiBookWithNotes
import su.sv.books.catalog.presentation.bookmarks.model.UiBookmarkNote
import su.sv.books.catalog.presentation.bookmarks.model.UiBookmarksState
import su.sv.books.catalog.presentation.bookmarks.viewmodel.BookmarksAction
import su.sv.books.catalog.presentation.bookmarks.viewmodel.BookmarksEffect
import su.sv.books.catalog.presentation.bookmarks.viewmodel.BookmarksViewModel
import su.sv.books.catalog.presentation.detail.nav.BookDetailScreen
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.theme.SVAPPTheme
import su.sv.commonui.ui.FullScreenError
import su.sv.commonui.ui.FullScreenLoading
import su.sv.commonui.ui.OneTimeEffect
import su.sv.commonui.ui.components.AppToolbarWithBack
import su.sv.commonui.ui.components.FullScreenEmpty
import su.sv.models.ui.book.UiBook
import timber.log.Timber

/**
 * Экран Заметки (закладки)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksContent(
    viewModel: BookmarksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deleteDialogState by viewModel.deleteDialogState.collectAsStateWithLifecycle()

    val currentViewMode = when (state) {
        is UiBookmarksState.NotesList -> (state as UiBookmarksState.NotesList).viewMode
        else -> NotesViewMode.LIST
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            BookmarksTopBar(
                viewMode = currentViewMode,
                showViewModeToggle = state is UiBookmarksState.NotesList || state is UiBookmarksState.BooksList,
                onBackClick = { viewModel.onAction(BookmarksAction.OnBackClick) },
                onToggleViewMode = { viewModel.onAction(BookmarksAction.OnToggleViewMode) },
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val currentState = state) {
                is UiBookmarksState.Loading -> FullScreenLoading()
                is UiBookmarksState.Empty -> FullScreenEmpty(
                    title = stringResource(R.string.bookmarks_empty_title),
                    icon = Icons.AutoMirrored.Filled.MenuBook
                )
                is UiBookmarksState.NotesList -> NotesListContent(
                    notes = currentState.notes,
                    onNoteClick = { viewModel.onAction(BookmarksAction.OnNoteClick(it)) },
                    onBookClick = { viewModel.onAction(BookmarksAction.OnBookCardClick(it)) },
                    onDeleteRequest = { viewModel.onAction(BookmarksAction.OnDeleteNoteRequest(it)) },
                    onShareClick = { viewModel.onAction(BookmarksAction.OnShareNote(it)) },
                )
                is UiBookmarksState.BooksList -> BooksListContent(
                    books = currentState.books,
                    onBookClick = { viewModel.onAction(BookmarksAction.OnBookClick(it)) },
                )
                is UiBookmarksState.BookNotes -> BookNotesContent(
                    book = currentState.book,
                    notes = currentState.notes,
                    onNoteClick = { viewModel.onAction(BookmarksAction.OnNoteClick(it)) },
                    onBookCardClick = { viewModel.onAction(BookmarksAction.OnBookCardClick(it)) },
                    onDeleteRequest = { viewModel.onAction(BookmarksAction.OnDeleteNoteRequest(it)) },
                    onShareClick = { viewModel.onAction(BookmarksAction.OnShareNote(it)) },
                )
                is UiBookmarksState.Error -> FullScreenError {
                    viewModel.onAction(BookmarksAction.OnRetryClick)
                }
            }
        }
    }

    // Диалог удаления
    val noteToDelete = deleteDialogState.note
    if (deleteDialogState.isVisible && noteToDelete != null) {
        DeleteNoteDialog(
            noteText = noteToDelete.text,
            onConfirm = { viewModel.onAction(BookmarksAction.OnDeleteNoteConfirm) },
            onDismiss = { viewModel.onAction(BookmarksAction.OnDeleteNoteCancel) },
        )
    }

    // Обработка эффектов
    HandleEffects(viewModel)
}

/**
 * Тулбар экрана заметок
 */
@Composable
fun BookmarksTopBar(
    viewMode: NotesViewMode,
    showViewModeToggle: Boolean,
    onBackClick: () -> Unit,
    onToggleViewMode: () -> Unit,
) {
    AppToolbarWithBack(
        title = stringResource(R.string.bookmarks_title),
        onBackClick = onBackClick,
        actions = {
            if (showViewModeToggle) {
                IconButton(onClick = onToggleViewMode) {
                    Icon(
                        imageVector = if (viewMode == NotesViewMode.LIST) {
                            Icons.Default.ViewAgenda
                        } else {
                            Icons.AutoMirrored.Filled.ViewList
                        },
                        contentDescription = stringResource(R.string.bookmarks_toggle_view_mode),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    )
}

/**
 * Контент: список заметок
 */
@Composable
fun NotesListContent(
    notes: List<UiBookmarkNote>,
    onNoteClick: (UiBookmarkNote) -> Unit,
    onBookClick: (UiBookmarkNote) -> Unit,
    onDeleteRequest: (UiBookmarkNote) -> Unit,
    onShareClick: (UiBookmarkNote) -> Unit,
) {
    val dimensions = LocalAppDimensions.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacingMedium),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        items(items = notes, key = { it.id }) { note ->
            NoteItem(
                note = note,
                showBookInfo = true,
                onClick = { onNoteClick(note) },
                onBookClick = { onBookClick(note) },
                onDeleteRequest = { onDeleteRequest(note) },
                onShareClick = { onShareClick(note) },
            )
        }
    }
}

/**
 * Контент: список книг с заметками
 */
@Composable
fun BooksListContent(
    books: List<UiBookWithNotes>,
    onBookClick: (String) -> Unit,
) {
    val dimensions = LocalAppDimensions.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacingMedium),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        items(items = books, key = { it.bookId }) { book ->
            BookWithNotesItem(
                book = book,
                onClick = { onBookClick(book.bookId) },
            )
        }
    }
}

/**
 * Контент: заметки одной книги
 */
@Composable
fun BookNotesContent(
    book: UiBookWithNotes,
    notes: List<UiBookmarkNote>,
    onNoteClick: (UiBookmarkNote) -> Unit,
    onBookCardClick: (UiBookmarkNote) -> Unit,
    onDeleteRequest: (UiBookmarkNote) -> Unit,
    onShareClick: (UiBookmarkNote) -> Unit,
) {
    val dimensions = LocalAppDimensions.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Компактная карточка книги
        CompactBookCard(book = book)

        // Список заметок
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacingMedium),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(items = notes, key = { it.id }) { note ->
                NoteItem(
                    note = note,
                    showBookInfo = false,
                    onClick = { onNoteClick(note) },
                    onBookClick = { onBookCardClick(note) },
                    onDeleteRequest = { onDeleteRequest(note) },
                    onShareClick = { onShareClick(note) },
                )
            }
        }
    }
}

/**
 * Обработка одноразовых эффектов
 */
@Composable
private fun HandleEffects(viewModel: BookmarksViewModel) {
    val context = LocalContext.current
    val stackNavigation = LocalStackNavigation.current

    OneTimeEffect(viewModel.effect) { effect ->
        when (effect) {
            BookmarksEffect.NavigateBack -> {
                stackNavigation.back()
            }
            is BookmarksEffect.OpenReader -> {
                val note = effect.note
                val bookUri = note.bookFileUri

                if (bookUri != null) {
                    // Создаём параметры позиции для навигации к заметке
                    val position = BookmarkPosition(
                        startParagraph = note.startParagraph,
                        startElement = note.startElement,
                        startChar = note.startChar,
                        endParagraph = note.endParagraph,
                        endElement = note.endElement,
                        endChar = note.endChar
                    )

                    Timber.d("Opening reader at position: paragraph=${note.startParagraph}, uri=$bookUri, coverUrl=${note.bookCoverUrl}")

                    stackNavigation.forward(
                        ReaderScreen(
                            bookUri = Uri.parse(bookUri),
                            bookCoverUrl = note.bookCoverUrl,
                            bookTitle = note.bookTitle,
                            bookAuthor = note.bookAuthor,
                            bookmarkPosition = position
                        )
                    )
                } else {
                    Timber.w("Cannot open reader: bookFileUri is null for note ${note.id}")
                    // Показываем сообщение что книга не найдена
                    android.widget.Toast.makeText(
                        context,
                        context.getString(su.sv.books.R.string.bookmarks_book_not_found),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            is BookmarksEffect.ShareNote -> {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, effect.text)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            }
            is BookmarksEffect.OpenBookCard -> {
                // Переход на карточку книги (когда файл книги удалён)
                val note = effect.note
                val uiBook = UiBook(
                    id = note.bookId,  // Используем bookId из заметки
                    title = note.bookTitle,
                    author = note.bookAuthor,
                    description = "",
                    image = note.bookCoverUrl,
                    downloadUrl = "",
                    fileNameWithExt = "",
                    category = "",
                    downloadState = su.sv.models.ui.book.UIBookState.AVAILABLE_TO_DOWNLOAD,
                    fileUri = null
                )
                stackNavigation.forward(BookDetailScreen(uiBook = uiBook))
            }
            is BookmarksEffect.ShowError -> {
                Timber.e("Error: ${effect.message}")
            }
        }
    }
}

//region Previews

@Preview(showBackground = true)
@Composable
private fun BookmarksTopBarPreview() {
    SVAPPTheme {
        BookmarksTopBar(
            viewMode = NotesViewMode.LIST,
            showViewModeToggle = true,
            onBackClick = {},
            onToggleViewMode = {}
        )
    }
}

//endregion
