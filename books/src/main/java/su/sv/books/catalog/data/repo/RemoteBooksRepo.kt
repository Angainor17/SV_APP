package su.sv.books.catalog.data.repo

import jakarta.inject.Inject
import su.sv.books.catalog.data.api.BooksApi
import su.sv.books.catalog.data.models.ApiBook
import su.sv.commonarchitecture.data.runCatchingHttpRequest

class RemoteBooksRepo @Inject constructor(
    private val booksApi: BooksApi,
) {
    suspend fun getBooks(): Result<List<ApiBook>> {
        return runCatchingHttpRequest {
            booksApi.getBooks()
        }
    }
}
