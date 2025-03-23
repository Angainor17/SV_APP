package su.sv.books.catalog.presentation.root.model

/**
 * Статус скачивания файла
 */
enum class UIBookState {

    /** Происходит ли сейчас скачивание файла */
    DOWNLOADING,

    /** Есть ли в кеше скачанный файл */
    DOWNLOADED,

    /** Файл доступен для загрузки */
    AVAILABLE_TO_DOWNLOAD,
}
