package com.github.axet.bookreader.screens.viewmodel

import com.github.axet.bookreader.widgets.FBReaderView

/**
 * Делегат для поиска по тексту книги.
 *
 * Отвечает за поиск, навигацию по результатам (вперёд/назад)
 * и закрытие поиска.
 *
 * Выделен из [ReaderViewModel] для уменьшения размера God-класса.
 */
class ReaderSearchDelegate(
    private val getState: () -> ReaderState.Content?,
    private val updateState: (ReaderState.Content) -> Unit,
    private val getFBReaderView: () -> FBReaderView?,
) {
    fun search(query: String) {
        val currentState = getState() ?: return
        val shouldSearch = query.length >= 2
        updateState(
            currentState.copy(
                searchState = currentState.searchState.copy(
                    isActive = true,
                    query = query,
                    resultsCount = 0,
                    currentResultIndex = 0,
                    isLoading = shouldSearch
                )
            )
        )
        if (shouldSearch) {
            getFBReaderView()?.performSearch(query) { count, index ->
                val state = getState() ?: return@performSearch
                updateState(
                    state.copy(
                        searchState = state.searchState.copy(
                            resultsCount = count,
                            currentResultIndex = index,
                            isLoading = false
                        )
                    )
                )
            }
        }
    }

    fun next() {
        getFBReaderView()?.performSearchNext { count, newIndex ->
            val state = getState() ?: return@performSearchNext
            updateState(
                state.copy(
                    searchState = state.searchState.copy(
                        resultsCount = count,
                        currentResultIndex = newIndex,
                        isLoading = false
                    )
                )
            )
        }
    }

    fun previous() {
        getFBReaderView()?.performSearchPrevious { count, newIndex ->
            val state = getState() ?: return@performSearchPrevious
            updateState(
                state.copy(
                    searchState = state.searchState.copy(
                        resultsCount = count,
                        currentResultIndex = newIndex,
                        isLoading = false
                    )
                )
            )
        }
    }

    fun close() {
        val currentState = getState() ?: return
        updateState(currentState.copy(searchState = SearchState()))
        getFBReaderView()?.searchClose()
    }
}
