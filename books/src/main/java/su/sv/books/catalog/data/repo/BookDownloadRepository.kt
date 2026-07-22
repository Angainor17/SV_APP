package su.sv.books.catalog.data.repo

import android.app.DownloadManager
import android.content.ContentUris
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.provider.MediaStore
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
                MediaStore.Downloads._ID,
                MediaStore.Downloads.DISPLAY_NAME
            )
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)

            context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndex(MediaStore.Downloads._ID)
                if (cursor.moveToFirst() && idColumn >= 0) {
                    val id = cursor.getLong(idColumn)
                    ContentUris.withAppendedId(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
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
                MediaStore.Downloads._ID,
                MediaStore.Downloads.DISPLAY_NAME
            )
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileNameWithExt)

            context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndex(MediaStore.Downloads._ID)
                if (cursor.moveToFirst() && idColumn >= 0) {
                    val id = cursor.getLong(idColumn)
                    ContentUris.withAppendedId(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
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
     * Проверяет доступность URI без открытия stream (быстро)
     */
    private fun checkUriAccessible(uri: Uri): Boolean {
        return try {
            // Проверяем только наличие URI в MediaStore, без открытия stream
            // Это значительно быстрее чем openInputStream
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val projection = arrayOf(MediaStore.Downloads._ID)
                val selection = "${MediaStore.Downloads._ID} = ?"
                val id = ContentUris.parseId(uri)
                val selectionArgs = arrayOf(id.toString())

                context.contentResolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    cursor.count > 0
                } ?: false
            } else {
                // Для старых Android просто проверяем что URI не null
                uri != null
            }
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
     * На Android 10+ файлы из Downloads нужно удалять через DownloadManager.remove()
     * @param uri URI файла для удаления
     * @return Result.success если удаление успешно, Result.failure с сообщением если нет
     */
    fun deleteBook(uri: Uri): Result<Boolean> {
        Timber.d("Attempting to delete book: $uri, scheme=${uri.scheme}")

        return try {
            // 1. Пытаемся найти downloadId через DownloadManager.Query()
            val downloadId = findDownloadIdByUri(uri)

            if (downloadId != null && downloadId > 0) {
                // Удаляем через DownloadManager.remove() - это правильный способ для Android 10+
                val removed = downloadManager.remove(downloadId)
                Timber.d("DownloadManager.remove($downloadId) returned $removed")
                if (removed > 0) {
                    Timber.d("Book deleted successfully via DownloadManager: $uri")
                    return Result.success(true)
                }
            }

            // 2. Если DownloadManager не помог, пытаемся через ContentResolver
            if (uri.scheme == "content") {
                try {
                    val deletedRows = context.contentResolver.delete(uri, null, null)
                    Timber.d("ContentResolver.delete() returned $deletedRows rows")
                    if (deletedRows > 0) {
                        Timber.d("Book deleted successfully via ContentResolver: $uri")
                        return Result.success(true)
                    }
                } catch (e: SecurityException) {
                    Timber.w(e, "SecurityException - cannot delete via ContentResolver (Scoped Storage)")
                }
            }

            // 3. Если не удалось - возвращаем ошибку с понятным сообщением
            Timber.w("Failed to delete book: $uri - Scoped Storage restriction")
            Result.failure(Exception("Не удалось удалить файл. На Android 10+ приложение не может удалять файлы из папки Downloads, загруженные через систему."))
        } catch (e: Exception) {
            Timber.e(e, "Error deleting book: $uri")
            Result.failure(e)
        }
    }

    /**
     * Найти ID загрузки в DownloadManager по URI
     */
    private fun findDownloadIdByUri(uri: Uri): Long? {
        return try {
            val query = DownloadManager.Query()
            query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL)

            val cursor = downloadManager.query(query) ?: return null

            cursor.use {
                val idColumn = it.getColumnIndex(DownloadManager.COLUMN_ID)
                val localUriColumn = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                val titleColumn = it.getColumnIndex(DownloadManager.COLUMN_TITLE)

                while (it.moveToNext()) {
                    val id = if (idColumn >= 0) it.getLong(idColumn) else -1
                    val localUri = if (localUriColumn >= 0) it.getString(localUriColumn) else null
                    val title = if (titleColumn >= 0) it.getString(titleColumn) else null

                    // Проверяем совпадение по URI или по имени файла в URI
                    if (uri.toString() == localUri || uri.toString().contains(localUri ?: "") || localUri?.contains(uri.toString()) == true) {
                        Timber.d("Found downloadId=$id for uri=$uri (localUri=$localUri)")
                        return id
                    }

                    // Проверяем совпадение по пути файла
                    val uriPath = uri.path ?: uri.toString()
                    if (localUri != null && localUri.contains(uriPath.substringAfterLast("/"))) {
                        Timber.d("Found downloadId=$id by filename match (localUri=$localUri)")
                        return id
                    }
                }
            }

            Timber.d("No downloadId found for uri=$uri")
            null
        } catch (e: Exception) {
            Timber.e(e, "Error finding downloadId for uri=$uri")
            null
        }
    }

    /**
     * Получить имя файла из URI
     */
    private fun getFileNameFromUri(uri: Uri): String? {
        return try {
            when (uri.scheme) {
                "file" -> uri.path?.substringAfterLast("/")
                "content" -> {
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                            if (nameIndex >= 0) it.getString(nameIndex) else null
                        } else null
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting file name from URI: $uri")
            null
        }
    }
}
