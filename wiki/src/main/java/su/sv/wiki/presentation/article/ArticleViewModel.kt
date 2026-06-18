package su.sv.wiki.presentation.article

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import su.sv.wiki.domain.repository.WikiResult
import su.sv.wiki.domain.usecase.AddFavoriteUseCase
import su.sv.wiki.domain.usecase.GetArticleUseCase
import su.sv.wiki.domain.usecase.IsFavoriteUseCase
import su.sv.wiki.domain.usecase.RemoveFavoriteUseCase
import su.sv.wiki.presentation.root.mapper.UiWikiMapper
import javax.inject.Inject

/**
 * ViewModel экрана статьи
 */
@HiltViewModel
class ArticleViewModel @Inject constructor(
    private val getArticleUseCase: GetArticleUseCase,
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase,
    private val isFavoriteUseCase: IsFavoriteUseCase,
    private val mapper: UiWikiMapper,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel() {

    /** Название статьи из навигации */
    val articleTitle: String = savedStateHandle.get<String>("title") ?: ""

    /** Состояние экрана */
    private val _state = MutableStateFlow<ArticleState>(ArticleState.Loading)
    val state: StateFlow<ArticleState> = _state.asStateFlow()

    init {
        loadArticle(articleTitle)
    }

    /**
     * Загрузить статью
     */
    fun loadArticle(title: String) {
        viewModelScope.launch {
            _state.value = ArticleState.Loading

            when (val result = getArticleUseCase.execute(title)) {
                is WikiResult.Success -> {
                    val article = result.data
                    val isFavorite = isFavoriteUseCase.execute(title)

                    _state.value = ArticleState.Content(
                        article = mapper.mapToUi(article, isFavorite),
                        isFavorite = isFavorite,
                    )
                }
                is WikiResult.NotFound -> {
                    _state.value = ArticleState.NotFound
                }
                is WikiResult.Error -> {
                    _state.value = ArticleState.Error(result.message)
                }
            }
        }
    }

    /**
     * Переключить статус избранного
     */
    fun toggleFavorite() {
        val currentState = _state.value
        if (currentState is ArticleState.Content) {
            viewModelScope.launch {
                if (currentState.isFavorite) {
                    removeFavoriteUseCase.execute(currentState.article.title)
                } else {
                    addFavoriteUseCase.execute(
                        su.sv.wiki.domain.model.WikiArticle(
                            title = currentState.article.title,
                            pageId = 0,
                            content = currentState.article.content,
                            links = currentState.article.links.map {
                                su.sv.wiki.domain.model.WikiLink(
                                    title = it.targetTitle,
                                    exists = it.exists,
                                )
                            },
                            externalLinks = currentState.article.externalLinks.map {
                                su.sv.wiki.domain.model.WikiExternalLink(
                                    text = it.text,
                                    url = it.url,
                                )
                            },
                            articleUrl = currentState.article.articleUrl,
                        ),
                    )
                }

                _state.update { state ->
                    if (state is ArticleState.Content) {
                        state.copy(isFavorite = !state.isFavorite)
                    } else {
                        state
                    }
                }
            }
        }
    }
}

/**
 * Состояния экрана статьи
 */
sealed class ArticleState {
    object Loading : ArticleState()
    data class Content(
        val article: su.sv.wiki.presentation.root.model.UiWikiArticle,
        val isFavorite: Boolean,
    ) : ArticleState()
    object NotFound : ArticleState()
    data class Error(val message: String) : ArticleState()
}
