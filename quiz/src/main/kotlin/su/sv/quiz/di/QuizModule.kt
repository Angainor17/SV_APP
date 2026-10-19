package su.sv.quiz.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.sv.quiz.data.repository.QuizRepositoryImpl
import su.sv.quiz.data.source.ApiQuizDataSource
import su.sv.quiz.data.source.MockQuizDataSource
import su.sv.quiz.data.source.QuizDataSource
import su.sv.quiz.domain.repository.QuizRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class QuizModule {

    @Binds
    @Singleton
    abstract fun bindQuizRepository(impl: QuizRepositoryImpl): QuizRepository

    companion object {

        /**
         * Переключатель источника данных.
         * true  — мок-данные с искусственной задержкой (текущий режим);
         * false — реальный API (будет реализовано позже).
         */
        private const val USE_MOCK_DATA_SOURCE = true

        @Provides
        @Singleton
        fun provideDataSource(
            mock: MockQuizDataSource,
            api: ApiQuizDataSource,
        ): QuizDataSource = if (USE_MOCK_DATA_SOURCE) mock else api
    }
}
