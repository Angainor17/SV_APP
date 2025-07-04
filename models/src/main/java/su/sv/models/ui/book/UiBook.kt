package su.sv.models.ui.book

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Отображение информации о книге в списке
 */
@Parcelize
data class UiBook(

    /** Идентификатор для хранения */
    val id: String,

    /** Заголовок. Например: "ОВЛ1" */
    val title: String,

    /** Автор. Например: "В.И. Ленин" */
    val author: String,

    /** Описание/содержание. Например: "8 статей" */
    val description: String,

    /** Центральная картинка */
    val image: String,

    /** Ссылка для скачивания */
    val downloadUrl: String,

    /** Имя скачиваемого файла с раширением. Типо "Lenin.pdf" */
    val fileNameWithExt: String,

    /** Категория */
    val category: String,

    /** Скачанный файл */
    val fileUri: Uri?,

    /** Наличие скачанного файла */
    val downloadState: UIBookState,
) : Parcelable
