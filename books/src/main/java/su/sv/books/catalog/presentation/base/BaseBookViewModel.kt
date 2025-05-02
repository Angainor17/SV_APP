package su.sv.books.catalog.presentation.base

import dagger.Lazy
import su.sv.books.R
import su.sv.books.catalog.data.receivers.BookDownloadedActionHandler
import su.sv.books.catalog.domain.GetBookUriUseCase
import su.sv.books.catalog.presentation.CommonDownloadBookStates
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import su.sv.models.ui.book.UIBookState
import su.sv.models.ui.book.UiBook

/**
 * Общая логика скачивания книги
 */
abstract class BaseBookViewModel(
    private val downloadBookStates: CommonDownloadBookStates,
    private val getBookUriUseCase: Lazy<GetBookUriUseCase>,
) : BaseViewModel() {

    protected abstract fun showErrorSnack(resId: Int)

    protected abstract fun updateBookState(action: (UiBook) -> UiBook)
    protected open fun handleBookDownloadEnd() = Unit

    protected fun handleDownloadedBook(state: BookDownloadedActionHandler.BookState) {
        val (downloadId) = state
        val bookId = downloadBookStates.loadingInProgressMap[downloadId]

        var currentBook: UiBook? = null // FIXME
        // обновляется на статус скачанного (или нет)
        updateBookState { oldBook ->
            if (oldBook.id == bookId) {
                currentBook = getBookWithActualDownloadState(oldBook)
                currentBook
            } else {
                oldBook
            }
        }

        //TODO : refactor
        val uri = getBookUriUseCase.get().execute(currentBook?.fileNameWithExt.orEmpty())
        val isSuccess = currentBook != null && uri != null
        if (!isSuccess) {
            showErrorSnack(R.string.books_download_snack_error)
        }

        handleBookDownloadEnd()

        downloadBookStates.loadingInProgressMap.remove(downloadId)
    }

    protected fun getBookWithActualDownloadState(book: UiBook): UiBook {
        val uri = getBookUriUseCase.get().execute(book.fileNameWithExt)

        return book.copy(
            downloadState = when {
                uri != null -> {
                    UIBookState.DOWNLOADED
                }
                // скачивание ещё идёт
                downloadBookStates.loadingInProgressMap.values.contains(book.id) -> UIBookState.DOWNLOADING
                else -> UIBookState.AVAILABLE_TO_DOWNLOAD
            },
            fileUri = uri,
        )
    }
}
