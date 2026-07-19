package com.github.axet.bookreader.screens.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.axet.bookreader.screens.testing.ReaderTestTags
import org.geometerplus.R as FbreaderR

/**
 * Compose панель для действий с выделенным текстом
 *
 * @param onBookmark callback для создания закладки
 * @param onShare callback для分享а
 * @param onCopy callback для копирования
 * @param onQuestion callback для вопроса
 * @param onAlert callback для сообщения об опечатке
 * @param onClose callback для закрытия
 */
@Composable
fun SelectionComposePanel(
    onBookmark: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onQuestion: () -> Unit,
    onAlert: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.testTag(ReaderTestTags.Selection.PANEL),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Bookmark
            SelectionIconButton(
                modifier = Modifier.testTag(ReaderTestTags.Selection.BOOKMARK_BUTTON),
                iconRes = FbreaderR.drawable.ic_bookmark_outline_white,
                contentDescription = stringResource(FbreaderR.string.panel_bookmark),
                onClick = onBookmark
            )

            // Share
            SelectionIconButton(
                modifier = Modifier.testTag(ReaderTestTags.Selection.SHARE_BUTTON),
                iconRes = FbreaderR.drawable.ic_share_white,
                contentDescription = stringResource(FbreaderR.string.panel_share),
                onClick = onShare
            )

            // Copy
            SelectionIconButton(
                modifier = Modifier.testTag(ReaderTestTags.Selection.COPY_BUTTON),
                iconRes = FbreaderR.drawable.ic_content_copy_white,
                contentDescription = stringResource(FbreaderR.string.panel_copy),
                onClick = onCopy
            )

            // Question
            SelectionIconButton(
                modifier = Modifier.testTag(ReaderTestTags.Selection.QUESTION_BUTTON),
                iconRes = FbreaderR.drawable.baseline_question_mark_24,
                contentDescription = stringResource(FbreaderR.string.panel_question),
                onClick = onQuestion
            )

            // Alert (misspell)
            SelectionIconButton(
                modifier = Modifier.testTag(ReaderTestTags.Selection.ALERT_BUTTON),
                iconRes = FbreaderR.drawable.ic_missplell,
                contentDescription = stringResource(FbreaderR.string.panel_misspell),
                onClick = onAlert
            )

            // Close
            SelectionIconButton(
                modifier = Modifier.testTag(ReaderTestTags.Selection.CLOSE_BUTTON),
                iconRes = FbreaderR.drawable.ic_close_white,
                contentDescription = stringResource(FbreaderR.string.panel_close),
                onClick = onClose
            )
        }
    }
}

@Composable
private fun SelectionIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}