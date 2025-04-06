package su.sv.books.catalog.presentation.detail.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.forward
import su.sv.books.catalog.presentation.detail.viewmodel.BookDetailViewModel
import su.sv.models.ui.book.UiBook
import su.sv.reader.presentation.BookReaderScreen

@Composable
fun BookDetailUi(
    viewModel: BookDetailViewModel = hiltViewModel(),
    uiBook: UiBook,
    modifier: Modifier
) {
    val stackNavigation = LocalStackNavigation.current

    Column (modifier = modifier.statusBarsPadding()){
        Text(
            text = "Detail Book",
            modifier = Modifier,
        )

        Button(
            onClick = {
                stackNavigation.forward(BookReaderScreen(uiBook))
            },
        ) {
            Text(text = "Open reader")
        }
    }
}
