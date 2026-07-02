package com.github.axet.bookreader.screens.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.geometerplus.R as FbreaderR

/**
 * Compose BottomSheet для навигации по страницам книги
 *
 * @param currentPage текущая страница
 * @param totalPages всего страниц
 * @param chapterTitle название текущей главы (опционально)
 * @param onPageChange callback при изменении страницы
 * @param onConfirm callback при подтверждении (сохранить позицию)
 * @param onCancel callback при отмене (вернуться к исходной позиции)
 * @param onDismiss callback при закрытии диалога
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationComposeDialog(
    currentPage: Int,
    totalPages: Int,
    chapterTitle: String? = null,
    onPageChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedPage by remember { mutableIntStateOf(currentPage) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        dragHandle = null, // Без drag handle для компактности
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Текст с информацией о странице и главе
            Text(
                text = buildProgressText(selectedPage, totalPages, chapterTitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Slider для навигации
            if (totalPages > 1) {
                Slider(
                    value = selectedPage.toFloat(),
                    onValueChange = { newPage ->
                        selectedPage = newPage.toInt()
                        onPageChange(selectedPage)
                    },
                    valueRange = 1f..totalPages.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопки
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        onCancel()
                        onDismiss()
                    },
                    modifier = Modifier.weight(0.45f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(stringResource(FbreaderR.string.navigation_cancel))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    modifier = Modifier.weight(0.45f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(stringResource(FbreaderR.string.navigation_ok))
                }
            }
        }
    }
}

/**
 * Формирует текст прогресса: "страница/всего  глава"
 */
private fun buildProgressText(page: Int, pagesNumber: Int, chapterTitle: String?): String {
    val builder = StringBuilder()
    builder.append(page)
    builder.append("/")
    builder.append(pagesNumber)
    if (chapterTitle != null) {
        builder.append("  ")
        builder.append(chapterTitle)
    }
    return builder.toString()
}