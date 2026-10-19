package su.sv.api.di

import android.content.Context
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import su.sv.api.data.api.VkApi
import su.sv.commonarchitecture.mock.MockInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val VK_BASE_URL = "https://api.vk.com"
private const val CONNECTION_TIMEOUT_MS = 20_000L
private const val CACHE_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB

@Module
@InstallIn(SingletonComponent::class)
internal class NewsApiModule {

    @Provides
    @Singleton
    fun provideVkApiService(
        @ApplicationContext context: Context,
        mockInterceptor: MockInterceptor
    ): VkApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .writeTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .cache(Cache(context.cacheDir.resolve("vk_http_cache"), CACHE_SIZE_BYTES))
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
