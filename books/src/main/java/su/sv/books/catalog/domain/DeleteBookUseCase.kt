package su.sv.books.catalog.domain

import android.net.Uri
import su.sv.books.catalog.data.repo.BookDownloadRepository
import javax.inject.Inject

/**
 * UseCase для удаления скачанной книги
 */
class DeleteBookUseCase @Inject constructor(
    private val downloadRepository: BookDownloadRepository,
) {

    /**
     * Удалить скачанную книгу
     * @param uri URI файла для удаления
     * @return Result.success если удаление успешно, Result.failure при ошибке
     */
    fun execute(uri: Uri): Result<Boolean> {
        return try {
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
