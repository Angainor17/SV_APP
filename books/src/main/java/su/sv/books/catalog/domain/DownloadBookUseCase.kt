package su.sv.books.catalog.domain

import su.sv.books.catalog.data.repo.BookDownloadRepository
import javax.inject.Inject

class DownloadBookUseCase @Inject constructor(
    private val downloadRepository: BookDownloadRepository,
) {

    fun execute(params: Params): Long {
        return downloadRepository.downloadBook(
            url = params.url,
            bookTitle = params.bookTitle,
            fileNameWithExt = params.fileNameWithExt,
        )
    }

    data class Params(
        val url: String,
        val bookTitle: String,
        val fileNameWithExt: String,
    )
}
