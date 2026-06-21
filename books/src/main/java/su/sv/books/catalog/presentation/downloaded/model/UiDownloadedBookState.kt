package su.sv.books.catalog.presentation.downloaded.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * UI модель скачанной книги для отображения в списке
 */
@Parcelize
data class UiDownloadedBook(
    val id: String,
    val title: String,
    val author: String,
    val category: String,
    val coverUrl: String,
    val fileUri: Uri,
    val currentPage: Int,
    val totalPages: Int,
) : Parcelable

/**
 * Состояние экрана скачанных книг
 */
sealed class UiDownloadedBooksState {

    object Loading : UiDownloadedBooksState()

    data class Content(
        val books: List<UiDownloadedBook>,
        val showSwipeHint: Boolean = false,
        val resetKey: Int = 0, // Ключ для сброса состояния свайпа
    ) : UiDownloadedBooksState()

    object Empty : UiDownloadedBooksState()
}

/**
 * Состояние диалога удаления
 */
data class DeleteDialogState(
    val book: UiDownloadedBook? = null,
    val isVisible: Boolean = false,
)
