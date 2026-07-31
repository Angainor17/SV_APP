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
 *
 * @param bookFileUri Опциональный URI файла книги для фильтрации (вычислит MD5)
 * @param bookTitle Опциональное название книги для заголовка
 */
@Parcelize
class BookmarksScreen(
    override val screenKey: ScreenKey = generateScreenKey(),
    val bookFileUri: String? = null,
    val bookTitle: String? = null,
) : Screen, Parcelable {

    @Composable
    override fun Content(modifier: Modifier) {
        BookmarksContent(
            filterBookFileUri = bookFileUri,
            filterBookTitle = bookTitle,
        )
    }
}