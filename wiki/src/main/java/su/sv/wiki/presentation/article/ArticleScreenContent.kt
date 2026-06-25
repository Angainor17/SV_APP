package su.sv.wiki.presentation.article

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import com.github.terrakok.modo.stack.forward
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.theme.SVAPPTheme
import su.sv.commonui.ui.FullScreenError
import su.sv.commonui.ui.FullScreenLoading
import su.sv.commonui.ui.components.AppToolbarWithBack
import su.sv.wiki.R
import su.sv.wiki.presentation.root.ui.ArticleContent

/**
 * Контент экрана статьи (для modo)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreenContent(
    articleTitle: String,
    viewModel: ArticleViewModel = hiltViewModel(),
) {
    val stackNavigation = LocalStackNavigation.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dimensions = LocalAppDimensions.current

    // Загружаем статью при первом отображении
    LaunchedEffect(articleTitle) {
        viewModel.loadArticle(articleTitle)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            when (val currentState = state) {
                is ArticleState.Content -> {
                    ArticleTopAppBar(
                        title = currentState.article.title,
                        isFavorite = currentState.isFavorite,
                        articleUrl = currentState.article.articleUrl,
                        onBackClick = { stackNavigation.back() },
                        onFavoriteClick = { viewModel.toggleFavorite() },
                        onExternalLinkClick = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)
                        },
                    )
                }
                else -> {
                    AppToolbarWithBack(
                        title = stringResource(R.string.wiki_loading),
                        onBackClick = { stackNavigation.back() }
                    )
                }
            }
        },
    ) { paddingValues ->
        when (val currentState = state) {
            is ArticleState.Loading -> {
                FullScreenLoading()
            }
            is ArticleState.Content -> {
                ArticleContent(
                    content = currentState.article.content,
                    links = currentState.article.links,
                    externalLinks = currentState.article.externalLinks,
                    onLinkClick = { title ->
                        stackNavigation.forward(ArticleScreen(title = title))
                    },
                    onExternalLinkClick = { url ->
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(dimensions.screenPaddingHorizontal),
                )
            }
            is ArticleState.NotFound -> {
                NotFoundContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
            is ArticleState.Error -> {
                FullScreenError {
                    viewModel.loadArticle(articleTitle)
                }
            }
        }
    }
}

/**
 * TopAppBar для экрана статьи
 */
@Composable
private fun ArticleTopAppBar(
    title: String,
    isFavorite: Boolean,
    articleUrl: String,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onExternalLinkClick: (String) -> Unit,
) {
    AppToolbarWithBack(
        title = title,
        onBackClick = onBackClick,
        actions = {
            // Кнопка избранного
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) {
                        stringResource(R.string.wiki_remove_favorite)
                    } else {
                        stringResource(R.string.wiki_add_favorite)
                    },
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
            // Кнопка открытия в браузере
            if (articleUrl.isNotEmpty()) {
                IconButton(onClick = { onExternalLinkClick(articleUrl) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_open_in_new),
                        contentDescription = stringResource(R.string.wiki_open_in_browser),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    )
}

/**
 * Контент "Ничего не найдено"
 */
@Composable
private fun NotFoundContent(modifier: Modifier = Modifier) {
    val dimensions = LocalAppDimensions.current

    Column(
        modifier = modifier.padding(dimensions.screenPaddingHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.wiki_not_found),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(dimensions.itemSpacingMedium))
        Text(
            text = stringResource(R.string.wiki_not_found_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ============================================
// Preview
// ============================================

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun ArticleTopAppBarPreview() {
    SVAPPTheme {
        ArticleTopAppBar(
            title = "Государство и революция",
            isFavorite = true,
            articleUrl = "https://svremya.su/wiki/Государство_и_революция",
            onBackClick = {},
            onFavoriteClick = {},
            onExternalLinkClick = {},
        )
    }
}

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun ArticleTopAppBarNotFavoritePreview() {
    SVAPPTheme {
        ArticleTopAppBar(
            title = "Государство и революция",
            isFavorite = false,
            articleUrl = "https://svremya.su/wiki/Государство_и_революция",
            onBackClick = {},
            onFavoriteClick = {},
            onExternalLinkClick = {},
        )
    }
}

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun NotFoundContentPreview() {
    SVAPPTheme {
        NotFoundContent(
            modifier = Modifier.fillMaxSize(),
        )
    }
}
