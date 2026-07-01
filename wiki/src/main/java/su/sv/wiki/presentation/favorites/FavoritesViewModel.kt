package su.sv.wiki.presentation.favorites

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import su.sv.wiki.domain.model.WikiArticle
import su.sv.wiki.domain.usecase.ClearFavoritesUseCase
import su.sv.wiki.domain.usecase.GetFavoritesUseCase
import javax.inject.Inject

/**
 * ViewModel экрана избранного
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    getFavoritesUseCase: GetFavoritesUseCase,
    private val clearFavoritesUseCase: ClearFavoritesUseCase,
) : BaseViewModel() {

    /** Список избранных статей с контентом */
    val favorites: Flow<List<WikiArticle>> = getFavoritesUseCase.invoke()

    /**
     * Очистить всё избранное
     */
    fun clearFavorites() {
        viewModelScope.launch {
            clearFavoritesUseCase.execute()
        }
    }
}
