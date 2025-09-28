package su.sv.news.presentation.root.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class UiNewsMedia : Parcelable {

    abstract val image: String

    /**
     * Элемент картинки в новости
     */
    @Parcelize
    data class ItemImage(

        /** Изображение */
        override val image: String,
    ) : UiNewsMedia()

    /**
     * Элемент видео в новости
     */
    @Parcelize
    data class ItemVideo(

        /** Идентификатор для хранения */
        val id: String,

        /** Превью видео */
        override val image: String,

        /** Ссылка на видео */
        val link: String,
    ) : UiNewsMedia()
}
