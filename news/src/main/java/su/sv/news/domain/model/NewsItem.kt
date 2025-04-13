package su.sv.news.domain.model

/**
 * Отображение информации о новости в списке
 */
data class NewsItem(

    /** Идентификатор для хранения */
    val id: String?,

    /** Заголовок. Например: "ОВЛ1" */
    val title: String?,

    /** Описание/содержание */
    val description: String?,

    /** Центральная картинка */
    val image: String?,
)
