package su.sv.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.Modo
import com.github.terrakok.modo.Modo.rememberRootScreen
import com.github.terrakok.modo.RootScreen
import com.github.terrakok.modo.stack.StackNavModel
import com.github.terrakok.modo.stack.StackScreen
import dagger.hilt.android.AndroidEntryPoint
import su.sv.commonarchitecture.presentation.base.BaseActivity
import su.sv.commonui.theme.SVAPPTheme
import su.sv.main.bottomnav.BottomNavScreen
import su.sv.managers.theme.ThemeViewModel

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private var rootScreen: RootScreen<StackScreen>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeConfig by themeViewModel.themeConfig.collectAsStateWithLifecycle()

            SVAPPTheme(
                themeMode = themeConfig.themeMode,
                useDynamicColors = themeConfig.useDynamicColors
            ) {
                // Remember root screen using rememberSaveable under the hood.
                val rootScreen = rememberRootScreen {
                    AppStackScreen(
                        StackNavModel(
                            BottomNavScreen()
                        )
                    )
                }
                rootScreen.Content(modifier = Modifier.fillMaxSize())
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Modo.save(outState, rootScreen)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            Modo.onRootScreenFinished(rootScreen)
        }
    }
}
