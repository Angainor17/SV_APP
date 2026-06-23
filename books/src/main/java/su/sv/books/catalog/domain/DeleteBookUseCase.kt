package su.sv.books.catalog.domain

import android.net.Uri
import su.sv.books.catalog.data.repo.BookDownloadRepository
import javax.inject.Inject

/**
 * UseCase для удаления скачанной книги с сохранением заметок
 */
class DeleteBookUseCase @Inject constructor(
    private val downloadRepository: BookDownloadRepository,
) {

    /**
     * Удалить скачанную книгу
     * Примечание: JSON файл с заметками сохраняется и не удаляется
     * @param uri URI файла для удаления
     * @return Result.success если удаление успешно, Result.failure при ошибке
     */
    fun execute(uri: Uri): Result<Boolean> {
        return try {
            // Удаляем только файл книги, но не JSON с заметками
            // JSON файл остаётся в хранилище и заметки доступны
            val success = downloadRepository.deleteBook(uri)
            if (success) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to delete book"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
