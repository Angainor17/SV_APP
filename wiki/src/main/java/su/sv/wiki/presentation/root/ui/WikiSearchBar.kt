package su.sv.wiki.presentation.root.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.theme.SVAPPThemeLightPreview
import su.sv.wiki.R
import kotlin.time.Duration.Companion.milliseconds

/**
 * Поле поиска статей с debounce и анимированным hint
 *
 * @param onSearch callback при поиске (вызывается через debounce только если suggestions не отображаются)
 * @param onQueryChanged callback при изменении текста (для подсказок)
 * @param onClearClick callback при нажатии на крестик (для скрытия клавиатуры)
 * @param hints список текстов для анимированного hint (показываются по очереди)
 * @param isSuggestionsVisible true если подсказки отображаются под полем поиска
 * @param selectedSuggestion текст выбранной подсказки для помещения в поле ввода
 * @param onSuggestionApplied callback после помещения suggestion в поле (для сброса)
 * @param minQueryLength минимальная длина запроса для поиска (по умолчанию 3)
 * @param debounceMillis задержка в миллисекундах (по умолчанию 1500)
 * @param typingSpeedMs скорость печати одного символа (по умолчанию 100ms)
 * @param pauseBetweenHintsMs пауза между сменой hints (по умолчанию 3000ms)
 */
@Composable
fun WikiSearchBar(
    onSearch: (String) -> Unit,
    onQueryChanged: (String) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
    hints: List<String> = emptyList(),
    isSuggestionsVisible: Boolean = false,
    selectedSuggestion: String? = null,
    onSuggestionApplied: () -> Unit = {},
    minQueryLength: Int = 3,
    debounceMillis: Long = 1500L,
    typingSpeedMs: Long = 110L,
    pauseBetweenHintsMs: Long = 3000L,
) {
    var searchText by remember { mutableStateOf("") }
    val dimensions = LocalAppDimensions.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // При выборе suggestion - помещаем текст в поле ввода
    LaunchedEffect(selectedSuggestion) {
        if (selectedSuggestion != null && selectedSuggestion != searchText) {
            searchText = selectedSuggestion
            onQueryChanged(selectedSuggestion)
            onSuggestionApplied()
        }
    }

    // Debounce поиск - только если suggestions НЕ отображаются
    LaunchedEffect(searchText, isSuggestionsVisible) {
        if (searchText.length >= minQueryLength && !isSuggestionsVisible) {
            delay(debounceMillis.milliseconds)
            onSearch(searchText)
        }
    }

    // Показываем анимированный hint только если:
    // 1. Поле пустое
    // 2. Нет фокуса
    // 3. Есть hints для показа
    val showAnimatedHint = searchText.isEmpty() && !isFocused && hints.isNotEmpty()

    OutlinedTextField(
        value = searchText,
        onValueChange = { newText ->
            searchText = newText
            onQueryChanged(newText)
        },
        interactionSource = interactionSource,
        placeholder = {
            if (showAnimatedHint) {
                AnimatedTypingHint(
                    hints = hints,
                    typingSpeedMs = typingSpeedMs,
                    pauseBetweenHintsMs = pauseBetweenHintsMs,
                )
            } else if (searchText.isEmpty()) {
                // Показываем "Поиск" когда поле в фокусе
                Text(
                    text = stringResource(R.string.wiki_search_label),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        singleLine = true,
        trailingIcon = {
            if (searchText.isNotEmpty()) {
                IconButton(onClick = {
                    searchText = ""
                    onQueryChanged("")
                    onClearClick()
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.wiki_search_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = dimensions.screenPaddingHorizontal,
                end = dimensions.itemSpacingMedium,
                top = 0.dp,
                bottom = dimensions.itemSpacingLarge
            ),
    )
}

// ============================================================
// Preview
// ============================================================

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun WikiSearchBarPreview() {
    SVAPPThemeLightPreview {
        WikiSearchBar(
            onSearch = {},
            onQueryChanged = {},
            onClearClick = {},
            hints = listOf(
                "Диктатура пролетариата",
                "Октябрьская революция",
                "Ленин",
            ),
            isSuggestionsVisible = false,
        )
    }
}

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun WikiSearchBarWithSuggestionsPreview() {
    SVAPPThemeLightPreview {
        WikiSearchBar(
            onSearch = {},
            onQueryChanged = {},
            onClearClick = {},
            isSuggestionsVisible = true,
        )
    }
}
