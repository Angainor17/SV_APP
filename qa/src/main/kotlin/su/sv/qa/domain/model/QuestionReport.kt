package su.sv.qa.domain.model

/**
 * Вопрос пользователя по выделенному тексту книги
 */
data class QuestionReport(
    val catalogBookId: String,
    val bookTitle: String,
    val page: Int,
    val selectedText: String,
    val authorName: String,
)
