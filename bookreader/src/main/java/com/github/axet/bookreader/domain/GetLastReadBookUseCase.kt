package com.github.axet.bookreader.domain

import android.content.Context
import android.net.Uri
import android.util.Log
import com.github.axet.bookreader.app.Storage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val TAG = "GetLastReadBook"

/**
 * Информация о последней прочитанной книге для UI.
 */
data class LastReadBookInfo(
    val title: String,
    val authors: String?,
    val coverUrl: String?,
    val bookFileUri: String,
)

/**
 * UseCase для получения последней прочитанной книги.
 *
 * Получает список книг из хранилища, сортирует по времени последнего чтения
 * и возвращает информацию о книге с самым свежим timestamp.
 *
 * Важно: book.url - это URI файла книги в хранилище
 * book.info - это метаданные из JSON файла {md5}.json (создаётся при первом открытии книги)
 */
class GetLastReadBookUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Получить последнюю прочитанную книгу.
     *
     * @return информация о книге или null если нет книг или истории чтения
     */
    operator fun invoke(): LastReadBookInfo? {
        val storage = Storage(context)
        val books = storage.list()

        Log.d(TAG, "invoke: found ${books.size} books in storage")

        if (books.isEmpty()) {
            Log.d(TAG, "invoke: no books in storage")
            return null
        }

        // Фильтруем книги у которых есть история чтения (info.last > 0)
        val booksWithHistory = books.filter { book ->
            val lastTime = book.info?.last ?: 0L
            lastTime > 0L
        }

        Log.d(TAG, "invoke: ${booksWithHistory.size} books with reading history")

        if (booksWithHistory.isEmpty()) {
            Log.d(TAG, "invoke: no books with reading history")
            return null
        }

        // Сортируем по last timestamp (последние прочитанные - первые)
        val sortedBooks = booksWithHistory.sortedByDescending { book ->
            book.info?.last ?: 0L
        }

        // Берём первую книгу (последняя прочитанная)
        val lastBook = sortedBooks.first()

        Log.d(TAG, "invoke: last book = ${lastBook.info?.title}, last=${lastBook.info?.last}")

        // book.url - это URI файла книги (файл существует, т.к. Storage.list() его нашёл)
        val bookFileUri = lastBook.url.toString()

        // Метаданные из info
        val title = lastBook.info?.title
            ?: lastBook.info?.bookFileUri?.let { getFileName(it) }
            ?: "Книга"

        return LastReadBookInfo(
            title = title,
            authors = lastBook.info?.authors,
            coverUrl = lastBook.info?.coverUrl,
            bookFileUri = bookFileUri,
        )
    }

    /**
     * Получить имя файла из URI.
     */
    private fun getFileName(uriString: String): String {
        return try {
            val uri = Uri.parse(uriString)
            Storage.getName(context, uri)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file name: $uriString", e)
            "Книга"
        }
    }
}