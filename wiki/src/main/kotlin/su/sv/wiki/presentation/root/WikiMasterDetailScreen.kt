package su.sv.wiki.presentation.root

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import su.sv.commonui.theme.LocalAdaptiveDimensions
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.theme.favorite
import su.sv.commonui.ui.adaptive.layout.MasterDetailLayout
import su.sv.commonui.ui.components.FullScreenError
import su.sv.commonui.ui.components.FullScreenLoading
import su.sv.wiki.R
import su.sv.wiki.presentation.favorites.FavoritesScreen
import su.sv.wiki.presentation.root.model.UiWikiState
import su.sv.wiki.presentation.root.ui.ArticleView
import su.sv.wiki.presentation.root.ui.HistoryList
import su.sv.wiki.presentation.root.ui.SearchSuggestions
import su.sv.wiki.presentation.root.ui.WikiSearchBar
import su.sv.wiki.presentation.root.viewmodel.RootWikiViewModel
import su.sv.wiki.presentation.root.viewmodel.actions.WikiActions

/**
 * Экран Wiki с master-detail layout для планшетов (Expanded)
 *
 * Показывает две панели:
 * - Master (слева): поиск + история поиска
 * - Detail (справа): выбранная статья
 */
@Composable
fun WikiMasterDetailScreen(
    viewModel: RootWikiViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsState(initial = emptyList())
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val selectedSuggestion by viewModel.selectedSuggestion.collectAsStateWithLifecycle()
    val hasFavorites by viewModel.hasFavorites.collectAsState(initial = false)
    val selectedArticleTitle by viewModel.selectedArticleTitle.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val stackNavigation = LocalStackNavigation.current
    val focusManager = LocalFocusManager.current
    val dimensions = LocalAppDimensions.current
    val adaptiveDims = LocalAdaptiveDimensions.current

    val hints = remember {
        context.resources.getStringArray(R.array.wiki_search_hints).toList()
    }

    // Подсказки поиска - фильтруем suggestion совпадающую с текущей статьёй
    val currentArticleTitle = if (state is UiWikiState.Content) {
        (state as UiWikiState.Content).article.title
    } else null

    val filteredSuggestions = suggestions.filter { suggestion ->
        suggestion != currentArticleTitle
    }

    MasterDetailLayout(
        master = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        focusManager.clearFocus()
                    },
            ) {
                // Поле поиска с иконкой избранного
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions.screenPaddingHorizontal)
                        .padding(top = dimensions.screenPaddingHorizontal),
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
                        hints = hints,
                        isSuggestionsVisible = suggestions.isNotEmpty(),
                        selectedSuggestion = selectedSuggestion,
                        onSuggestionApplied = {
                            viewModel.onAction(WikiActions.OnSuggestionApplied)
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
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = stringResource(R.string.wiki_favorites),
                                tint = MaterialTheme.colorScheme.favorite,
                            )
                        }
                    }
                }

                // Подсказки поиска
                if (selectedSuggestion == null && filteredSuggestions.isNotEmpty()) {
                    SearchSuggestions(
                        suggestions = filteredSuggestions,
                        onSuggestionClick = { title ->
                            focusManager.clearFocus()
                            viewModel.onAction(WikiActions.OnSuggestionClick(title))
                        },
                    )
                }

                Spacer(modifier = Modifier.height(dimensions.itemSpacingMedium))

                // История поиска
                HistoryList(
                    history = history,
                    onItemClick = { title ->
                        focusManager.clearFocus()
                        viewModel.onAction(WikiActions.OnHistoryItemClick(title))
                    },
                    onClearClick = {
                        viewModel.onAction(WikiActions.OnClearHistory)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        },
        detail = {
            when (val currentState = state) {
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
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                is UiWikiState.Error -> {
                    FullScreenError(
                        onRetry = { viewModel.onAction(WikiActions.OnRetryClick) }
                    )
                }

                else -> {
                    // Initial, NotFound - пустое состояние
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.wiki_favorites_select_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        isDetailVisible = selectedArticleTitle != null,
        detailTitle = selectedArticleTitle,
        onCloseDetail = {
            viewModel.onAction(WikiActions.OnCloseDetail)
        },
        modifier = Modifier.fillMaxSize(),
    )
}