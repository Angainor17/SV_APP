package com.github.axet.bookreader.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import kotlinx.parcelize.Parcelize
import su.sv.commonui.theme.SVAPPTheme
import su.sv.managers.theme.ThemeViewModel

/**
 * Экран настроек читалки (Modo Screen)
 */
@Parcelize
class ReaderSettingsScreen(
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen {

    @Composable
    override fun Content(modifier: Modifier) {
        val themeViewModel: ThemeViewModel = hiltViewModel()
        val themeConfig by themeViewModel.themeConfig.collectAsStateWithLifecycle()

        SVAPPTheme(
            themeMode = themeConfig.themeMode,
            useDynamicColors = themeConfig.useDynamicColors
        ) {
            ReaderSettingsContent(
                modifier = modifier,
            )
        }
    }
}
