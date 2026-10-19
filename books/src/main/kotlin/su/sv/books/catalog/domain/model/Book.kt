package su.sv.books.catalog.domain.model

import android.net.Uri

/**
 * Книга, которую отображаем для скачивания
 */
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val image: String,
    val link: String,
    val fileNameWithExt: String,

    /** Категория */
    val category: String,

    val fileUri: Uri?,
)
