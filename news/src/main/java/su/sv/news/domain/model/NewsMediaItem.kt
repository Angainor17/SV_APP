package su.sv.news.domain.model

sealed class NewsMediaItem {

    data class ImageItem(

        /** Превью видео */
        val image: String,
    ) : NewsMediaItem()

    data class VideoItem(

        /** Идентификатор */
        val id: String,

        /** Превью видео */
        val image: String,

        /** Ссылка на видео */
        val link: String,
    ) : NewsMediaItem()
}
