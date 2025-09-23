package su.sv.news.presentation.root.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Отображение информации о новости в списке
 */
@Parcelize
data class UiNewsItem(

    /** Идентификатор для хранения */
    val id: String,

    /** Дата поста*/
    val dateFormatted: String,

    /** Описание/содержание. Например: "8 статей" */
    val description: String,

    /** Список видео. Может быть пустым */
    val videos: List<UiItemVideo>,

    /** Список картинок. Может быть пустым */
    val images: List<String>,
) : Parcelable
