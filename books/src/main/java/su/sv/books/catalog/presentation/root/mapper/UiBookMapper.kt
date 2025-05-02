package su.sv.books.catalog.presentation.root.mapper

import su.sv.books.R
import su.sv.books.catalog.domain.model.Book
import su.sv.commonui.ext.formatDecimal
import su.sv.commonui.managers.DateFormatter
import su.sv.commonui.managers.ResourcesRepository
import su.sv.models.ui.book.UIBookState
import su.sv.models.ui.book.UiBook
import javax.inject.Inject

class UiBookMapper @Inject constructor(
    private val resRepo: ResourcesRepository,
    private val dateFormatter: DateFormatter,
) {

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
            pagesCountFormatted = resRepo.getString(
                R.string.books_item_page_formatted,
                domain.pagesCount.formatDecimal()
            ),
            dateFormatted = dateFormatter.formatDateFull(domain.publicationDate),

            downloadState = when {
                domain.fileUri != null -> UIBookState.DOWNLOADED
                else -> UIBookState.AVAILABLE_TO_DOWNLOAD
            },
            fileUri = domain.fileUri,
        )
    }
}