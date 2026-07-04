package su.sv.books.catalog.presentation.bookmarks.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

/**
 * UI модель заметки для отображения в списке
 * @Immutable - оптимизация Compose recomposition
 */
@Immutable
@Parcelize
data class UiBookmarkNote(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val bookAuthor: String,
    val bookCoverUrl: String,
    val bookFileUri: String?,          // URI файла книги для навигации
    val text: String,
    val name: String?,
    val page: Int,
    val createdAt: Long,
    val startParagraph: Int,
    val startElement: Int,
    val startChar: Int,
    val endParagraph: Int,
    val endElement: Int,
    val endChar: Int,
) : Parcelable {

    /**
     * Форматированный текст для шаринга
     */
    fun getShareText(): String {
        return "\"$text\"\n$bookTitle, стр. $page"
    }
}

/**
 * UI модель книги с заметками
 * @Immutable - оптимизация Compose recomposition
 */
@Immutable
@Parcelize
data class UiBookWithNotes(
    val bookId: String,
    val bookTitle: String,
    val bookAuthor: String,
    val bookCoverUrl: String,
    val notesCount: Int,
) : Parcelable

/**
 * Режим отображения заметок
 */
enum class NotesViewMode {
    LIST,       // Список заметок в хронологическом порядке
    BY_BOOK     // Список книг с заметками
}

/**
 * Состояние экрана заметок
 */
sealed class UiBookmarksState {
    object Loading : UiBookmarksState()
    object Empty : UiBookmarksState()

    /**
     * @Immutable - оптимизация Compose recomposition
     */
    @Immutable
    data class NotesList(
        val notes: List<UiBookmarkNote>,
        val viewMode: NotesViewMode = NotesViewMode.LIST,
    ) : UiBookmarksState()

    /**
     * @Immutable - оптимизация Compose recomposition
     */
    @Immutable
    data class BooksList(
        val books: List<UiBookWithNotes>,
    ) : UiBookmarksState()

    /**
     * @Immutable - оптимизация Compose recomposition
     */
    @Immutable
    data class BookNotes(
        val book: UiBookWithNotes,
        val notes: List<UiBookmarkNote>,
    ) : UiBookmarksState()

    data class Error(val message: String) : UiBookmarksState()
}

/**
 * Состояние диалога удаления заметки
 */
data class DeleteNoteDialogState(
    val note: UiBookmarkNote? = null,
    val isVisible: Boolean = false,
)
