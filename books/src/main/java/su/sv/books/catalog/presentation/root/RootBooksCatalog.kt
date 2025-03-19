package su.sv.books.catalog.presentation.root

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController

@Composable
fun RootBooksCatalog(
    navController: NavHostController,
    viewModel: RootBooksCatalogViewModel = hiltViewModel(),
) {
//    val state by viewModel.state.collectAsStateWithLifecycle()

//    LazyColumn {
//        items(key = {}) {
//
//        }
//    }
}
