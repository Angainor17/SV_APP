package su.sv.books.catalog.domain

import android.net.Uri
import su.sv.books.catalog.data.repo.BookDownloadRepository
import javax.inject.Inject

/**
 * Получение Uri файла по его названию
 */
class GetBookUriUseCase @Inject constructor(
    private val downloadRepository: BookDownloadRepository,
) {

    fun execute(fileNameWithExt: String): Uri? {
        return downloadRepository.getDownloadsUri(fileNameWithExt)
    }
}

