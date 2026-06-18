package su.sv.wiki.presentation.favorites

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import su.sv.wiki.domain.usecase.ClearFavoritesUseCase
import su.sv.wiki.domain.usecase.GetFavoriteTitlesUseCase
import javax.inject.Inject

/**
 * ViewModel экрана избранного
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    getFavoriteTitlesUseCase: GetFavoriteTitlesUseCase,
    private val clearFavoritesUseCase: ClearFavoritesUseCase,
) : BaseViewModel() {

    /** Список названий избранных статей */
    val favorites: Flow<List<String>> = getFavoriteTitlesUseCase.execute()

    /**
     * Очистить всё избранное
     */
    fun clearFavorites() {
        viewModelScope.launch {
            clearFavoritesUseCase.execute()
        }
    }
}
