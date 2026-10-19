package su.sv.books.catalog.presentation.root.mapper

import su.sv.books.catalog.domain.model.BookFilter
import su.sv.books.catalog.domain.model.BookFilterWithCount
import su.sv.books.catalog.presentation.root.model.UiBookFilter
import javax.inject.Inject

/**
 * Маппер для преобразования фильтров в UI модели
 */
class UiBookFilterMapper @Inject constructor() {

    fun mapToUi(
        filters: List<BookFilterWithCount>,
        selectedFilters: Set<BookFilter>,
        availableFilterTypes: Set<BookFilter> = emptySet(),
    ): List<UiBookFilter> {
        return filters.map { filterWithCount ->
            val isSelected = selectedFilters.contains(filterWithCount.filter)
            val isAvailable = availableFilterTypes.isEmpty() ||
                    availableFilterTypes.contains(filterWithCount.filter) ||
                    filterWithCount.filter is BookFilter.All

            UiBookFilter(
                filter = filterWithCount.filter,
                displayName = filterWithCount.displayName,
                count = filterWithCount.count,
                isSelected = isSelected,
                isAvailable = isAvailable,
            )
        }
    }
}
