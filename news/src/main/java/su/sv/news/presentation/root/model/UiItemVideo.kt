package su.sv.news.presentation.root.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Элемент видео в новости
 */
@Parcelize
data class UiItemVideo(

    /** Идентификатор для хранения */
    val id: String,

    /** Превью видео */
    val image: String,

    /** Ссылка на видео */
    val link: String,
) : Parcelable
