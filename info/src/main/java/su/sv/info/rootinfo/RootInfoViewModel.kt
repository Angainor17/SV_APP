package su.sv.info.rootinfo

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import su.sv.info.domain.GetInfoLinksUseCase
import su.sv.info.rootinfo.mapper.InfoUiMapper
import su.sv.info.rootinfo.model.UiInfoState
import su.sv.info.rootinfo.viewmodel.RootInfoActions
import su.sv.info.rootinfo.viewmodel.RootInfoActionsHandler
import javax.inject.Inject

@HiltViewModel
class RootInfoViewModel @Inject constructor(
    private val uiMapper: InfoUiMapper,
    private val getInfoLinksUseCase: GetInfoLinksUseCase,
) : BaseViewModel(), RootInfoActionsHandler {

    /** Контент экрана */
    private val _state = MutableStateFlow<UiInfoState>(UiInfoState.Loading)
    val state: StateFlow<UiInfoState> get() = _state

    init {
        refreshData()
    }

    override fun onAction(action: RootInfoActions) {
        when (action) {
            is RootInfoActions.OnSwipeRefresh -> {
                updateState { contentState ->
                    contentState.copy(isRefreshing = true)
                }
                refreshData()
            }

            is RootInfoActions.OnRetryClick -> {
                _state.value = UiInfoState.Loading
                refreshData()
            }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            getInfoLinksUseCase.execute().fold(
                onSuccess = { list ->
                    _state.update {
                        UiInfoState.Content(
                            items = uiMapper.fromDomainToUi(list)
                        )
                    }
                },
                onFailure = {
                    _state.value = UiInfoState.Failure
                },
            )
        }
    }

    private fun updateState(action: (UiInfoState.Content) -> UiInfoState.Content) {
        _state.update { state ->
            if (state is UiInfoState.Content) {
                action.invoke(state)
            } else {
                state
            }
        }
    }
}
