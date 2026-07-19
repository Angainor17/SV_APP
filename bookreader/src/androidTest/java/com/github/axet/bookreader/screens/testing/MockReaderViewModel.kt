package com.github.axet.bookreader.screens.testing

import com.github.axet.bookreader.screens.viewmodel.ReaderActions
import com.github.axet.bookreader.screens.viewmodel.ReaderState
import com.github.axet.bookreader.screens.viewmodel.SearchState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Мок ViewModel для тестирования ReaderScreen.
 */
class MockReaderViewModel {

    private val _state = MutableStateFlow<ReaderState>(ReaderState.Loading)
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    // Логирование действий
    val actionsLog = mutableListOf<ReaderActions>()

    // Флаги
    var wasNavigatedBack = false
    var wasTocToggled = false
    var wasBookmarksToggled = false
    var wasFontSettingsToggled = false
    var wasSearchClosed = false

    fun onAction(action: ReaderActions) {
        actionsLog.add(action)

        when (action) {
            ReaderActions.NavigateBack -> wasNavigatedBack = true
            ReaderActions.ToggleToc -> wasTocToggled = true
            ReaderActions.ToggleBookmarks -> wasBookmarksToggled = true
            ReaderActions.ToggleFontSettings -> wasFontSettingsToggled = true
            ReaderActions.SearchClose -> {
                wasSearchClosed = true
                _searchState.value = SearchState()
            }
            is ReaderActions.Search -> {
                _searchState.value = _searchState.value.copy(
                    isActive = true,
                    query = action.query,
                    isLoading = action.query.length >= 2
                )
            }
            ReaderActions.SearchNext -> {
                val current = _searchState.value.currentResultIndex
                val total = _searchState.value.resultsCount
                if (total > 0 && current < total - 1) {
                    _searchState.value = _searchState.value.copy(
                        currentResultIndex = current + 1
                    )
                }
            }
            ReaderActions.SearchPrevious -> {
                val current = _searchState.value.currentResultIndex
                if (current > 0) {
                    _searchState.value = _searchState.value.copy(
                        currentResultIndex = current - 1
                    )
                }
            }
            is ReaderActions.SetFullscreen -> {
                updateContentState { it.copy(isFullscreen = action.isFullscreen) }
            }
            else -> { /* log */ }
        }
    }

    fun setState(newState: ReaderState) {
        _state.value = newState
    }

    fun setSearchState(newState: SearchState) {
        _searchState.value = newState
    }

    fun simulateSearchResults(count: Int) {
        _searchState.value = _searchState.value.copy(
            isLoading = false,
            resultsCount = count,
            currentResultIndex = 0
        )
    }

    fun clearLog() {
        actionsLog.clear()
        wasNavigatedBack = false
        wasTocToggled = false
        wasBookmarksToggled = false
        wasFontSettingsToggled = false
        wasSearchClosed = false
    }

    private inline fun updateContentState(transform: (ReaderState.Content) -> ReaderState.Content) {
        val current = _state.value
        if (current is ReaderState.Content) {
            _state.value = transform(current)
        }
    }
}