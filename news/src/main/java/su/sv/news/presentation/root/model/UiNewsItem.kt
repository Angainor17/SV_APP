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

    /** Центральная картинка */
    val image: String,
) : Parcelable
