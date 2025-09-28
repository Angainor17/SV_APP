package su.sv.news.domain.model

import org.threeten.bp.LocalDateTime

/**
 * Отображение информации о новости в списке
 */
data class NewsItem(

    /** Идентификатор для хранения */
    val id: String?,

    /** Дата публикации */
    val date: LocalDateTime?,

    /** Описание/содержание */
    val description: String?,

    /** Центральная картинка (список) */
    val images: List<NewsMediaItem.ImageItem>,

    /** Список видео */
    val videos: List<NewsMediaItem.VideoItem>,

    /** Список медиа */
    val mediaItems: List<NewsMediaItem>,
)
