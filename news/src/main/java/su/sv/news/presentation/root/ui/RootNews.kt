package su.sv.news.presentation.root.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.stack.LocalStackNavigation
import kotlinx.coroutines.launch
import su.sv.commonui.ui.LoadingIndicator
import su.sv.commonui.ui.OneTimeEffect
import su.sv.news.R
import su.sv.news.presentation.root.RootNewsViewModel
import su.sv.news.presentation.root.model.UiRootNewsState
import su.sv.news.presentation.root.viewmodel.actions.RootNewsActions
import su.sv.news.presentation.root.viewmodel.actions.RootNewsActionsHandler
import su.sv.news.presentation.root.viewmodel.effects.NewsListOneTimeEffect
import su.sv.commonui.R as CommonR

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RootNews(viewModel: RootNewsViewModel = hiltViewModel()) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    HandleEffects(viewModel, snackbarHostState)

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { contentPadding ->
        when (state.value) {
            is UiRootNewsState.Content -> {
                BookList(
                    actions = viewModel,
                    state = state.value as UiRootNewsState.Content,
                )
            }

            UiRootNewsState.EmptyState -> {
                NoNews()
            }

            UiRootNewsState.Loading -> {
                Loading()
            }

            is UiRootNewsState.Failure -> {
                Error(actions = viewModel)
            }
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
fun Error(actions: RootNewsActionsHandler) {
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
            onClick = { actions.onAction(RootNewsActions.OnRetryClick) },
        ) {
            Text(
                text = stringResource(CommonR.string.common_retry),
            )
        }
    }
}

@Composable
private fun HandleEffects(
    viewModel: RootNewsViewModel,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val stackNavigation = LocalStackNavigation.current

    OneTimeEffect(viewModel.oneTimeEffect) { effect ->
        when (effect) {
            is NewsListOneTimeEffect.OpenNewsItem -> {
//                stackNavigation.forward(BookDetailScreen(effect.book))
            }

            is NewsListOneTimeEffect.ShowErrorSnackBar -> {
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

@Composable
fun NoNews() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(modifier = Modifier.wrapContentSize()) {
            Text(stringResource(R.string.news_empty_list_title))
        }
    }
}
