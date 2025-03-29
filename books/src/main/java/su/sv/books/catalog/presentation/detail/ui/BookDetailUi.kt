package su.sv.books.catalog.presentation.detail.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import su.sv.books.catalog.presentation.detail.viewmodel.BookDetailViewModel
import su.sv.books.catalog.presentation.root.model.UiBook

@Composable
fun BookDetailUi(
    viewModel: BookDetailViewModel = hiltViewModel(),
    uiBook: UiBook,
) {
    Box {
        Text(
            text = "Detail Book",
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
