package su.sv.books.catalog.presentation.root.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.axet.bookreader.activities.BookReaderMainActivity
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.forward
import kotlinx.coroutines.launch
import su.sv.books.R
import su.sv.books.catalog.presentation.detail.nav.BookDetailScreen
import su.sv.books.catalog.presentation.root.model.UiRootBooksState
import su.sv.books.catalog.presentation.root.viewmodel.RootBooksCatalogViewModel
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBookActions
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBookActions.OnBookStateHandle
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBooksActions
import su.sv.books.catalog.presentation.root.viewmodel.effects.BooksListOneTimeEffect
import su.sv.books.catalog.presentation.root.viewmodel.effects.BooksListOneTimeEffect.OpenStoredBooksList
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

    HandleEffects(viewModel, snackbarHostState)

    when (state.value) {
        is UiRootBooksState.Content -> {
            BookList(
                actions = viewModel,
                state = state.value as UiRootBooksState.Content,
                snackbarHostState = snackbarHostState,
            )
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
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
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
            OpenStoredBooksList -> {
                openStoredBooks(context)
            }

            is BooksListOneTimeEffect.OpenBook -> {
                stackNavigation.forward(
                    BookDetailScreen(
                        uiBook = effect.book,
                    )
                )
            }

            is BooksListOneTimeEffect.ShowErrorSnackBar -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = effect.text,
                        duration = SnackbarDuration.Short,
                    )
                }
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

private fun openStoredBooks(context: Context) {
    val intent = Intent(context, BookReaderMainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
    }
    context.startActivity(intent)
}
