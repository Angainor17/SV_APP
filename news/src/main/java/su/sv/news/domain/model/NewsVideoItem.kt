package su.sv.news.domain.model

data class NewsVideoItem (

    /** Идентификатор */
    val id: String,

    /** Превью видео */
    val image: String,

    /** Ссылка на видео */
    val link: String,
)