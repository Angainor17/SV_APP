package com.github.axet.bookreader.screens

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import com.github.terrakok.modo.stack.forward
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import su.sv.commonui.theme.SVAPPTheme
import su.sv.managers.theme.ThemeViewModel

/**
 * Экран чтения книги (Modo Screen)
 *
 * @param bookUri URI файла книги
 * @param position Начальная позиция в книге (опционально)
 */
@Parcelize
class ReaderScreen(
    val bookUri: @RawValue Uri,
    val position: @RawValue Parcelable? = null,
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen, Parcelable {

    @Composable
    override fun Content(modifier: Modifier) {
        val stackNavigation = LocalStackNavigation.current
        val themeViewModel: ThemeViewModel = hiltViewModel()
        val themeConfig by themeViewModel.themeConfig.collectAsStateWithLifecycle()

        SVAPPTheme(
            themeMode = themeConfig.themeMode,
            useDynamicColors = themeConfig.useDynamicColors
        ) {
            ReaderContent(
                bookUri = bookUri,
                initialPosition = position,
                onNavigateBack = {
                    stackNavigation.back()
                },
                onNavigateToSettings = {
                    stackNavigation.forward(ReaderSettingsScreen())
                },
                modifier = modifier,
            )
        }
    }
}
