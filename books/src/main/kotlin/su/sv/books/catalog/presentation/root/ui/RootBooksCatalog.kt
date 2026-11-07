package su.sv.books.catalog.presentation.root.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
import su.sv.books.testing.BooksTestTags
import su.sv.commonui.ui.OneTimeEffect
import su.sv.commonui.ui.components.AppToolbar
import su.sv.commonui.ui.components.FullScreenError
import su.sv.commonui.ui.components.FullScreenLoading

/**
 * Главный экран каталога книг
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootBooksCatalog(
    viewModel: RootBooksCatalogViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var isSearchVisible by remember { mutableStateOf(false) }

    HandleEffects(viewModel, snackbarHostState)

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .testTag(BooksTestTags.BooksCatalog.ROOT),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            when (val currentState = state.value) {
                is UiRootBooksState.Content -> {
                    if (isSearchVisible) {
                        BooksCatalogSearchTopBar(
                            query = currentState.searchQuery,
                            onQueryChange = { query ->
                                viewModel.onAction(RootBookActions.OnSearchQueryChange(query))
                            },
                            onClose = {
                                // При закрытии поиска сбрасываем запрос
                                viewModel.onAction(RootBookActions.OnSearchQueryChange(""))
                                isSearchVisible = false
                            },
                            scrollBehavior = scrollBehavior,
                        )
                    } else {
                        BooksCatalogTopBar(
                            scrollBehavior = scrollBehavior,
                            actions = viewModel,
                            hasDownloadedBooks = currentState.hasDownloadedBooks,
                            onSearchClick = { isSearchVisible = true },
                        )
                    }
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
                    contentPadding = contentPadding,
                    isSearchVisible = isSearchVisible,
                )
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
    onSearchClick: () -> Unit,
) {
    AppToolbar(
        title = stringResource(R.string.books_title),
        windowInsets = WindowInsets(0.dp),
        scrollBehavior = scrollBehavior,
        actions = {
            // Иконка поиска
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.testTag(BooksTestTags.BooksCatalog.SEARCH_BUTTON),
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.books_search_content_description),
                )
            }

            // Иконка заметок (закладки)
            IconButton(
                onClick = { actions.onAction(RootBookActions.OnToolbarBookmarksClick) },
                modifier = Modifier.testTag(BooksTestTags.BooksCatalog.BOOKMARKS_BUTTON),
            ) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = stringResource(R.string.books_bookmarks_content_description),
                )
            }

            // Иконка скачанных книг
            if (hasDownloadedBooks) {
                IconButton(
                    onClick = { actions.onAction(RootBookActions.OnToolbarBooksClick) },
                    modifier = Modifier.testTag(BooksTestTags.BooksCatalog.DOWNLOADED_BUTTON),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BooksCatalogSearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val focusRequester = remember { FocusRequester() }
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val backgroundColor = MaterialTheme.colorScheme.background

    // Автофокус при открытии поиска
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag(BooksTestTags.BooksCatalog.SEARCH_FIELD),
                placeholder = {
                    Text(
                        text = stringResource(R.string.books_search_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.testTag(BooksTestTags.BooksCatalog.SEARCH_CLEAR_BUTTON),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.books_search_clear_content_description),
                            )
                        }
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag(BooksTestTags.BooksCatalog.SEARCH_CLOSE_BUTTON),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.books_back_content_description),
                    tint = onSurfaceColor,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor,
            scrolledContainerColor = backgroundColor,
            titleContentColor = onSurfaceColor,
            navigationIconContentColor = onSurfaceColor,
            actionIconContentColor = onSurfaceColor,
        ),
        scrollBehavior = scrollBehavior,
        windowInsets = WindowInsets(0.dp),
    )
}

@Composable
private fun HandleEffects(
    viewModel: RootBooksCatalogViewModel,
    snackbarHostState: SnackbarHostState,
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
                // Обрабатывается в BookList через filterScrollResetKey
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
