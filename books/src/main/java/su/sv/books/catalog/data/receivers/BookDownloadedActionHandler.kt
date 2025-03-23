package su.sv.books.catalog.data.receivers

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * По сути своей event-bus
 * Переносит уведомление о скачивании файла от BroadcastReceiver до VM
 */
@Singleton
class BookDownloadedActionHandler @Inject constructor() {

    private val _sharedStateFlow = MutableSharedFlow<BookState>()
    val sharedStateFlow: SharedFlow<BookState> get() = _sharedStateFlow

    suspend fun postFileDownloaded(downloadID: Long) {
        _sharedStateFlow.emit(
            BookState(
                downloadID = downloadID,
            )
        )
    }

    data class BookState(
        val downloadID: Long,
    )
}