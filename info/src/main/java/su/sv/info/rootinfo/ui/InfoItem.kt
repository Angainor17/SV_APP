package su.sv.info.rootinfo.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.theme.SVAPPThemeLightPreview
import su.sv.info.R
import su.sv.info.rootinfo.model.UiLinkItem

/**
 * Элемент списка ссылок
 *
 * @param item данные ссылки
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InfoItem(item: UiLinkItem) {
    val context = LocalContext.current
    val dimensions = LocalAppDimensions.current
    val clipboardManager = LocalClipboardManager.current

    Row(
        modifier = Modifier
            .padding(horizontal = dimensions.screenPaddingHorizontal)
            .padding(vertical = dimensions.itemSpacingSmall)
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { openLink(context, item.url) },
                onLongClick = {
                    clipboardManager.setText(AnnotatedString(item.url))
                    Toast.makeText(context, context.getString(R.string.link_copied), Toast.LENGTH_SHORT).show()
                }
            )
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .height(dimensions.listItemHeight)
            .fillMaxWidth()
            .padding(horizontal = dimensions.itemSpacingLarge),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(item.logo),
            modifier = Modifier
                .size(dimensions.iconSizeLarge),
            alignment = Alignment.Center,
            contentDescription = stringResource(R.string.resource_logo_content_description),
            contentScale = ContentScale.Crop,
        )

        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = dimensions.itemSpacingMedium)
        )
    }
}

private fun openLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback: копируем ссылку если не удалось открыть
        Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
    }
}

// ============================================================
// Previews
// ============================================================

@Preview(showBackground = true)
@Composable
fun InfoItemPreview() {
    SVAPPThemeLightPreview {
        val item = UiLinkItem(
            text = "YouTube канал Академия Смыслов Lobbyo",
            url = "https://www.youtube.com/channel/UCCWUurfuoWhXkFpjNZkLc-A",
            logo = R.drawable.ic_vk,
        )
        InfoItem(item)
    }
}
