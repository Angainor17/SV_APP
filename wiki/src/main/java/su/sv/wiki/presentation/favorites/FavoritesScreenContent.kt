package su.sv.wiki.presentation.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import com.github.terrakok.modo.stack.forward
import su.sv.commonui.theme.SVAPPTheme
import su.sv.commonui.ui.components.AppAlertDialog
import su.sv.commonui.ui.components.AppToolbarWithBack
import su.sv.commonui.ui.components.FullScreenEmpty
import su.sv.wiki.R
import su.sv.wiki.domain.model.WikiArticle
import su.sv.wiki.presentation.article.ArticleScreen

/**
 * Контент экрана избранного (для modo)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreenContent(
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val stackNavigation = LocalStackNavigation.current
    val favorites by viewModel.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    var showClearDialog by remember { mutableStateOf(false) }

    // Диалог подтверждения очистки
    if (showClearDialog) {
        ClearFavoritesDialog(
            onConfirm = {
                showClearDialog = false
                viewModel.clearFavorites()
                stackNavigation.back()
            },
            onDismiss = {
                showClearDialog = false
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            AppToolbarWithBack(
                title = stringResource(R.string.wiki_favorites_title),
                onBackClick = { stackNavigation.back() },
                actions = {
                    if (favorites.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.wiki_favorites_clear),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        },
    ) { paddingValues ->
        if (favorites.isEmpty()) {
            FullScreenEmpty(
                title = stringResource(R.string.wiki_favorites_empty),
                icon = Icons.Default.Favorite,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
            ) {
                items(
                    items = favorites,
                    key = { article -> article.title },
                ) { article ->
                    FavoriteItem(
                        article = article,
                        onClick = {
                            stackNavigation.forward(ArticleScreen(title = article.title))
                        },
                    )
                }
            }
        }
    }
}

/**
 * Элемент списка избранного
 */
@Composable
private fun FavoriteItem(
    article: WikiArticle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Картинка статьи (если есть)
            if (article.imageUrl != null) {
                SubcomposeAsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .width(80.dp)
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    loading = {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (article.imageUrl != null) 12.dp else 0.dp)
            ) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Описание статьи (extract plain text from HTML)
                val description = remember(article.content) {
                    extractPlainText(article.content)
                }

                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/**
 * Извлечь plain text из HTML контента, удалив картинку и её URL
 */
private fun extractPlainText(html: String): String {
    // 1. Удаляем img теги и их содержимое
    var text = html.replace(Regex("<img[^>]*>"), "")

    // 2. Удаляем ссылки на картинки (типичные MediaWiki форматы)
    // Удаляем URL картинки который может быть в тексте
    text = text.replace(Regex("https?://[^\\s<>\"']+\\.(jpg|jpeg|png|gif|webp|bmp)[^\\s<>\"']*"), "")

    // 3. Удаляем HTML теги
    text = text.replace(Regex("<[^>]*>"), "")

    // 4. Удаляем HTML entities
    text = text
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")

    // 5. Убираем лишние пробелы и пустые строки
    text = text.replace(Regex("\\s+"), " ").trim()

    // 6. Ограничиваем длину для превью
    return if (text.length > 200) {
        text.substring(0, 200) + "..."
    } else {
        text
    }
}

/**
 * Диалог подтверждения очистки избранного
 */
@Composable
private fun ClearFavoritesDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        title = stringResource(R.string.wiki_favorites_clear_confirm),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        confirmText = stringResource(R.string.wiki_favorites_clear_yes),
        dismissText = stringResource(R.string.wiki_favorites_clear_no),
    )
}

// ============================================
// Preview
// ============================================

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun FavoriteItemPreview() {
    SVAPPTheme {
        FavoriteItem(
            article = WikiArticle(
                title = "Государство и революция",
                pageId = 1,
                content = "Государство и революция — классическая работа В.И. Ленина...",
                links = emptyList(),
                externalLinks = emptyList(),
                imageUrl = "https://example.com/image.jpg",
            ),
            onClick = {},
        )
    }
}

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun FavoriteItemNoImagePreview() {
    SVAPPTheme {
        FavoriteItem(
            article = WikiArticle(
                title = "Марксизм",
                pageId = 2,
                content = "Марксизм — система взглядов...",
                links = emptyList(),
                externalLinks = emptyList(),
                imageUrl = null,
            ),
            onClick = {},
        )
    }
}

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun ClearFavoritesDialogPreview() {
    SVAPPTheme {
        ClearFavoritesDialog(
            onConfirm = {},
            onDismiss = {},
        )
    }
}
