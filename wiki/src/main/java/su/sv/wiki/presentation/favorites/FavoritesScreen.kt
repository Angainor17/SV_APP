package su.sv.wiki.presentation.favorites

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import kotlinx.parcelize.Parcelize
import su.sv.commonui.theme.SVAPPTheme

@Parcelize
class FavoritesScreen(
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen {

    @Composable
    override fun Content(modifier: Modifier) {
        SVAPPTheme {
            FavoritesScreenContent()
        }
    }
}
