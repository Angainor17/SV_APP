package su.sv.info.rootinfo.model

/**
 * Состояние экрана инфо
 */
sealed class UiInfoState() {

    data class Content(
        val isRefreshing: Boolean = false,
        val items: List<UiLinkItem>,
    ) : UiInfoState()

    object Loading : UiInfoState()

    object Failure : UiInfoState()
}
