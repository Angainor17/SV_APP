package su.sv.books.catalog.data.api

import retrofit2.http.GET
import su.sv.books.catalog.data.models.ApiBook

interface BooksApi {

    @GET("/")
    suspend fun getBooks(): List<ApiBook>
}
