package su.sv.books.catalog.data.repo

import com.github.axet.bookreader.domain.BookWithNotesData
import com.github.axet.bookreader.domain.BookmarkData
import su.sv.books.catalog.domain.model.BookWithNotes
import su.sv.books.catalog.domain.model.BookmarkNote
import javax.inject.Inject
import javax.inject.Singleton
import su.sv.books.catalog.domain.repository.BookmarksRepository as BooksBookmarksRepository

/**
 * Реализация репозитория заметок - делегирует вызовы в bookreader модуль
 */
@Singleton
class BookmarksRepositoryImpl @Inject constructor(
    private val bookreaderRepository: com.github.axet.bookreader.domain.BookmarksRepository,
) : BooksBookmarksRepository {

    override suspend fun getAllNotes(sortByDateAscending: Boolean): Result<List<BookmarkNote>> {
        return bookreaderRepository.getAllNotes(sortByDateAscending).map { notes ->
            notes.map { it.toDomain() }
        }
    }

    override suspend fun getNotesForBook(bookId: String): Result<List<BookmarkNote>> {
        return bookreaderRepository.getNotesForBook(bookId).map { notes ->
            notes.map { it.toDomain() }
        }
    }

    override suspend fun getBooksWithNotes(): Result<List<BookWithNotes>> {
        return bookreaderRepository.getBooksWithNotes().map { books ->
            books.map { it.toDomain() }
        }
    }

    override suspend fun deleteNote(noteId: String): Result<Unit> {
        return bookreaderRepository.deleteNote(noteId)
    }

    private fun BookmarkData.toDomain() = BookmarkNote(
        id = id,
        bookId = bookId,
        bookTitle = bookTitle,
        bookAuthor = bookAuthor,
        bookCoverUrl = bookCoverPath ?: "",
        bookFileUri = bookFileUri,
        text = text,
        name = name,
        page = page,
        createdAt = createdAt,
        startParagraph = startParagraph,
        startElement = startElement,
        startChar = startChar,
        endParagraph = endParagraph,
        endElement = endElement,
        endChar = endChar,
    )

    private fun BookWithNotesData.toDomain() = BookWithNotes(
        bookId = bookId,
        bookTitle = bookTitle,
        bookAuthor = bookAuthor,
        bookCoverUrl = bookCoverPath ?: "",
        notesCount = notesCount,
        lastNoteDate = lastNoteDate,
    )
}
