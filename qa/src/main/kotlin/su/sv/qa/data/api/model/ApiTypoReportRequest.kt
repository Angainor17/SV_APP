package su.sv.qa.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * Тело запроса отправки репорта об опечатке
 */
data class ApiTypoReportRequest(
    @SerializedName("bookId") val bookId: String,
    @SerializedName("bookTitle") val bookTitle: String,
    @SerializedName("page") val page: Int,
    @SerializedName("selectedText") val selectedText: String,
    @SerializedName("comment") val comment: String?,
)
