package com.github.axet.bookreader.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import kotlinx.parcelize.Parcelize

/**
 * Экран настроек читалки (Modo Screen)
 */
@Parcelize
class ReaderSettingsScreen(
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen {

    @Composable
    override fun Content(modifier: Modifier) {
        ReaderSettingsContent(
            modifier = modifier,
        )
    }
}
