package su.sv.qa.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * Отвеченный вопрос в ответе GET /qa
 */
data class ApiAnsweredQuestion(
    @SerializedName("id") val id: String,
    @SerializedName("bookId") val bookId: String,
    @SerializedName("bookTitle") val bookTitle: String,
    @SerializedName("page") val page: Int,
    @SerializedName("selectedText") val selectedText: String,
    @SerializedName("authorName") val authorName: String,
    @SerializedName("questionCreatedAt") val questionCreatedAt: Long,
    @SerializedName("answerText") val answerText: String,
    @SerializedName("answerUpdatedAt") val answerUpdatedAt: Long,
    @SerializedName("startParagraph") val startParagraph: Int,
    @SerializedName("startElement") val startElement: Int,
    @SerializedName("startChar") val startChar: Int,
    @SerializedName("endParagraph") val endParagraph: Int,
    @SerializedName("endElement") val endElement: Int,
    @SerializedName("endChar") val endChar: Int,
)
