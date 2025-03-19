package su.sv.books.catalog.presentation.root.mapper

import su.sv.books.catalog.domain.model.Book
import su.sv.books.catalog.presentation.root.model.UiBook
import javax.inject.Inject

class UiBookMapper @Inject constructor(

) {

    fun fromDomainToUi(domains: List<Book>): List<UiBook> {
        return domains.map { fromDomainToUi(it) }
    }

    private fun fromDomainToUi(domain: Book): UiBook {
        return UiBook(
            id = domain.id,
            title = domain.title,
            image = domain.image,
            link = domain.link,
            isDownloaded = domain.fileUri != null,
        )
    }
}