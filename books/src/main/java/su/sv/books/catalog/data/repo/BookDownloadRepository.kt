package su.sv.books.catalog.data.repo

import android.app.DownloadManager
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.net.Uri
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import su.sv.books.R
import su.sv.commonui.managers.ResourcesRepository
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
        Timber.tag("voronin").d("downloadID = $downloadID")
        return downloadID
    }

    /**
     * Получаем адрес скачановго файла
     * Может отсутствовать
     */
    fun getDownloadsUri(fileNameWithExt: String): Uri? {
        val folder = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
        val uri = Uri.withAppendedPath(Uri.fromFile(folder), fileNameWithExt)
        return uri.takeIf {
            val file = File(it.path.orEmpty())
            val fileSize = file.length() / 1024
            Timber.tag("voronin").d("$fileNameWithExt = size ${fileSize} | exists ${file.exists()}")
            file.exists() && fileSize > 0
        } // TODO: тут надо чекать на наличие или размер. А то в очереди может быть
    }
}
