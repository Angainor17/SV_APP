package su.sv.books.catalog.presentation.detail.ui

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.axet.bookreader.activities.MainActivity
import su.sv.books.catalog.presentation.detail.viewmodel.BookDetailViewModel
import su.sv.models.ui.book.UiBook


@Composable
fun BookDetailUi(
    viewModel: BookDetailViewModel = hiltViewModel(),
    uiBook: UiBook,
    modifier: Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier.statusBarsPadding()) {
        Text(
            text = "Detail Book",
            modifier = Modifier,
        )

        Button(
            onClick = {
                val intent = Intent(context, MainActivity::class.java)
                intent.action = Intent.ACTION_VIEW
                intent.data = uiBook.fileUri
                context.startActivity(intent)
            },
        ) {
            Text(text = "Open reader")
        }
    }
}
