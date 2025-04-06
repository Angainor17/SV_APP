package su.sv.reader.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import su.sv.models.ui.book.UiBook

@Composable
fun BookReader(uiBook: UiBook, modifier: Modifier) {
    Box(modifier = modifier.statusBarsPadding()) {
        Text("Reader")
    }
}
