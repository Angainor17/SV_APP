package su.sv.books.catalog.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import su.sv.books.catalog.data.api.BooksApi
import su.sv.books.catalog.data.api.BooksApiMock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // FIXME: think
internal class BooksApiModule {

    @Provides
    @Singleton
    fun provideBooksApiService(
        retrofit: Retrofit,
    ): BooksApi {
        return BooksApiMock() // TODO: remove mock
        return retrofit.create(BooksApi::class.java)
    }
}
