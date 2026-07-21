package su.sv.news.presentation.debug.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import su.sv.news.R

/**
 * Диалог выбора цвета.
 *
 * @param attributeName имя атрибута
 * @param originalColor исходный цвет (для сравнения)
 * @param currentColor текущий выбранный цвет
 * @param onColorSelected callback при выборе цвета
 * @param onDismiss callback при закрытии
 */
@Composable
fun ColorPickerDialog(
    attributeName: String,
    originalColor: Color,
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var hexInput by remember { mutableStateOf(colorToHex(currentColor)) }
    var selectedColor by remember { mutableStateOf(currentColor) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.color_picker_title),
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = attributeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Сравнение исходного и выбранного цвета
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.color_picker_original),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(originalColor)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = colorToHex(originalColor),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.color_picker_selected),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(selectedColor)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = colorToHex(selectedColor),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Предустановленные цвета
                Text(
                    text = stringResource(R.string.color_picker_palette),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                ColorPalette(
                    selectedColor = selectedColor,
                    onColorSelected = { color ->
                        selectedColor = color
                        hexInput = colorToHex(color)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // HEX ввод
                Text(
                    text = stringResource(R.string.color_picker_hex_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                TextField(
                    value = hexInput,
                    onValueChange = { input ->
                        hexInput = input.uppercase()
                        // Пытаемся распарсить цвет
                        parseHexColor(input)?.let { color ->
                            selectedColor = color
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.color_picker_hex_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Characters
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.color_picker_cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(onClick = { onColorSelected(selectedColor) }) {
                        Text(stringResource(R.string.color_picker_select))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPalette(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    // Материальная палитра цветов
    val colors = listOf(
        // Reds
        Color(0xFFE53935), Color(0xFFEF5350), Color(0xFFFFCDD2),
        // Pinks
        Color(0xFFEC407A), Color(0xFFF06292), Color(0xFFF8BBD9),
        // Purples
        Color(0xFF9C27B0), Color(0xFFAB47BC), Color(0xFFE1BEE7),
        // Indigos
        Color(0xFF3F51B5), Color(0xFF5C6BC0), Color(0xFFC5CAE9),
        // Blues
        Color(0xFF1976D2), Color(0xFF42A5F5), Color(0xFFBBDEFB),
        // Teals
        Color(0xFF009688), Color(0xFF26A69A), Color(0xFFB2DFDB),
        // Greens
        Color(0xFF388E3C), Color(0xFF66BB6A), Color(0xFFC8E6C9),
        // Yellows
        Color(0xFFFBC02D), Color(0xFFFFEB3B), Color(0xFFFFF9C4),
        // Oranges
        Color(0xFFF57C00), Color(0xFFFFA726), Color(0xFFFFE0B2),
        // Browns
        Color(0xFF6D4C41), Color(0xFF8D6E63), Color(0xFFD7CCC8),
        // Grays
        Color(0xFF212121), Color(0xFF757575), Color(0xFFE0E0E0),
        Color.White, Color.Black,
    )

    Column {
        colors.chunked(7).forEach { rowColors ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowColors.forEach { color ->
                    ColorItem(
                        color = color,
                        isSelected = selectedColor == color,
                        onClick = { onColorSelected(color) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorItem(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
                }
            )
            .clickable(onClick = onClick)
    )
}

/**
 * Конвертировать Color в HEX строку.
 */
private fun colorToHex(color: Color): String {
    val argb = color.value.toInt()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return "#${r.toString(16).uppercase().padStart(2, '0')}${g.toString(16).uppercase().padStart(2, '0')}${b.toString(16).uppercase().padStart(2, '0')}"
}

/**
 * Распарсить HEX строку в Color.
 */
private fun parseHexColor(hex: String): Color? {
    val cleanHex = hex.removePrefix("#")
    return try {
        when (cleanHex.length) {
            6 -> {
                val r = cleanHex.substring(0, 2).toInt(16)
                val g = cleanHex.substring(2, 4).toInt(16)
                val b = cleanHex.substring(4, 6).toInt(16)
                Color(r, g, b)
            }
            8 -> {
                val a = cleanHex.substring(0, 2).toInt(16)
                val r = cleanHex.substring(2, 4).toInt(16)
                val g = cleanHex.substring(4, 6).toInt(16)
                val b = cleanHex.substring(6, 8).toInt(16)
                Color(r, g, b, a)
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}