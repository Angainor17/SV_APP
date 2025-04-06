package su.sv.models.ui.book

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Статус скачивания файла
 */
@Parcelize
enum class UIBookState : Parcelable {

    /** Происходит ли сейчас скачивание файла */
    DOWNLOADING,

    /** Есть ли в кеше скачанный файл */
    DOWNLOADED,

    /** Файл доступен для загрузки */
    AVAILABLE_TO_DOWNLOAD,
}
