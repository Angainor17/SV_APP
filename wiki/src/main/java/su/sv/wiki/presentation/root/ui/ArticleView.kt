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
import su.sv.wiki.presentation.root.model.UiExternalLink
import su.sv.wiki.presentation.root.model.UiWikiArticle
import su.sv.wiki.presentation.root.model.UiWikiLink

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

            // Контент статьи с кликабельными ссылками
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

/**
 * Контент статьи с обработанными ссылками
 */
@Composable
private fun ArticleContent(
    content: String,
    links: List<UiWikiLink>,
    externalLinks: List<UiExternalLink>,
    onLinkClick: (String) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val annotatedContent = buildAnnotatedContent(
        htmlContent = content,
        links = links,
        externalLinks = externalLinks,
        onLinkClick = onLinkClick,
        onExternalLinkClick = onExternalLinkClick,
    )

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
    links: List<UiWikiLink>,
    externalLinks: List<UiExternalLink>,
    onLinkClick: (String) -> Unit,
    onExternalLinkClick: (String) -> Unit,
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

        // Собираем все позиции ссылок (внутренние и внешние)
        data class LinkPosition(
            val startIndex: Int,
            val endIndex: Int,
            val isExternal: Boolean,
            val url: String = "",
            val targetTitle: String = "",
        )

        val linkPositions = mutableListOf<LinkPosition>()

        // Находим позиции внутренних ссылок
        for (link in links) {
            val startIndex = plainText.indexOf(link.text, ignoreCase = true)
            if (startIndex >= 0) {
                linkPositions.add(
                    LinkPosition(
                        startIndex = startIndex,
                        endIndex = startIndex + link.text.length,
                        isExternal = false,
                        targetTitle = link.targetTitle,
                    )
                )
            }
        }

        // Находим позиции внешних ссылок
        for (link in externalLinks) {
            val startIndex = plainText.indexOf(link.text, ignoreCase = true)
            if (startIndex >= 0) {
                linkPositions.add(
                    LinkPosition(
                        startIndex = startIndex,
                        endIndex = startIndex + link.text.length,
                        isExternal = true,
                        url = link.url,
                    )
                )
            }
        }

        // Сортируем по позиции
        linkPositions.sortBy { it.startIndex }

        // Строим текст с ссылками
        var currentIndex = 0

        for (linkPos in linkPositions) {
            // Пропускаем пересекающиеся ссылки
            if (linkPos.startIndex < currentIndex) continue

            // Добавляем текст до ссылки
            if (linkPos.startIndex > currentIndex) {
                append(plainText.substring(currentIndex, linkPos.startIndex))
            }

            val linkText = plainText.substring(linkPos.startIndex, linkPos.endIndex)

            if (linkPos.isExternal) {
                // Внешняя ссылка - открываем в браузере
                withLink(LinkAnnotation.Clickable(tag = linkPos.url) {
                    onExternalLinkClick(linkPos.url)
                }) {
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.tertiary,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        ),
                    ) {
                        append(linkText)
                    }
                }
            } else {
                // Внутренняя ссылка - открываем статью
                withLink(LinkAnnotation.Clickable(tag = linkPos.targetTitle) {
                    onLinkClick(linkPos.targetTitle)
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
            }

            currentIndex = linkPos.endIndex
        }

        // Добавляем оставшийся текст
        if (currentIndex < plainText.length) {
            append(plainText.substring(currentIndex))
        }
    }
}