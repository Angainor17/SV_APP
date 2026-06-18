package su.sv.wiki.presentation.root.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
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

            // Контент статьи с кликабельными ссылками
            ArticleContent(
                content = article.content,
                links = article.links,
                onLinkClick = onLinkClick,
            )
        }
    }
}

/**
 * Контент статьи с обработанными ссылками
 */
@Composable
private fun ArticleContent(
    content: String,
    links: List<su.sv.wiki.presentation.root.model.UiWikiLink>,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val annotatedContent = buildAnnotatedContent(content, links, onLinkClick)

    SelectionContainer {
        Text(
            text = annotatedContent,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = modifier.verticalScroll(rememberScrollState()),
        )
    }
}

/**
 * Создаёт AnnotatedString из HTML с кликабельными ссылками
 */
@Composable
private fun buildAnnotatedContent(
    htmlContent: String,
    links: List<su.sv.wiki.presentation.root.model.UiWikiLink>,
    onLinkClick: (String) -> Unit,
): AnnotatedString {
    return buildAnnotatedString {
        // Убираем HTML теги и оставляем текст
        val plainText = htmlContent
            .replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .trim()

        // Добавляем текст с кликабельными ссылками
        var currentIndex = 0
        val sortedLinks = links.sortedBy { plainText.indexOf(it.text, ignoreCase = true) }

        for (link in sortedLinks) {
            val linkText = link.text
            val startIndex = plainText.indexOf(linkText, currentIndex, ignoreCase = true)

            if (startIndex >= 0) {
                // Добавляем текст до ссылки
                if (startIndex > currentIndex) {
                    append(plainText.substring(currentIndex, startIndex))
                }

                // Добавляем ссылку с помощью LinkAnnotation
                withLink(LinkAnnotation.Clickable(tag = link.targetTitle) {
                    onLinkClick(link.targetTitle)
                }) {
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        ),
                    ) {
                        append(linkText)
                    }
                }

                currentIndex = startIndex + linkText.length
            }
        }

        // Добавляем оставшийся текст
        if (currentIndex < plainText.length) {
            append(plainText.substring(currentIndex))
        }
    }
}
