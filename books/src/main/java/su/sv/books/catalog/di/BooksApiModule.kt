package su.sv.books.catalog.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import su.sv.books.catalog.data.api.BooksApi
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class BooksApiModule {

    @Provides
    @Singleton
    fun provideBooksApiService(
        retrofit: Retrofit,
    ): BooksApi {
        return retrofit.create(BooksApi::class.java)
    }
}
