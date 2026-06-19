package su.sv.wiki.presentation.root.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import su.sv.commonui.theme.SVAPPTheme
import su.sv.wiki.presentation.root.model.UiExternalLink
import su.sv.wiki.presentation.root.model.UiWikiLink

/**
 * Контент статьи с обработанными ссылками (общий компонент)
 */
@Composable
fun ArticleContent(
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
) = buildAnnotatedString {
    // Убираем HTML теги и оставляем текст
    val plainText = htmlContent
        .replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .trim()

    // Собираем все позиции ссылок
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
        if (linkPos.startIndex < currentIndex) continue

        if (linkPos.startIndex > currentIndex) {
            append(plainText.substring(currentIndex, linkPos.startIndex))
        }

        val linkText = plainText.substring(linkPos.startIndex, linkPos.endIndex)

        if (linkPos.isExternal) {
            withLink(LinkAnnotation.Clickable(tag = linkPos.url) {
                onExternalLinkClick(linkPos.url)
            }) {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.tertiary,
                        textDecoration = TextDecoration.Underline,
                    ),
                ) {
                    append(linkText)
                }
            }
        } else {
            withLink(LinkAnnotation.Clickable(tag = linkPos.targetTitle) {
                onLinkClick(linkPos.targetTitle)
            }) {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                    ),
                ) {
                    append(linkText)
                }
            }
        }

        currentIndex = linkPos.endIndex
    }

    if (currentIndex < plainText.length) {
        append(plainText.substring(currentIndex))
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
fun ArticleContentPreview() {
    SVAPPTheme {
        ArticleContent(
            content = "Государство и революция — работа В. И. Ленина, написанная в 1917 году. " +
                "В этом труде Ленин развивает марксистское учение о государстве и определяет " +
                "задачи пролетариата в революции.",
            links = listOf(
                UiWikiLink(
                    text = "Ленина",
                    targetTitle = "Ленин",
                    exists = true,
                ),
            ),
            externalLinks = listOf(
                UiExternalLink(
                    text = "марксистское",
                    url = "https://ru.wikipedia.org/wiki/Марксизм",
                ),
            ),
            onLinkClick = {},
            onExternalLinkClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
