package su.sv.commonui.ui.adaptive.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import su.sv.commonui.theme.navigationBarColor

/**
 * Элемент навигации для BottomNavigation
 */
data class NavigationItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val showBadge: Boolean = false,
)

/**
 * Compact Navigation - BottomNavigation для телефонов
 *
 * Это текущая реализация BottomNavigation, вынесенная в отдельный компонент.
 * Используется для устройств с Compact форм-фактором (< 600dp).
 *
 * @param items элементы навигации
 * @param selectedItem индекс выбранного элемента
 * @param onItemSelected callback при выборе элемента
 * @param modifier модификатор
 */
@Composable
fun CompactNavigation(
    items: List<NavigationItem>,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.navigationBarColor,
        tonalElevation = 4.dp,
    ) {
        items.forEachIndexed { index, navigationItem ->
            NavigationBarItem(
                selected = index == selectedItem,
                label = {
                    Text(
                        text = navigationItem.label,
                        textAlign = TextAlign.Center,
                    )
                },
                icon = {
                    NavigationIcon(
                        icon = navigationItem.icon,
                        label = navigationItem.label,
                        showBadge = navigationItem.showBadge,
                    )
                },
                onClick = { onItemSelected(index) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun NavigationIcon(
    icon: ImageVector,
    label: String,
    showBadge: Boolean,
) {
    BadgedBox(
        badge = {
            if (showBadge) {
                Badge()
            }
        }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
        )
    }
}