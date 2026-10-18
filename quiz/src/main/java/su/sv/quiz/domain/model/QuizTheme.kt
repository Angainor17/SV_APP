package su.sv.quiz.domain.model

/**
 * Тема квизов.
 *
 * @param id уникальный идентификатор темы
 * @param name название темы
 * @param colorHex цвет темы (hex, опционально)
 * @param iconUrl иконка темы (опционально)
 * @param quizCount количество квизов в теме
 */
data class QuizTheme(
    val id: String,
    val name: String,
    val colorHex: String? = null,
    val iconUrl: String? = null,
    val quizCount: Int = 0,
)
