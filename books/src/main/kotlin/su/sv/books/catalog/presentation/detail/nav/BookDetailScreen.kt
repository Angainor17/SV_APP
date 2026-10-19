package su.sv.books.catalog.presentation.detail.nav

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import kotlinx.parcelize.Parcelize
import su.sv.books.catalog.presentation.detail.ui.BookDetailUi
import su.sv.models.ui.book.UiBook

@Parcelize
class BookDetailScreen(
    private val uiBook: UiBook,
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(modifier: Modifier) {
        BookDetailUi(
            uiBook = uiBook,
            modifier = modifier,
        )
    }
}
