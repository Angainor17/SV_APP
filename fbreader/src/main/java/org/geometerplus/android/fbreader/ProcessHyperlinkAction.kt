/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.view.View
import com.github.johnpersano.supertoasts.SuperActivityToast
import com.github.johnpersano.supertoasts.SuperToast
import com.github.johnpersano.supertoasts.util.OnClickWrapper
import com.github.johnpersano.supertoasts.util.OnDismissWrapper
import org.geometerplus.android.fbreader.dict.DictionaryUtil
import org.geometerplus.android.fbreader.image.ImageViewActivity
import org.geometerplus.android.fbreader.network.BookDownloader
import org.geometerplus.android.fbreader.network.BookDownloaderService
import org.geometerplus.android.fbreader.network.Util
import org.geometerplus.android.fbreader.network.auth.ActivityNetworkContext
import org.geometerplus.android.util.OrientationUtil
import org.geometerplus.android.util.UIMessageUtil
import org.geometerplus.fbreader.Paths
import org.geometerplus.fbreader.bookmodel.FBHyperlinkType
import org.geometerplus.fbreader.fbreader.FBReaderApp
import org.geometerplus.fbreader.fbreader.options.MiscOptions
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.util.AutoTextSnippet
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.text.view.ZLTextHyperlinkRegionSoul
import org.geometerplus.zlibrary.text.view.ZLTextImageRegionSoul
import org.geometerplus.zlibrary.text.view.ZLTextWordRegionSoul

internal class ProcessHyperlinkAction(baseActivity: FBReader, fbreader: FBReaderApp) : FBAndroidAction(baseActivity, fbreader) {

    override fun isEnabled(): Boolean = Reader.getTextView().getOutlinedRegion() != null

    override fun run(vararg params: Any?) {
        val region = Reader.getTextView().getOutlinedRegion() ?: return

        val soul = region.soul
        if (soul is ZLTextHyperlinkRegionSoul) {
            Reader.getTextView().hideOutline()
            Reader.viewWidget.repaint()
            val hyperlink = soul.hyperlink
            when (hyperlink.type) {
                FBHyperlinkType.EXTERNAL -> openInBrowser(hyperlink.id ?: return)
                FBHyperlinkType.INTERNAL, FBHyperlinkType.FOOTNOTE -> {
                    val snippet: AutoTextSnippet? = Reader.getFootnoteData(hyperlink.id ?: return)
                    if (snippet == null) {
                        return
                    }

                    Reader.collection.markHyperlinkAsVisited(Reader.currentBook ?: return, hyperlink.id ?: return)
                    val showToast = when (Reader.miscOptions.showFootnoteToast.value) {
                        MiscOptions.FootnoteToastEnum.never -> false
                        MiscOptions.FootnoteToastEnum.footnotesOnly -> hyperlink.type == FBHyperlinkType.FOOTNOTE
                        MiscOptions.FootnoteToastEnum.footnotesAndSuperscripts ->
                            hyperlink.type == FBHyperlinkType.FOOTNOTE || region.isVerticallyAligned()
                        MiscOptions.FootnoteToastEnum.allInternalLinks -> true
                        null -> false
                    }
                    if (showToast) {
                        val toast: SuperActivityToast = if (snippet.isEndOfText) {
                            SuperActivityToast(BaseActivity, SuperToast.Type.STANDARD)
                        } else {
                            SuperActivityToast(BaseActivity, SuperToast.Type.BUTTON).apply {
                                setButtonIcon(
                                    android.R.drawable.ic_menu_more,
                                    ZLResource.resource("toast").getResource("more").value ?: ""
                                )
                                setOnClickWrapper(OnClickWrapper("ftnt", object : SuperToast.OnClickListener {
                                    override fun onClick(view: View, token: Parcelable?) {
                                        Reader.getTextView().hideOutline()
                                        Reader.tryOpenFootnote(hyperlink.id ?: return@onClick)
                                    }
                                }))
                            }
                        }
                        toast.setText(snippet.getText())
                        toast.setDuration(Reader.miscOptions.footnoteToastDuration.value?.Value ?: 5)
                        toast.setOnDismissWrapper(OnDismissWrapper("ftnt", object : SuperToast.OnDismissListener {
                            override fun onDismiss(view: View) {
                                Reader.getTextView().hideOutline()
                                Reader.viewWidget.repaint()
                            }
                        }))
                        Reader.getTextView().outlineRegion(region)
                        BaseActivity.showToast(toast)
                    } else {
                        Reader.tryOpenFootnote(hyperlink.id ?: return)
                    }
                }
            }
        } else if (soul is ZLTextImageRegionSoul) {
            Reader.getTextView().hideOutline()
            Reader.viewWidget.repaint()
            val url = soul.ImageElement.URL
            if (url != null) {
                try {
                    val intent = Intent()
                    intent.setClass(BaseActivity, ImageViewActivity::class.java)
                    intent.putExtra(ImageViewActivity.URL_KEY, url)
                    intent.putExtra(
                        ImageViewActivity.BACKGROUND_COLOR_KEY,
                        Reader.imageOptions.imageViewBackground.value?.intValue() ?: 0xFFFFFF
                    )
                    OrientationUtil.startActivity(BaseActivity, intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else if (soul is ZLTextWordRegionSoul) {
            DictionaryUtil.openTextInDictionary(
                BaseActivity,
                soul.Word.getString(),
                true,
                region.getTop(),
                region.getBottom()
            ) {
                BaseActivity.outlineRegion(soul)
            }
        }
    }

    private fun openInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        val externalUrl: Boolean
        if (BookDownloader.acceptsUri(Uri.parse(url), null)) {
            intent.setClass(BaseActivity, BookDownloader::class.java)
            intent.putExtra(BookDownloaderService.Key.SHOW_NOTIFICATIONS, BookDownloaderService.Notifications.ALL)
            externalUrl = false
        } else {
            externalUrl = true
        }
        val nLibrary = NetworkLibrary.Instance(Paths.systemInfo(BaseActivity))
        Thread {
            if (!url.startsWith("fbreader-action:")) {
                try {
                    nLibrary.initialize(ActivityNetworkContext(BaseActivity))
                } catch (e: ZLNetworkException) {
                    e.printStackTrace()
                    UIMessageUtil.showMessageText(BaseActivity, e.message ?: "")
                    return@Thread
                }
            }
            intent.data = Util.rewriteUri(Uri.parse(nLibrary.rewriteUrl(url, externalUrl) ?: url))
            BaseActivity.runOnUiThread {
                try {
                    OrientationUtil.startActivity(BaseActivity, intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }
}
