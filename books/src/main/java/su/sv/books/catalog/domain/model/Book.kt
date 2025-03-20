package su.sv.books.catalog.domain.model

import android.net.Uri
import java.time.LocalDate

/**
 * Книга, которую отображаем для скачивания
 */
data class Book(
    val id: String,
    val title: String,
    val description: String,
    val image: String,
    val link: String,
    val pagesCount: Int,
    val publicationDate: LocalDate,

    val fileUri: Uri?,
)
