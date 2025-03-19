package su.sv.news.presentation.root

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController

@Composable
fun RootNews(navController: NavHostController, viewModel : RootNewsViewModel = hiltViewModel()) {
    Text("RootNews")
}
