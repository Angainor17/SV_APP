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

import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import org.geometerplus.R
import org.geometerplus.android.util.UIMessageUtil
import org.geometerplus.android.util.UIUtil
import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.fbreader.BookElement
import org.geometerplus.fbreader.fbreader.FBReaderApp
import org.geometerplus.fbreader.fbreader.options.ColorProfile
import org.geometerplus.fbreader.network.urlInfo.BookUrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.zlibrary.core.network.QuietNetworkContext
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.text.view.ExtensionRegionSoul
import org.geometerplus.zlibrary.text.view.ZLTextRegion
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageData
import java.io.File

internal class DisplayBookPopupAction(baseActivity: FBReader, fbreader: FBReaderApp) : FBAndroidAction(baseActivity, fbreader) {

    private fun openBook(popup: PopupWindow, book: Book?) {
        if (book == null) {
            return
        }

        BaseActivity.runOnUiThread {
            popup.dismiss()
            Reader.openBook(book, null, null, null)
        }
    }

    override fun run(vararg params: Any?) {
        if (params.size != 1 || params[0] !is ZLTextRegion) {
            return
        }
        val region = params[0] as ZLTextRegion
        if (region.soul !is ExtensionRegionSoul) {
            return
        }
        val soul = region.soul as ExtensionRegionSoul
        val e = soul.Element
        if (e !is BookElement) {
            return
        }
        val element = e as BookElement
        if (!element.isInitialized()) {
            return
        }

        val mainView = BaseActivity.viewWidget as View
        val bookView = BaseActivity.layoutInflater.inflate(
            if (ColorProfile.NIGHT == Reader.viewOptions.colorProfileName.value)
                R.layout.book_popup_night
            else
                R.layout.book_popup,
            null
        )
        val inch = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_IN, 1f, BaseActivity.resources.displayMetrics
        ).toInt()
        val popup = PopupWindow(
            bookView,
            (4 * inch).coerceAtMost(mainView.width * 9 / 10),
            (3 * inch).coerceAtMost(mainView.height * 9 / 10)
        )
        popup.isFocusable = true
        popup.isOutsideTouchable = true

        val coverView = bookView.findViewById<ImageView>(R.id.book_popup_cover)
        if (coverView != null) {
            val imageData = element.getImageData() as? ZLAndroidImageData
            if (imageData != null) {
                coverView.setImageBitmap(imageData.fullSizeBitmap)
            }
        }

        val item = element.getItem()

        val headerView = bookView.findViewById<TextView>(R.id.book_popup_header_text)
        val text = StringBuilder()
        for (author in item?.authors ?: emptyList()) {
            text.append("<p><i>").append(author.displayName).append("</i></p>")
        }
        text.append("<h3>").append(item?.title).append("</h3>")
        headerView.text = Html.fromHtml(text.toString())

        val descriptionView = bookView.findViewById<TextView>(R.id.book_popup_description_text)
        descriptionView.text = item?.getSummary()
        descriptionView.movementMethod = LinkMovementMethod.getInstance()

        val buttonResource = ZLResource.resource("dialog").getResource("button")
        val buttonsView = bookView.findViewById<View>(R.id.book_popup_buttons)

        val downloadButton = buttonsView.findViewById<Button>(R.id.ok_button)
        downloadButton.text = buttonResource.getResource("download").value
        val infos = item?.getAllInfos(UrlInfo.Type.Book) ?: emptyList()
        if (infos.isEmpty() || infos[0] !is BookUrlInfo) {
            downloadButton.isEnabled = false
        } else {
            val bookInfo = infos[0] as BookUrlInfo
            val fileName = bookInfo.makeBookFileName(UrlInfo.Type.Book)
            if (fileName == null) {
                downloadButton.isEnabled = false
            } else {
                val book = Reader.collection.getBookByFile(fileName)
                if (book != null) {
                    downloadButton.text = buttonResource.getResource("openBook").value
                    downloadButton.setOnClickListener {
                        openBook(popup, book)
                    }
                } else {
                    val file = File(fileName)
                    if (file.exists()) {
                        file.delete()
                    }
                    if (!file.parentFile.exists()) {
                        file.parentFile.mkdirs()
                    }
                    downloadButton.setOnClickListener {
                        UIUtil.wait(
                            "downloadingBook", item?.title.toString(),
                            {
                                try {
                                    QuietNetworkContext().downloadToFile(bookInfo.url, file)
                                    openBook(popup, Reader.collection.getBookByFile(fileName))
                                } catch (e: ZLNetworkException) {
                                    UIMessageUtil.showErrorMessage(BaseActivity, "downloadFailed")
                                    e.printStackTrace()
                                }
                            },
                            BaseActivity
                        )
                    }
                }
            }
        }

        val cancelButton = buttonsView.findViewById<Button>(R.id.cancel_button)
        cancelButton.text = buttonResource.getResource("cancel").value
        cancelButton.setOnClickListener {
            popup.dismiss()
        }

        downloadButton.setTextColor(-0x888889)
        cancelButton.setTextColor(-0x888889)

        popup.setOnDismissListener { }

        popup.showAtLocation(BaseActivity.currentFocus, Gravity.CENTER, 0, 0)
    }
}
