package su.sv.books.catalog.presentation.detail.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import kotlinx.parcelize.Parcelize
import su.sv.books.catalog.presentation.detail.ui.BookDetailUi
import su.sv.books.catalog.presentation.root.model.UiBook

@Parcelize
class BookDetailScreen(
    private val uiBook: UiBook,
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen {

    @Composable
    override fun Content(modifier: Modifier) {
        BookDetailUi(
            uiBook = uiBook,
        )
    }
}
