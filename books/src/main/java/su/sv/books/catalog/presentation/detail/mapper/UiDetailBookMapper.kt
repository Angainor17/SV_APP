package su.sv.books.catalog.presentation.detail.mapper

import su.sv.books.R
import su.sv.books.catalog.presentation.detail.model.UiBookDetailState.Content
import su.sv.commonui.managers.ResourcesRepository
import su.sv.models.ui.book.UIBookState
import su.sv.models.ui.book.UiBook
import javax.inject.Inject

class UiDetailBookMapper @Inject constructor(
    private val resourcesRepository: ResourcesRepository,
) {

    fun createStateAfterDownload(previousState: Content): Content {
        val book = previousState.book

        return Content(
            book = book,
            isActionLoading = false,
            actionText = book.getActionText()
        )
    }

    fun createState(book: UiBook): Content {
        return Content(
            book = book,
            isActionLoading = book.downloadState == UIBookState.DOWNLOADING,
            actionText = book.getActionText()
        )
    }

    private fun UiBook.getActionText(): String {
        val hasDownloadedBook = fileUri != null

        return resourcesRepository.getString(
            if (hasDownloadedBook && downloadState == UIBookState.DOWNLOADED) {
                R.string.books_read
            } else {
                R.string.books_download
            }
        )
    }
}
