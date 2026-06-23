package su.sv.books.catalog.domain

import su.sv.books.catalog.domain.model.BookWithNotes
import su.sv.books.catalog.domain.model.BookmarkNote
import su.sv.books.catalog.domain.repository.BookmarksRepository
import javax.inject.Inject

/**
 * UseCase для получения всех заметок из всех книг
 */
class GetAllNotesUseCase @Inject constructor(
    private val repository: BookmarksRepository,
) {
    suspend fun execute(sortByDateAscending: Boolean = false): Result<List<BookmarkNote>> {
        return repository.getAllNotes(sortByDateAscending)
    }
}

/**
 * UseCase для получения заметок конкретной книги
 */
class GetNotesForBookUseCase @Inject constructor(
    private val repository: BookmarksRepository,
) {
    suspend fun execute(bookId: String): Result<List<BookmarkNote>> {
        return repository.getNotesForBook(bookId)
    }
}

/**
 * UseCase для получения списка книг с заметками
 */
class GetBooksWithNotesUseCase @Inject constructor(
    private val repository: BookmarksRepository,
) {
    suspend fun execute(): Result<List<BookWithNotes>> {
        return repository.getBooksWithNotes()
    }
}

/**
 * UseCase для удаления заметки
 */
class DeleteNoteUseCase @Inject constructor(
    private val repository: BookmarksRepository,
) {
    suspend fun execute(noteId: String): Result<Unit> {
        return repository.deleteNote(noteId)
    }
}
