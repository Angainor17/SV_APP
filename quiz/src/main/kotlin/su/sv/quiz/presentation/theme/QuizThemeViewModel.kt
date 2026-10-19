package su.sv.quiz.presentation.theme

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import su.sv.quiz.domain.model.Quiz
import su.sv.quiz.domain.usecase.GetQuizzesByThemeUseCase
import javax.inject.Inject

/**
 * ViewModel экрана списка квизов по теме.
 */
@HiltViewModel
class QuizThemeViewModel @Inject constructor(
    private val getQuizzesByThemeUseCase: GetQuizzesByThemeUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow<QuizThemeState>(QuizThemeState.Loading)
    val state: StateFlow<QuizThemeState> = _state.asStateFlow()

    fun load(themeId: String) {
        viewModelScope.launch {
            _state.value = QuizThemeState.Loading
            _state.value = getQuizzesByThemeUseCase(themeId).fold(
                onSuccess = { QuizThemeState.Content(it) },
                onFailure = { QuizThemeState.Error(it.message ?: UNKNOWN_ERROR) },
            )
        }
    }

    private companion object {
        const val UNKNOWN_ERROR = "Неизвестная ошибка"
    }
}

/**
 * Состояние экрана списка квизов по теме.
 */
sealed class QuizThemeState {

    object Loading : QuizThemeState()

    data class Content(val quizzes: List<Quiz>) : QuizThemeState()

    data class Error(val message: String) : QuizThemeState()
}
