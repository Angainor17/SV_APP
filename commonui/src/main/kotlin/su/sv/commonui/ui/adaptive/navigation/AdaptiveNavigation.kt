package su.sv.commonui.ui.adaptive.navigation

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import su.sv.commonui.theme.DeviceFormFactor
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