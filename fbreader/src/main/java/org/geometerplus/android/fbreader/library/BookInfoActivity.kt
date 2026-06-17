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

package org.geometerplus.android.fbreader.library

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.DisplayMetrics
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.geometerplus.R
import org.geometerplus.android.fbreader.FBReader
import org.geometerplus.android.fbreader.api.FBReaderIntents
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow
import org.geometerplus.android.fbreader.preferences.EditBookInfoActivity
import org.geometerplus.android.fbreader.util.AndroidImageSynchronizer
import org.geometerplus.android.util.OrientationUtil
import org.geometerplus.fbreader.Paths
import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.BookEvent
import org.geometerplus.fbreader.book.BookUtil
import org.geometerplus.fbreader.book.CoverUtil
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.fbreader.formats.PluginCollection
import org.geometerplus.fbreader.network.HtmlUtil
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.zlibrary.core.image.ZLImage
import org.geometerplus.zlibrary.core.image.ZLImageProxy
import org.geometerplus.zlibrary.core.language.Language
import org.geometerplus.zlibrary.core.language.ZLLanguageUtil
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageManager
import java.text.DateFormat
import java.util.Date

class BookInfoActivity : Activity(), IBookCollection.Listener<Book> {

    private val myResource = ZLResource.resource("bookInfo")
    private val myImageSynchronizer = AndroidImageSynchronizer(this)
    private val myCollection = BookCollectionShadow()
    private var myBook: Book? = null
    private var myDontReloadBook = false

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        Thread.setDefaultUncaughtExceptionHandler(
            org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler(this)
        )

