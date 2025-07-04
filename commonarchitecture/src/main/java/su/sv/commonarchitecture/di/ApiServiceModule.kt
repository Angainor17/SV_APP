package su.sv.commonarchitecture.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val CONNECTION_TIMEOUTS_MS = 20_000L
private const val BASE_URL = "https://svremya.org/"

@InstallIn(SingletonComponent::class)
@Module
class ApiServiceModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().apply {
            setTimeouts()
        }.build()
    }

    @Singleton
    @Provides
    fun provideRetrofit(
        retrofitBuilder: Retrofit.Builder,
        gson: Gson,
    ): Retrofit {
        return retrofitBuilder
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    fun provideRetrofitBuilder(
        client: OkHttpClient,
    ): Retrofit.Builder {
        return Retrofit
            .Builder()
            .client(client)
            .baseUrl(BASE_URL)
    }

    @Singleton
    @Provides
    fun provideGson(): Gson {
        return GsonBuilder().create()
    }

    private fun OkHttpClient.Builder.setTimeouts() {
        connectTimeout(CONNECTION_TIMEOUTS_MS, TimeUnit.MILLISECONDS)
        readTimeout(CONNECTION_TIMEOUTS_MS, TimeUnit.MILLISECONDS)
        writeTimeout(CONNECTION_TIMEOUTS_MS, TimeUnit.MILLISECONDS)
    }
}