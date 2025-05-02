package su.sv.commonui.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.core.net.toUri
import su.sv.commonui.R
import uz.kjuraev.linkify.LinkifyContent
import uz.kjuraev.linkify.LinkifyText

@OptIn(ExperimentalTextApi::class)
@Composable
fun ExpandingText(
    modifier: Modifier = Modifier,
    text: String,
    minimizedMaxLines: Int,
    fontSize: TextUnit = TextUnit.Unspecified,
) {
    val context = LocalContext.current

    var isExpanded by remember { mutableStateOf(false) }
    val textLayoutResultState = remember { mutableStateOf<TextLayoutResult?>(null) }

    val textLayoutResult = textLayoutResultState.value

    val showMore = stringResource(R.string.common_expand_text_show_more)
    val showLess = stringResource(R.string.common_expand_text_show_less)

    Column {
        LinkifyText(
            content = LinkifyContent(text),
            style = TextStyle.Default.copy(
                fontSize = fontSize,
            ),
            maxLines = if (isExpanded) Int.MAX_VALUE else minimizedMaxLines,
            onTextLayout = { textLayoutResultState.value = it },
            modifier = modifier.animateContentSize(),
            onUrlClicked = {
                openUrl(context, it)
            },
        )

        if (isExpanded || textLayoutResult?.hasVisualOverflow == true) {
            val labelText = if (isExpanded) showLess else showMore

            Text(
                text = labelText,
                fontSize = fontSize,
                color = Color.Blue,
                modifier = modifier.clickable {
                    isExpanded = !isExpanded
                },
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
