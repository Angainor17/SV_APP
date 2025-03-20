package su.sv.main.bottomnav

import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavigationItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
)
