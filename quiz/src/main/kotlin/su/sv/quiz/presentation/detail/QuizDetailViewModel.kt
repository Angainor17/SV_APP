package su.sv.quiz.presentation.detail

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import su.sv.quiz.domain.model.Quiz
import su.sv.quiz.domain.model.QuizQuestion
import su.sv.quiz.domain.model.QuizResult
import su.sv.quiz.domain.usecase.GetQuizUseCase
import su.sv.quiz.domain.usecase.SaveQuizResultUseCase
import javax.inject.Inject

/**
 * ViewModel прохождения квиза.
 *
 * Ответ на вопрос финальный (ТЗ): повторный выбор не сохраняется.
 */
@HiltViewModel
class QuizDetailViewModel @Inject constructor(
    private val getQuizUseCase: GetQuizUseCase,
    private val saveQuizResultUseCase: SaveQuizResultUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(QuizDetailState())
    val state: StateFlow<QuizDetailState> = _state.asStateFlow()

    fun load(quizId: String) {
        viewModelScope.launch {
            _state.value = QuizDetailState(loading = true)
            _state.value = getQuizUseCase(quizId).fold(
                onSuccess = { QuizDetailState(loading = false, quiz = it) },
                onFailure = { QuizDetailState(loading = false, error = it.message ?: UNKNOWN_ERROR) },
            )
        }
    }

    /** Сохранить ответ на текущий вопрос. Повторный выбор игнорируется. */
    fun selectAnswer(answerId: String) {
        val current = _state.value
        val question = current.currentQuestion ?: return
        if (current.answers.containsKey(question.id)) return
        _state.value = current.copy(answers = current.answers + (question.id to answerId))
    }

    /** Перейти к вопросу по индексу (индикатор прогресса). */
    fun goTo(index: Int) {
        val current = _state.value
        val count = current.quiz?.questions?.size ?: return
        if (index !in 0 until count) return
        _state.value = current.copy(currentIndex = index)
    }

    /** Посчитать результат по сохранённым ответам (null, если квиз не загружен). */
    fun computeResult(): QuizResult? {
        val current = _state.value
        val quiz = current.quiz ?: return null
        val correct = quiz.questions.count { question ->
            question.answers.firstOrNull { it.id == current.answers[question.id] }?.isCorrect == true
        }
        val total = quiz.questions.size
        val percentage = if (total > 0) correct * 100 / total else 0
        return QuizResult(
            quizId = quiz.id,
            quizTitle = quiz.title,
            totalQuestions = total,
            correctAnswers = correct,
            percentage = percentage,
            completedAtEpochMillis = System.currentTimeMillis(),
        )
    }

    /** Сохранить результат (лучший результат хранит репозиторий). */
    fun saveResult(result: QuizResult) {
        viewModelScope.launch { saveQuizResultUseCase(result) }
    }

    private companion object {
        const val UNKNOWN_ERROR = "Неизвестная ошибка"
    }
}

/**
 * Состояние прохождения квиза.
 */
data class QuizDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val quiz: Quiz? = null,
    val currentIndex: Int = 0,
    val answers: Map<String, String> = emptyMap(),
) {
    val currentQuestion: QuizQuestion?
        get() = quiz?.questions?.getOrNull(currentIndex)

    val isAllAnswered: Boolean
        get() = quiz != null && answers.size == quiz.questions.size
}
