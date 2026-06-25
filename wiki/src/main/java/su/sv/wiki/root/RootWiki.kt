package su.sv.wiki.root

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.forward
import kotlinx.coroutines.launch
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.ui.OneTimeEffect
import su.sv.commonui.ui.components.FullScreenError
import su.sv.commonui.ui.components.FullScreenLoading
import su.sv.wiki.R
import su.sv.wiki.presentation.article.ArticleScreen
import su.sv.wiki.presentation.favorites.FavoritesScreen
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
    val hasFavorites by viewModel.hasFavorites.collectAsState(initial = false)
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val stackNavigation = LocalStackNavigation.current
    val focusManager = LocalFocusManager.current
    val dimensions = LocalAppDimensions.current

    HandleEffects(viewModel, snackbarHostState)

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(bottom = paddingValues.calculateBottomPadding())
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    focusManager.clearFocus()
                },
        ) {
            // Поле поиска с иконкой избранного
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WikiSearchBar(
                    onSearch = { query ->
                        if (query.length >= 3) {
                            viewModel.onAction(WikiActions.OnSearch(query))
                        }
                    },
                    onQueryChanged = { query ->
                        viewModel.onAction(WikiActions.OnSearchQueryChanged(query))
                    },
                    onClearClick = {
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.weight(1f),
                )

                // Иконка избранного с анимацией
                AnimatedVisibility(
                    visible = hasFavorites,
                    enter = fadeIn(
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                    ) + scaleIn(
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        initialScale = 0.8f,
                    ),
                    exit = fadeOut(
                        animationSpec = tween(200, easing = FastOutSlowInEasing),
                    ) + scaleOut(
                        animationSpec = tween(200, easing = FastOutSlowInEasing),
                        targetScale = 0.8f,
                    ),
                ) {
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            stackNavigation.forward(FavoritesScreen())
                        },
                        modifier = Modifier.padding(end = dimensions.screenPaddingHorizontal / 2),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = stringResource(R.string.wiki_favorites),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Подсказки поиска
            SearchSuggestions(
                suggestions = suggestions,
                onSuggestionClick = { title ->
                    focusManager.clearFocus()
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
                            focusManager.clearFocus()
                            stackNavigation.forward(ArticleScreen(title = title))
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
                    FullScreenError(
                        onRetry = { viewModel.onAction(WikiActions.OnRetryClick) }
                    )
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
    val dimensions = LocalAppDimensions.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.wiki_not_found),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(dimensions.itemSpacingMedium))
            Text(
                text = stringResource(R.string.wiki_not_found_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
