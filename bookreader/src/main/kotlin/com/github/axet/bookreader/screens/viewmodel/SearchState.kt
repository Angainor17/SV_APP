package com.github.axet.bookreader.screens.viewmodel

/**
 * Состояние поиска в книге
 */
data class SearchState(
    val isActive: Boolean = false,
    val query: String = "",
    val resultsCount: Int = 0,
    val currentResultIndex: Int = 0,
    val isLoading: Boolean = false,
)