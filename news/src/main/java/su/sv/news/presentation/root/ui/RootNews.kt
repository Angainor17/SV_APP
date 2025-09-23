package su.sv.news.presentation.root.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.launch
import su.sv.commonui.ui.FullScreenError
import su.sv.commonui.ui.FullScreenLoading
import su.sv.commonui.ui.OneTimeEffect
import su.sv.news.R
import su.sv.news.presentation.root.RootNewsViewModel
import su.sv.news.presentation.root.model.UiItemVideo
import su.sv.news.presentation.root.viewmodel.actions.RootNewsActions
import su.sv.news.presentation.root.viewmodel.effects.NewsListOneTimeEffect

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RootNews(viewModel: RootNewsViewModel = hiltViewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }

    val lazyPagingItems = viewModel.pagingDataFlow.collectAsLazyPagingItems()
    val loadState = lazyPagingItems.loadState.refresh

    HandleEffects(viewModel, snackbarHostState)

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { contentPadding ->
        val hasItems = lazyPagingItems.itemSnapshotList.isNotEmpty()
        when {
            loadState == LoadState.Loading && !hasItems -> {
                FullScreenLoading()
            }

            loadState is LoadState.Error && !hasItems -> {
                FullScreenError {
                    lazyPagingItems.refresh()
                }
            }

            else -> {
                val state = viewModel.state.collectAsStateWithLifecycle()
                val stateValue = state.value

                if (stateValue.isRefreshing) {
                    viewModel.onAction(RootNewsActions.OnSwipeRefreshFinished)
                }

                if (hasItems) {
                    NewsList(
                        lazyPagingItems = lazyPagingItems,
                        actions = viewModel,
                        state = stateValue,
                    )
                } else {
                    NoNews()
                }
            }
        }
    }
}

@Composable
private fun HandleEffects(
    viewModel: RootNewsViewModel,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    OneTimeEffect(viewModel.oneTimeEffect) { effect ->
        when (effect) {
            is NewsListOneTimeEffect.OpenNewsItem -> {
                // TODO open news
            }

            is NewsListOneTimeEffect.ShowErrorSnackBar -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = effect.text,
                        duration = SnackbarDuration.Short,
                    )
                }
            }

            is NewsListOneTimeEffect.OpenNewsVideo -> {
                context.openVideo(effect.item)
            }
        }
    }
}

private fun Context.openVideo(item: UiItemVideo) {
    val builder: CustomTabsIntent.Builder = CustomTabsIntent.Builder()
//    builder.
//    val url = item.link
    val url = "https://vk.com/video_ext.php?oid=-206226873&id=456239875&hash=e31639c922357e44"

    // Optional: Customize the appearance and behavior of the Custom Tab
    // builder.setToolbarColor(ContextCompat.getColor(this, R.color.my_toolbar_color));
    // builder.setShowTitle(true);
    // builder.addMenuItem("Share", pendingIntent); // Add a custom menu item
    val customTabsIntent: CustomTabsIntent = builder.build()
    customTabsIntent.launchUrl(this, url.toUri())
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
