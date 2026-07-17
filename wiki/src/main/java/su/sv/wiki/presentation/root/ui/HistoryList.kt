package su.sv.wiki.presentation.root.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import su.sv.commonui.theme.SVAPPTheme
import su.sv.wiki.R

/**
 * Список истории поиска
 */
@Composable
fun HistoryList(
    history: List<String>,
    onItemClick: (String) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Заголовок с кнопкой очистки
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.wiki_history_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )

            if (history.isNotEmpty()) {
                IconButton(onClick = onClearClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.wiki_history_clear),
                    )
                }
            }
        }

        if (history.isEmpty()) {
            EmptyHistory()
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(
                    items = history,
                    key = { title -> title },
                ) { title ->
                    HistoryItem(
                        title = title,
                        onClick = { onItemClick(title) },
                    )
                }
            }
        }
    }
}

/**
 * Элемент истории
 */
@Composable
private fun HistoryItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.padding(horizontal = 12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

/**
 * Пустая история
 */
@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.wiki_history_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
fun HistoryListPreview() {
    SVAPPTheme {
        HistoryList(
            history = listOf(
                "Государство и революция",
                "Ленин",
                "Октябрьская революция",
            ),
            onItemClick = {},
            onClearClick = {},
        )
    }
}

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun HistoryListEmptyPreview() {
    SVAPPTheme {
        HistoryList(
            history = emptyList(),
            onItemClick = {},
            onClearClick = {},
        )
    }
}

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun HistoryItemPreview() {
    SVAPPTheme {
        HistoryItem(
            title = "Государство и революция",
            onClick = {},
        )
    }
}

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun EmptyHistoryPreview() {
    SVAPPTheme {
        EmptyHistory()
    }
}
