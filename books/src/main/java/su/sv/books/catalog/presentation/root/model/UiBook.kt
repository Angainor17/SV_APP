package su.sv.books.catalog.presentation.root.model

data class UiBook(
    val id: String,
    val title: String,
    val image: String,
    val link: String,
    val isDownloaded: Boolean,
)
