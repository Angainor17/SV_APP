package su.sv.wiki.presentation.article

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import su.sv.wiki.domain.usecase.GetArticleUseCase
import su.sv.wiki.domain.usecase.IsFavoriteUseCase
import su.sv.wiki.domain.usecase.RemoveFavoriteUseCase
import su.sv.wiki.presentation.root.mapper.UiWikiMapper
import su.sv.wiki.presentation.root.model.UiWikiArticle
import javax.inject.Inject

/**
 * ViewModel экрана статьи
 * Все тяжёлые операции вынесены на соответствующие диспетчеры:
 * - IO: сетевые запросы, DB операции
 * - Default: маппинг (CPU-intensive)
 */
@HiltViewModel
class ArticleViewModel @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val getArticleUseCase: GetArticleUseCase,
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase,
    private val isFavoriteUseCase: IsFavoriteUseCase,
    private val mapper: UiWikiMapper,
) : BaseViewModel() {

    /** Состояние экрана */
    private val _state = MutableStateFlow<ArticleState>(ArticleState.Loading)
    val state: StateFlow<ArticleState> = _state.asStateFlow()

    /**
     * Загрузить статью
     */
    fun loadArticle(title: String) {
        viewModelScope.launch {
            _state.value = ArticleState.Loading

            // UseCase уже main-safe - выполняют IO на IO dispatcher
            when (val result = getArticleUseCase.execute(title)) {
                is WikiResult.Success -> {
                    val article = result.data

                    // DB операция - UseCase уже main-safe
                    val isFavorite = isFavoriteUseCase.execute(title)

                    // Маппинг - CPU intensive, выполняем на Default dispatcher
                    val uiArticle = withContext(dispatcherProvider.default) {
                        mapper.mapToUi(article, isFavorite)
                    }

                    // UI обновление - на Main (viewModelScope уже на Main)
                    _state.value = ArticleState.Content(
                        article = uiArticle,
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
                // UseCase уже main-safe - выполняют DB на IO dispatcher
                if (currentState.isFavorite) {
                    removeFavoriteUseCase.execute(currentState.article.title)
                } else {
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
                }

                // UI обновление - на Main
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
        val article: UiWikiArticle,
        val isFavorite: Boolean,
    ) : ArticleState()
    object NotFound : ArticleState()
    data class Error(val message: String) : ArticleState()
}
