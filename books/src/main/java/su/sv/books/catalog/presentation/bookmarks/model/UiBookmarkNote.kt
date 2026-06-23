package su.sv.books.catalog.presentation.bookmarks.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * UI модель заметки для отображения в списке
 */
@Parcelize
data class UiBookmarkNote(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val bookAuthor: String,
    val bookCoverUrl: String,
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
 */
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

    data class NotesList(
        val notes: List<UiBookmarkNote>,
        val viewMode: NotesViewMode = NotesViewMode.LIST,
    ) : UiBookmarksState()

    data class BooksList(
        val books: List<UiBookWithNotes>,
    ) : UiBookmarksState()

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
