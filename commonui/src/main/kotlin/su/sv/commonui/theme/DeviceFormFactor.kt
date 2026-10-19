package su.sv.commonui.theme

/**
 * Форм-фактор устройства для адаптивного UI
 *
 * Определяется на основе ширины экрана:
 * - Compact: < 600dp (телефоны)
 * - Medium: 600-840dp (планшеты портрет, foldables сложенные)
 * - Expanded: >= 840dp (планшеты ландшафт, foldables разложенные)
 *
 * @see <a href="https://developer.android.com/guide/topics/large-screens/support-large-screens">Large screens guide</a>
 */
sealed class DeviceFormFactor {

    /**
     * Компактные устройства (телефоны)
     * Ширина экрана < 600dp
     *
     * UI: BottomNavigation, 2 колонки
     */
    data object Compact : DeviceFormFactor()

    /**
     * Средние устройства (планшеты портрет, foldables сложенные)
     * Ширина экрана 600-840dp
     *
     * UI: NavigationRail, 3 колонки, ограничение ширины контента
     */
    data object Medium : DeviceFormFactor()

    /**
     * Большие устройства (планшеты ландшафт, foldables разложенные)
     * Ширина экрана >= 840dp
     *
     * UI: NavigationRail, 4 колонки, master-detail layout
     */
    data object Expanded : DeviceFormFactor()

    /**
     * Проверка, является ли устройство компактным
     */
    fun isCompact(): Boolean = this is Compact

    /**
     * Проверка, является ли устройство средним
     */
    fun isMedium(): Boolean = this is Medium

    /**
     * Проверка, является ли устройство большим (планшет)
     */
    fun isExpanded(): Boolean = this is Expanded

    /**
     * Проверка, нужно ли использовать NavigationRail
     * (Medium и Expanded)
     */
    fun shouldUseNavigationRail(): Boolean = this is Medium || this is Expanded

    /**
     * Проверка, нужно ли использовать master-detail layout
     * (только Expanded)
     */
    fun shouldUseMasterDetail(): Boolean = this is Expanded

    companion object {
        /**
         * Порог для перехода от Compact к Medium
         */
        const val MEDIUM_THRESHOLD_DP = 600

        /**
         * Порог для перехода от Medium к Expanded
         */
        const val EXPANDED_THRESHOLD_DP = 840

        /**
         * Определить форм-фактор по ширине экрана в dp
         *
         * @param widthDp ширина экрана в dp
         * @return соответствующий форм-фактор
         */
        fun fromWidthDp(widthDp: Int): DeviceFormFactor = when {
            widthDp < MEDIUM_THRESHOLD_DP -> Compact
            widthDp < EXPANDED_THRESHOLD_DP -> Medium
            else -> Expanded
        }

        /**
         * Определить форм-фактор по ширине экрана в dp (Float версия)
         */
        fun fromWidthDp(widthDp: Float): DeviceFormFactor = fromWidthDp(widthDp.toInt())
    }
}