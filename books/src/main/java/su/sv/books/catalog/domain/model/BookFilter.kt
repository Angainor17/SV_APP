package su.sv.books.catalog.domain.model

/**
 * Тип фильтра для книг
 */
sealed class BookFilter {

    /** Фильтр "Все книги" */
    object All : BookFilter()

    /** Фильтр по категории */
    data class Category(val name: String) : BookFilter()

    /** Фильтр по автору */
    data class Author(val name: String) : BookFilter()

    /** Фильтр по серии книг (тома, книги) */
    data class Series(val name: String) : BookFilter()
}

/**
 * Параметры сортировки фильтров
 */
data class BookFilterWithCount(
    val filter: BookFilter,
    val displayName: String,
    val count: Int,
)
