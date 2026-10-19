package su.sv.books.catalog.domain

import android.net.Uri
import com.github.axet.bookreader.domain.BookContextService
import com.github.axet.bookreader.domain.NotePosition
import su.sv.books.catalog.domain.model.BookmarkNote
import javax.inject.Inject

/**
 * Результат получения контекста заметки
 *
 * @param note Заметка
 * @param sentenceBefore Текст предложения до заметки
 * @param sentenceAfter Текст предложения после заметки
 */
data class NoteWithContext(
    val note: BookmarkNote,
    val sentenceBefore: String?,
    val sentenceAfter: String?,
)

/**
 * UseCase для получения контекста вокруг заметки из книги
 *
 * Извлекает полное предложение, в котором находится заметка,
 * разделяя его на части: до заметки, сама заметка, после заметки
 */
class GetNoteContextUseCase @Inject constructor(
    private val bookContextService: BookContextService,
) {

    /**
     * Получить контекст заметки (полное предложение)
     *
     * @param fileUri URI файла книги
     * @param note Заметка с позициями в тексте
     * @return NoteWithContext с частями предложения или null при ошибке
     */
    suspend fun execute(
        fileUri: Uri,
        note: BookmarkNote,
    ): Result<NoteWithContext?> {
        val position = NotePosition(
            startParagraph = note.startParagraph,
            startElement = note.startElement,
            startChar = note.startChar,
            endParagraph = note.endParagraph,
            endElement = note.endElement,
            endChar = note.endChar,
        )

        // Передаём текст заметки для поиска в предложении
        return bookContextService.getNoteContext(fileUri, position, note.text)
            .map { contextResult ->
                contextResult?.let {
                    NoteWithContext(
                        note = note,
                        sentenceBefore = it.sentenceBefore,
                        sentenceAfter = it.sentenceAfter,
                    )
                }
            }
    }
}