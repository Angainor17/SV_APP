package com.github.axet.bookreader.screens.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.axet.bookreader.R
import com.github.axet.bookreader.screens.testing.ReaderTestTags
import com.github.axet.bookreader.widgets.FBReaderView
import org.geometerplus.fbreader.bookmodel.TOCTree
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextPosition

/**
 * Compose диалог содержания (TOC) с вложенной иерархией
 */
@Composable
internal fun TocComposeDialog(
    fbReaderView: FBReaderView?,
    onDismiss: () -> Unit,
    onNavigate: (ZLTextPosition) -> Unit,
) {
    // Собираем TOC элементы с информацией о дочерних элементах
    val tocItems = remember(fbReaderView) {
        val items = mutableListOf<ExpandableTocItem>()
        fbReaderView?.app?.Model?.TOCTree?.let { tree ->
            collectExpandableTocItems(tree, items, 0)
        }
        items
    }

    // Состояние раскрытия для каждого элемента
    val expandedStates = remember { mutableStateListOf<String>() }

    // Функция для проверки видимости элемента
    fun isItemVisible(item: ExpandableTocItem): Boolean {
        // Элементы уровня 0 всегда видимы
        if (item.parentId == null) return true
        // Проверяем все родительские цепочки
        var currentParentId: String? = item.parentId
        while (currentParentId != null) {
            if (!expandedStates.contains(currentParentId)) return false
            // Находим parentId родителя
            val parentItem = tocItems.find { it.id == currentParentId }
            currentParentId = parentItem?.parentId
        }
        return true
    }

    if (tocItems.isEmpty()) {
        AlertDialog(
            modifier = Modifier.testTag(ReaderTestTags.Toc.DIALOG),
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.sv_toc_title)) },
            text = {
                Text(
                    modifier = Modifier.testTag(ReaderTestTags.Toc.EMPTY_STATE),
                    text = stringResource(R.string.sv_toc_not_available)
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.sv_close))
                }
            }
        )
    } else {
        AlertDialog(
            modifier = Modifier.testTag(ReaderTestTags.Toc.DIALOG),
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.sv_toc_title)) },
            text = {
                LazyColumn {
                    items(tocItems, key = { it.id }) { item ->
                        // Анимированное появление/исчезновение
                        AnimatedVisibility(
                            visible = isItemVisible(item),
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            TocItemRow(
                                item = item,
                                isExpanded = expandedStates.contains(item.id),
                                onToggleExpand = {
                                    if (expandedStates.contains(item.id)) {
                                        expandedStates.remove(item.id)
                                    } else {
                                        expandedStates.add(item.id)
                                    }
                                },
                                onNavigate = onNavigate
                            )
                        }
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

/**
 * Элемент оглавления с возможностью раскрытия
 */
@Composable
private fun TocItemRow(
    item: ExpandableTocItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onNavigate: (ZLTextPosition) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate(item.position) }
            .padding(
                vertical = 12.dp,
                horizontal = 8.dp + (item.level * 16).dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Иконка главы
        Icon(
            imageVector = Icons.AutoMirrored.Filled.List,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        // Заголовок главы
        Text(
            text = item.title,
            fontWeight = if (item.level == 0) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        // Иконка раскрытия/закрытия для элементов с дочерними
        if (item.hasChildren) {
            IconButton(onClick = onToggleExpand) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(
                        if (isExpanded) R.string.sv_collapse_content else R.string.sv_expand_content
                    ),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Модель элемента оглавления с поддержкой раскрытия
 */
private data class ExpandableTocItem(
    val id: String,
    val title: String,
    val position: ZLTextPosition,
    val level: Int = 0,
    val hasChildren: Boolean = false,
    val parentId: String? = null,  // ID родительского элемента
)

private fun collectExpandableTocItems(
    tree: TOCTree,
    items: MutableList<ExpandableTocItem>,
    level: Int,
    parentId: String? = null
) {
    for (child in tree.subtrees()) {
        val text = child.text
        val ref = child.reference
        if (text != null && ref != null) {
            val hasChildren = child.subtrees().iterator().hasNext()
            val itemId = "${level}_${ref.ParagraphIndex}_${text.hashCode()}"
            items.add(
                ExpandableTocItem(
                    id = itemId,
                    title = text,
                    position = ZLTextFixedPosition(ref.ParagraphIndex, 0, 0),
                    level = level,
                    hasChildren = hasChildren,
                    parentId = parentId
                )
            )
            // Рекурсивно собираем дочерние элементы с текущим parentId
            collectExpandableTocItems(child, items, level + 1, itemId)
        } else {
            // Если нет текста/рефа, продолжаем обход с тем же parentId
            collectExpandableTocItems(child, items, level, parentId)
        }
    }
}
