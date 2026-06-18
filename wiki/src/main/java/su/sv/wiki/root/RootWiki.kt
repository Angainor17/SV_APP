package su.sv.wiki.root

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import su.sv.commonui.ui.FullScreenError
import su.sv.commonui.ui.FullScreenLoading
import su.sv.commonui.ui.OneTimeEffect
import su.sv.wiki.R
import su.sv.wiki.presentation.root.model.UiWikiState
import su.sv.wiki.presentation.root.ui.ArticleView
import su.sv.wiki.presentation.root.ui.HistoryList
import su.sv.wiki.presentation.root.ui.SearchSuggestions
import su.sv.wiki.presentation.root.ui.WikiSearchBar
import su.sv.wiki.presentation.root.viewmodel.RootWikiViewModel
import su.sv.wiki.presentation.root.viewmodel.actions.WikiActions
import su.sv.wiki.presentation.root.viewmodel.effects.WikiOneTimeEffect

/**
 * Главный экран Wiki
 */
@Composable
fun RootWiki(
    viewModel: RootWikiViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsState(initial = emptyList())
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    HandleEffects(viewModel, snackbarHostState)

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding()),
        ) {
            // Поле поиска с debounce
            WikiSearchBar(
                onSearch = { query ->
                    if (query.length >= 3) {
                        viewModel.onAction(WikiActions.OnSearch(query))
                    }
                },
                onQueryChanged = { query ->
                    viewModel.onAction(WikiActions.OnSearchQueryChanged(query))
                },
            )

            // Подсказки поиска
            SearchSuggestions(
                suggestions = suggestions,
                onSuggestionClick = { title ->
                    viewModel.onAction(WikiActions.OnSuggestionClick(title))
                },
            )

            // Контент в зависимости от состояния
            when (val currentState = state) {
                is UiWikiState.Initial -> {
                    // Показываем историю
                    HistoryList(
                        history = history,
                        onItemClick = { title ->
                            viewModel.onAction(WikiActions.OnHistoryItemClick(title))
                        },
                        onClearClick = {
                            viewModel.onAction(WikiActions.OnClearHistory)
                        },
                    )
                }

                is UiWikiState.Loading -> {
                    FullScreenLoading()
                }

                is UiWikiState.Content -> {
                    ArticleView(
                        article = currentState.article,
                        isFavorite = currentState.isFavorite,
                        onLinkClick = { title ->
                            viewModel.onAction(WikiActions.OnLinkClick(title))
                        },
                        onExternalLinkClick = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)
                        },
                        onFavoriteClick = { title, isFavorite ->
                            if (isFavorite) {
                                viewModel.onAction(WikiActions.OnRemoveFavorite(title))
                            } else {
                                viewModel.onAction(WikiActions.OnAddFavorite(title))
                            }
                        },
                    )
                }

                is UiWikiState.NotFound -> {
                    NotFoundContent()
                }

                is UiWikiState.Error -> {
                    FullScreenError {
                        viewModel.onAction(WikiActions.OnRetryClick)
                    }
                }
            }
        }
    }
}

/**
 * Обработка одноразовых эффектов
 */
@Composable
private fun HandleEffects(
    viewModel: RootWikiViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    OneTimeEffect(viewModel.oneTimeEffect) { effect ->
        when (effect) {
            is WikiOneTimeEffect.ShowErrorSnackBar -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = effect.text,
                        duration = SnackbarDuration.Short,
                    )
                }
            }

            is WikiOneTimeEffect.ShowAddedToFavorites -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.wiki_added_to_favorites, effect.title),
                        duration = SnackbarDuration.Short,
                    )
                }
            }

            is WikiOneTimeEffect.ShowRemovedFromFavorites -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.wiki_removed_from_favorites, effect.title),
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }
}

/**
 * Контент "Ничего не найдено"
 */
@Composable
private fun NotFoundContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.wiki_not_found),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.wiki_not_found_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
