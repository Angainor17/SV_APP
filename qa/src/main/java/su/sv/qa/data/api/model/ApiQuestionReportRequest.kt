package su.sv.qa.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * Тело запроса отправки вопроса по выделенному тексту
 */
data class ApiQuestionReportRequest(
    @SerializedName("bookId") val bookId: String,
    @SerializedName("bookTitle") val bookTitle: String,
    @SerializedName("page") val page: Int,
    @SerializedName("selectedText") val selectedText: String,
    @SerializedName("authorName") val authorName: String,
)
