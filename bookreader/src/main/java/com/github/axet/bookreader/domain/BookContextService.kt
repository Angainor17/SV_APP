package com.github.axet.bookreader.domain

import android.content.Context
import android.net.Uri
import com.github.axet.bookreader.app.Storage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import org.geometerplus.fbreader.bookmodel.BookModel
import org.geometerplus.fbreader.formats.BookReadingException
import org.geometerplus.zlibrary.text.model.ZLTextParagraph
import su.sv.commonarchitecture.di.module.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Позиция заметки в тексте книги
 */
data class NotePosition(
    val startParagraph: Int,
    val startElement: Int,
    val startChar: Int,
    val endParagraph: Int,
    val endElement: Int,
    val endChar: Int,
)

/**
 * Результат получения контекста заметки
 *
 * @param sentenceBefore Текст предложения до заметки (без точки в конце)
 * @param noteText Текст заметки (выделенный фрагмент)
 * @param sentenceAfter Текст предложения после заметки (без точки в начале)
 */
data class NoteContextResult(
    val sentenceBefore: String?,
    val noteText: String,
    val sentenceAfter: String?,
)

/**
 * Сервис для получения контекста из книги
 *
 * Работает с FBReader internals (BookModel, ZLTextModel)
 * для извлечения полного предложения вокруг позиции заметки
 */
