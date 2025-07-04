package su.sv.books.catalog.presentation.root.mapper

import su.sv.books.catalog.domain.model.Book
import su.sv.models.ui.book.UIBookState
import su.sv.models.ui.book.UiBook
import javax.inject.Inject

class UiBookMapper @Inject constructor() {

    fun fromDomainToUi(domains: List<Book>): List<UiBook> {
        return domains.map { fromDomainToUi(it) }
    }

    private fun fromDomainToUi(domain: Book): UiBook {
        return UiBook(
            id = domain.id,
            title = domain.title,
            author = domain.author,
            description = domain.description,
            image = domain.image,
            downloadUrl = domain.link,
            fileNameWithExt = domain.fileNameWithExt,
            category = domain.category,

            downloadState = when {
                domain.fileUri != null -> UIBookState.DOWNLOADED
                else -> UIBookState.AVAILABLE_TO_DOWNLOAD
            },
            fileUri = domain.fileUri,
        )
    }
}
