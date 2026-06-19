package su.sv.wiki.presentation.root.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import su.sv.commonui.theme.SVAPPTheme

/**
 * Список подсказок поиска с анимацией
 *
 * @param suggestions список подсказок
 * @param onSuggestionClick callback при клике на подсказку
 */
@Composable
fun SearchSuggestions(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = suggestions.isNotEmpty(),
        enter = expandVertically(
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing,
            ),
            expandFrom = Alignment.Top,
        ),
        exit = shrinkVertically(
            animationSpec = tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing,
            ),
            shrinkTowards = Alignment.Top,
        ),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            suggestions.forEachIndexed { index, suggestion ->
                SuggestionItem(
                    text = suggestion,
                    onClick = { onSuggestionClick(suggestion) },
                    showDivider = index < suggestions.lastIndex,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Элемент подсказки
 */
@Composable
private fun SuggestionItem(
    text: String,
    onClick: () -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }

    if (showDivider) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(start = 52.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
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
fun SearchSuggestionsPreview() {
    SVAPPTheme {
        SearchSuggestions(
            suggestions = listOf(
                "Государство",
                "Государство и революция",
                "Государственный строй",
            ),
            onSuggestionClick = {},
        )
    }
}

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun SearchSuggestionsEmptyPreview() {
    SVAPPTheme {
        SearchSuggestions(
            suggestions = emptyList(),
            onSuggestionClick = {},
        )
    }
}

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun SuggestionItemPreview() {
    SVAPPTheme {
        SuggestionItem(
            text = "Государство и революция",
            onClick = {},
            showDivider = true,
        )
    }
}
