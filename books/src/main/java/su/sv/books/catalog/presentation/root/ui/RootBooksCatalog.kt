package su.sv.books.catalog.presentation.root.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import su.sv.books.R
import su.sv.books.catalog.presentation.root.model.UiRootBooksState
import su.sv.books.catalog.presentation.root.viewmodel.RootBooksCatalogViewModel
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBookActions
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBooksActions
import su.sv.books.catalog.presentation.root.viewmodel.effects.BooksListOneTimeEffect
import su.sv.commonui.ui.LoadingIndicator
import su.sv.commonui.ui.OneTimeEffect
import su.sv.commonui.R as CommonR

@Composable
fun RootBooksCatalog(
    navController: NavHostController, // TODO
    viewModel: RootBooksCatalogViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    HandleEffects(viewModel)

    when (state.value) {
        is UiRootBooksState.Content -> {
            BookList(
                actions = viewModel,
                state = state.value as UiRootBooksState.Content,
            )
        }
        UiRootBooksState.EmptyState -> {
            NoBooks()
        }
        UiRootBooksState.Loading -> {
            Loading()
        }
        is UiRootBooksState.Failure -> {
            Error(actions = viewModel)
        }
    }
}

@Composable
private fun HandleEffects(viewModel: RootBooksCatalogViewModel) {
    OneTimeEffect(viewModel.oneTimeEffect) { effect ->
        when (effect) {
            is BooksListOneTimeEffect.ShowErrorSnackBar -> {
//                Toast.makeText(, "", Toast.LENGTH_SHORT).show()

//                val text  = LocalContext.current.getString(effect.textResId)
                // TODO: show snackbar
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

@Composable
fun Loading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        LoadingIndicator()
    }
}

@Composable
fun Error(actions: RootBooksActions) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(CommonR.string.common_error_title),
        )
        Button(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 6.dp,
            ),
            onClick = { actions.onAction(RootBookActions.OnRetryClick) },
        ) {
            Text(
                text = stringResource(CommonR.string.common_retry),
            )
        }
    }
}
