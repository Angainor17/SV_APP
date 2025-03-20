package su.sv.books.catalog.presentation.root.model

data class UiBook(

    /** Идентификатор для хранения */
    val id: String,

    /** Заголовок. Например: "ОВЛ1" */
    val title: String,

    /** Описание/содержание. Например: "8 статей" */
    val description: String,

    /** Центральная картинка */
    val image: String,

    /** Ссылка для скачивания */
    val link: String,

    /** Количество страниц в формате "1 231 стр." */
    val pagesCountFormatted: String,

    /** Дата публикации книги в формате "6 дек. 2021 г." */
    val dateFormatted: String,

    /** Есть ли в кеше скачанный файл */
    val isDownloaded: Boolean,
)
