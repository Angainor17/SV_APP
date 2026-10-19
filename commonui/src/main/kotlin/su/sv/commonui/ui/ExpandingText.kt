package su.sv.commonui.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import su.sv.commonui.R
import su.sv.commonui.ui.linkify.LinkifyContent
import su.sv.commonui.ui.linkify.LinkifyText

/**
 * Раскрывающийся текст с кнопкой "Показать ещё"/"Скрыть"
 *
 * Кнопка показывается ТОЛЬКО если текст не умещается в minimizedMaxLines.
 * Если текст умещается полностью - кнопка не показывается.
 *
 * @param text текст для отображения
 * @param minimizedMaxLines максимальное количество строк в свёрнутом состоянии
 * @param fontSize размер шрифта
 * @param modifier модификатор
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun ExpandingText(
    text: String,
    minimizedMaxLines: Int,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
) {
    val context = LocalContext.current

    var isExpanded by remember { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }
    val textLayoutResultState = remember { mutableStateOf<TextLayoutResult?>(null) }

    val showMore = stringResource(R.string.common_expand_text_show_more)
    val showLess = stringResource(R.string.common_expand_text_show_less)

    Column(modifier = modifier.animateContentSize()) {
        LinkifyText(
            content = LinkifyContent(text),
            style = TextStyle.Default.copy(
                fontSize = fontSize,
                color = MaterialTheme.colorScheme.onTertiary,
            ),
            maxLines = if (isExpanded) Int.MAX_VALUE else minimizedMaxLines,
            onTextLayout = { layoutResult ->
                textLayoutResultState.value = layoutResult
                // Проверяем, обрезан ли текст (только в свёрнутом состоянии)
                if (!isExpanded) {
                    hasOverflow = layoutResult.hasVisualOverflow
                }
            },
            modifier = Modifier,
            onUrlClicked = { link ->
                openUrl(context, link)
            },
        )

        // Показываем кнопку только если:
        // 1. Текст развёрнут (показываем "Скрыть")
        // 2. Текст свёрнут И есть переполнение (показываем "Показать ещё")
        if (isExpanded || hasOverflow) {
            val labelText = if (isExpanded) showLess else showMore

            Text(
                text = labelText,
                fontSize = fontSize,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { isExpanded = !isExpanded }
                    .padding(top = 4.dp),
            )
        }
    }
}

private fun openUrl(context: Context, link: String) {
    val intent = Intent(Intent.ACTION_VIEW, link.toUri())

    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}