package su.sv.books.catalog.data.repo

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import su.sv.books.R
import su.sv.commonui.managers.ResourcesRepository
import javax.inject.Inject

class BookDownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val resourcesRepository: ResourcesRepository,
) {

    private val downloadIntentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)

    private val downloadManager by lazy {
        context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
    }

    /**
     * Запускаем скачивание файла и ждём результат
     */
    fun downloadBook(url: String, bookTitle: String, fileNameWithExt: String): Flow<Uri?> {
        val request = DownloadManager.Request(url.toUri())
            .setTitle(bookTitle)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION)
            .setDestinationInExternalPublicDir(DIRECTORY_DOWNLOADS, fileNameWithExt)
            .setDescription(resourcesRepository.getString(R.string.books_download_description))
            .setRequiresCharging(false)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val flow = MutableSharedFlow<Uri?>()
        val downloadID = downloadManager.enqueue(request)
        var receiver: BroadcastReceiver? = null

        val onComplete = fun(isSuccess: Boolean) {
            // перестаём слушать после получения результата скачивания
            context.unregisterReceiver(receiver)

            flow.tryEmit(
                if (isSuccess) getDownloadsUri(fileNameWithExt) else null
            )
        }
        receiver = createReceiver(downloadID, onComplete)

        // слушаем результат скачивания
        context.registerReceiver(receiver, downloadIntentFilter)

        return flow
    }

    /**
     * Получаем адрес скачановго файла
     */
    fun getDownloadsUri(fileNameWithExt: String): Uri {
        val file = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
        return Uri.withAppendedPath(Uri.fromFile(file), fileNameWithExt)
    }

    private fun createReceiver(
        downloadID: Long,
        onComplete: (Boolean) -> Unit,
    ): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE != intent.action) return

                val query = DownloadManager.Query().setFilterById(downloadID)
                val cursor = downloadManager.query(query)

                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (columnIndex < 0) return

                    when (cursor.getInt(columnIndex)) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            onComplete(true)
                        }

                        DownloadManager.STATUS_FAILED -> {
                            onComplete(false)
                        }
                    }
                }
            }
        }
    }
}