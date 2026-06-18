package su.sv.wiki.presentation.article

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import com.github.terrakok.modo.stack.forward
import su.sv.commonui.ui.FullScreenError
import su.sv.commonui.ui.FullScreenLoading
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

    Scaffold(
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
                    CenterAlignedTopAppBar(
                        title = { Text(stringResource(R.string.wiki_loading)) },
                        navigationIcon = {
                            IconButton(onClick = { stackNavigation.back() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.wiki_back),
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
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
                        .padding(16.dp),
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
                    viewModel.loadArticle(viewModel.articleTitle)
                }
            }
        }
    }
}

/**
 * TopAppBar для экрана статьи
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleTopAppBar(
    title: String,
    isFavorite: Boolean,
    articleUrl: String,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onExternalLinkClick: (String) -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.wiki_back),
                )
            }
        },
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
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Кнопка открытия в браузере
            if (articleUrl.isNotEmpty()) {
                IconButton(onClick = { onExternalLinkClick(articleUrl) }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.wiki_open_in_browser),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

/**
 * Контент "Ничего не найдено"
 */
@Composable
private fun NotFoundContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
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
