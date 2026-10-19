package su.sv.quiz.domain.model

/**
 * Тип вопроса квиза.
 *
 * Пока реализован только [SINGLE_CHOICE]; [TEXT_INPUT] зарезервирован
 * под вопросы со свободным вводом (см. ТЗ, «Тип ответов: микс»).
 */
enum class QuestionType {
    SINGLE_CHOICE,
    TEXT_INPUT,
}

/**
 * Вариант ответа на вопрос.
 *
 * @param id уникальный идентификатор варианта
 * @param text текст варианта
 * @param isCorrect признак правильного ответа
 */
data class QuizAnswer(
    val id: String,
    val text: String,
    val isCorrect: Boolean = false,
)

/**
 * Вопрос квиза.
 *
 * @param id уникальный идентификатор вопроса
 * @param text текст вопроса
 * @param answers варианты ответов
 * @param explanation пояснение к правильному ответу (опционально)
 * @param type тип вопроса
 * @param imageUrl изображение к вопросу (опционально)
 */
data class QuizQuestion(
    val id: String,
    val text: String,
    val answers: List<QuizAnswer>,
    val explanation: String? = null,
    val type: QuestionType = QuestionType.SINGLE_CHOICE,
    val imageUrl: String? = null,
)
