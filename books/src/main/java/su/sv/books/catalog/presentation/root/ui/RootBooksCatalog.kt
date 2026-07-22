package su.sv.books.catalog.presentation.root.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.axet.bookreader.screens.ReaderScreen
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.forward
import kotlinx.coroutines.launch
import su.sv.books.R
import su.sv.books.catalog.presentation.bookmarks.nav.BookmarksScreen
import su.sv.books.catalog.presentation.detail.nav.BookDetailScreen
import su.sv.books.catalog.presentation.downloaded.ui.DownloadedBooksScreen
import su.sv.books.catalog.presentation.root.model.UiRootBooksState
import su.sv.books.catalog.presentation.root.viewmodel.RootBooksCatalogViewModel
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBookActions
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBookActions.OnBookStateHandle
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBooksActions
import su.sv.books.catalog.presentation.root.viewmodel.effects.BooksListOneTimeEffect
import su.sv.commonui.ui.OneTimeEffect
import su.sv.commonui.ui.components.AppToolbar
import su.sv.commonui.ui.components.FullScreenError
import su.sv.commonui.ui.components.FullScreenLoading

/**
 * Главный экран каталога книг
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RootBooksCatalog(
    viewModel: RootBooksCatalogViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var scrollEffect by remember { mutableStateOf<BooksListOneTimeEffect.ScrollToTop?>(null) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    HandleEffects(viewModel, snackbarHostState) { effect ->
        when (effect) {
            is BooksListOneTimeEffect.ScrollToTop -> {
                scrollEffect = effect
            }

            else -> { /* другие эффекты обрабатываются в HandleEffects */
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            when (val currentState = state.value) {
                is UiRootBooksState.Content -> {
                    BooksCatalogTopBar(
                        scrollBehavior = scrollBehavior,
                        actions = viewModel,
                        hasDownloadedBooks = currentState.hasDownloadedBooks,
                    )
                }

                else -> {
                    AppToolbar(
                        title = stringResource(R.string.books_toolbar_title),
                        windowInsets = WindowInsets(0.dp),
                        scrollBehavior = scrollBehavior,
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { contentPadding ->
        when (state.value) {
            is UiRootBooksState.Content -> {
                BookList(
                    actions = viewModel,
                    state = state.value as UiRootBooksState.Content,
                    scrollEffect = scrollEffect,
                    contentPadding = contentPadding,
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
                FullScreenError(
                    onRetry = { viewModel.onAction(RootBookActions.OnRetryClick) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BooksCatalogTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    actions: RootBooksActions,
    hasDownloadedBooks: Boolean,
) {
    AppToolbar(
        title = stringResource(R.string.books_title),
        windowInsets = WindowInsets(0.dp),
        scrollBehavior = scrollBehavior,
        actions = {
            // Иконка заметок (закладки)
            IconButton(
                onClick = { actions.onAction(RootBookActions.OnToolbarBookmarksClick) }
            ) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = stringResource(R.string.books_bookmarks_content_description),
                )
            }

            // Иконка скачанных книг
            if (hasDownloadedBooks) {
                IconButton(
                    onClick = { actions.onAction(RootBookActions.OnToolbarBooksClick) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = stringResource(R.string.books_menu_action_content_description),
                    )
                }
            }
        }
    )
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

            BooksListOneTimeEffect.OpenBookmarks -> {
                stackNavigation.forward(BookmarksScreen())
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
                        ReaderScreen(
                            bookUri = uri,
                            bookCoverUrl = effect.book.image,
                            bookTitle = effect.book.title,
                            bookAuthor = effect.book.author
                        )
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
            Text(
                text = stringResource(R.string.books_empty_list_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
