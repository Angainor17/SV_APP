package com.github.axet.bookreader.screens.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.axet.bookreader.R

/**
 * Compose панель для поиска в книге
 *
 * @param searchQuery текущий запрос поиска
 * @param onQueryChange callback при изменении запроса
 * @param resultsCount количество найденных результатов
 * @param currentResultIndex индекс текущего результата (0-based)
 * @param onPrevious callback для перехода к предыдущему результату
 * @param onNext callback для перехода к следующему результату
 * @param onClose callback для закрытия поиска
 */
@Composable
fun SearchComposePanel(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    resultsCount: Int,
    currentResultIndex: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                singleLine = true,
                modifier = Modifier.width(200.dp)
            )

            // Results counter and navigation
            if (resultsCount > 0) {
                Text(
                    text = "${currentResultIndex + 1}/${resultsCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Previous button
                IconButton(
                    onClick = onPrevious,
                    enabled = currentResultIndex > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.search_previous),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Next button
                IconButton(
                    onClick = onNext,
                    enabled = currentResultIndex < resultsCount - 1
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.search_next),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (searchQuery.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.search_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Close button
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.search_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}