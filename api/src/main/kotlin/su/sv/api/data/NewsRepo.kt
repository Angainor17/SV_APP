package su.sv.api.data

import su.sv.api.R
import su.sv.api.data.api.VkApi
import su.sv.api.data.model.ApiNewsItem
import su.sv.commonarchitecture.data.runCatchingHttpRequest
import su.sv.commonarchitecture.managers.ResourcesRepository
import javax.inject.Inject
import su.sv.commonarchitecture.utils.ApiKeyObfuscator

class NewsRepo @Inject constructor(
    private val resRepo: ResourcesRepository,
    private val vkApi: VkApi,
) {
    suspend fun getNews(offset: Int, count: Int): Result<List<ApiNewsItem>> {
        return runCatchingHttpRequest {
            val response = vkApi.getPosts(
                accessToken = ApiKeyObfuscator.decode(VK_SERVICE_KEY_ENCODED, VK_SERVICE_KEY_XOR),
                domain = resRepo.getString(R.string.vk_public_name),

                offset = offset,
                count = count,
            ).response
            response.items.orEmpty()
        }
    }

    companion object {
        // VK service key (https://vkhost.github.io/), обфусцирован XOR+Base64.
        private const val VK_SERVICE_KEY_ENCODED = "EU9oRl0QRW8UUkRGaRVYQxRmQVtFFWxGDkIUPkMNQRI5FwkRT2hGCUpBb0AIQEY7E1MSEm8TXktBbEIIFRU5EwpGR29EXUI="
        private const val VK_SERVICE_KEY_XOR = "sv_vk"
    }
}
