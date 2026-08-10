package su.sv.commonui.ui.adaptive.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Rail Navigation - NavigationRail для планшетов и foldables
 *
 * Вертикальная навигация слева для устройств с Medium и Expanded форм-фактором.
 * Компактный дизайн - только иконки.
 *
 * @param items элементы навигации
 * @param selectedItem индекс выбранного элемента
 * @param onItemSelected callback при выборе элемента
 * @param modifier модификатор
 * @param showLabels показывать ли подписи (по умолчанию false - только иконки)
 */
@Composable
fun RailNavigation(
    items: List<NavigationItem>,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showLabels: Boolean = false,
) {
    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            items.forEachIndexed { index, navigationItem ->
                NavigationRailItem(
                    selected = index == selectedItem,
                    icon = {
                        RailNavigationIcon(
                            icon = navigationItem.icon,
                            label = navigationItem.label,
                            showBadge = navigationItem.showBadge,
                        )
                    },
                    label = if (showLabels) {
                        { Text(navigationItem.label) }
                    } else {
                        null
                    },
                    onClick = { onItemSelected(index) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSurface,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun RailNavigationIcon(
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