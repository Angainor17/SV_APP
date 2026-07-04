package su.sv.wiki.presentation.root.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import su.sv.commonarchitecture.di.module.DispatcherProvider
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import su.sv.wiki.domain.model.WikiArticle
import su.sv.wiki.domain.model.WikiExternalLink
import su.sv.wiki.domain.model.WikiLink
import su.sv.wiki.domain.repository.WikiResult
import su.sv.wiki.domain.usecase.AddFavoriteUseCase
import su.sv.wiki.domain.usecase.AddHistoryUseCase
import su.sv.wiki.domain.usecase.ClearHistoryUseCase
import su.sv.wiki.domain.usecase.GetArticleUseCase
import su.sv.wiki.domain.usecase.GetHistoryUseCase
import su.sv.wiki.domain.usecase.GetSearchSuggestionsUseCase
import su.sv.wiki.domain.usecase.HasFavoritesUseCase
import su.sv.wiki.domain.usecase.IsFavoriteUseCase
import su.sv.wiki.domain.usecase.RemoveFavoriteUseCase
import su.sv.wiki.domain.usecase.SearchArticleUseCase
import su.sv.wiki.presentation.root.mapper.UiWikiMapper
import su.sv.wiki.presentation.root.model.UiWikiState
import su.sv.wiki.presentation.root.viewmodel.actions.WikiActions
import su.sv.wiki.presentation.root.viewmodel.actions.WikiActionsHandler
import su.sv.wiki.presentation.root.viewmodel.effects.WikiOneTimeEffect
import javax.inject.Inject

/**
 * ViewModel экрана Wiki
 *
 * Все тяжёлые операции вынесены на соответствующие диспетчеры:
 * - IO: сетевые запросы, DB операции (через main-safe UseCase)
 * - Default: маппинг (CPU-intensive)
 */
