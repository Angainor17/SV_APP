package su.sv.books.catalog.presentation.root.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import su.sv.books.catalog.domain.model.BookFilter
import su.sv.books.catalog.presentation.root.model.UiBookFilter
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.theme.SVAPPThemeLightPreview

/**
 * Горизонтальный список chip-фильтров
 *
 * @param filters список фильтров
 * @param onFilterClick обработчик клика на фильтр
 * @param isVisible видимость списка
 * @param resetScrollKey ключ для сброса скролла в начало
 */
@Composable
fun BookFiltersChips(
    filters: List<UiBookFilter>,
    onFilterClick: (BookFilter) -> Unit,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    resetScrollKey: Any? = null,
) {
    val scrollState = rememberLazyListState()
    val dimensions = LocalAppDimensions.current

    // Сбрасываем скролл только при изменении resetScrollKey (клик на чип)
    LaunchedEffect(resetScrollKey) {
        if (resetScrollKey != null) {
            scrollState.animateScrollToItem(0)
        }
    }

    AnimatedVisibility(visible = isVisible) {
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            state = scrollState,
            horizontalArrangement = Arrangement.spacedBy(dimensions.itemSpacingMedium),
            contentPadding = PaddingValues(horizontal = dimensions.screenPaddingHorizontal / 2),
        ) {
            // Сначала выбранные фильтры (кроме "Все")
            val selectedFilters = filters.filter {
                it.isSelected && it.filter !is BookFilter.All
            }
            // Потом "Все"
            val allFilter = filters.find { it.filter is BookFilter.All }
            // Потом остальные доступные
            val otherFilters = filters.filter {
                !it.isSelected && it.filter !is BookFilter.All && it.isAvailable
            }

            // "Все" всегда первый
            if (allFilter != null) {
                item(key = "all") {
                    BookFilterChip(
                        filter = allFilter,
                        onClick = { onFilterClick(allFilter.filter) },
                    )
                }
            }

            // Выбранные фильтры с крестиком
            items(
                items = selectedFilters,
                key = { it.filter.toString() }
            ) { filter ->
                SelectedFilterChip(
                    filter = filter,
                    onClick = { onFilterClick(filter.filter) },
                )
            }

            // Остальные доступные фильтры
            items(
                items = otherFilters,
                key = { it.filter.toString() }
            ) { filter ->
                BookFilterChip(
                    filter = filter,
                    onClick = { onFilterClick(filter.filter) },
                )
            }
        }
    }
}

@Composable
private fun BookFilterChip(
    filter: UiBookFilter,
    onClick: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current

    FilterChip(
        selected = filter.isSelected,
        onClick = onClick,
        enabled = filter.isAvailable,
        label = {
            Text(
                text = filter.displayName,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = Modifier.padding(vertical = dimensions.itemSpacingSmall),
        shape = MaterialTheme.shapes.small,
        border = FilterChipDefaults.filterChipBorder(
            enabled = filter.isAvailable,
            selected = filter.isSelected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            disabledSelectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            borderWidth = dimensions.borderWidthStandard,
            selectedBorderWidth = dimensions.borderWidthStandard,
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            disabledSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        ),
    )
}

@Composable
private fun SelectedFilterChip(
    filter: UiBookFilter,
    onClick: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current

    FilterChip(
        selected = true,
        onClick = onClick,
        label = {
            Text(
                text = filter.displayName,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(dimensions.iconSizeSmall),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        },
        modifier = Modifier.padding(vertical = dimensions.itemSpacingSmall),
        shape = MaterialTheme.shapes.small,
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = true,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
            borderWidth = dimensions.borderWidthStandard,
            selectedBorderWidth = dimensions.borderWidthStandard,
        ),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    )
}

// ============================================================
// Previews
// ============================================================

@Preview(showBackground = true)
@Composable
private fun BookFilterChipPreview() {
    SVAPPThemeLightPreview {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            BookFilterChip(
                filter = UiBookFilter(
                    filter = BookFilter.All,
                    displayName = "Все",
                    count = 34,
                    isSelected = true,
                    isAvailable = true,
                ),
                onClick = {},
            )
            BookFilterChip(
                filter = UiBookFilter(
                    filter = BookFilter.Category("Свободное Время"),
                    displayName = "Свободное Время",
                    count = 29,
                    isSelected = false,
                    isAvailable = true,
                ),
                onClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectedFilterChipPreview() {
    SVAPPThemeLightPreview {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            SelectedFilterChip(
                filter = UiBookFilter(
                    filter = BookFilter.Category("Свободное Время"),
                    displayName = "Свободное Время",
                    count = 29,
                    isSelected = true,
                    isAvailable = true,
                ),
                onClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BookFiltersChipsPreview() {
    SVAPPThemeLightPreview {
        val filters = listOf(
            UiBookFilter(filter = BookFilter.All, displayName = "Все", count = 34, isSelected = true, isAvailable = true),
            UiBookFilter(filter = BookFilter.Category("Свободное Время"), displayName = "Свободное Время", count = 29, isSelected = false, isAvailable = true),
            UiBookFilter(filter = BookFilter.Author("Ленин В. И."), displayName = "Ленин В. И.", count = 16, isSelected = false, isAvailable = true),
        )
        BookFiltersChips(
            filters = filters,
            onFilterClick = {},
            isVisible = true,
            modifier = Modifier.padding(8.dp),
        )
    }
}
