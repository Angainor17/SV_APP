package su.sv.wiki.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import su.sv.wiki.data.local.dao.FavoriteDao
import su.sv.wiki.data.local.dao.HistoryDao
import su.sv.wiki.data.local.database.WikiDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object WikiDatabaseModule {

    @Provides
    @Singleton
    fun provideWikiDatabase(
        @ApplicationContext context: Context,
    ): WikiDatabase {
        return Room.databaseBuilder(
            context,
            WikiDatabase::class.java,
            "wiki_database",
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideFavoriteDao(database: WikiDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    fun provideHistoryDao(database: WikiDatabase): HistoryDao {
        return database.historyDao()
    }
}
