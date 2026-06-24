package su.sv.api.di

import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import su.sv.api.data.api.VkApi
import su.sv.commonarchitecture.mock.MockInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val VK_BASE_URL = "https://api.vk.com"
private const val CONNECTION_TIMEOUT_MS = 20_000L

@Module
@InstallIn(SingletonComponent::class)
internal class NewsApiModule {

    @Provides
    @Singleton
    fun provideVkApiService(
        mockInterceptor: MockInterceptor
    ): VkApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .writeTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .addInterceptor(mockInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(VK_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
            .create(VkApi::class.java)
    }
}
