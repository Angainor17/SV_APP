package su.sv.commonui.ui.adaptive.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import su.sv.commonui.theme.DeviceFormFactor
import su.sv.commonui.theme.LocalAdaptiveDimensions
import su.sv.commonui.theme.LocalDeviceFormFactor

/**
 * Адаптивная навигация - выбирает нужный тип навигации в зависимости от форм-фактора
 *
 * - Compact: BottomNavigation (снизу)
 * - Medium/Expanded: NavigationRail (слева)
 *
 * Использование:
 * ```kotlin
 * AdaptiveNavigation(
 *     items = listOf(
 *         NavigationItem("News", Icons.Default.Home, "news"),
 *         NavigationItem("Books", Icons.Default.Book, "books"),
 *     ),
 *     selectedItem = 0,
 *     onItemSelected = { index -> ... }
 * )
 * ```
 *
 * @param items элементы навигации
 * @param selectedItem индекс выбранного элемента
 * @param onItemSelected callback при выборе элемента
 * @param modifier модификатор
 */
@Composable
fun AdaptiveNavigation(
    items: List<NavigationItem>,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formFactor = LocalDeviceFormFactor.current

    when (formFactor) {
        is DeviceFormFactor.Compact -> {
            CompactNavigation(
                items = items,
                selectedItem = selectedItem,
                onItemSelected = onItemSelected,
                modifier = modifier.fillMaxWidth(),
            )
        }

        is DeviceFormFactor.Medium,
        is DeviceFormFactor.Expanded -> {
            RailNavigation(
                items = items,
                selectedItem = selectedItem,
                onItemSelected = onItemSelected,
                modifier = modifier.fillMaxHeight(),
            )
        }
    }
}

/**
 * Адаптивный контейнер для контента с учётом навигации
 *
 * - Compact: Scaffold с BottomNavigation
 * - Medium/Expanded: Row с NavigationRail и контентом
 *
 * @param navigationContent контент навигации (вызывается с выбранным элементом)
 * @param content основной контент
 * @param modifier модификатор
 */
@Composable
fun AdaptiveNavigationScaffold(
    navigationContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formFactor = LocalDeviceFormFactor.current
    val adaptiveDims = LocalAdaptiveDimensions.current

    when (formFactor) {
        is DeviceFormFactor.Compact -> {
            // Для Compact - навигация снизу, content сам управляет Scaffold
            Box(modifier = modifier.fillMaxSize()) {
                content()
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                ) {
                    navigationContent()
                }
            }
        }

        is DeviceFormFactor.Medium,
        is DeviceFormFactor.Expanded -> {
            // Для Medium/Expanded - навигация слева, контент справа
            androidx.compose.foundation.layout.Row(
                modifier = modifier.fillMaxSize(),
            ) {
                // NavigationRail слева
                navigationContent()

                // Контент справа с ограничением ширины
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                adaptiveDims.contentMaxWidth?.let { maxWidth ->
                                    Modifier.fillMaxWidth()
                                    // Ограничение ширины будет обработано в content
                                } ?: Modifier.fillMaxSize(),
                            ),
                    ) {
                        content()
                    }
                }
            }
        }
    }
}