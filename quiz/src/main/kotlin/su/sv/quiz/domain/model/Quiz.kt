package su.sv.quiz.domain.model

/**
 * Квиз (тест).
 *
 * @param id уникальный идентификатор квиза
 * @param title название квиза
 * @param description описание (опционально)
 * @param themeId идентификатор темы (опционально)
 * @param questions список вопросов
 * @param createdAtEpochMillis дата создания (epoch millis)
 * @param imageUrl изображение (опционально)
 */
data class Quiz(
    val id: String,
    val title: String,
    val description: String? = null,
    val themeId: String? = null,
    val questions: List<QuizQuestion>,
    val createdAtEpochMillis: Long,
    val imageUrl: String? = null,
)
