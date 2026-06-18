package su.sv.wiki.presentation.root.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import su.sv.wiki.R

/**
 * Поле поиска статей с debounce
 *
 * @param onSearch callback при поиске (вызывается через 1.5 сек после прекращения ввода)
 * @param minQueryLength минимальная длина запроса для поиска (по умолчанию 3)
 * @param debounceMillis задержка в миллисекундах (по умолчанию 1500)
 */
@Composable
fun WikiSearchBar(
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    minQueryLength: Int = 3,
    debounceMillis: Long = 1500L,
) {
    var searchText by remember { mutableStateOf("") }

    // Debounce поиск
    LaunchedEffect(searchText) {
        if (searchText.length >= minQueryLength) {
            delay(debounceMillis)
            onSearch(searchText)
        }
    }

    OutlinedTextField(
        value = searchText,
        onValueChange = { newText ->
            searchText = newText
        },
        label = {
            Text(stringResource(R.string.wiki_search_label))
        },
        placeholder = {
            Text(stringResource(R.string.wiki_search_placeholder))
        },
        singleLine = true,
        trailingIcon = {
            if (searchText.isNotEmpty()) {
                IconButton(onClick = { searchText = "" }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.wiki_search_clear),
                    )
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    )
}
