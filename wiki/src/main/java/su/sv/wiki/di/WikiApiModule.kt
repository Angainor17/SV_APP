package su.sv.wiki.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import su.sv.wiki.data.api.WikiApi
import su.sv.wiki.data.repository.WikiRepositoryImpl
import su.sv.wiki.domain.repository.WikiRepository
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier для Wiki Retrofit
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WikiRetrofit

private const val CONNECTION_TIMEOUT_MS = 20_000L
private const val WIKI_BASE_URL = "https://svremya.su/"

@Module
@InstallIn(SingletonComponent::class)
internal interface WikiApiModule {

    @Binds
    @Singleton
    fun bindWikiRepository(impl: WikiRepositoryImpl): WikiRepository

    companion object {

        @Provides
        @Singleton
        @WikiRetrofit
        fun provideWikiOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build()
        }

        @Provides
        @Singleton
        @WikiRetrofit
        fun provideWikiRetrofit(
            @WikiRetrofit client: OkHttpClient,
        ): Retrofit {
            return Retrofit.Builder()
                .baseUrl(WIKI_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        @Provides
        @Singleton
        fun provideWikiApi(
            @WikiRetrofit retrofit: Retrofit,
        ): WikiApi {
            return retrofit.create(WikiApi::class.java)
        }
    }
}
