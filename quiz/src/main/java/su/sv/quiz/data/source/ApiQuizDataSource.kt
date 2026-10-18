package su.sv.quiz.data.source

import su.sv.quiz.domain.model.Quiz
import su.sv.quiz.domain.model.QuizTheme
import javax.inject.Inject

/**
 * Заглушка будущего API-источника квизов.
 *
 * Когда данные переедут на сервер (MediaWiki / REST), здесь будет Retrofit-вызов.
 */
class ApiQuizDataSource @Inject constructor() : QuizDataSource {

    override suspend fun getThemes(): List<QuizTheme> =
        throw NotImplementedError("API-источник квизов ещё не реализован")

    override suspend fun getQuizzes(): List<Quiz> =
        throw NotImplementedError("API-источник квизов ещё не реализован")

    override suspend fun getQuiz(quizId: String): Quiz? =
        throw NotImplementedError("API-источник квизов ещё не реализован")
}
