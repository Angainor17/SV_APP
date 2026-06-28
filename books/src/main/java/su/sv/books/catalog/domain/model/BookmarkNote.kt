package su.sv.books.catalog.domain.model

/**
 * Модель заметки (закладки) из книги
 */
data class BookmarkNote(
    val id: String,                  // Уникальный ID (md5_book + timestamp)
    val bookId: String,              // MD5 книги
    val bookTitle: String,           // Название книги
    val bookAuthor: String,          // Автор книги
    val bookCoverUrl: String,        // URL обложки книги
    val bookFileUri: String?,        // URI файла книги для навигации
    val text: String,                // Текст заметки
    val name: String?,               // Название заметки (опционально)
    val page: Int,                   // Номер страницы
    val createdAt: Long,             // Timestamp создания
    val startParagraph: Int,         // Начальная позиция - параграф
    val startElement: Int,           // Начальная позиция - элемент
    val startChar: Int,              // Начальная позиция - символ
    val endParagraph: Int,           // Конечная позиция - параграф
    val endElement: Int,             // Конечная позиция - элемент
    val endChar: Int,                // Конечная позиция - символ
) {
    /**
     * Позиция для навигации к заметке в читалке
     */
    fun toPositionString(): String {
        return "$startParagraph:$startElement:$startChar"
    }
}

/**
 * Книга с заметками для режима отображения "по книгам"
 */
data class BookWithNotes(
    val bookId: String,
    val bookTitle: String,
    val bookAuthor: String,
    val bookCoverUrl: String,
    val notesCount: Int,
    val lastNoteDate: Long,          // Timestamp последней заметки
)
