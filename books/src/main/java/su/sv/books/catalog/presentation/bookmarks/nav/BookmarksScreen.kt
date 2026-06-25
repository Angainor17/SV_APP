package su.sv.books.catalog.presentation.bookmarks.nav

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import kotlinx.parcelize.Parcelize
import su.sv.books.catalog.presentation.bookmarks.ui.BookmarksContent
import su.sv.commonui.theme.SVAPPTheme
import su.sv.managers.theme.ThemeViewModel

/**
 * Modo Screen для экрана заметок
 */
@Parcelize
class BookmarksScreen(
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen, Parcelable {

    @Composable
    override fun Content(modifier: Modifier) {
        val themeViewModel: ThemeViewModel = hiltViewModel()
        val themeConfig by themeViewModel.themeConfig.collectAsStateWithLifecycle()

        SVAPPTheme(
            themeMode = themeConfig.themeMode,
            useDynamicColors = themeConfig.useDynamicColors
        ) {
            BookmarksContent()
        }
    }
}
