package su.sv.books.catalog.data.repo

import android.app.DownloadManager
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import su.sv.books.R
import su.sv.commonarchitecture.managers.ResourcesRepository
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class BookDownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val resourcesRepository: ResourcesRepository,
) {

    private val downloadManager by lazy {
        context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
    }

    /**
     * Запускаем скачивание файла и ждём результат
     */
    fun downloadBook(url: String, bookTitle: String, fileNameWithExt: String): Long {
        val request = DownloadManager.Request(url.toUri())
            .setTitle(bookTitle)
            .setDescription(resourcesRepository.getString(R.string.books_download_description))

            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(DIRECTORY_DOWNLOADS, fileNameWithExt)

            .setRequiresCharging(false)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadID = downloadManager.enqueue(request)
        Timber.d("downloadID = $downloadID")
        return downloadID
    }

    /**
     * Получаем адрес скачанного файла
     * Для Android 10+ используем DownloadManager.getUriForDownloadedFile()
     */
    fun getDownloadsUri(fileNameWithExt: String): Uri? {
        Timber.d("Looking for file: $fileNameWithExt")

        // Извлекаем базовое имя для поиска
        // "В.помощь.товарищу.2025.pdf" -> ["В", "помощь", "товарищу", "2025"]
        val fileNameWithoutExt = fileNameWithExt.substringBeforeLast(".")
        val extension = fileNameWithExt.substringAfterLast(".", "")

        // Убираем год и спецсимволы из имени для поиска
        // "Краткая.история.Будущего.2024_А6" -> "краткая история будущего"
        val normalizedSearch = fileNameWithoutExt
            .replace(Regex("[._]"), " ")
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\s*\\d{4}.*"), "") // Убираем год и всё после него
            .trim()
            .lowercase()

        // Извлекаем ключевые слова (первые 2-3 слова)
        val searchWords = normalizedSearch.split(" ").filter { it.length > 2 }.take(3)

        Timber.d("Search words: $searchWords")

        // Ищем успешную загрузку
        val query = DownloadManager.Query()
        query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL)

        val cursor = downloadManager.query(query) ?: return null

        cursor.use {
            val idColumn = it.getColumnIndex(DownloadManager.COLUMN_ID)
            val titleColumn = it.getColumnIndex(DownloadManager.COLUMN_TITLE)
            val localUriColumn = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)

            Timber.d("Cursor count: ${it.count}")

            // Собираем все подходящие загрузки с оценкой совпадения
            val matches = mutableListOf<Triple<Long, String, Int>>() // id, localUri, score

            while (it.moveToNext()) {
                val id = if (idColumn >= 0) it.getLong(idColumn) else -1
                val title = if (titleColumn >= 0) it.getString(titleColumn) else null
                val localUri = if (localUriColumn >= 0) it.getString(localUriColumn) else null

                Timber.d("Download: id=$id, title=$title, localUri=$localUri")

                if (id < 0) continue

                // Оцениваем совпадение
                var score = 0

                // Точное совпадение по имени файла
                if (localUri != null && localUri.endsWith(fileNameWithExt)) {
                    score = 100
                }
                // Совпадение по ключевым словам в title
                else if (title != null) {
                    val normalizedTitle = title.lowercase()
                    var matchedWords = 0
                    for (word in searchWords) {
                        if (normalizedTitle.contains(word)) {
                            matchedWords++
                        }
                    }
                    // Если нашли хотя бы 2 ключевых слова
                    if (matchedWords >= 2) {
                        score = matchedWords * 10
                    }
                }
                // Совпадение с суффиксом (файл-1.pdf, файл-2.pdf)
                else if (localUri != null && localUri.contains(normalizedSearch.replace(" ", ".")) && localUri.endsWith(".$extension")) {
                    score = 50
                }

                if (score > 0) {
                    matches.add(Triple(id, localUri ?: "", score))
                }
            }

            // Сортируем по убыванию score
            matches.sortByDescending { it.third }

            Timber.d("Matches: ${matches.map { Triple(it.first, it.second, it.third) }}")

            // Пробуем каждую подходящую загрузку
            for ((id, localUri, score) in matches) {
                // 1. Пробуем getUriForDownloadedFile
                val uri = downloadManager.getUriForDownloadedFile(id)
                Timber.d("getUriForDownloadedFile($id) = $uri (score=$score)")

                if (uri != null && checkUriAccessible(uri)) {
                    Timber.d("Found accessible URI: $uri")
                    return uri
                }

                // 2. Пробуем через MediaStore по localUri
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && localUri.isNotEmpty()) {
                    val mediaUri = findFileByLocalUri(localUri)
                    if (mediaUri != null && checkUriAccessible(mediaUri)) {
                        Timber.d("Found via MediaStore: $mediaUri")
                        return mediaUri
                    }
                }
            }
        }

        // Fallback: пробуем найти через MediaStore по имени
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val mediaUri = findFileViaMediaStore(fileNameWithExt)
            if (mediaUri != null && checkUriAccessible(mediaUri)) {
                Timber.d("Found via MediaStore fallback: $mediaUri")
                return mediaUri
            }
        }

        // Fallback: прямой путь для старых Android
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val folder = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
            val file = File(folder, fileNameWithExt)
            if (file.exists()) {
                Timber.d("Found via direct path: ${file.absolutePath}")
                return Uri.fromFile(file)
            }
        }

        Timber.w("File not found: $fileNameWithExt")
        return null
    }

    /**
     * Поиск файла через MediaStore по localUri из DownloadManager
     */
    private fun findFileByLocalUri(localUri: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

        return try {
            // Извлекаем имя файла из localUri
            val fileName = Uri.decode(localUri.substringAfterLast("/"))
            Timber.d("Searching MediaStore for: $fileName")

            val projection = arrayOf(
                android.provider.MediaStore.Downloads._ID,
                android.provider.MediaStore.Downloads.DISPLAY_NAME
            )
            val selection = "${android.provider.MediaStore.Downloads.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)

            context.contentResolver.query(
                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndex(android.provider.MediaStore.Downloads._ID)
                if (cursor.moveToFirst() && idColumn >= 0) {
                    val id = cursor.getLong(idColumn)
                    android.content.ContentUris.withAppendedId(
                        android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        id
                    )
                } else null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error querying MediaStore by localUri")
            null
        }
    }

    /**
     * Поиск файла через MediaStore (Android 10+)
     */
    private fun findFileViaMediaStore(fileNameWithExt: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

        return try {
            val projection = arrayOf(
                android.provider.MediaStore.Downloads._ID,
                android.provider.MediaStore.Downloads.DISPLAY_NAME
            )
            val selection = "${android.provider.MediaStore.Downloads.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileNameWithExt)

            context.contentResolver.query(
                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndex(android.provider.MediaStore.Downloads._ID)
                if (cursor.moveToFirst() && idColumn >= 0) {
                    val id = cursor.getLong(idColumn)
                    android.content.ContentUris.withAppendedId(
                        android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        id
                    )
                } else null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error querying MediaStore")
            null
        }
    }

    /**
     * Проверяет доступность URI
     */
    private fun checkUriAccessible(uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.available() >= 0
            } ?: false
        } catch (e: Exception) {
            Timber.w(e, "URI not accessible: $uri")
            false
        }
    }

    /**
     * Проверяет существует ли файл (для отображения статуса)
     */
    fun fileExists(fileNameWithExt: String): Boolean {
        return getDownloadsUri(fileNameWithExt) != null
    }

    /**
     * Удаляет скачанный файл книги
     * @param uri URI файла для удаления
     * @return true если удаление успешно, иначе false
     */
    fun deleteBook(uri: Uri): Boolean {
        return try {
            val deletedRows = context.contentResolver.delete(uri, null, null)
            val success = deletedRows > 0
            if (success) {
                Timber.d("Book deleted successfully: $uri")
            } else {
                Timber.w("Book deletion returned 0 rows: $uri")
            }
            success
        } catch (e: Exception) {
            Timber.e(e, "Error deleting book: $uri")
            false
        }
    }
}
