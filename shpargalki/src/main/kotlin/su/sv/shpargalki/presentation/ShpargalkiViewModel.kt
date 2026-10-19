package su.sv.shpargalki.presentation

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import su.sv.shpargalki.domain.usecase.GetDialecticsUseCase
import su.sv.shpargalki.presentation.model.ShpargalkiState
import javax.inject.Inject

/**
 * ViewModel экрана «Шпаргалки».
 * Загружает документ диалектики один раз и раздаёт его на оба экрана
 * (список разделов и древо понятий).
 */
@HiltViewModel
class ShpargalkiViewModel @Inject constructor(
    private val getDialecticsUseCase: GetDialecticsUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow<ShpargalkiState>(ShpargalkiState.Loading)
    val state: StateFlow<ShpargalkiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = ShpargalkiState.Loading
            _state.value = getDialecticsUseCase.execute().fold(
                onSuccess = { ShpargalkiState.Content(it) },
                onFailure = { ShpargalkiState.Error(it.message ?: UNKNOWN_ERROR) },
            )
        }
    }

    private companion object {
        const val UNKNOWN_ERROR = "Неизвестная ошибка"
    }
}
