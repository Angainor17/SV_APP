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

    /**
     * Элемент картинки в новости
     * @Immutable - оптимизация Compose recomposition
     */
    @Immutable
    @Parcelize
    data class ItemImage(

        /** Изображение */
        override val image: String,
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

        /** Ссылка на видео */
        val link: String,
    ) : UiNewsMedia()
}
