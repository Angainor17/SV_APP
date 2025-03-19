package su.sv.books.catalog.data.api

import kotlinx.coroutines.delay
import su.sv.books.catalog.data.models.ApiBook
import javax.inject.Inject

class BooksApiMock @Inject constructor() : BooksApi {

    override suspend fun getBooks(): List<ApiBook> {
        delay(3_000)

        return listOf(
            ApiBook(
                id = "0",
                title = "123123",
                image = "123123",
                link = "",
            ),
        )
    }
}
