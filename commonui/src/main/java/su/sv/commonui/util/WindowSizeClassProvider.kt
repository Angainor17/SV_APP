package su.sv.commonui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
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
 * Запомнить форм-фактор устройства на основе текущей конфигурации
 *
 * Автоматически обновляется при изменении размера экрана
 * (поворот, fold/unfold на foldables)
 *
 * @return текущий форм-фактор устройства
 */
@Composable
fun rememberDeviceFormFactor(): DeviceFormFactor {
    val configuration = LocalConfiguration.current

    return remember(configuration.screenWidthDp) {
        DeviceFormFactor.fromWidthDp(configuration.screenWidthDp)
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