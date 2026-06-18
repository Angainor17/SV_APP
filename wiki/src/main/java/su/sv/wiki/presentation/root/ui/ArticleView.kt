package su.sv.wiki.presentation.root.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.sv.wiki.R
import su.sv.wiki.presentation.root.model.UiWikiArticle

/**
 * Карточка статьи с кликабельными ссылками
 */
@Composable
fun ArticleView(
    article: UiWikiArticle,
    isFavorite: Boolean,
    onLinkClick: (String) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Заголовок и кнопка избранного
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )

                IconButton(onClick = { onFavoriteClick(article.title, isFavorite) }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) {
                            stringResource(R.string.wiki_remove_favorite)
                        } else {
                            stringResource(R.string.wiki_add_favorite)
                        },
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.Gray,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Контент статьи с кликабельными ссылками (общий компонент)
            ArticleContent(
                content = article.content,
                links = article.links,
                externalLinks = article.externalLinks,
                onLinkClick = onLinkClick,
                onExternalLinkClick = onExternalLinkClick,
            )
        }
    }
}
