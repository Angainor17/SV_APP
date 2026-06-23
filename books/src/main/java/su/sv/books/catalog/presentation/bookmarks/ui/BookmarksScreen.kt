package su.sv.books.catalog.presentation.bookmarks.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import su.sv.books.R
import su.sv.books.catalog.presentation.bookmarks.model.NotesViewMode
import su.sv.books.catalog.presentation.bookmarks.model.UiBookWithNotes
import su.sv.books.catalog.presentation.bookmarks.model.UiBookmarkNote
import su.sv.books.catalog.presentation.bookmarks.model.UiBookmarksState
import su.sv.books.catalog.presentation.bookmarks.viewmodel.BookmarksAction
import su.sv.books.catalog.presentation.bookmarks.viewmodel.BookmarksEffect
import su.sv.books.catalog.presentation.bookmarks.viewmodel.BookmarksViewModel
import su.sv.commonui.theme.SVAPPTheme
import su.sv.commonui.ui.FullScreenError
import su.sv.commonui.ui.FullScreenLoading
import su.sv.commonui.ui.OneTimeEffect
import timber.log.Timber

/**
 * Экран Заметки (закладки)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    viewModel: BookmarksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deleteDialogState by viewModel.deleteDialogState.collectAsStateWithLifecycle()

    val currentViewMode = when (state) {
        is UiBookmarksState.NotesList -> (state as UiBookmarksState.NotesList).viewMode
        else -> NotesViewMode.LIST
    }

    Scaffold(
        topBar = {
            BookmarksTopBar(
                viewMode = currentViewMode,
                showViewModeToggle = state is UiBookmarksState.NotesList || state is UiBookmarksState.BooksList,
                onBackClick = { viewModel.onAction(BookmarksAction.OnBackClick) },
                onToggleViewMode = { viewModel.onAction(BookmarksAction.OnToggleViewMode) },
            )
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            when (val currentState = state) {
                is UiBookmarksState.Loading -> FullScreenLoading()
                is UiBookmarksState.Empty -> EmptyNotesState()
                is UiBookmarksState.NotesList -> NotesListContent(
                    notes = currentState.notes,
                    onNoteClick = { viewModel.onAction(BookmarksAction.OnNoteClick(it)) },
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksTopBar(
    viewMode: NotesViewMode,
    showViewModeToggle: Boolean,
    onBackClick: () -> Unit,
    onToggleViewMode: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(stringResource(R.string.bookmarks_title))
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.books_back_content_description)
                )
            }
        },
        actions = {
            if (showViewModeToggle) {
                IconButton(onClick = onToggleViewMode) {
                    Icon(
                        imageVector = if (viewMode == NotesViewMode.LIST) {
                            Icons.Default.ViewAgenda
                        } else {
                            Icons.AutoMirrored.Filled.ViewList
                        },
                        contentDescription = stringResource(R.string.bookmarks_toggle_view_mode)
                    )
                }
            }
        }
    )
}

/**
 * Пустое состояние заметок
 */
@Composable
fun EmptyNotesState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.bookmarks_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Контент: список заметок
 */
@Composable
fun NotesListContent(
    notes: List<UiBookmarkNote>,
    onNoteClick: (UiBookmarkNote) -> Unit,
    onDeleteRequest: (UiBookmarkNote) -> Unit,
    onShareClick: (UiBookmarkNote) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = notes, key = { it.id }) { note ->
            NoteItem(
                note = note,
                showBookInfo = true,
                onClick = { onNoteClick(note) },
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
    onDeleteRequest: (UiBookmarkNote) -> Unit,
    onShareClick: (UiBookmarkNote) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Компактная карточка книги
        CompactBookCard(book = book)

        // Список заметок
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items = notes, key = { it.id }) { note ->
                NoteItem(
                    note = note,
                    showBookInfo = false,
                    onClick = { onNoteClick(note) },
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
                // TODO: Открыть читалку на позиции заметки
                Timber.d("Open reader at position: ${effect.note.startParagraph}")
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
            is BookmarksEffect.ShowError -> {
                Timber.e("Error: ${effect.message}")
            }
        }
    }
}

//region Previews

@Preview(showBackground = true)
@Composable
private fun EmptyNotesStatePreview() {
    SVAPPTheme {
        EmptyNotesState()
    }
}

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
