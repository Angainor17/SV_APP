package com.github.axet.bookreader.screens.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.github.axet.bookreader.R
import com.github.axet.bookreader.app.BookReaderInitializer
import com.github.axet.bookreader.app.ReaderPreferences
import com.github.axet.bookreader.screens.testing.ReaderTestTags

/**
 * Compose BottomSheet для настроек шрифтов
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FontsComposeBottomSheet(
    onDismiss: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onFontFamilyChange: (String) -> Unit,
    onIgnoreEmbeddedFontsChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val shared = remember {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    var fontSize by remember {
        mutableFloatStateOf(
            shared.getInt(
                ReaderPreferences.PREFERENCE_FONTSIZE_FBREADER,
                16
            ).toFloat()
        )
    }
    var selectedFont by remember {
        mutableStateOf(
            shared.getString(
                ReaderPreferences.PREFERENCE_FONTFAMILY_FBREADER,
                "sans-serif"
            ) ?: "sans-serif"
        )
    }
    var ignoreEmbeddedFonts by remember {
        mutableStateOf(
            shared.getBoolean(
                ReaderPreferences.PREFERENCE_IGNORE_EMBEDDED_FONTS,
                false
            )
        )
    }

    // Получаем список доступных шрифтов
    val fonts = remember {
        val ttf = BookReaderInitializer.getTTFManager()
        val fontList = mutableListOf("sans-serif", "serif", "monospace")
        ttf?.let {
            // Добавляем системные шрифты
            org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil.ourFontFileMap.keys.forEach { name ->
                if (!fontList.contains(name)) {
                    fontList.add(name)
                }
            }
        }
        fontList.sorted()
    }

    ModalBottomSheet(
        modifier = Modifier.testTag(ReaderTestTags.FontSettings.SHEET),
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.sv_font_settings_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Размер шрифта
            Text(
                modifier = Modifier.testTag(ReaderTestTags.FontSettings.SIZE_VALUE),
                text = stringResource(R.string.sv_font_size_label, fontSize.toInt()),
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                modifier = Modifier.testTag(ReaderTestTags.FontSettings.SIZE_SLIDER),
                value = fontSize,
                onValueChange = { newSize ->
                    fontSize = newSize
                    onFontSizeChange(newSize.toInt())
                },
                valueRange = 8f..48f,
                steps = 40
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Выбор шрифта
            Text(
                text = stringResource(R.string.sv_font_label),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                items(
                    items = fonts,
                    key = { font -> font },
                ) { font ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedFont = font
                                onFontFamilyChange(font)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFont == font,
                            onClick = {
                                selectedFont = font
                                onFontFamilyChange(font)
                            }
                        )
                        Text(
                            text = font,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Игнорировать встроенные шрифты
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        ignoreEmbeddedFonts = !ignoreEmbeddedFonts
                        onIgnoreEmbeddedFontsChange(ignoreEmbeddedFonts)
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = ignoreEmbeddedFonts,
                    onCheckedChange = { checked ->
                        ignoreEmbeddedFonts = checked
                        onIgnoreEmbeddedFontsChange(checked)
                    }
                )
                Text(
                    text = stringResource(R.string.sv_ignore_embedded_fonts),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
