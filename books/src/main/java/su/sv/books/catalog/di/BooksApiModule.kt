package su.sv.books.catalog.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context,
    ): SharedPreferences {
        return context.getSharedPreferences("books_prefs", Context.MODE_PRIVATE)
    }
}
