package su.sv.shpargalki.presentation.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import su.sv.shpargalki.R

/**
 * Готический (Fraktur) шрифт для немецких названий понятий.
 *
 * UnifrakturCook — открытый шрифт под лицензией SIL OFL,
 * имитирует классическую немецкую готическую печать.
 */
val GothicFontFamily = FontFamily(
    Font(R.font.unifrakturcook, FontWeight.Normal),
)
