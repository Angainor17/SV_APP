package su.sv.wiki.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import su.sv.commonarchitecture.mock.MockInterceptor
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
private const val CACHE_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB

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
        fun provideWikiOkHttpClient(
            @ApplicationContext context: Context,
            mockInterceptor: MockInterceptor
        ): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .cache(Cache(context.cacheDir.resolve("wiki_http_cache"), CACHE_SIZE_BYTES))
                .addInterceptor(mockInterceptor)
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
