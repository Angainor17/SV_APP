package su.sv.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import su.sv.commonarchitecture.presentation.base.BaseActivity
import su.sv.commonui.theme.SVAPPTheme
import su.sv.main.bottomnav.BottomNavigationBar

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SVAPPTheme {
                BottomNavigationBar()
            }
        }
    }
}
