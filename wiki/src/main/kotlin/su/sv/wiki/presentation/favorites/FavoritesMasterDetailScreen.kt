package su.sv.wiki.presentation.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import su.sv.commonui.ui.adaptive.layout.MasterDetailLayout
import su.sv.commonui.ui.components.AppToolbarWithBack
import su.sv.wiki.R
import su.sv.wiki.presentation.root.model.UiWikiState
import su.sv.wiki.presentation.root.ui.ArticleView
import su.sv.wiki.presentation.root.viewmodel.RootWikiViewModel
import su.sv.wiki.presentation.root.viewmodel.actions.WikiActions

/**
 * Экран Избранного с master-detail layout для планшетов (Expanded)
 *
 * Показывает две панели:
 * - Master (слева): список избранного
 * - Detail (справа): выбранная статья
 */
@Composable
fun FavoritesMasterDetailScreen(
    rootViewModel: RootWikiViewModel = hiltViewModel(),
    favoritesViewModel: FavoritesViewModel = hiltViewModel(),
) {
    val favorites by favoritesViewModel.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    val articleState by rootViewModel.state.collectAsStateWithLifecycle()
    val selectedArticleTitle by rootViewModel.selectedArticleTitle.collectAsStateWithLifecycle()
    val stackNavigation = LocalStackNavigation.current

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            AppToolbarWithBack(
                title = stringResource(R.string.wiki_favorites_title),
                onBackClick = { stackNavigation.back() }
            )
        },
    ) { paddingValues ->
        MasterDetailLayout(
            master = {
                WikiFavoritesPanel(
                    articles = favorites,
                    isLoading = false,
                    onArticleClick = { article ->
                        rootViewModel.onAction(WikiActions.OnFavoriteClick(article))
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            },
            detail = {
                when (val state = articleState) {
                    is UiWikiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is UiWikiState.Content -> {
                        ArticleView(
                            article = state.article,
                            isFavorite = state.isFavorite,
                            onLinkClick = { title ->
                                rootViewModel.onAction(WikiActions.OnLinkClick(title))
                            },
                            onExternalLinkClick = { url ->
                                // Внешние ссылки обрабатываются в ArticleView
                            },
                            onFavoriteClick = { title, isFavorite ->
                                if (isFavorite) {
                                    rootViewModel.onAction(WikiActions.OnRemoveFavorite(title))
                                } else {
                                    rootViewModel.onAction(WikiActions.OnAddFavorite(title))
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    is UiWikiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.wiki_error_loading),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
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
                rootViewModel.onAction(WikiActions.OnCloseDetail)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        )
    }
}