package su.sv.books.catalog.presentation.downloaded.ui

import android.os.Parcelable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import su.sv.books.catalog.presentation.downloaded.model.DeleteDialogState
import su.sv.books.catalog.presentation.downloaded.model.UiDownloadedBooksState
import su.sv.books.catalog.presentation.downloaded.viewmodel.DownloadedBooksViewModel
import su.sv.commonui.ui.OneTimeEffect
import su.sv.commonui.ui.components.AppAlertDialog
import su.sv.commonui.ui.components.AppToolbarWithBack
import su.sv.commonui.ui.components.FullScreenLoading
import su.sv.models.ui.book.UIBookState
import su.sv.models.ui.book.UiBook

/**
 * Экран "Ваши книги" (Modo Screen)
 */
@Parcelize
class DownloadedBooksScreen(
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen, Parcelable {

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

@Composable
private fun DownloadedBooksContent(
    state: UiDownloadedBooksState,
    deleteDialogState: DeleteDialogState,
    onAction: (DownloadedBookActions) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            AppToolbarWithBack(
                title = stringResource(R.string.books_downloaded_title),
                onBackClick = { onAction(DownloadedBookActions.OnBackClick) }
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
        AppAlertDialog(
            title = stringResource(R.string.books_downloaded_delete_dialog_title),
            text = stringResource(R.string.books_downloaded_delete_dialog_message, deleteDialogState.book.title),
            onDismiss = { onAction(DownloadedBookActions.OnDeleteCancel) },
            onConfirm = { onAction(DownloadedBookActions.OnDeleteConfirm) },
            confirmText = stringResource(R.string.books_downloaded_delete_dialog_yes),
            dismissText = stringResource(R.string.books_downloaded_delete_dialog_no)
        )
    }
}

@Composable
private fun EmptyBooksState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column {
            Text(
                text = stringResource(R.string.books_downloaded_empty_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun HandleEffects(viewModel: DownloadedBooksViewModel) {
    val stackNavigation = LocalStackNavigation.current

    OneTimeEffect(viewModel.effect) { effect ->
        when (effect) {
            is DownloadedBookEffect.OpenBookDetail -> {
                // Конвертируем UiDownloadedBook в UiBook для BookDetailScreen
                val uiBook = UiBook(
                    id = effect.book.id,
                    title = effect.book.title,
                    author = effect.book.author,
                    description = "",
                    image = effect.book.coverUrl,
                    downloadUrl = "",
                    fileNameWithExt = "",
                    category = effect.book.category,
                    fileUri = effect.book.fileUri,
                    downloadState = UIBookState.DOWNLOADED,
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