        val intent = intent
        myDontReloadBook = intent.getBooleanExtra(FROM_READING_MODE_KEY, false)
        myBook = FBReaderIntents.getBookExtra(intent, myCollection)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.book_info)
    }

    override fun onStart() {
        super.onStart()

        OrientationUtil.setOrientation(this, intent)

        val pluginCollection = PluginCollection.Instance(Paths.systemInfo(this))

        myBook?.let { book ->
            // we force language & encoding detection
            BookUtil.getEncoding(book, pluginCollection)

            setupCover(book, pluginCollection)
            setupBookInfo(book)
            setupAnnotation(book, pluginCollection)
            setupFileInfo(book)
        }

        setupButton(R.id.book_info_button_open, "openBook") {
            if (myDontReloadBook) {
                finish()
            } else {
                myBook?.let { FBReader.openBookActivity(this@BookInfoActivity, it, null) }
            }
        }
        setupButton(R.id.book_info_button_edit, "edit") {
            myBook?.let { book ->
                val editIntent = Intent(applicationContext, EditBookInfoActivity::class.java)
                FBReaderIntents.putBookExtra(editIntent, book)
                OrientationUtil.startActivity(this@BookInfoActivity, editIntent)
            }
        }
        setupButton(R.id.book_info_button_reload, "reloadInfo") {
            myBook?.let { book ->
                BookUtil.reloadInfoFromFile(book, pluginCollection)
                setupBookInfo(book)
                myDontReloadBook = false
                myCollection.bindToService(this@BookInfoActivity) {
                    myCollection.saveBook(book)
                }
            }
        }

        val root = findViewById<View>(R.id.book_info_root)
        root.invalidate()
        root.requestLayout()

        myCollection.bindToService(this, null)
        myCollection.addListener(this)
    }

    override fun onNewIntent(intent: Intent) {
        OrientationUtil.setOrientation(this, intent)
    }

    override fun onDestroy() {
        myCollection.removeListener(this)
        myCollection.unbind()
        myImageSynchronizer.clear()

        super.onDestroy()
    }

    private fun findButton(buttonId: Int): Button = findViewById(buttonId)

    private fun setupButton(buttonId: Int, resourceKey: String, listener: View.OnClickListener) {
        val buttonResource = ZLResource.resource("dialog").getResource("button")
        val button = findButton(buttonId)
        button.text = buttonResource.getResource(resourceKey).value
        button.setOnClickListener(listener)
    }

    private fun setupInfoPair(id: Int, key: String, value: CharSequence?) {
        setupInfoPair(id, key, value, 0)
    }

    private fun setupInfoPair(id: Int, key: String, value: CharSequence?, param: Int) {
        val layout = findViewById<LinearLayout>(id)
        if (value.isNullOrEmpty()) {
            layout.visibility = View.GONE
            return
        }
        layout.visibility = View.VISIBLE
        (layout.findViewById<View>(R.id.book_info_key) as TextView).text = myResource.getResource(key).getValue(param)
        (layout.findViewById<View>(R.id.book_info_value) as TextView).text = value
    }

    private fun setupCover(book: Book, pluginCollection: PluginCollection) {
        val coverView = findViewById<ImageView>(R.id.book_cover)
        val oldBook = coverView.tag
        if (oldBook is Book && book.id == oldBook.id) {
            return
        }
        coverView.tag = book

        coverView.visibility = View.GONE
        coverView.setImageDrawable(null)

        val image = CoverUtil.getCover(book, pluginCollection) ?: return

        if (image is ZLImageProxy) {
            image.startSynchronization(myImageSynchronizer) {
                runOnUiThread {
                    setCover(coverView, image)
                }
            }
        } else {
            setCover(coverView, image)
        }
    }

    private fun setCover(coverView: ImageView, image: ZLImage) {
        val data = (ZLAndroidImageManager.Instance() as ZLAndroidImageManager).getImageData(image) ?: return

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        val maxHeight = metrics.heightPixels * 2 / 3
        val maxWidth = maxHeight * 2 / 3

        val coverBitmap = data.getBitmap(2 * maxWidth, 2 * maxHeight) ?: return

        coverView.visibility = View.VISIBLE
        coverView.layoutParams.width = maxWidth
        coverView.layoutParams.height = maxHeight
        coverView.setImageBitmap(coverBitmap)
    }

    private fun setupBookInfo(book: Book) {
        (findViewById<View>(R.id.book_info_title) as TextView).text = myResource.getResource("bookInfo").value

        setupInfoPair(R.id.book_title, "title", book.title)
        setupInfoPair(R.id.book_authors, "authors", book.authorsString(", "), book.authors?.size ?: 0)

        val series = book.seriesInfo
        setupInfoPair(R.id.book_series, "series", series?.series?.title)
        val seriesIndexString = series?.index?.toPlainString()
        setupInfoPair(R.id.book_series_index, "indexInSeries", seriesIndexString)
        setupInfoPair(R.id.book_tags, "tags", book.tagsString(", "), book.tags?.size ?: 0)
        var language = book.language
        if (language !in ZLLanguageUtil.languageCodes()) {
            language = Language.OTHER_CODE
        }
        setupInfoPair(R.id.book_language, "language", Language(language).Name)
    }

    private fun setupAnnotation(book: Book, pluginCollection: PluginCollection) {
        val titleView = findViewById<TextView>(R.id.book_info_annotation_title)
        val bodyView = findViewById<TextView>(R.id.book_info_annotation_body)
        val annotation = BookUtil.getAnnotation(book, pluginCollection)
        if (annotation == null) {
            titleView.visibility = View.GONE
            bodyView.visibility = View.GONE
        } else {
            titleView.text = myResource.getResource("annotation").value
            bodyView.text = HtmlUtil.getHtmlText(NetworkLibrary.Instance(Paths.systemInfo(this)), annotation)
            bodyView.movementMethod = LinkMovementMethod()
            bodyView.setTextColor(ColorStateList.valueOf(bodyView.textColors.defaultColor))
        }
    }

    private fun setupFileInfo(book: Book) {
        (findViewById<View>(R.id.file_info_title) as TextView).text = myResource.getResource("fileInfo").value

        setupInfoPair(R.id.file_name, "name", book.getPath())
        if (ENABLE_EXTENDED_FILE_INFO) {
            val bookFile = BookUtil.fileByBook(book)
            setupInfoPair(R.id.file_type, "type", bookFile.extension)

            val physFile = bookFile.physicalFile
            val file = physFile?.javaFile()
            if (file != null && file.exists() && file.isFile) {
                setupInfoPair(R.id.file_size, "size", formatSize(file.length()))
                setupInfoPair(R.id.file_time, "time", formatDate(file.lastModified()))
            } else {
                setupInfoPair(R.id.file_size, "size", null)
                setupInfoPair(R.id.file_time, "time", null)
            }
        } else {
            setupInfoPair(R.id.file_type, "type", null)
            setupInfoPair(R.id.file_size, "size", null)
            setupInfoPair(R.id.file_time, "time", null)
        }
    }

    private fun formatSize(size: Long): String? {
        if (size <= 0) {
            return null
        }
        val kilo = 1024
        if (size < kilo) { // less than 1 kilobyte
            return myResource.getResource("sizeInBytes").getValue(size.toInt()).replace("%s", size.toString())
        }
        val value = if (size < kilo * kilo) { // less than 1 megabyte
            String.format("%.2f", size.toFloat() / kilo)
        } else {
            (size / kilo).toString()
        }
        return myResource.getResource("sizeInKiloBytes").value.replace("%s", value)
    }

    private fun formatDate(date: Long): String? {
        if (date == 0L) {
            return null
        }
        return DateFormat.getDateTimeInstance().format(Date(date))
    }

    override fun onBookEvent(event: BookEvent, book: Book?) {
        if (event == BookEvent.Updated && book != null && myBook != null && myCollection.sameBook(book, myBook!!)) {
            myBook?.updateFrom(book)
            setupBookInfo(book)
            myDontReloadBook = false
        }
    }

    override fun onBuildEvent(status: IBookCollection.Status) {
    }

    companion object {
        const val FROM_READING_MODE_KEY = "fbreader.from.reading.mode"
        private const val ENABLE_EXTENDED_FILE_INFO = false
    }
}
