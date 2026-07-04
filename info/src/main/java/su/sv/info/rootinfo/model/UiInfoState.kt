package su.sv.info.rootinfo.model

import androidx.compose.runtime.Immutable

/**
 * Состояние экрана инфо
 */
sealed class UiInfoState() {

    /**
     * @Immutable - оптимизация Compose recomposition
     */
    @Immutable
    data class Content(
        val isRefreshing: Boolean = false,
        val items: List<UiLinkItem>,
    ) : UiInfoState()

    object Loading : UiInfoState()

    object Failure : UiInfoState()
}
