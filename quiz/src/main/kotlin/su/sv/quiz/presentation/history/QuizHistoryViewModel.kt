package su.sv.quiz.presentation.history

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import su.sv.quiz.domain.model.QuizResult
import su.sv.quiz.domain.usecase.GetQuizHistoryUseCase
import javax.inject.Inject

/**
 * ViewModel экрана «Мои квизы» (история прохождений).
 */
@HiltViewModel
class QuizHistoryViewModel @Inject constructor(
    private val getQuizHistoryUseCase: GetQuizHistoryUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow<QuizHistoryState>(QuizHistoryState.Loading)
    val state: StateFlow<QuizHistoryState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = QuizHistoryState.Loading
            _state.value = getQuizHistoryUseCase().fold(
                onSuccess = { QuizHistoryState.Content(it) },
                onFailure = { QuizHistoryState.Error(it.message ?: UNKNOWN_ERROR) },
            )
        }
    }

    private companion object {
        const val UNKNOWN_ERROR = "Неизвестная ошибка"
    }
}

/**
 * Состояние экрана «Мои квизы».
 */
sealed class QuizHistoryState {

    object Loading : QuizHistoryState()

    data class Content(val results: List<QuizResult>) : QuizHistoryState()

    data class Error(val message: String) : QuizHistoryState()
}
