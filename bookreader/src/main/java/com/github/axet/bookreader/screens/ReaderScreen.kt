package com.github.axet.bookreader.screens

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import com.github.terrakok.modo.stack.forward
import kotlinx.parcelize.Parcelize

/**
 * Параметры позиции в книге для навигации к заметке
 */
@Parcelize
data class BookmarkPosition(
    val startParagraph: Int,
    val startElement: Int,
    val startChar: Int,
    val endParagraph: Int,
    val endElement: Int,
    val endChar: Int,
) : Parcelable

/**
 * Экран чтения книги (Modo Screen)
 *
 * @param bookUri URI файла книги
 * @param bookCoverUrl URL обложки книги (из API/модуля books) для сохранения в заметках
 * @param bookmarkPosition Позиция заметки для навигации (опционально)
 */
@Parcelize
class ReaderScreen(
    val bookUri: Uri,
    val bookCoverUrl: String? = null,
    val bookmarkPosition: BookmarkPosition? = null,
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen, Parcelable {

    @Composable
    override fun Content(modifier: Modifier) {
        val stackNavigation = LocalStackNavigation.current

        ReaderContent(
            bookUri = bookUri,
            bookCoverUrl = bookCoverUrl,
            bookmarkPosition = bookmarkPosition,
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
