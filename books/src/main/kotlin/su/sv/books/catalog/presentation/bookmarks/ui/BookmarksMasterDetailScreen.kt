package su.sv.books.catalog.presentation.bookmarks.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import su.sv.books.R
import su.sv.books.catalog.presentation.bookmarks.model.UiBookmarksState
import su.sv.books.catalog.presentation.bookmarks.viewmodel.BookmarksAction
import su.sv.books.catalog.presentation.bookmarks.viewmodel.BookmarksViewModel
import su.sv.commonui.theme.LocalDeviceFormFactor
import su.sv.commonui.ui.adaptive.layout.MasterDetailLayout

/**
 * Экран Заметки с master-detail layout для планшетов (Expanded)
 *
 * Показывает две панели:
 * - Master (слева): список заметок
 * - Detail (справа): предпросмотр выбранной заметки
 */
@Composable
fun BookmarksMasterDetailScreen(
    viewModel: BookmarksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedNote by viewModel.selectedNote.collectAsStateWithLifecycle()
    val formFactor = LocalDeviceFormFactor.current

    MasterDetailLayout(
        master = {
            when (val currentState = state) {
                is UiBookmarksState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is UiBookmarksState.NotesList -> {
                    NotesListContent(
                        notes = currentState.notes,
                        onNoteClick = { note ->
                            viewModel.onAction(BookmarksAction.OnNoteSelect(note))
                        },
                        onBookClick = { note ->
                            // Открыть карточку книги
                        },
                        onDeleteRequest = { note ->
                            viewModel.onAction(BookmarksAction.OnDeleteNoteRequest(note))
                        },
                        onShareClick = { note ->
                            viewModel.onAction(BookmarksAction.OnShareNote(note))
                        },
                    )
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.bookmarks_empty_title),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        detail = {
            selectedNote?.let { note ->
                NotePreviewDetail(
                    note = note,
                    onGoToReader = {
                        viewModel.onAction(BookmarksAction.OnNoteClick(note))
                    },
                    onShare = {
                        viewModel.onAction(BookmarksAction.OnShareNote(note))
                    },
                    onDelete = {
                        viewModel.onAction(BookmarksAction.OnDeleteNoteRequest(note))
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.bookmarks_select_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        isDetailVisible = selectedNote != null,
        detailTitle = selectedNote?.text?.take(50)?.plus("…"),
        onCloseDetail = {
            viewModel.onAction(BookmarksAction.OnNoteDeselect)
        },
        modifier = Modifier.fillMaxSize(),
    )
}