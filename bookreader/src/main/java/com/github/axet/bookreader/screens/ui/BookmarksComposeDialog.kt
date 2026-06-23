package com.github.axet.bookreader.screens.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.github.axet.bookreader.R
import com.github.axet.bookreader.app.Storage
import com.github.axet.bookreader.widgets.FBReaderView
import com.github.axet.bookreader.widgets.PagerWidget
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import su.sv.commonui.theme.LocalAppDimensions

/**
 * Compose диалог списка закладок
 */
@Composable
fun BookmarksComposeDialog(
    book: Storage.Book?,
    fbReaderView: FBReaderView?,
    onDismiss: () -> Unit,
    onDelete: (Storage.Bookmark) -> Unit,
) {
    val dimensions = LocalAppDimensions.current

    // Локальный список для анимации удаления
    val bookmarksState = remember { mutableStateListOf<Storage.Bookmark>() }

    // Инициализируем список из book
    val sourceBookmarks = book?.info?.bookmarks?.toList() ?: emptyList()
    if (bookmarksState.toList() != sourceBookmarks) {
        bookmarksState.clear()
        bookmarksState.addAll(sourceBookmarks)
    }

    if (bookmarksState.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.sv_bookmarks_title)) },
            text = { Text(stringResource(R.string.sv_bookmarks_empty)) },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.sv_close))
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.sv_bookmarks_title)) },
            text = {
                LazyColumn {
                    items(bookmarksState, key = { it.start.paragraphIndex.toLong() }) { bookmark ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = dimensions.itemSpacingMedium)
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Номер страницы
                            Text(
                                text = stringResource(R.string.sv_bookmark_page_prefix) + " ${bookmark.start.paragraphIndex + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = dimensions.itemSpacingMedium)
                            )

                            // Текст закладки (кликабельный)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple()
                                    ) {
                                        onDismiss()
                                        fbReaderView?.apply {
                                            // Игнорируем offset, открываем страницу с закладкой
                                            val position = ZLTextFixedPosition(
                                                bookmark.start.paragraphIndex,
                                                0,
                                                0
                                            )
                                            if (widget is PagerWidget) {
                                                // Постраничный режим
                                                gotoPosition(position)
                                                widget?.reset()
                                                widget?.repaint()
                                            } else {
                                                // Непрерывный режим - центрируем страницу
                                                gotoPositionCentered(position)
                                            }
                                        }
                                    }
                            ) {
                                Text(
                                    text = bookmark.text.take(100) + if (bookmark.text.length > 100) "..." else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!bookmark.name.isNullOrBlank()) {
                                    Text(
                                        text = bookmark.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Кнопка удаления
                            IconButton(
                                onClick = {
                                    // Удаляем из локального списка для анимации
                                    bookmarksState.remove(bookmark)
                                    // Вызываем callback для удаления из данных
                                    onDelete(bookmark)
                                },
                                modifier = Modifier.padding(start = dimensions.itemSpacingSmall)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.sv_delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(top = dimensions.itemSpacingSmall))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.sv_close))
                }
            }
        )
    }
}
