package su.sv.books.catalog.presentation

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Общая информаия для считывания статуса загрузки
 */
@Singleton
class CommonDownloadBookStates @Inject constructor() {

    /** Список downloadId, которые в прогрессе скачивания. Значение - Book.id */
    val loadingInProgressMap = hashMapOf<Long, String>()
}