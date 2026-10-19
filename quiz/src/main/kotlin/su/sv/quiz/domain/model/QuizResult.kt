package su.sv.quiz.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Результат прохождения квиза.
 *
 * @param quizId идентификатор квиза
 * @param quizTitle название квиза
 * @param totalQuestions всего вопросов
 * @param correctAnswers верных ответов
 * @param percentage процент верных ответов (0–100)
 * @param completedAtEpochMillis время завершения (epoch millis)
 */
@Parcelize
data class QuizResult(
    val quizId: String,
    val quizTitle: String,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val percentage: Int,
    val completedAtEpochMillis: Long,
) : Parcelable
