package su.sv.qa.di

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
import su.sv.qa.data.api.QaApi
import su.sv.qa.data.repository.QaRepositoryImpl
import su.sv.qa.domain.repository.QaRepository
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier для Qa Retrofit
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class QaRetrofit

private const val CONNECTION_TIMEOUT_MS = 20_000L
private const val QA_BASE_URL = "https://svremya.su/"
private const val CACHE_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB

@Module
@InstallIn(SingletonComponent::class)
internal interface QaApiModule {

    @Binds
    @Singleton
    fun bindQaRepository(impl: QaRepositoryImpl): QaRepository

    companion object {

        @Provides
        @Singleton
        @QaRetrofit
        fun provideQaOkHttpClient(
            @ApplicationContext context: Context,
            mockInterceptor: MockInterceptor
        ): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .cache(Cache(context.cacheDir.resolve("qa_http_cache"), CACHE_SIZE_BYTES))
                .addInterceptor(mockInterceptor)
                .build()
        }

        @Provides
        @Singleton
        @QaRetrofit
        fun provideQaRetrofit(
            @QaRetrofit client: OkHttpClient,
        ): Retrofit {
            return Retrofit.Builder()
                .baseUrl(QA_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        @Provides
        @Singleton
        fun provideQaApi(
            @QaRetrofit retrofit: Retrofit,
        ): QaApi {
            return retrofit.create(QaApi::class.java)
        }
    }
}
