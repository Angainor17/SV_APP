package su.sv.books.catalog.presentation.downloaded.model

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

/**
 * UI модель скачанной книги для отображения в списке
 * @Immutable - оптимизация Compose recomposition
 */
@Immutable
@Parcelize
data class UiDownloadedBook(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
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

    /**
     * @Immutable - оптимизация Compose recomposition
     */
    @Immutable
    data class Content(
        val books: List<UiDownloadedBook>,
        val showSwipeHint: Boolean = false,
        val resetKey: Int = 0, // Ключ для сброса состояния свайпа
    ) : UiDownloadedBooksState()

    object Empty : UiDownloadedBooksState()
}

/**
 * Состояние диалога удаления
 * @Immutable - оптимизация Compose recomposition
 */
@Immutable
data class DeleteDialogState(
    val book: UiDownloadedBook? = null,
    val isVisible: Boolean = false,
)
