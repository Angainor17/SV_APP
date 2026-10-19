package su.sv.quiz.presentation.root

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import su.sv.quiz.domain.model.Quiz
import su.sv.quiz.domain.model.QuizTheme
import su.sv.quiz.domain.usecase.GetNewQuizzesUseCase
import su.sv.quiz.domain.usecase.GetThemesUseCase
import javax.inject.Inject

/**
 * ViewModel корневого экрана квизов: темы + новые квизы.
 */
@HiltViewModel
class QuizRootViewModel @Inject constructor(
    private val getThemesUseCase: GetThemesUseCase,
    private val getNewQuizzesUseCase: GetNewQuizzesUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow<QuizRootState>(QuizRootState.Loading)
    val state: StateFlow<QuizRootState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = QuizRootState.Loading
            _state.value = coroutineScope {
                val themesDeferred = async { getThemesUseCase() }
                val quizzesDeferred = async { getNewQuizzesUseCase(NEW_QUIZZES_LIMIT) }
                val themes = themesDeferred.await()
                val quizzes = quizzesDeferred.await()
                if (themes.isSuccess && quizzes.isSuccess) {
                    QuizRootState.Content(
                        themes = themes.getOrThrow(),
                        newQuizzes = quizzes.getOrThrow(),
                    )
                } else {
                    QuizRootState.Error(
                        (themes.exceptionOrNull() ?: quizzes.exceptionOrNull())
                            ?.message ?: UNKNOWN_ERROR,
                    )
                }
            }
        }
    }

    private companion object {
        const val NEW_QUIZZES_LIMIT = 3
        const val UNKNOWN_ERROR = "Неизвестная ошибка"
    }
}

/**
 * Состояние корневого экрана квизов.
 */
sealed class QuizRootState {

    object Loading : QuizRootState()

    data class Content(
        val themes: List<QuizTheme>,
        val newQuizzes: List<Quiz>,
    ) : QuizRootState()

    data class Error(val message: String) : QuizRootState()
}
