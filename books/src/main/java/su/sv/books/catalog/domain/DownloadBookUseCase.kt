package su.sv.books.catalog.domain

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import su.sv.books.catalog.data.repo.BookDownloadRepository
import timber.log.Timber
import javax.inject.Inject

class DownloadBookUseCase @Inject constructor(
    private val downloadRepository: BookDownloadRepository,
) {

    fun execute(params: Params): Flow<Uri?> {
        return downloadRepository.downloadBook(
            url = params.url,
            bookTitle = params.bookTitle,
            fileNameWithExt = params.fileNameWithExt,
        )
            .catch { Timber.tag("voronin").e(it) }
    }

    data class Params(
        val url: String,
        val bookTitle: String,
        val fileNameWithExt: String,
    )
}