@Singleton
class BookContextService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
) {

    private val storage by lazy { Storage(context) }

    /**
     * Получить полное предложение с заметкой
     *
     * @param bookUri URI файла книги
     * @param position Позиция заметки в тексте
     * @param noteText Текст заметки для поиска в предложении
     * @return NoteContextResult с частями предложения или null при ошибке
     */
    suspend fun getNoteContext(
        bookUri: Uri,
        position: NotePosition,
        noteText: String,
    ): Result<NoteContextResult?> {
        return runCatching {
            withContext(dispatcherProvider.io) {
                extractNoteContext(bookUri, position, noteText)
            }
        }
    }

    /**
     * Вычислить MD5 хеш файла книги
     */
    suspend fun calculateMd5(bookUri: Uri): Result<String> {
        return runCatching {
            withContext(dispatcherProvider.io) {
                val book = storage.load(bookUri)
                book.md5
            }
        }
    }

    private fun extractNoteContext(
        bookUri: Uri,
        position: NotePosition,
        noteText: String,
    ): NoteContextResult? {
        var fbook: Storage.FBook? = null
        try {
            // 1. Загружаем книгу через Storage
            val storageBook = storage.load(bookUri)
            Timber.d("Loaded book: ${storageBook.md5}, ext: ${storageBook.ext}")

            // 2. Создаём FBook для работы с FBReader
            fbook = storage.read(storageBook)

            // 3. Получаем плагин для формата
            val info = Storage.Info(context)
            val plugin = Storage.getPlugin(info, fbook)

            // 4. Создаём BookModel
            val bookModel = BookModel.createModel(fbook.book, plugin)
            val textModel = bookModel.getTextModel()

            if (textModel == null) {
                Timber.w("TextModel is null for book: ${storageBook.md5}")
                return null
            }

            // 5. Извлекаем больший контекст вокруг заметки
            val contextParagraphs = 5
            val startParagraph = maxOf(0, position.startParagraph - contextParagraphs)
            val endParagraph = minOf(
                textModel.getParagraphsNumber() - 1,
                position.endParagraph + contextParagraphs
            )

            val fullText = extractTextBetweenParagraphs(
                textModel = textModel,
                startParagraph = startParagraph,
                endParagraph = endParagraph
            )

            if (fullText == null) {
                return NoteContextResult(
                    sentenceBefore = null,
                    noteText = noteText,
                    sentenceAfter = null,
                )
            }

            // 6. Находим заметку в тексте и извлекаем предложение
            return extractSentenceWithContext(fullText, noteText)

        } catch (e: BookReadingException) {
            Timber.e(e, "BookReadingException while extracting context from: $bookUri")
            return null
        } catch (e: Exception) {
            Timber.e(e, "Error extracting context from: $bookUri")
            return null
        } finally {
            // Очищаем временные файлы
            fbook?.close()
        }
    }

    /**
     * Извлечь предложение с заметкой из текста
     */
    private fun extractSentenceWithContext(
        fullText: String,
        noteText: String,
    ): NoteContextResult {
        // Ищем заметку в тексте (без учёта регистра для надёжности)
        val noteIndex = fullText.indexOf(noteText)

        if (noteIndex == -1) {
            // Если не нашли точное совпадение, пробуем без учёта регистра
            val lowerFullText = fullText.lowercase()
            val lowerNoteText = noteText.lowercase()
            val lowerIndex = lowerFullText.indexOf(lowerNoteText)

            if (lowerIndex == -1) {
                // Не нашли заметку - возвращаем только текст заметки
                return NoteContextResult(
                    sentenceBefore = null,
                    noteText = noteText,
                    sentenceAfter = null,
                )
            }

            // Нашли без учёта регистра - используем оригинальный текст
            val actualNoteText = fullText.substring(lowerIndex, lowerIndex + noteText.length)
            return splitSentenceAtNote(fullText, lowerIndex, actualNoteText)
        }

        return splitSentenceAtNote(fullText, noteIndex, noteText)
    }

    /**
     * Разделить предложение на части: до заметки, заметка, после заметки
     */
    private fun splitSentenceAtNote(
        fullText: String,
        noteIndex: Int,
        noteText: String,
    ): NoteContextResult {
        // Находим начало предложения (ищем точку/восклицательный/вопросительный знак назад)
        val sentenceStart = findSentenceStart(fullText, noteIndex)

        // Находим конец предложения (ищем точку/восклицательный/вопросительный знак вперёд)
        val sentenceEnd = findSentenceEnd(fullText, noteIndex + noteText.length)

        // Извлекаем части
        val beforeNote = fullText.substring(sentenceStart, noteIndex).trim()
        val afterNote = fullText.substring(noteIndex + noteText.length, sentenceEnd).trim()

        return NoteContextResult(
            sentenceBefore = if (beforeNote.isEmpty()) null else beforeNote,
            noteText = noteText,
            sentenceAfter = if (afterNote.isEmpty()) null else afterNote,
        )
    }

    /**
     * Найти начало предложения (назад от позиции до точки/!/?)
     */
    private fun findSentenceStart(text: String, fromIndex: Int): Int {
        var index = fromIndex - 1
        while (index >= 0) {
            val char = text[index]
            if (char == '.' || char == '!' || char == '?' || char == '\n') {
                // Нашли конец предыдущего предложения - начинаем со следующего символа
                return index + 1
            }
            index--
        }
        return 0
    }

    /**
     * Найти конец предложения (вперёд от позиции до точки/!/?)
     */
    private fun findSentenceEnd(text: String, fromIndex: Int): Int {
        var index = fromIndex
        while (index < text.length) {
            val char = text[index]
            if (char == '.' || char == '!' || char == '?') {
                // Включаем знак препинания
                return index + 1
            }
            if (char == '\n') {
                // Перенос строки - конец предложения без знака
                return index
            }
            index++
        }
        return text.length
    }

    /**
     * Извлечь текст между параграфами
     */
    private fun extractTextBetweenParagraphs(
        textModel: org.geometerplus.zlibrary.text.model.ZLTextModel,
        startParagraph: Int,
        endParagraph: Int,
    ): String? {
        val sb = StringBuilder()

        for (i in startParagraph..endParagraph) {
            val paragraph = textModel.getParagraph(i) ?: continue
            val iterator = paragraph.iterator()

            while (iterator.next()) {
                val entryType = iterator.getType()
                if (entryType == ZLTextParagraph.Entry.TEXT) {
                    val data = iterator.getTextData()
                    val offset = iterator.getTextOffset()
                    val length = iterator.getTextLength()
                    sb.append(String(data, offset, length))
                }
            }
            sb.append(" ")
        }

        val result = sb.toString().trim()
        return if (result.isEmpty()) null else result
    }
}