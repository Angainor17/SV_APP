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
     * @return Result.success если удаление успешно, Result.failure при ошибке с описанием
     */
    fun execute(uri: Uri): Result<Boolean> {
        // Удаляем только файл книги, но не JSON с заметками
        // JSON файл остаётся в хранилище и заметки доступны
        return downloadRepository.deleteBook(uri)
    }
}
