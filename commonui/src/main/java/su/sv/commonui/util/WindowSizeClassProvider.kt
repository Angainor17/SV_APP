package su.sv.commonui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import su.sv.commonui.theme.AdaptiveDimensions
import su.sv.commonui.theme.AppDimensions
import su.sv.commonui.theme.DeviceFormFactor
import su.sv.commonui.theme.LocalAdaptiveDimensions
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.theme.LocalDeviceFormFactor

/**
 * Утилиты для определения размера экрана и форм-фактора
 */

/**
 * Текущий класс размера окна приложения через androidx.window (WindowSizeClass).
 *
 * В отличие от `LocalConfiguration.screenWidthDp` (ширина всего экрана) учитывает
 * фактический размер окна — корректно работает в split-screen/multi-window и на
 * foldable при складывании/раскладывании.
 */
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val size = windowInfo.containerSize

    return remember(size, density.density) {
        WindowSizeClass.compute(
            size.width.toInt(),
            size.height.toInt(),
            density.density,
        )
    }
}

/**
 * Запомнить форм-фактор устройства на основе класса размера окна.
 *
 * Автоматически обновляется при изменении размера окна
 * (поворот, fold/unfold на foldables, split-screen).
 */
@Composable
fun rememberDeviceFormFactor(): DeviceFormFactor {
    val windowSizeClass = rememberWindowSizeClass()
    val widthClass = windowSizeClass.windowWidthSizeClass

    return remember(widthClass) {
        when {
            widthClass == WindowWidthSizeClass.COMPACT -> DeviceFormFactor.Compact
            widthClass == WindowWidthSizeClass.MEDIUM -> DeviceFormFactor.Medium
            else -> DeviceFormFactor.Expanded
        }
    }
}

/**
 * Запомнить адаптивные размеры на основе форм-фактора
 *
 * @param formFactor форм-фактор (по умолчанию определяется автоматически)
 * @param baseDimensions базовые размеры (по умолчанию из LocalAppDimensions)
 * @return адаптивные размеры для текущего форм-фактора
 */
@Composable
fun rememberAdaptiveDimensions(
    formFactor: DeviceFormFactor = rememberDeviceFormFactor(),
    baseDimensions: AppDimensions = LocalAppDimensions.current,
): AdaptiveDimensions {
    return remember(formFactor, baseDimensions) {
        AdaptiveDimensions.fromFormFactor(formFactor, baseDimensions)
    }
}

/**
 * Предоставить адаптивные размеры через CompositionLocal
 *
 * Использование:
 * ```kotlin
 * ProvideAdaptiveDimensions {
 *     // Внутри можно использовать LocalAdaptiveDimensions.current
 *     val adaptiveDims = LocalAdaptiveDimensions.current
 * }
 * ```
 *
 * @param content контент, которому предоставляются адаптивные размеры
 */
@Composable
fun ProvideAdaptiveDimensions(
    content: @Composable () -> Unit,
) {
    val formFactor = rememberDeviceFormFactor()
    val adaptiveDimensions = rememberAdaptiveDimensions(formFactor)

    // Предоставляем оба CompositionLocal
    androidx.compose.runtime.CompositionLocalProvider(
        LocalDeviceFormFactor provides formFactor,
        LocalAdaptiveDimensions provides adaptiveDimensions,
    ) {
        content()
    }
}

/**
 * Ширина экрана в dp
 */
@Composable
fun rememberScreenWidthDp(): Int {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp) {
        configuration.screenWidthDp
    }
}

/**
 * Высота экрана в dp
 */
@Composable
fun rememberScreenHeightDp(): Int {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenHeightDp) {
        configuration.screenHeightDp
    }
}

/**
 * Проверка, находится ли устройство в ландшафтной ориентации
 */
@Composable
fun isLandscapeOrientation(): Boolean {
    val widthDp = rememberScreenWidthDp()
    val heightDp = rememberScreenHeightDp()
    return widthDp > heightDp
}

/**
 * Проверка, находится ли устройство в портретной ориентации
 */
@Composable
fun isPortraitOrientation(): Boolean = !isLandscapeOrientation()