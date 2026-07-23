package su.sv.books.catalog.presentation.downloaded.mapper

import su.sv.books.catalog.domain.DownloadedBook
import su.sv.books.catalog.presentation.downloaded.model.UiDownloadedBook
import javax.inject.Inject

/**
 * Маппер для преобразования доменной модели в UI модель
 */
class UiDownloadedBookMapper @Inject constructor() {

    fun mapToUi(book: DownloadedBook): UiDownloadedBook {
        return UiDownloadedBook(
            id = book.id,
            title = book.title,
            author = book.author,
            description = book.description,
            category = book.category,
            coverUrl = book.coverUrl,
            fileUri = book.fileUri,
            currentPage = book.currentPage,
            totalPages = book.totalPages,
        )
    }

    fun mapToUi(books: List<DownloadedBook>): List<UiDownloadedBook> {
        return books.map { mapToUi(it) }
    }
}
