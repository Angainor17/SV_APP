package su.sv.api.di

import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import su.sv.api.data.api.VkApi
import javax.inject.Singleton

private const val VK_BASE_URL = "https://api.vk.com"

@Module
@InstallIn(SingletonComponent::class)
internal class NewsApiModule {

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(VK_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create())) // FIXME refactoring
            .build();
    }

    @Provides
    @Singleton
    fun provideVkApiService(): VkApi {
        return retrofit.create(VkApi::class.java)
    }
}
