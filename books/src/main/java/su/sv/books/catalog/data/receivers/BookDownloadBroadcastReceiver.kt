package su.sv.books.catalog.data.receivers

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import su.sv.commonarchitecture.di.module.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

/**
 * Слушаем события скачивания (или ошибки) файла книги через DownloadManager
 * Иначе никак, т.к. такое вот АПИ
 */
@AndroidEntryPoint
class BookDownloadBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var downloadedActionHandler: BookDownloadedActionHandler
    @Inject lateinit var dispatcherProvider: DispatcherProvider

    override fun onReceive(context: Context?, intent: Intent?) {
        val downloadID: Long = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: return
        Timber.tag("voronin").d("BookDownloadBroadcastReceiver EXTRA_DOWNLOAD_ID = $downloadID")

        CoroutineScope(dispatcherProvider.default).launch {
            downloadedActionHandler.postFileDownloaded(downloadID)
        }
    }
}
