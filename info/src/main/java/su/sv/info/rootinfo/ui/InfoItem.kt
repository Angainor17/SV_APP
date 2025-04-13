package su.sv.info.rootinfo.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import su.sv.info.R
import su.sv.info.rootinfo.model.UiLinkItem

@Composable
fun InfoItem(item: UiLinkItem) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clickable { openLink(context, item.url) }
            .background(Color.LightGray)
            .height(80.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(item.logo),
            modifier = Modifier
                .padding(
                    start = 8.dp,
                )
                .size(40.dp),
            alignment = Alignment.Center,
            contentDescription = stringResource(R.string.resource_logo_content_description),
            contentScale = ContentScale.Crop,
        )

        Text(
            text = item.text,
            modifier = Modifier.padding(
                start = 6.dp,
                end = 6.dp,
            )
        )
    }
}

private fun openLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())

    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}

@Preview
@Composable
fun InfoItemPreview() {
    val item = UiLinkItem(
        text = "YouTube канал Академия Смыслов Lobbyo",
        url = "https://www.youtube.com/channel/UCCWUurfuoWhXkFpjNZkLc-A",
        logo = R.drawable.ic_vk,
    )
    InfoItem(item)
}
