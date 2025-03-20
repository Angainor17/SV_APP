package su.sv.books.catalog.data.models

import java.time.LocalDate

class ApiBook(
    val id: String?,
    val title: String?,
    val description: String?,
    val pagesCount: Int?,
    val image: String?,
    val publicationDate: LocalDate?,
    val link: String?,
    val fileNameWithExt: String?,
)