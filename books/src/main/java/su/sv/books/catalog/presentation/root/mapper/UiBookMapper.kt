package su.sv.books.catalog.presentation.root.mapper

import su.sv.books.R
import su.sv.books.catalog.domain.model.Book
import su.sv.books.catalog.presentation.root.model.UiBook
import su.sv.commonui.ext.formatDecimal
import su.sv.commonui.managers.DateFormatter
import su.sv.commonui.managers.ResourcesRepository
import timber.log.Timber
import javax.inject.Inject

class UiBookMapper @Inject constructor(
    private val resRepo: ResourcesRepository,
    private val dateFormatter: DateFormatter,
) {

    fun fromDomainToUi(domains: List<Book>): List<UiBook> {
        return domains.map { fromDomainToUi(it) }
    }

    private fun fromDomainToUi(domain: Book): UiBook {
        Timber.tag("voronin").d("${domain.fileNameWithExt} = ${domain.fileUri}")

        return UiBook(
            id = domain.id,
            title = domain.title,
            description = domain.description,
            image = domain.image,
            downloadUrl = domain.link,
            fileNameWithExt = domain.fileNameWithExt,
            pagesCountFormatted = resRepo.getString(
                R.string.books_item_page_formatted,
                domain.pagesCount.formatDecimal()
            ),
            dateFormatted = dateFormatter.formatDateFull(domain.publicationDate),

            isDownloaded = domain.fileUri != null,
            isDownloading = false,
            fileUri = domain.fileUri,
        )
    }
}