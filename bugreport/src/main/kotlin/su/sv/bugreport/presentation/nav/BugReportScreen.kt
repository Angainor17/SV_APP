package su.sv.bugreport.presentation.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import kotlinx.parcelize.Parcelize
import su.sv.bugreport.presentation.bugreport.ui.BugReportContent

/**
 * Modo Screen для отправки баг-репорта
 */
@Parcelize
class BugReportScreen(
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen {

    @Composable
    override fun Content(modifier: Modifier) {
        BugReportContent()
    }
}