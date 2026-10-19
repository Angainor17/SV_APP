package su.sv.commonui.ui.linkify

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import su.sv.commonui.theme.LinkInTextColor

/**
 * Created by: androdev
 * Date: 03-11-2024
 * Time: 1:41 PM
 * Email: Kjuraev.001@mail.ru
 */
object LinkifyContentDefaults {
    val defaultSpanStyle = SpanStyle(
        color = LinkInTextColor,
        textDecoration = TextDecoration.Underline
    )

    val defaultLinkMatcher = LinkMatcher.Companion.webUrlMatcher

    val defaultWordDividers = setOf(' ', '\n')
}