package su.sv.commonui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Адаптивные размеры для UI в зависимости от форм-фактора
 *
 * Предоставляет размеры, которые меняются в зависимости от размера экрана:
 * - Количество колонок в сетке
 * - Отступы от краёв экрана
 * - Максимальная ширина контента
 *
 * @param gridColumns количество колонок в сетке
 * @param screenPadding горизонтальный отступ от краёв экрана
 * @param contentMaxWidth максимальная ширина контента (null = без ограничения)
 * @param gridSpacing отступ между элементами сетки
 */
data class AdaptiveDimensions(
    val gridColumns: Int,
    val screenPadding: Dp,
    val contentMaxWidth: Dp?,
    val gridSpacing: Dp,
) {
    companion object {
        /**
         * Размеры для Compact (телефоны)
         * - 2 колонки
         * - Стандартные отступы
         * - Без ограничения ширины
         */
        fun compact(baseDimensions: AppDimensions = AppDimensions.Default): AdaptiveDimensions =
            AdaptiveDimensions(
                gridColumns = 2,
                screenPadding = baseDimensions.screenPaddingHorizontal,
                contentMaxWidth = null, // Без ограничения
                gridSpacing = baseDimensions.itemSpacingMedium,
            )

        /**
         * Размеры для Medium (планшеты портрет, foldables сложенные)
         * - 3 колонки
         * - Увеличенные отступы
         * - Ограничение ширины 600dp
         */
        fun medium(baseDimensions: AppDimensions = AppDimensions.Default): AdaptiveDimensions =
            AdaptiveDimensions(
                gridColumns = 3,
                screenPadding = baseDimensions.screenPaddingHorizontalLarge,
                contentMaxWidth = 600.dp,
                gridSpacing = baseDimensions.itemSpacingLarge,
            )

        /**
         * Размеры для Expanded (планшеты ландшафт, foldables разложенные)
         * - 4 колонки
         * - Большие отступы
         * - Ограничение ширины 840dp
         */
        fun expanded(baseDimensions: AppDimensions = AppDimensions.Default): AdaptiveDimensions =
            AdaptiveDimensions(
                gridColumns = 4,
                screenPadding = baseDimensions.screenPaddingHorizontalLarge,
                contentMaxWidth = 840.dp,
                gridSpacing = baseDimensions.itemSpacingLarge,
            )

        /**
         * Получить размеры по форм-фактору
         */
        fun fromFormFactor(
            formFactor: DeviceFormFactor,
            baseDimensions: AppDimensions = AppDimensions.Default,
        ): AdaptiveDimensions = when (formFactor) {
            is DeviceFormFactor.Compact -> compact(baseDimensions)
            is DeviceFormFactor.Medium -> medium(baseDimensions)
            is DeviceFormFactor.Expanded -> expanded(baseDimensions)
        }
    }
}