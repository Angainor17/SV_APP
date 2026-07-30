package su.sv.news.presentation.root.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

/**
 * Модель медиа-контента новости
 * @Immutable - оптимизация Compose recomposition
 */
@Immutable
sealed class UiNewsMedia : Parcelable {

    abstract val image: String
    abstract val width: Int?
    abstract val height: Int?

    /**
     * Элемент картинки в новости
     * @Immutable - оптимизация Compose recomposition
     */
    @Immutable
    @Parcelize
    data class ItemImage(

        /** Изображение */
        override val image: String,

        /** Ширина изображения в пикселях */
        override val width: Int? = null,

        /** Высота изображения в пикселях */
        override val height: Int? = null,
    ) : UiNewsMedia()

    /**
     * Элемент видео в новости
     * @Immutable - оптимизация Compose recomposition
     */
    @Immutable
    @Parcelize
    data class ItemVideo(

        /** Идентификатор для хранения */
        val id: String,

        /** Превью видео */
        override val image: String,

        /** Ширина превью в пикселях */
        override val width: Int? = null,

        /** Высота превью в пикселях */
        override val height: Int? = null,

        /** Ссылка на видео */
        val link: String,
    ) : UiNewsMedia()
}
