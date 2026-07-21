package su.sv.books.catalog.domain

import android.net.Uri
import su.sv.books.catalog.data.repo.BookDownloadRepository
import su.sv.commonarchitecture.di.module.DispatcherProvider
import javax.inject.Inject

/**
 * UseCase для получения списка скачанных книг
 */
class GetDownloadedBooksUseCase @Inject constructor(
    private val getBooksListUseCase: GetBooksListUseCase,
    private val downloadRepository: BookDownloadRepository,
    private val dispatcherProvider: DispatcherProvider,
) {

    /**
     * Получить список скачанных книг
     * @param onProgressUpdate callback для получения прогресса чтения (опционально)
     */
    suspend fun execute(
        onProgressUpdate: ((bookId: String) -> Pair<Int, Int>)? = null
    ): Result<List<DownloadedBook>> {
        return getBooksListUseCase.execute().map { books ->
            // Используем уже полученный fileUri из Book, вместо повторного запроса
            books.mapNotNull { book ->
                val uri = book.fileUri
                if (uri != null) {
                    val (currentPage, totalPages) = onProgressUpdate?.invoke(book.id) ?: (0 to 0)
                    DownloadedBook(
                        id = book.id,
                        title = book.title,
                        author = book.author,
                        category = book.category,
                        coverUrl = book.image,
                        fileUri = uri,
                        currentPage = currentPage,
                        totalPages = totalPages,
                    )
                } else null
            }
        }
    }
}

/**
 * Модель скачанной книги
 */
data class DownloadedBook(
    val id: String,
    val title: String,
    val author: String,
    val category: String,
    val coverUrl: String,
    val fileUri: Uri,
    val currentPage: Int,
    val totalPages: Int,
)
