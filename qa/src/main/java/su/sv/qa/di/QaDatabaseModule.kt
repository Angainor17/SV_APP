package su.sv.qa.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import su.sv.qa.data.local.dao.AnsweredQuestionDao
import su.sv.qa.data.local.database.QaDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object QaDatabaseModule {

    @Provides
    @Singleton
    fun provideQaDatabase(@ApplicationContext context: Context): QaDatabase {
        return Room.databaseBuilder(context, QaDatabase::class.java, "qa_database")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideAnsweredQuestionDao(database: QaDatabase): AnsweredQuestionDao {
        return database.answeredQuestionDao()
    }
}
