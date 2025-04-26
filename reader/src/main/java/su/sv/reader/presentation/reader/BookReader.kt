package su.sv.reader.presentation.reader

//import com.rizzi.bouquet.VerticalPDFReader/**/
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import su.sv.commonui.ui.FullScreenLoading
import su.sv.models.ui.book.UiBook
import su.sv.reader.presentation.reader.model.BookReaderState

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun BookReader(
    uiBook: UiBook,
    modifier: Modifier,
    viewModel: BookReaderViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadBook(uiBook)
    }

    Scaffold(
        modifier = modifier.statusBarsPadding(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { contentPadding ->
        when (val stateValue = state.value) {
//            is BookReaderState.Content -> {
//                Content(
//                    state = stateValue,
//                )
//            }

            BookReaderState.Loading -> {
                FullScreenLoading()
            }
        }
    }
}

//@Composable
//private fun Content(state: BookReaderState.Content) {
//    Box {
//        VerticalPDFReader(
//            state = state.pdfReaderState,
//            modifier = Modifier.fillMaxSize()
//        )
//    }
//}
