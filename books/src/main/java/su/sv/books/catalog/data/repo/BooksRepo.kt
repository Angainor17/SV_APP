package su.sv.books.catalog.data.repo

import android.net.Uri
import jakarta.inject.Inject
import su.sv.books.catalog.data.api.BooksApi
import su.sv.books.catalog.data.models.ApiBook
import su.sv.commonarchitecture.data.runCatchingHttpRequest

class BooksRepo @Inject constructor(
    private val booksApi: BooksApi,
) {
    suspend fun getBooks(): Result<List<ApiBook>> {
        return runCatchingHttpRequest {
            booksApi.getBooks()
        }
    }

    suspend fun getDownloadedBookUri(id: String): Uri? {
        return null // TODO
    }
}
