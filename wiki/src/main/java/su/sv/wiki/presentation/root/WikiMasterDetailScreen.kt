package su.sv.wiki.presentation.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import su.sv.commonui.ui.adaptive.layout.MasterDetailLayout
import su.sv.wiki.R
import su.sv.wiki.presentation.favorites.WikiFavoritesPanel
import su.sv.wiki.presentation.root.model.UiWikiState
import su.sv.wiki.presentation.root.ui.WikiArticleDetail
import su.sv.wiki.presentation.root.viewmodel.RootWikiViewModel
import su.sv.wiki.presentation.root.viewmodel.actions.WikiActions

/**
 * Экран Wiki с master-detail layout для планшетов (Expanded)
 *
 * Показывает две панели:
 * - Master (слева): список избранного
 * - Detail (справа): выбранная статья
 */
@Composable
fun WikiMasterDetailScreen(
    viewModel: RootWikiViewModel = hiltViewModel(),
) {
    val favorites by viewModel.favorites.collectAsState(initial = emptyList())
    val articleState by viewModel.state.collectAsStateWithLifecycle()
    val selectedArticleTitle by viewModel.selectedArticleTitle.collectAsStateWithLifecycle()

    MasterDetailLayout(
        master = {
            WikiFavoritesPanel(
                articles = favorites,
                isLoading = false,
                onArticleClick = { article ->
                    viewModel.onAction(WikiActions.OnFavoriteClick(article))
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
                    WikiArticleDetail(
                        article = state.article,
                        isFavorite = state.isFavorite,
                        onLinkClick = { title ->
                            viewModel.onAction(WikiActions.OnLinkClick(title))
                        },
                        onExternalLinkClick = { url ->
                            // Внешние ссылки обрабатываются в ArticleView
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
            viewModel.onAction(WikiActions.OnCloseDetail)
        },
        modifier = Modifier.fillMaxSize(),
    )
}