package su.sv.news.domain.model

sealed class NewsMediaItem {

    data class ImageItem(

        /** Изображение */
        val image: String,

        /** Ширина изображения в пикселях */
        val width: Int? = null,

        /** Высота изображения в пикселях */
        val height: Int? = null,
    ) : NewsMediaItem()

    data class VideoItem(

        /** Идентификатор */
        val id: String,

        /** Превью видео */
        val image: String,

        /** Ширина превью в пикселях */
        val width: Int? = null,

        /** Высота превью в пикселях */
        val height: Int? = null,

        /** Ссылка на видео */
        val link: String,
    ) : NewsMediaItem()
}
