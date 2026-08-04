package su.sv.qa.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity для локального кэша отвеченных вопросов
 */
@Entity(
    tableName = "answered_questions",
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["answerUpdatedAt"]),
    ]
)
data class AnsweredQuestionEntity(
    @PrimaryKey val id: String,
    val bookId: String,
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
