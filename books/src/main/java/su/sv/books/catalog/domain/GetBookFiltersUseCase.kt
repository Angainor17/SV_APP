package su.sv.books.catalog.domain

import su.sv.books.catalog.domain.model.Book
import su.sv.books.catalog.domain.model.BookFilter
import su.sv.books.catalog.domain.model.BookFilterWithCount
import javax.inject.Inject

/**
 * UseCase для формирования списка фильтров на основе списка книг
 */
class GetBookFiltersUseCase @Inject constructor() {

    /**
     * Максимальное количество фильтров (кроме "Все")
     */
    private companion object {
        const val MAX_FILTERS = 19
        // Паттерны для определения серий: "том 1", "тома 1-2", "книга 1", "книги 1-2"
        val SERIES_PATTERNS = listOf(
            Regex(",\\s*(том|тома|т\\.|книга|книги|кн\\.)\\s*[\\d\\-]+\\s*$", RegexOption.IGNORE_CASE),
            Regex("\\s+(том|тома|т\\.|книга|книги|кн\\.)\\s*[\\d\\-]+\\s*$", RegexOption.IGNORE_CASE),
        )
    }

    /**
     * Сформировать список фильтров на основе книг
     */
    fun execute(books: List<Book>): List<BookFilterWithCount> {
        val categoryFreq = mutableMapOf<String, Int>()
        val authorFreq = mutableMapOf<String, Int>()
        val seriesFreq = mutableMapOf<String, Int>()

        // Собираем частоты категорий, авторов и серий
        books.forEach { book ->
            // Категория
            if (book.category.isNotBlank()) {
                categoryFreq[book.category] = categoryFreq.getOrDefault(book.category, 0) + 1
            }

            // Авторы (разбиваем по запятой)
            book.author.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .forEach { author ->
                    authorFreq[author] = authorFreq.getOrDefault(author, 0) + 1
                }

            // Серии книг (ищем паттерны "том N", "книга N" в названии)
            val seriesName = extractSeriesName(book.title)
            if (seriesName != null && seriesName.length >= 3) {
                seriesFreq[seriesName] = seriesFreq.getOrDefault(seriesName, 0) + 1
            }
        }

        // Сортируем категории по убыванию частоты
        val sortedCategories = categoryFreq.entries
            .sortedByDescending { it.value }
            .map { (name, count) ->
                BookFilterWithCount(
                    filter = BookFilter.Category(name),
                    displayName = name,
                    count = count,
                )
            }

        // Сортируем серии по убыванию частоты (только если > 1 книги)
        val sortedSeries = seriesFreq.entries
            .filter { it.value > 1 } // Показываем только серии с несколькими книгами
            .sortedByDescending { it.value }
            .map { (name, count) ->
                BookFilterWithCount(
                    filter = BookFilter.Series(name),
                    displayName = name,
                    count = count,
                )
            }

        // Сортируем авторов по убыванию частоты
        val sortedAuthors = authorFreq.entries
            .sortedByDescending { it.value }
            .map { (name, count) ->
                BookFilterWithCount(
                    filter = BookFilter.Author(name),
                    displayName = name,
                    count = count,
                )
            }

        // Комбинируем: категории, серии, авторы, лимит MAX_FILTERS
        val allFilters = (sortedCategories + sortedSeries + sortedAuthors).take(MAX_FILTERS)

        // Добавляем "Все" в начало
        val allFilter = BookFilterWithCount(
            filter = BookFilter.All,
            displayName = "Все",
            count = books.size,
        )

        return listOf(allFilter) + allFilters
    }

    /**
     * Извлечь название серии из названия книги
     * @return Название серии без указания тома/книги, или null если это не серия
     */
    private fun extractSeriesName(title: String): String? {
        for (pattern in SERIES_PATTERNS) {
            val match = pattern.find(title)
            if (match != null) {
                // Удаляемmatched часть и очищаем от лишних символов
                val seriesName = title.substring(0, match.range.first)
                    .trim()
                    .trimEnd(',', '.', ':', '-')
                    .trim()
                if (seriesName.isNotBlank()) {
                    return seriesName
                }
            }
        }
        return null
    }

    /**
     * Отфильтровать книги по выбранным фильтрам
     */
    fun filterBooks(
        books: List<Book>,
        selectedFilters: Set<BookFilter>
    ): List<Book> {
        if (selectedFilters.isEmpty() || selectedFilters.contains(BookFilter.All)) {
            return books
        }

        return books.filter { book ->
            selectedFilters.all { filter ->
                when (filter) {
                    is BookFilter.All -> true
                    is BookFilter.Category -> book.category == filter.name
                    is BookFilter.Author -> book.author.contains(filter.name)
                    is BookFilter.Series -> {
                        val seriesName = extractSeriesName(book.title)
                        seriesName == filter.name
                    }
                }
            }
        }
    }

    /**
     * Получить доступные фильтры для оставшихся книг
     */
    fun getAvailableFilters(
        allFilters: List<BookFilterWithCount>,
        selectedFilters: Set<BookFilter>,
        filteredBooks: List<Book>
    ): List<BookFilterWithCount> {
        if (selectedFilters.isEmpty() || selectedFilters.contains(BookFilter.All)) {
            return allFilters
        }

        return allFilters.map { filterWithCount ->
            val isAvailable = when (filterWithCount.filter) {
                is BookFilter.All -> true
                is BookFilter.Category -> filteredBooks.any { it.category == filterWithCount.filter.name }
                is BookFilter.Author -> filteredBooks.any { it.author.contains(filterWithCount.filter.name) }
                is BookFilter.Series -> filteredBooks.any { book ->
                    extractSeriesName(book.title) == filterWithCount.filter.name
                }
            }
            filterWithCount.copy(count = if (isAvailable) filterWithCount.count else 0)
        }
    }
}
