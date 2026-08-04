package su.sv.qa.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import su.sv.qa.data.api.model.ApiAnsweredQuestion
import su.sv.qa.data.api.model.ApiQuestionReportRequest
import su.sv.qa.data.api.model.ApiTypoReportRequest

/**
 * API интерфейс для репортов об опечатках/вопросах и синхронизации отвеченных вопросов
 * Базовый URL: https://svremya.su/
 */
interface QaApi {

    /**
     * Отправка репорта об опечатке в выделенном тексте
     */
    @POST("reports/typo")
    suspend fun submitTypoReport(@Body request: ApiTypoReportRequest)

    /**
     * Отправка вопроса по выделенному тексту
     */
    @POST("reports/question")
    suspend fun submitQuestionReport(@Body request: ApiQuestionReportRequest)

    /**
     * Получение отвеченных (и не удалённых) вопросов, обновлённых после [since]
     * @param since epoch millis последней известной [ApiAnsweredQuestion.answerUpdatedAt], 0 для полной загрузки
     */
    @GET("qa")
    suspend fun getAnsweredQuestions(@Query("since") since: Long): List<ApiAnsweredQuestion>

    companion object {
        const val BASE_URL = "https://svremya.su/"
    }
}
