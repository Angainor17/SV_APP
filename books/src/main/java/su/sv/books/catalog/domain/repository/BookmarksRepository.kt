package su.sv.books.catalog.domain.repository

import su.sv.books.catalog.domain.model.BookWithNotes
import su.sv.books.catalog.domain.model.BookmarkNote

/**
 * Репозиторий для работы с заметками (закладками) из всех книг
 */
interface BookmarksRepository {

    /**
     * Получить все заметки из всех книг
     * @param sortByDateAscending true - от старых к новым, false - от новых к старым
     */
    suspend fun getAllNotes(sortByDateAscending: Boolean = false): Result<List<BookmarkNote>>

    /**
     * Получить заметки для конкретной книги
     */
    suspend fun getNotesForBook(bookId: String): Result<List<BookmarkNote>>

    /**
     * Получить список книг, в которых есть заметки
     */
    suspend fun getBooksWithNotes(): Result<List<BookWithNotes>>

    /**
     * Удалить заметку
     */
    suspend fun deleteNote(noteId: String): Result<Unit>
}
