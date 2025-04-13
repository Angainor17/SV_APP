package su.sv.news.domain.model

import java.time.LocalDateTime

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

    /** Центральная картинка */
    val images: List<String>,
)
