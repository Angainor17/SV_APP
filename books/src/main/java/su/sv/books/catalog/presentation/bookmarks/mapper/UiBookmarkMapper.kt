package su.sv.books.catalog.presentation.bookmarks.mapper

import su.sv.books.catalog.domain.model.BookWithNotes
import su.sv.books.catalog.domain.model.BookmarkNote
import su.sv.books.catalog.domain.model.cleanBookmarkText
import su.sv.books.catalog.presentation.bookmarks.model.UiBookWithNotes
import su.sv.books.catalog.presentation.bookmarks.model.UiBookmarkNote
import javax.inject.Inject

class UiBookmarkMapper @Inject constructor() {

    fun mapNote(domain: BookmarkNote): UiBookmarkNote {
        return UiBookmarkNote(
            id = domain.id,
            bookId = domain.bookId,
            bookTitle = domain.bookTitle,
            bookAuthor = domain.bookAuthor,
            bookCoverUrl = domain.bookCoverUrl,
            bookFileUri = domain.bookFileUri,
            text = cleanBookmarkText(domain.text), // Очищаем текст от лишних символов
            name = domain.name,
            page = domain.page,
            createdAt = domain.createdAt,
            startParagraph = domain.startParagraph,
            startElement = domain.startElement,
            startChar = domain.startChar,
            endParagraph = domain.endParagraph,
            endElement = domain.endElement,
            endChar = domain.endChar,
        )
    }

    fun mapNotes(domain: List<BookmarkNote>): List<UiBookmarkNote> {
        return domain.map { mapNote(it) }
    }

    fun mapBookWithNotes(domain: BookWithNotes): UiBookWithNotes {
        return UiBookWithNotes(
            bookId = domain.bookId,
            bookTitle = domain.bookTitle,
            bookAuthor = domain.bookAuthor,
            bookCoverUrl = domain.bookCoverUrl,
            notesCount = domain.notesCount,
        )
    }

    fun mapBooksWithNotes(domain: List<BookWithNotes>): List<UiBookWithNotes> {
        return domain.map { mapBookWithNotes(it) }
    }
}
