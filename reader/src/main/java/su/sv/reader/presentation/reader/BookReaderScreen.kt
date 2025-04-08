package su.sv.reader.presentation.reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import kotlinx.parcelize.Parcelize
import su.sv.models.ui.book.UiBook

@Parcelize
class BookReaderScreen(
    private val uiBook: UiBook,
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen {

    @Composable
    override fun Content(modifier: Modifier) {
        BookReader(uiBook, modifier)
    }
}
