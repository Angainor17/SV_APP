package su.sv.books.catalog.presentation.root.model

import su.sv.books.catalog.domain.model.BookFilter

/**
 * UI модель фильтра для отображения в Chip
 */
data class UiBookFilter(
    val filter: BookFilter,
    val displayName: String,
    val count: Int,
    val isSelected: Boolean,
    val isAvailable: Boolean,
)

/**
 * Состояние фильтров
 */
data class FiltersState(
    val allFilters: List<UiBookFilter>,
    val selectedFilters: Set<BookFilter>,
    val isCollapsed: Boolean = false,
)
