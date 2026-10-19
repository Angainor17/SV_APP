package su.sv.books.catalog.presentation.root.model

import androidx.compose.runtime.Immutable
import su.sv.books.catalog.domain.model.BookFilter

/**
 * UI модель фильтра для отображения в Chip
 * @Immutable - оптимизация Compose recomposition
 */
@Immutable
data class UiBookFilter(
    val filter: BookFilter,
    val displayName: String,
    val count: Int,
    val isSelected: Boolean,
    val isAvailable: Boolean,
)

/**
 * Состояние фильтров
 * @Immutable - оптимизация Compose recomposition
 */
@Immutable
data class FiltersState(
    val allFilters: List<UiBookFilter>,
    val selectedFilters: Set<BookFilter>,
    val isCollapsed: Boolean = false,
)
