package su.sv.books.catalog.presentation.detail.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.axet.bookreader.screens.ReaderScreen
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import com.github.terrakok.modo.stack.forward
import kotlinx.coroutines.launch
import su.sv.books.catalog.presentation.detail.actions.DetailBookActions
import su.sv.books.catalog.presentation.detail.effects.BookDetailOneTimeEffect
import su.sv.books.catalog.presentation.detail.model.UiBookDetailState
import su.sv.books.catalog.presentation.detail.viewmodel.BookDetailViewModel
import su.sv.commonui.ui.OneTimeEffect
import su.sv.commonui.ui.components.AppToolbarWithBack
import su.sv.models.ui.book.UiBook

@ExperimentalMaterial3Api
@Composable
fun BookDetailUi(
    viewModel: BookDetailViewModel = hiltViewModel(),
    uiBook: UiBook,
    modifier: Modifier
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val stackNavigation = LocalStackNavigation.current

    LaunchedEffect(Unit) {
        viewModel.onAction(DetailBookActions.LoadState(uiBook))
    }

    HandleEffects(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
        topBar = {
            AppToolbarWithBack(
                title = uiBook.title,
                onBackClick = { stackNavigation.back() }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { _ ->
        Box {
            when (val value = state.value) {
                is UiBookDetailState.Content -> {
                    BookDetailInfoUi(
                        state = value,
                        actionsHandler = viewModel,
                    )
                }

                UiBookDetailState.NoContent -> Unit
            }
        }
    }
}

@Composable
private fun HandleEffects(
    viewModel: BookDetailViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    val stackNavigation = LocalStackNavigation.current

    val downloadState = viewModel.bookDownloadedActionHandler.get()
        .sharedStateFlow
        .collectAsStateWithLifecycle(null)
        .value

    LaunchedEffect(downloadState) {
        downloadState?.let { viewModel.onAction(DetailBookActions.OnBookStateHandle(it)) }
    }

    OneTimeEffect(viewModel.oneTimeEffect) { effect ->
        when (effect) {
            is BookDetailOneTimeEffect.OpenBook -> {
                // Открываем книгу через ReaderScreen (Modo навигация)
                val uri = effect.book.fileUri
                if (uri != null) {
                    stackNavigation.forward(
                        ReaderScreen(bookUri = uri)
                    )
                }
            }

            is BookDetailOneTimeEffect.ShowErrorSnackBar -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = effect.text,
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }
}
