package su.sv.books.catalog.presentation.base

import android.net.Uri
import dagger.Lazy
import su.sv.books.R
import su.sv.books.catalog.data.receivers.BookDownloadedActionHandler
import su.sv.books.catalog.domain.GetBookUriUseCase
import su.sv.books.catalog.presentation.CommonDownloadBookStates
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import su.sv.models.ui.book.UIBookState
import su.sv.models.ui.book.UiBook

/**
 * Базовый ViewModel с общей логикой скачивания книг
 */
abstract class BaseBookViewModel(
    private val downloadBookStates: CommonDownloadBookStates,
    private val getBookUriUseCase: Lazy<GetBookUriUseCase>,
) : BaseViewModel() {

    protected abstract fun showErrorSnack(resId: Int)
    protected abstract fun updateBookState(action: (UiBook) -> UiBook)
    protected open fun onBookDownloadEnd() = Unit

    /**
     * Обработать завершение скачивания книги
     */
    protected fun handleDownloadedBook(state: BookDownloadedActionHandler.BookState) {
        val downloadId = state.downloadID
        val bookId = downloadBookStates.loadingInProgressMap[downloadId] ?: return

        // Обновляем состояние книги и получаем обновлённую версию
        var updatedBook: UiBook? = null
        updateBookState { book ->
            if (book.id == bookId) {
                getBookWithActualDownloadState(book).also { updatedBook = it }
            } else {
                book
            }
        }

        // Проверяем успешность загрузки
        val isSuccess = updatedBook?.fileUri != null
        if (!isSuccess) {
            showErrorSnack(R.string.books_download_snack_error)
        }

        // Уведомляем о завершении
        onBookDownloadEnd()

        // Очищаем карту загрузок
        downloadBookStates.loadingInProgressMap.remove(downloadId)
    }

    /**
     * Получить книгу с актуальным состоянием скачивания
     */
    protected fun getBookWithActualDownloadState(book: UiBook): UiBook {
        val uri = getBookUriUseCase.get().execute(book.fileNameWithExt)
        val isDownloading = downloadBookStates.loadingInProgressMap.values.contains(book.id)

        return book.copy(
            downloadState = determineDownloadState(uri, isDownloading),
            fileUri = uri,
        )
    }

    /**
     * Определить состояние скачивания книги
     */
    private fun determineDownloadState(uri: Uri?, isDownloading: Boolean): UIBookState {
        return when {
            uri != null -> UIBookState.DOWNLOADED
            isDownloading -> UIBookState.DOWNLOADING
            else -> UIBookState.AVAILABLE_TO_DOWNLOAD
        }
    }
}
