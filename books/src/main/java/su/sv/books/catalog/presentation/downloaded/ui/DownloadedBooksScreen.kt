package su.sv.books.catalog.presentation.downloaded.ui

import android.os.Parcelable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import com.github.terrakok.modo.stack.forward
import kotlinx.parcelize.Parcelize
import su.sv.books.R
import su.sv.books.catalog.presentation.detail.nav.BookDetailScreen
import su.sv.books.catalog.presentation.downloaded.actions.DownloadedBookActions
import su.sv.books.catalog.presentation.downloaded.effects.DownloadedBookEffect
import su.sv.books.catalog.presentation.downloaded.model.UiDownloadedBooksState
import su.sv.books.catalog.presentation.downloaded.viewmodel.DownloadedBooksViewModel
import su.sv.commonui.ui.FullScreenLoading
import su.sv.commonui.ui.OneTimeEffect

/**
 * Экран "Ваши книги" (Modo Screen)
 */
@Parcelize
class DownloadedBooksScreen(
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen, Parcelable {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(modifier: Modifier) {
        val viewModel: DownloadedBooksViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val deleteDialogState by viewModel.deleteDialogState.collectAsState()

        DownloadedBooksContent(
            state = state,
            deleteDialogState = deleteDialogState,
            onAction = viewModel::onAction,
            modifier = modifier,
        )

        HandleEffects(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadedBooksContent(
    state: UiDownloadedBooksState,
    deleteDialogState: su.sv.books.catalog.presentation.downloaded.model.DeleteDialogState,
    onAction: (DownloadedBookActions) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.books_downloaded_title))
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(DownloadedBookActions.OnBackClick) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.books_back_content_description)
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            when (state) {
                is UiDownloadedBooksState.Loading -> {
                    FullScreenLoading()
                }

                is UiDownloadedBooksState.Content -> {
                    DownloadedBooksList(
                        books = state.books,
                        onReadClick = { book ->
                            onAction(DownloadedBookActions.OnReadClick(book))
                        },
                        onDeleteRequest = { book ->
                            onAction(DownloadedBookActions.OnDeleteRequest(book))
                        },
                        showSwipeHint = state.showSwipeHint,
                        onSwipeHintShown = { onAction(DownloadedBookActions.OnSwipeHintShown) },
                        resetKey = state.resetKey,
                    )
                }

                is UiDownloadedBooksState.Empty -> {
                    EmptyBooksState()
                }
            }
        }
    }

    // Диалог подтверждения удаления
    if (deleteDialogState.isVisible && deleteDialogState.book != null) {
        DeleteConfirmDialog(
            bookTitle = deleteDialogState.book.title,
            onConfirm = { onAction(DownloadedBookActions.OnDeleteConfirm) },
            onDismiss = { onAction(DownloadedBookActions.OnDeleteCancel) },
        )
    }
}

@Composable
private fun EmptyBooksState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Column {
            Text(stringResource(R.string.books_downloaded_empty_title))
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    bookTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.books_downloaded_delete_dialog_title))
        },
        text = {
            Text(stringResource(R.string.books_downloaded_delete_dialog_message, bookTitle))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.books_downloaded_delete_dialog_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.books_downloaded_delete_dialog_no))
            }
        },
    )
}

@Composable
private fun HandleEffects(viewModel: DownloadedBooksViewModel) {
    val stackNavigation = LocalStackNavigation.current

    OneTimeEffect(viewModel.effect) { effect ->
        when (effect) {
            is DownloadedBookEffect.OpenBookDetail -> {
                // Конвертируем UiDownloadedBook в UiBook для BookDetailScreen
                val uiBook = su.sv.models.ui.book.UiBook(
                    id = effect.book.id,
                    title = effect.book.title,
                    author = effect.book.author,
                    description = "",
                    image = effect.book.coverUrl,
                    downloadUrl = "",
                    fileNameWithExt = "",
                    category = effect.book.category,
                    fileUri = effect.book.fileUri,
                    downloadState = su.sv.models.ui.book.UIBookState.DOWNLOADED,
                )
                stackNavigation.forward(
                    BookDetailScreen(uiBook = uiBook)
                )
            }

            DownloadedBookEffect.NavigateBack -> {
                stackNavigation.back()
            }

            is DownloadedBookEffect.ShowError -> {
                // Можно показать Snackbar, если добавить SnackbarHostState
            }
        }
    }
}
