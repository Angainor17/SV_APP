package su.sv.news.data.api

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import su.sv.news.data.response.VkNewsResponse

interface VkApi {

    // https://api.vk.com/method/wall.get

    @FormUrlEncoded
    @POST("/method/wall.get")
    suspend fun getPosts(
        @Field("access_token") accessToken: String,
        @Field("domain") domain: String,
        @Field("offset") offset: Int,
        @Field("count") count: Int,
        @Field("v") version: String = VK_API_VERSION,
    ): VkNewsResponse

    companion object {
        const val VK_API_VERSION = "5.199"
    }
}