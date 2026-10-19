package su.sv.commonui.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal для доступа к форм-фактору устройства
 *
 * Использование:
 * ```kotlin
 * val formFactor = LocalDeviceFormFactor.current
 *
 * if (formFactor.shouldUseNavigationRail()) {
 *     // Показать NavigationRail
 * } else {
 *     // Показать BottomNavigation
 * }
 * ```
 *
 * Должен быть предоставлен через CompositionLocalProvider:
 * ```kotlin
 * val formFactor = rememberDeviceFormFactor()
 *
 * CompositionLocalProvider(LocalDeviceFormFactor provides formFactor) {
 *     // Контент
 * }
 * ```
 */
val LocalDeviceFormFactor = compositionLocalOf<DeviceFormFactor> {
    // По умолчанию Compact - для Preview и тестов
    DeviceFormFactor.Compact
}

/**
 * CompositionLocal для доступа к адаптивным размерам
 *
 * Использование:
 * ```kotlin
 * val adaptiveDims = LocalAdaptiveDimensions.current
 *
 * LazyVerticalGrid(
 *     columns = GridCells.Fixed(adaptiveDims.gridColumns)
 * ) { ... }
 * ```
 */
val LocalAdaptiveDimensions = compositionLocalOf<AdaptiveDimensions> {
    // По умолчанию Compact - для Preview и тестов
    AdaptiveDimensions.compact()
}