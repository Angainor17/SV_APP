package su.sv.qa.domain.model

/**
 * Отвеченный вопрос пользователя, синхронизированный с бэкендом
 */
data class AnsweredQuestion(
    val id: String,
    val catalogBookId: String,
    val bookTitle: String,
    val page: Int,
    val selectedText: String,
    val authorName: String,
    val questionCreatedAt: Long,
    val answerText: String,
    val answerUpdatedAt: Long,
    val startParagraph: Int,
    val startElement: Int,
    val startChar: Int,
    val endParagraph: Int,
    val endElement: Int,
    val endChar: Int,
)
