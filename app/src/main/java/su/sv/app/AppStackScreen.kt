package su.sv.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.terrakok.modo.stack.StackNavModel
import com.github.terrakok.modo.stack.StackScreen
import kotlinx.parcelize.Parcelize

/**
 * Кастомный StackScreen с плавными анимациями переходов.
 *
 * Использует [AppScreenTransition] для красивых slide-анимаций между экранами:
 * - Push: новый экран въезжает справа, старый уезжает влево
 * - Pop: предыдущий экран въезжает слева, текущий уезжает вправо
 * - Replace: плавный fade переход
 *
 * Пример использования:
 * ```kotlin
 * val rootScreen = rememberRootScreen {
 *     AppStackScreen(StackNavModel(BottomNavScreen()))
 * }
 * rootScreen.Content(modifier = Modifier.fillMaxSize())
 * ```
 */
@Parcelize
class AppStackScreen(
    private val navModel: StackNavModel
) : StackScreen(navModel) {

    @Composable
    override fun Content(modifier: Modifier) {
        TopScreenContent(modifier) { screenModifier ->
            AppScreenTransition(modifier = screenModifier)
        }
    }
}