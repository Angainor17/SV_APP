package su.sv.qa.domain.model

/**
 * Репорт об опечатке в выделенном тексте книги
 */
data class TypoReport(
    val catalogBookId: String,
    val bookTitle: String,
    val page: Int,
    val selectedText: String,
    val comment: String? = null,
)
