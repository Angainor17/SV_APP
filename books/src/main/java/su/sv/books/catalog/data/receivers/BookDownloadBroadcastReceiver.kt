package su.sv.books.catalog.data.receivers

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Слушаем события скачивания (или ошибки) файла книги через DownloadManager
 * Иначе никак, т.к. такое вот АПИ
 */
@AndroidEntryPoint
class BookDownloadBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var downloadedActionHandler: BookDownloadedActionHandler

    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.tag("voronin").d("onReceive = ${downloadedActionHandler}")

        val downloadID: Long = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: return
        Timber.tag("voronin").d("BookDownloadBroadcastReceiver EXTRA_DOWNLOAD_ID = $downloadID")

        downloadedActionHandler.postFileDownloaded(downloadID)
    }
}
