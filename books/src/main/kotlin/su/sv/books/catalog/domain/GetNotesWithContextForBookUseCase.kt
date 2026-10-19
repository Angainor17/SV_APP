package su.sv.books.catalog.domain

import android.net.Uri
import su.sv.books.catalog.presentation.detail.model.UiNoteWithContext
import timber.log.Timber
import javax.inject.Inject

/**
 * UseCase для получения заметок книги с контекстом
 *
 * Использует сохранённый контекст из BookmarkNote (для новых заметок).
 * Если контекст отсутствует (старые заметки), не вычисляет его -
 * миграция происходит при открытии книги в ReaderViewModel.
 */
class GetNotesWithContextForBookUseCase @Inject constructor(
    private val calculateMd5UseCase: CalculateBookMd5UseCase,
    private val getNotesForBookUseCase: GetNotesForBookUseCase,
) {

    /**
     * Получить заметки с контекстом для книги
     *
     * @param fileUri URI файла книги
     * @param limit Максимальное количество заметок (default: 5)
     * @return Список UiNoteWithContext с контекстом
     */
    suspend fun execute(
        fileUri: Uri,
        limit: Int = DEFAULT_LIMIT,
    ): Result<List<UiNoteWithContext>> {
        return runCatching {
            // 1. Вычисляем MD5 из URI
            val md5 = calculateMd5UseCase.execute(fileUri).getOrNull()

            if (md5 == null) {
                Timber.w("Failed to calculate MD5 for fileUri: $fileUri")
                return Result.success(emptyList())
            }

            // 2. Получаем заметки по MD5 (уже с сохранённым контекстом)
            val notes = getNotesForBookUseCase.execute(md5).getOrNull() ?: emptyList()

            if (notes.isEmpty()) {
                return Result.success(emptyList())
            }

            // 3. Берём только первые limit заметок
            val limitedNotes = notes.take(limit)

            // 4. Преобразуем в UI модель (контекст уже сохранён)
            limitedNotes.map { note ->
                UiNoteWithContext(
                    id = note.id,
                    text = note.text,
                    sentenceBefore = note.sentenceBefore,
                    sentenceAfter = note.sentenceAfter,
                    page = note.page,
                    createdAt = note.createdAt,
                    startParagraph = note.startParagraph,
                    startElement = note.startElement,
                    startChar = note.startChar,
                    endParagraph = note.endParagraph,
                    endElement = note.endElement,
                    endChar = note.endChar,
                )
            }
        }
    }

    private companion object {
        const val DEFAULT_LIMIT = 5
    }
}