package su.sv.managers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.net.toUri
import su.sv.commonui.managers.ResourcesRepository
import javax.inject.Inject

private const val MISSPELL_TG_LINK = "https://t.me/SVremya/9244"
private const val QUESTION_TG_LINK = "https://t.me/SVremya"

class OnBookPagerManager @Inject constructor(
    private val resourcesRepository: ResourcesRepository,
) {

    fun tellAboutMisspell(
        context: Context,

        text: String,
        title: String,
        author: String,
        page: Int,
    ) {
        val text = createMisspellText(
            text = text,
            title = title,
            author = author,
            page = page,
        )
        copyTextToClipboard(
            context = context,
            titleRes = R.string.misspell_clip_title,
            text = text,
        )
        showToast(context, R.string.misspell_copy_to_clipboard)
        openUrl(context, MISSPELL_TG_LINK)
    }

    fun askQuestion(
        context: Context,

        text: String,
        title: String,
        author: String,
        page: Int,
    ) {
        val text = createQuestionText(
            text = text,
            title = title,
            author = author,
            page = page,
        )
        copyTextToClipboard(
            context = context,
            titleRes = R.string.misspell_clip_title,
            text = text,
        )
        showToast(context, R.string.question_copy_to_clipboard)
        openUrl(context, QUESTION_TG_LINK)
    }

    private fun createMisspellText(
        text: String,
        title: String,
        author: String,
        page: Int,
    ): String {
        return resourcesRepository.getString(
            R.string.question_text_template,
            text,
            title,
            page.toString(),
            author,
        )
    }

    private fun createQuestionText(
        text: String,
        title: String,
        author: String,
        page: Int,
    ): String {
        return resourcesRepository.getString(
            R.string.question_text_template,
            text,
            title,
            page.toString(),
            author,
        )
    }

    private fun copyTextToClipboard(
        context: Context,
        @StringRes titleRes: Int,
        text: String,
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(resourcesRepository.getString(titleRes), text)

        clipboard.setPrimaryClip(clip)
    }

    private fun showToast(
        context: Context,
        @StringRes textRes: Int,
    ) {
        Toast.makeText(context, resourcesRepository.getString(textRes), Toast.LENGTH_SHORT).show()
    }

    private fun openUrl(context: Context, link: String) {
        val intent = Intent(Intent.ACTION_VIEW, link.toUri())

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }
}
