package su.sv.books.catalog.presentation.root.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.axet.bookreader.screens.ReaderScreen
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.forward
import kotlinx.coroutines.launch
import su.sv.books.R
import su.sv.books.catalog.presentation.detail.nav.BookDetailScreen
import su.sv.books.catalog.presentation.downloaded.ui.DownloadedBooksScreen
import su.sv.books.catalog.presentation.root.model.UiRootBooksState
import su.sv.books.catalog.presentation.root.viewmodel.RootBooksCatalogViewModel
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBookActions
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBookActions.OnBookStateHandle
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBooksActions
import su.sv.books.catalog.presentation.root.viewmodel.effects.BooksListOneTimeEffect
import su.sv.commonui.ui.FullScreenError
import su.sv.commonui.ui.FullScreenLoading
import su.sv.commonui.ui.OneTimeEffect

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RootBooksCatalog(
    viewModel: RootBooksCatalogViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var scrollEffect by remember { mutableStateOf<BooksListOneTimeEffect.ScrollToTop?>(null) }

    HandleEffects(viewModel, snackbarHostState) { effect ->
        when (effect) {
            is BooksListOneTimeEffect.ScrollToTop -> {
                scrollEffect = effect
            }
            else -> { /* другие эффекты обрабатываются в HandleEffects */ }
        }
    }

    when (state.value) {
        is UiRootBooksState.Content -> {
            BookList(
                actions = viewModel,
                state = state.value as UiRootBooksState.Content,
                snackbarHostState = snackbarHostState,
                scrollEffect = scrollEffect,
            )
            // Сбрасываем эффект после обработки
            LaunchedEffect(scrollEffect) {
                scrollEffect = null
            }
        }

        UiRootBooksState.EmptyState -> {
            NoBooks()
        }

        UiRootBooksState.Loading -> {
            FullScreenLoading()
        }

        is UiRootBooksState.Failure -> {
            FullScreenError {
                viewModel.onAction(RootBookActions.OnRetryClick)
            }
        }
    }
}

@Composable
private fun HandleEffects(
    viewModel: RootBooksCatalogViewModel,
    snackbarHostState: SnackbarHostState,
    onEffect: (BooksListOneTimeEffect) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val stackNavigation = LocalStackNavigation.current

    val downloadState = viewModel.bookDownloadedActionHandler.get()
        .sharedStateFlow
        .collectAsStateWithLifecycle(null)
        .value

    LaunchedEffect(downloadState) {
        downloadState?.let { viewModel.onAction(OnBookStateHandle(it)) }
    }

    OneTimeEffect(viewModel.oneTimeEffect) { effect ->
        when (effect) {
            BooksListOneTimeEffect.OpenStoredBooksList -> {
                stackNavigation.forward(DownloadedBooksScreen())
            }

            is BooksListOneTimeEffect.OpenBook -> {
                stackNavigation.forward(
                    BookDetailScreen(
                        uiBook = effect.book,
                    )
                )
            }

            is BooksListOneTimeEffect.OpenReader -> {
                val uri = effect.book.fileUri
                if (uri != null) {
                    stackNavigation.forward(
                        ReaderScreen(bookUri = uri)
                    )
                }
            }

            is BooksListOneTimeEffect.ShowErrorSnackBar -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = effect.text,
                        duration = SnackbarDuration.Short,
                    )
                }
            }

            is BooksListOneTimeEffect.ScrollToTop -> {
                onEffect(effect)
            }
        }
    }
    HandleLifecycleEvents(viewModel)
}

@Composable
private fun HandleLifecycleEvents(actions: RootBooksActions) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState = lifecycleOwner.lifecycle.currentStateFlow.collectAsState().value

    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            actions.onAction(RootBookActions.UpdateStates)
        }
    }
}

@Composable
fun NoBooks() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(modifier = Modifier.wrapContentSize()) {
            Text(stringResource(R.string.books_empty_list_title))
        }
    }
}
