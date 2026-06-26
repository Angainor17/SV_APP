package su.sv.books.catalog.presentation.bookmarks.nav

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import kotlinx.parcelize.Parcelize
import su.sv.books.catalog.presentation.bookmarks.ui.BookmarksContent

/**
 * Modo Screen для экрана заметок
 */
@Parcelize
class BookmarksScreen(
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen, Parcelable {

    @Composable
    override fun Content(modifier: Modifier) {
        BookmarksContent()
    }
}
