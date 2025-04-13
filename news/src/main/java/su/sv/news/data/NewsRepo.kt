package su.sv.news.data

import su.sv.commonarchitecture.data.runCatchingHttpRequest
import su.sv.commonui.managers.ResourcesRepository
import su.sv.news.R
import su.sv.news.data.api.VkApi
import su.sv.news.data.model.ApiNewsItem
import javax.inject.Inject

class NewsRepo @Inject constructor(
    private val resRepo: ResourcesRepository,
    private val vkApi: VkApi,
) {
    suspend fun getNews(offset: Int, count: Int): Result<List<ApiNewsItem>> {
        return runCatchingHttpRequest {
            val response = vkApi.getPosts(
                accessToken = resRepo.getString(R.string.vk_service_key_mini_app_for_api_request),
                domain = resRepo.getString(R.string.vk_public_name),

                offset = offset,
                count = count,
            ).response
            response.items.orEmpty()
        }
    }
}