@HiltViewModel
class RootWikiViewModel @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val searchArticleUseCase: SearchArticleUseCase,
    private val getArticleUseCase: GetArticleUseCase,
    private val getHistoryUseCase: GetHistoryUseCase,
    private val addHistoryUseCase: AddHistoryUseCase,
    private val clearHistoryUseCase: ClearHistoryUseCase,
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase,
    private val isFavoriteUseCase: IsFavoriteUseCase,
    private val getSearchSuggestionsUseCase: GetSearchSuggestionsUseCase,
    private val hasFavoritesUseCase: HasFavoritesUseCase,
    private val mapper: UiWikiMapper,
) : BaseViewModel(), WikiActionsHandler {

    /** Состояние экрана */
    private val _state = MutableStateFlow<UiWikiState>(UiWikiState.Initial)
    val state: StateFlow<UiWikiState> = _state.asStateFlow()

    /** Подсказки поиска */
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    /** Выбранная подсказка для помещения в поле поиска */
    private val _selectedSuggestion = MutableStateFlow<String?>(null)
    val selectedSuggestion: StateFlow<String?> = _selectedSuggestion.asStateFlow()

    /** История поиска */
    val history: Flow<List<String>> = getHistoryUseCase()

    /** Наличие избранных статей */
    val hasFavorites: Flow<Boolean> = hasFavoritesUseCase.execute()

    /** Одноразовые события */
    private val _oneTimeEffect = Channel<WikiOneTimeEffect>(capacity = Channel.BUFFERED)
    val oneTimeEffect: Flow<WikiOneTimeEffect> = _oneTimeEffect.receiveAsFlow()

    /** Текущий поисковый запрос */
    private var currentQuery: String = ""

    override fun onAction(action: WikiActions) {
        when (action) {
            is WikiActions.OnSearch -> onSearch(action.query)
            is WikiActions.OnLinkClick -> onLinkClick(action.title)
            is WikiActions.OnAddFavorite -> onAddFavorite(action.title)
            is WikiActions.OnRemoveFavorite -> onRemoveFavorite(action.title)
            is WikiActions.OnHistoryItemClick -> onHistoryItemClick(action.title)
            is WikiActions.OnClearHistory -> onClearHistory()
            is WikiActions.OnRetryClick -> onRetry()
            is WikiActions.OnCloseArticle -> onCloseArticle()
            is WikiActions.OnSearchQueryChanged -> onSearchQueryChanged(action.query)
            is WikiActions.OnSuggestionClick -> onSuggestionClick(action.title)
            is WikiActions.OnSuggestionApplied -> onSuggestionApplied()
        }
    }

    private fun onSearchQueryChanged(query: String) {
        viewModelScope.launch {
            if (query.length >= 2) {
                // UseCase уже main-safe
                val suggestions = getSearchSuggestionsUseCase.execute(query)
                _suggestions.value = suggestions.map { it.title }
            } else {
                _suggestions.value = emptyList()
                // При очистке поля возвращаемся к начальному состоянию
                if (query.isEmpty()) {
                    _state.value = UiWikiState.Initial
                    currentQuery = ""
                }
            }
        }
    }

    private fun onSuggestionClick(title: String) {
        _suggestions.value = emptyList()
        _selectedSuggestion.value = title  // Помещаем текст в поле поиска
        currentQuery = title
        loadArticle(title)
    }

    private fun onSuggestionApplied() {
        _selectedSuggestion.value = null  // Сбрасываем после применения
    }

    private fun onSearch(query: String) {
        if (query.isBlank()) return

        _suggestions.value = emptyList()
        currentQuery = query
        searchAndLoadArticle(query)
    }

    private fun onLinkClick(title: String) {
        _suggestions.value = emptyList()
        loadArticle(title)
    }

    private fun onHistoryItemClick(title: String) {
        _suggestions.value = emptyList()
        currentQuery = title
        loadArticle(title)
    }

    private fun searchAndLoadArticle(query: String) {
        viewModelScope.launch {
            _state.value = UiWikiState.Loading

            // UseCase уже main-safe (IO dispatcher)
            when (val result = searchArticleUseCase.execute(query)) {
                is WikiResult.Success -> {
                    loadArticle(result.data.title, addToHistory = true)
                }
                is WikiResult.NotFound -> {
                    _state.value = UiWikiState.NotFound
                }
                is WikiResult.Error -> {
                    _state.value = UiWikiState.Error(result.message)
                }
            }
        }
    }

    private fun loadArticle(title: String, addToHistory: Boolean = true) {
        viewModelScope.launch {
            _state.value = UiWikiState.Loading

            // UseCase уже main-safe (IO dispatcher)
            when (val result = getArticleUseCase.execute(title)) {
                is WikiResult.Success -> {
                    val article = result.data
                    // UseCase уже main-safe (IO dispatcher)
                    val isFavorite = isFavoriteUseCase.execute(title)

                    // Маппинг - CPU intensive, на Default dispatcher
                    val uiArticle = withContext(dispatcherProvider.default) {
                        mapper.mapToUi(article, isFavorite)
                    }

                    // UI обновление - на Main
                    _state.value = UiWikiState.Content(
                        article = uiArticle,
                        isFavorite = isFavorite,
                    )

                    if (addToHistory) {
                        // UseCase уже main-safe (IO dispatcher)
                        addHistoryUseCase.execute(title)
                    }
                }
                is WikiResult.NotFound -> {
                    _state.value = UiWikiState.NotFound
                }
                is WikiResult.Error -> {
                    _state.value = UiWikiState.Error(result.message)
                }
            }
        }
    }

    private fun onAddFavorite(title: String) {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState is UiWikiState.Content) {
                // UseCase уже main-safe (IO dispatcher)
                addFavoriteUseCase.execute(
                    WikiArticle(
                        title = currentState.article.title,
                        pageId = 0,
                        content = currentState.article.content,
                        links = currentState.article.links.map {
                            WikiLink(
                                title = it.targetTitle,
                                exists = it.exists,
                            )
                        },
                        externalLinks = currentState.article.externalLinks.map {
                            WikiExternalLink(
                                text = it.text,
                                url = it.url,
                            )
                        },
                        articleUrl = currentState.article.articleUrl,
                    ),
                )

                // UI обновление - на Main
                _state.update { state ->
                    if (state is UiWikiState.Content) {
                        state.copy(isFavorite = true)
                    } else {
                        state
                    }
                }

                _oneTimeEffect.trySend(
                    WikiOneTimeEffect.ShowAddedToFavorites(title),
                )
            }
        }
    }

    private fun onRemoveFavorite(title: String) {
        viewModelScope.launch {
            // UseCase уже main-safe (IO dispatcher)
            removeFavoriteUseCase.execute(title)

            // UI обновление - на Main
            _state.update { state ->
                if (state is UiWikiState.Content) {
                    state.copy(isFavorite = false)
                } else {
                    state
                }
            }

            _oneTimeEffect.trySend(
                WikiOneTimeEffect.ShowRemovedFromFavorites(title),
            )
        }
    }

    private fun onClearHistory() {
        viewModelScope.launch {
            // UseCase уже main-safe (IO dispatcher)
            clearHistoryUseCase.execute()
        }
    }

    private fun onRetry() {
        if (currentQuery.isNotBlank()) {
            searchAndLoadArticle(currentQuery)
        }
    }

    private fun onCloseArticle() {
        _state.value = UiWikiState.Initial
        currentQuery = ""
    }
}
