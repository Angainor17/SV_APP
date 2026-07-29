package su.sv.commonui.ui.adaptive.layout

/**
 * Состояние master-detail layout
 *
 * Управляет выбранным элементом и видимостью detail панели.
 *
 * @param T тип выбранного элемента
 * @param selectedItem выбранный элемент (null если ничего не выбрано)
 * @param isDetailVisible видима ли detail панель
 */
data class MasterDetailState<T>(
    val selectedItem: T? = null,
    val isDetailVisible: Boolean = false,
) {
    /**
     * Выбрать элемент и показать detail панель
     */
    fun selectItem(item: T): MasterDetailState<T> = copy(
        selectedItem = item,
        isDetailVisible = true
    )

    /**
     * Сбросить выбор и скрыть detail панель
     */
    fun clearSelection(): MasterDetailState<T> = copy(
        selectedItem = null,
        isDetailVisible = false
    )

    /**
     * Проверить, есть ли выбранный элемент
     */
    fun hasSelection(): Boolean = selectedItem != null

    companion object {
        /**
         * Создать начальное состояние
         */
        fun <T> initial(): MasterDetailState<T> = MasterDetailState()
    }
}