package com.github.axet.bookreader.domain

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.github.axet.bookreader.app.Storage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Данные о заметке из книги
 */
data class BookmarkData(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val bookAuthor: String,
    val bookCoverPath: String?,
    val bookFileUri: String?,         // URI файла книги для навигации
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
)

/**
 * Данные о книге с заметками
 */
data class BookWithNotesData(
    val bookId: String,
    val bookTitle: String,
    val bookAuthor: String,
    val bookCoverPath: String?,
    val notesCount: Int,
    val lastNoteDate: Long,
)

/**
 * Репозиторий для работы с заметками
 * Сканирует JSON файлы напрямую, не завися от наличия файлов книг
 */
@Singleton
class BookmarksRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val storage by lazy { Storage(context) }

    /**
     * Получить все заметки из всех книг
     * Сканирует JSON файлы напрямую, не завися от наличия файла книги
     */
    suspend fun getAllNotes(sortByDateAscending: Boolean = false): Result<List<BookmarkData>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val notes = mutableListOf<BookmarkData>()

                // Сканируем JSON файлы напрямую из хранилища
                val jsonFiles = listJsonFiles()

                for (jsonFile in jsonFiles) {
                    try {
                        val bookNotes = loadNotesFromJsonUri(jsonFile.uri, jsonFile.bookId)
                        notes.addAll(bookNotes)
                    } catch (e: Exception) {
                        Timber.e(e, "Error loading bookmarks from: ${jsonFile.uri}")
                    }
                }

                if (sortByDateAscending) {
                    notes.sortedBy { it.createdAt }
                } else {
                    notes.sortedByDescending { it.createdAt }
                }
            }
        }
    }

    /**
     * Получить заметки для конкретной книги
     */
    suspend fun getNotesForBook(bookId: String): Result<List<BookmarkData>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val jsonUri = findJsonFileForBook(bookId)
                if (jsonUri != null) {
                    loadNotesFromJsonUri(jsonUri, bookId)
                } else {
                    emptyList()
                }
            }
        }
    }

    /**
     * Получить список книг с заметками
     */
    suspend fun getBooksWithNotes(): Result<List<BookWithNotesData>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val booksWithNotes = mutableListOf<BookWithNotesData>()
                val jsonFiles = listJsonFiles()

                for (jsonFile in jsonFiles) {
                    try {
                        val notes = loadNotesFromJsonUri(jsonFile.uri, jsonFile.bookId)
                        if (notes.isNotEmpty()) {
                            // Получаем информацию о книге из JSON
                            val json = readJsonFromUri(jsonFile.uri)
                            booksWithNotes.add(
                                BookWithNotesData(
                                    bookId = jsonFile.bookId,
                                    bookTitle = json?.optString("title") ?: "Неизвестная книга",
                                    bookAuthor = json?.optString("authors") ?: "",
                                    bookCoverPath = null, // Обложка может быть недоступна для удалённых книг
                                    notesCount = notes.size,
                                    lastNoteDate = notes.maxOf { it.createdAt }
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error loading book info: ${jsonFile.bookId}")
                    }
                }

                booksWithNotes.sortedByDescending { it.lastNoteDate }
            }
        }
    }

    /**
     * Удалить заметку
     */
    suspend fun deleteNote(noteId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val parts = noteId.split("_")
                if (parts.size < 2) {
                    throw IllegalArgumentException("Invalid note ID format: $noteId")
                }

                val bookId = parts[0]
                val timestamp = parts[1].toLongOrNull() ?: 0L

                // Находим JSON файл
                val jsonUri = findJsonFileForBook(bookId)
                    ?: throw IllegalArgumentException("Book JSON not found: $bookId")

                // Читаем и обновляем JSON
                val json = readJsonFromUri(jsonUri)
                    ?: throw IllegalArgumentException("Cannot read JSON: $bookId")

                val bookmarksArray = json.optJSONArray("bookmarks")
                if (bookmarksArray != null) {
                    // Создаём новый массив без удаляемой заметки
                    val newArray = org.json.JSONArray()
                    for (i in 0 until bookmarksArray.length()) {
                        val bookmark = bookmarksArray.getJSONObject(i)
                        if (bookmark.optLong("last") != timestamp) {
                            newArray.put(bookmark)
                        }
                    }
                    json.put("bookmarks", newArray)

                    // Сохраняем обновлённый JSON
                    writeJsonToUri(jsonUri, json)
                    Timber.d("Note deleted: $noteId")
                }
            }
        }
    }

    /**
     * Список JSON файлов в хранилище
     */
    private data class JsonFileInfo(
        val uri: Uri,
        val bookId: String,
    )

    private fun listJsonFiles(): List<JsonFileInfo> {
        val files = mutableListOf<JsonFileInfo>()
        val storageUri = storage.storagePath
        val scheme = storageUri?.scheme

        try {
            when (scheme) {
                ContentResolver.SCHEME_CONTENT -> {
                    val contentResolver = context.contentResolver
                    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                        storageUri,
                        DocumentsContract.getTreeDocumentId(storageUri)
                    )
                    val cursor = contentResolver.query(childrenUri, null, null, null, null)
                    cursor?.use {
                        while (it.moveToNext()) {
                            val id = it.getString(it.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
                            val name = it.getString(it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME))

                            // Ищем JSON файлы с именем {md5}.json
                            if (name.endsWith(".json")) {
                                val bookId = name.substringBeforeLast(".")
                                if (bookId.length == Storage.MD5_SIZE) {
                                    val uri = DocumentsContract.buildDocumentUriUsingTree(storageUri, id)
                                    files.add(JsonFileInfo(uri, bookId))
                                }
                            }
                        }
                    }
                }
                ContentResolver.SCHEME_FILE -> {
                    val dir = File(storageUri.path!!)
                    dir.listFiles()?.forEach { file ->
                        if (file.extension == "json") {
                            val bookId = file.nameWithoutExtension
                            if (bookId.length == Storage.MD5_SIZE) {
                                files.add(JsonFileInfo(Uri.fromFile(file), bookId))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error listing JSON files")
        }

        return files
    }

    /**
     * Найти JSON файл для конкретной книги
     */
    private fun findJsonFileForBook(bookId: String): Uri? {
        val storageUri = storage.storagePath ?: return null
        val scheme = storageUri.scheme

        return try {
            when (scheme) {
                ContentResolver.SCHEME_CONTENT -> {
                    // Ищем файл {bookId}.json
                    val fileName = "$bookId.json"
                    val contentResolver = context.contentResolver
                    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                        storageUri,
                        DocumentsContract.getTreeDocumentId(storageUri)
                    )
                    val cursor = contentResolver.query(childrenUri, null, null, null, null)
                    cursor?.use {
                        while (it.moveToNext()) {
                            val id = it.getString(it.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
                            val name = it.getString(it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
                            if (name == fileName) {
                                return DocumentsContract.buildDocumentUriUsingTree(storageUri, id)
                            }
                        }
                    }
                    null
                }
                ContentResolver.SCHEME_FILE -> {
                    val file = File(storageUri.path!!, "$bookId.json")
                    if (file.exists()) Uri.fromFile(file) else null
                }
                else -> null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error finding JSON file for book: $bookId")
            null
        }
    }

    /**
     * Загрузить заметки из JSON файла
     */
    private fun loadNotesFromJsonUri(uri: Uri, bookId: String): List<BookmarkData> {
        val notes = mutableListOf<BookmarkData>()

        try {
            val json = readJsonFromUri(uri) ?: return notes
            val bookmarksArray = json.optJSONArray("bookmarks") ?: return notes

            val bookTitle = json.optString("title", "Неизвестная книга")
            val bookAuthor = json.optString("authors", "")

            // Получаем coverUrl из JSON (сохранённый при загрузке книги)
            val coverPath = json.optString("coverUrl", null) ?: findCoverForBook(bookId)

            // Получаем URI файла книги для навигации
            // Если не сохранён в JSON - ищем файл по MD5 в хранилище
            val bookFileUri = json.optString("bookFileUri", null) ?: findBookFileUri(bookId)

            for (i in 0 until bookmarksArray.length()) {
                try {
                    val bookmarkJson = bookmarksArray.getJSONObject(i)
                    val text = bookmarkJson.optString("text")
                    if (text.isBlank()) continue

                    val startArray = bookmarkJson.optJSONArray("start")
                    val endArray = bookmarkJson.optJSONArray("end")

                    notes.add(
                        BookmarkData(
                            id = "${bookId}_${bookmarkJson.optLong("last")}",
                            bookId = bookId,
                            bookTitle = bookTitle,
                            bookAuthor = bookAuthor,
                            bookCoverPath = coverPath,
                            bookFileUri = bookFileUri,
                            text = text,
                            name = bookmarkJson.optString("name").takeIf { it.isNotEmpty() },
                            page = calculatePageNumber(startArray?.optInt(0) ?: 0),
                            createdAt = bookmarkJson.optLong("last"),
                            startParagraph = startArray?.optInt(0) ?: 0,
                            startElement = startArray?.optInt(1) ?: 0,
                            startChar = startArray?.optInt(2) ?: 0,
                            endParagraph = endArray?.optInt(0) ?: 0,
                            endElement = endArray?.optInt(1) ?: 0,
                            endChar = endArray?.optInt(2) ?: 0,
                        )
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing bookmark at index $i")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading notes from JSON: $uri")
        }

        return notes
    }

    /**
     * Найти обложку книги в кэше по MD5
     */
    private fun findCoverForBook(bookId: String): String? {
        return try {
            // Обложки кэшируются в externalCacheDir
            val cacheDir = context.externalCacheDir ?: context.cacheDir
            if (cacheDir != null && cacheDir.exists()) {
                // Ищем файл с именем, содержащим bookId (MD5)
                cacheDir.listFiles()?.forEach { file ->
                    if (file.name.contains(bookId) &&
                        (file.extension == "png" || file.extension == "jpg" || file.extension == "jpeg")) {
                        return file.absolutePath
                    }
                }
            }
            null
        } catch (e: Exception) {
            Timber.e(e, "Error finding cover for book: $bookId")
            null
        }
    }

    /**
     * Прочитать JSON из URI
     */
    private fun readJsonFromUri(uri: Uri): JSONObject? {
        return try {
            val inputStream: InputStream? = when (uri.scheme) {
                ContentResolver.SCHEME_CONTENT -> context.contentResolver.openInputStream(uri)
                ContentResolver.SCHEME_FILE -> FileInputStream(File(uri.path!!))
                else -> null
            }

            inputStream?.use {
                val jsonStr = it.bufferedReader().readText()
                JSONObject(jsonStr)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reading JSON from URI: $uri")
            null
        }
    }

    /**
     * Записать JSON в URI
     */
    private fun writeJsonToUri(uri: Uri, json: JSONObject) {
        try {
            when (uri.scheme) {
                ContentResolver.SCHEME_CONTENT -> {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { os ->
                        os.write(json.toString(2).toByteArray())
                    }
                }
                ContentResolver.SCHEME_FILE -> {
                    File(uri.path!!).writeText(json.toString(2))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error writing JSON to URI: $uri")
            throw e
        }
    }

    /**
     * Найти URI файла книги по MD5 (bookId)
     * Ищет файл {md5}.{ext} в хранилище
     */
    fun findBookFileUri(bookId: String): String? {
        val storageUri = storage.storagePath ?: return null
        val scheme = storageUri.scheme

        return try {
            when (scheme) {
                ContentResolver.SCHEME_CONTENT -> {
                    val contentResolver = context.contentResolver
                    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                        storageUri,
                        DocumentsContract.getTreeDocumentId(storageUri)
                    )
                    val cursor = contentResolver.query(childrenUri, null, null, null, null)
                    cursor?.use {
                        while (it.moveToNext()) {
                            val name = it.getString(it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
                            // Ищем файл который начинается с bookId (MD5) и не является JSON
                            if (name.startsWith(bookId) && !name.endsWith(".json")) {
                                val id = it.getString(it.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
                                val uri = DocumentsContract.buildDocumentUriUsingTree(storageUri, id)
                                Timber.d("Found book file: $name for bookId: $bookId")
                                return uri.toString()
                            }
                        }
                    }
                    null
                }
                ContentResolver.SCHEME_FILE -> {
                    val dir = File(storageUri.path!!)
                    dir.listFiles()?.forEach { file ->
                        if (file.name.startsWith(bookId) && file.extension != "json") {
                            Timber.d("Found book file: ${file.name} for bookId: $bookId")
                            return Uri.fromFile(file).toString()
                        }
                    }
                    null
                }
                else -> null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error finding book file for bookId: $bookId")
            null
        }
    }

    private fun calculatePageNumber(paragraphIndex: Int): Int {
        return (paragraphIndex / 30) + 1
    }
}
