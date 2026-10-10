/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
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
package org.geometerplus.fbreader.fbreader

import org.fbreader.util.ComparisonUtil.equal
import org.geometerplus.fbreader.book.AbstractBook
import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.BookEvent
import org.geometerplus.fbreader.book.BookUtil.fileByBook
import org.geometerplus.fbreader.book.BookUtil.getHelpFile
import org.geometerplus.fbreader.book.BookUtil.getPlugin
import org.geometerplus.fbreader.book.Bookmark
import org.geometerplus.fbreader.book.Bookmark.ByTimeComparator
import org.geometerplus.fbreader.book.BookmarkQuery
import org.geometerplus.fbreader.book.BookmarkUtil.findEnd
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.fbreader.bookmodel.BookModel
import org.geometerplus.fbreader.bookmodel.BookModel.Companion.createModel
import org.geometerplus.fbreader.bookmodel.TOCTree
import org.geometerplus.fbreader.fbreader.options.CancelMenuHelper
import org.geometerplus.fbreader.fbreader.options.ImageOptions
import org.geometerplus.fbreader.fbreader.options.MiscOptions
import org.geometerplus.fbreader.fbreader.options.PageTurningOptions
import org.geometerplus.fbreader.fbreader.options.ViewOptions
import org.geometerplus.fbreader.formats.BookReadingException
import org.geometerplus.fbreader.formats.ExternalFormatPlugin
import org.geometerplus.fbreader.formats.FormatPlugin
import org.geometerplus.fbreader.formats.PluginCollection.Companion.Instance
import org.geometerplus.fbreader.util.AutoTextSnippet
import org.geometerplus.fbreader.util.EmptyTextSnippet
import org.geometerplus.zlibrary.core.application.ZLApplication
import org.geometerplus.zlibrary.core.application.ZLKeyBindings
import org.geometerplus.zlibrary.core.drm.EncryptionMethod.Companion.isSupported
import org.geometerplus.zlibrary.core.view.ZLViewEnums
import org.geometerplus.zlibrary.core.util.RationalNumber
import org.geometerplus.zlibrary.core.util.SystemInfo
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator
import org.geometerplus.zlibrary.text.model.ZLTextModel
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextParagraphCursor
import org.geometerplus.zlibrary.text.view.ZLTextPosition
import org.geometerplus.zlibrary.text.view.ZLTextView
import org.geometerplus.zlibrary.text.view.ZLTextWordCursor
import java.util.Collections
import java.util.Date
import java.util.LinkedList
import kotlin.concurrent.Volatile

open class FBReaderApp(systemInfo: SystemInfo?, val Collection: IBookCollection<Book>) :
    ZLApplication(systemInfo) {
    val MiscOptions: MiscOptions = MiscOptions()
    val ImageOptions: ImageOptions = ImageOptions()
    val ViewOptions: ViewOptions = ViewOptions()
    val PageTurningOptions: PageTurningOptions = PageTurningOptions()
    private val myBindings = ZLKeyBindings()
    private val mySaverThread = SaverThread()
    var BookTextView: FBView
    var FootnoteView: FBView

    @Volatile
    var Model: BookModel? = null

    @Volatile
    var ExternalBook: Book? = null
    private var myExternalFileOpener: ExternalFileOpener? = null
    private var myFootnoteModelId: String? = null
    private var myJumpEndPosition: ZLTextPosition? = null
    private var myJumpTimeStamp: Date? = null

    @Volatile
    private var myStoredPosition: ZLTextPosition? = null

    @Volatile
    private var myStoredPositionBook: Book? = null

    init {
        Collection.addListener(object : IBookCollection.Listener<Book> {
            override fun onBookEvent(event: BookEvent, book: Book) {
                when (event) {
                    BookEvent.BookmarkStyleChanged, BookEvent.BookmarksUpdated -> if (Model != null && Collection.sameBook(
                            book,
                            Model!!.Book
                        )
                    ) {
                        if (BookTextView!!.model != null) {
                            setBookmarkHighlightings(BookTextView!!, null)
                        }
                        if (FootnoteView.model != null && myFootnoteModelId != null) {
                            setBookmarkHighlightings(FootnoteView, myFootnoteModelId)
                        }
                    }

                    BookEvent.Updated -> onBookUpdated(book)

                    else -> {}
                }
            }

            override fun onBuildEvent(status: IBookCollection.Status) {
            }
        })

        addAction(ActionCode.INCREASE_FONT, ChangeFontSizeAction(this, +2))
        addAction(ActionCode.DECREASE_FONT, ChangeFontSizeAction(this, -2))

        addAction(ActionCode.FIND_NEXT, FindNextAction(this))
        addAction(ActionCode.FIND_PREVIOUS, FindPreviousAction(this))
        addAction(ActionCode.CLEAR_FIND_RESULTS, ClearFindResultsAction(this))

        addAction(ActionCode.SELECTION_CLEAR, SelectionClearAction(this))

        addAction(ActionCode.TEL_ABOUT_MISSPELL, SelectionClearAction(this))
        addAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD, SelectionClearAction(this))

        addAction(ActionCode.MOVE_CURSOR_UP, MoveCursorAction(this, ZLViewEnums.Direction.up))
        addAction(ActionCode.MOVE_CURSOR_DOWN, MoveCursorAction(this, ZLViewEnums.Direction.down))
        addAction(ActionCode.MOVE_CURSOR_LEFT, MoveCursorAction(this, ZLViewEnums.Direction.rightToLeft))
        addAction(
            ActionCode.MOVE_CURSOR_RIGHT,
            MoveCursorAction(this, ZLViewEnums.Direction.leftToRight)
        )

        addAction(ActionCode.VOLUME_KEY_SCROLL_FORWARD, VolumeKeyTurnPageAction(this, true))
        addAction(ActionCode.VOLUME_KEY_SCROLL_BACK, VolumeKeyTurnPageAction(this, false))

        addAction(ActionCode.EXIT, ExitAction(this))

        BookTextView = FBView(this)
        FootnoteView = FBView(this)

        setView(BookTextView)
    }

    fun setExternalFileOpener(o: ExternalFileOpener) {
        myExternalFileOpener = o
    }

    val currentBook: Book?
        get() {
            val m = Model
            return if (m != null) m.Book else ExternalBook
        }

    fun openHelpBook() {
        openBook(Collection.getBookByFile(getHelpFile().getPath()), null, null, null)
    }

    fun getCurrentServerBook(notifier: Notifier?): MissingBookInfo? {
        return null
    }

    fun openBook(book: Book?, bookmark: Bookmark?, postAction: Runnable?, notifier: Notifier?) {
        var book = book
        if (Model != null) {
            if (book == null || bookmark == null && Collection.sameBook(book, Model!!.Book)) {
                return
            }
        }

        if (book == null) {
            book = if (getCurrentServerBook(notifier) != null) Collection.getRecentBook(0) else null
            if (book == null) {
                book = Collection.getRecentBook(0)
            }
            if (book == null || !fileByBook(book).exists()) {
                book = Collection.getBookByFile(getHelpFile().getPath())
            }
            if (book == null) {
                return
            }
        }
        val bookToOpen: Book? = book
        bookToOpen!!.addNewLabel(AbstractBook.READ_LABEL)
        Collection.saveBook(bookToOpen)

        val executor = createExecutor("loadingBook")
        executor.execute(object : Runnable {
            override fun run() {
                openBookInternal(bookToOpen, bookmark, false)
            }
        }, postAction)
    }

    private fun reloadBook() {
        val book = this.currentBook
        if (book != null) {
            val executor = createExecutor("loadingBook")
            executor.execute(object : Runnable {
                override fun run() {
                    openBookInternal(book, null, true)
                }
            }, null)
        }
    }

    override fun keyBindings(): ZLKeyBindings {
        return myBindings
    }

    val textView: FBView
        get() = getCurrentView() as FBView

    fun getFootnoteData(id: String): AutoTextSnippet? {
        if (Model == null) {
            return null
        }
        val label = Model!!.getLabel(id)
        if (label == null) {
            return null
        }
        val model: ZLTextModel?
        if (label.ModelId != null) {
            model = Model!!.getFootnoteModel(label.ModelId)
        } else {
            model = Model!!.textModel
        }
        if (model == null) {
            return null
        }
        val cursor =
            ZLTextWordCursor(ZLTextParagraphCursor(model, label.ParagraphIndex))
        val longSnippet = AutoTextSnippet(cursor, 140)
        if (longSnippet.IsEndOfText) {
            return longSnippet
        } else {
            return AutoTextSnippet(cursor, 100)
        }
    }

    fun tryOpenFootnote(id: String) {
        if (Model != null) {
            myJumpEndPosition = null
            myJumpTimeStamp = null
            val label = Model!!.getLabel(id)
            if (label != null) {
                if (label.ModelId == null) {
                    if (this.textView === BookTextView) {
                        addInvisibleBookmark()
                        myJumpEndPosition = ZLTextFixedPosition(label.ParagraphIndex, 0, 0)
                        myJumpTimeStamp = Date()
                    }
                    BookTextView!!.gotoPosition(label.ParagraphIndex, 0, 0)
                    setView(BookTextView)
                } else {
                    setFootnoteModel(label.ModelId)
                    setView(FootnoteView)
                    FootnoteView.gotoPosition(label.ParagraphIndex, 0, 0)
                }
                getViewWidget()!!.repaint()
                storePosition()
            }
        }
    }

    fun clearTextCaches() {
        BookTextView!!.clearCaches()
        FootnoteView.clearCaches()
    }

    fun addSelectionBookmark(): Bookmark? {
        val fbView = this.textView
        val snippet = fbView.selectedSnippet
        if (snippet == null) {
            return null
        }

        val bookmark = Bookmark(
            Collection,
            Model!!.Book,
            fbView.model!!.getId(),
            snippet,
            true
        )
        Collection.saveBookmark(bookmark)
        fbView.clearSelection()

        return bookmark
    }

    private fun setBookmarkHighlightings(view: ZLTextView, modelId: String?) {
        view.removeHighlightings(BookmarkHighlighting::class.java)
        var query = BookmarkQuery(Model!!.Book, 20)
        while (true) {
            val bookmarks: List<Bookmark> = Collection.bookmarks(query)
            if (bookmarks.isEmpty()) {
                break
            }
            for (b in bookmarks) {
                if (b.getEnd() == null) {
                    findEnd(b, view)
                }
                if (equal(modelId, b.ModelId)) {
                    view.addHighlighting(BookmarkHighlighting(view, Collection, b))
                }
            }
            query = query.next()
        }
    }

    private fun setFootnoteModel(modelId: String) {
        val model = Model!!.getFootnoteModel(modelId)
        FootnoteView.model = model
        if (model != null) {
            myFootnoteModelId = modelId
            setBookmarkHighlightings(FootnoteView, modelId)
        }
    }

    @Synchronized
    private fun openBookInternal(book: Book, bookmark: Bookmark?, force: Boolean) {
        if (!force && Model != null && Collection.sameBook(book, Model!!.Book)) {
            if (bookmark != null) {
                gotoBookmark(bookmark, false)
            }
            return
        }

        hideActivePopup()
        storePosition()

        BookTextView!!.model = null
        FootnoteView.model = null
        clearTextCaches()
        Model = null
        ExternalBook = null
        System.gc()
        System.gc()

        val pluginCollection = Instance(SystemInfo!!)
        val plugin: FormatPlugin
        try {
            plugin = getPlugin(pluginCollection, book)
        } catch (e: BookReadingException) {
            processException(e)
            return
        }

        if (plugin is ExternalFormatPlugin) {
            ExternalBook = book
            val bm: Bookmark?
            if (bookmark != null) {
                bm = bookmark
            } else {
                var pos: ZLTextPosition = getStoredPosition(book)
                if (pos == null) {
                    pos = ZLTextFixedPosition(0, 0, 0)
                }
                bm = Bookmark(Collection, book, "", EmptyTextSnippet(pos), false)
            }
            myExternalFileOpener!!.openFile(plugin, book, bm)
            return
        }

        try {
            Model = createModel(book, plugin)
            Collection.saveBook(book)
            ZLTextHyphenator.Instance().load(book.getLanguage())
            BookTextView!!.model = Model!!.textModel
            setBookmarkHighlightings(BookTextView!!, null)
            gotoStoredPosition()
            if (bookmark == null) {
                setView(BookTextView)
            } else {
                gotoBookmark(bookmark, false)
            }
            Collection.addToRecentlyOpened(book)
            val title = StringBuilder(book.getTitle())
            if (!book.authors().isEmpty()) {
                var first = true
                for (a in book.authors()) {
                    title.append(if (first) " (" else ", ")
                    title.append(a.DisplayName)
                    first = false
                }
                title.append(")")
            }
            setTitle(title.toString())
        } catch (e: BookReadingException) {
            processException(e)
        }

        getViewWidget()!!.reset()
        getViewWidget()!!.repaint()

        for (info in plugin.readEncryptionInfos(book)) {
            if (info != null && !isSupported(info.Method)) {
                showErrorMessage("unsupportedEncryptionMethod", book.getPath())
                break
            }
        }
    }

    private fun invisibleBookmarks(): MutableList<Bookmark> {
        val bookmarks = Collection.bookmarks(
            BookmarkQuery(Model!!.Book, false, 10)
        ).toMutableList()
        Collections.sort(bookmarks, ByTimeComparator())
        return bookmarks
    }

    fun jumpBack(): Boolean {
        try {
            if (this.textView !== BookTextView) {
                showBookTextView()
                return true
            }

            if (myJumpEndPosition == null || myJumpTimeStamp == null) {
                return false
            }
            // more than 2 minutes ago
            if (myJumpTimeStamp!!.getTime() + 2 * 60 * 1000 < Date().getTime()) {
                return false
            }
            if (!myJumpEndPosition!!.equals(BookTextView!!.startCursor)) {
                return false
            }

            val bookmarks = invisibleBookmarks()
            if (bookmarks.isEmpty()) {
                return false
            }
            val b = bookmarks.get(0)
            Collection.deleteBookmark(b)
            gotoBookmark(b, true)
            return true
        } finally {
            myJumpEndPosition = null
            myJumpTimeStamp = null
        }
    }

    private fun gotoBookmark(bookmark: Bookmark, exactly: Boolean) {
        val modelId = bookmark.ModelId
        if (modelId == null) {
            addInvisibleBookmark()
            if (exactly) {
                BookTextView!!.gotoPosition(bookmark)
            } else {
                BookTextView!!.gotoHighlighting(
                    BookmarkHighlighting(BookTextView!!, Collection, bookmark)
                )
            }
            setView(BookTextView)
        } else {
            setFootnoteModel(modelId)
            if (exactly) {
                FootnoteView.gotoPosition(bookmark)
            } else {
                FootnoteView.gotoHighlighting(
                    BookmarkHighlighting(FootnoteView, Collection, bookmark)
                )
            }
            setView(FootnoteView)
        }
        getViewWidget()!!.repaint()
        storePosition()
    }

    fun showBookTextView() {
        setView(BookTextView)
    }

    override fun onWindowClosing() {
        storePosition()
    }

    fun useSyncInfo(openOtherBook: Boolean, notifier: Notifier?) {
        // Sync functionality removed - stub method
    }

    private fun getStoredPosition(book: Book): ZLTextFixedPosition {
        val local =
            Collection.getStoredPosition(book.getId())

        if (local == null) {
            return ZLTextFixedPosition(0, 0, 0)
        }
        return local
    }

    private fun gotoStoredPosition() {
        myStoredPositionBook = if (Model != null) Model!!.Book else null
        if (myStoredPositionBook == null) {
            return
        }
        myStoredPosition = getStoredPosition(myStoredPositionBook!!)
        BookTextView!!.gotoPosition(myStoredPosition)
        savePosition()
    }

    fun storePosition() {
        val bk = if (Model != null) Model!!.Book else null
        if (bk != null && bk === myStoredPositionBook && myStoredPosition != null && BookTextView != null) {
            val position: ZLTextPosition = ZLTextFixedPosition(BookTextView!!.startCursor)
            if (!myStoredPosition!!.equals(position)) {
                myStoredPosition = position
                savePosition()
            }
        }
    }

    private fun savePosition() {
        val progress = BookTextView!!.progress
        synchronized(mySaverThread) {
            if (!mySaverThread.isAlive()) {
                mySaverThread.start()
            }
            mySaverThread.add(PositionSaver(myStoredPositionBook!!, myStoredPosition!!, progress))
        }
    }

    fun hasCancelActions(): Boolean {
        return CancelMenuHelper().getActionsList(Collection).size > 1
    }

    fun runCancelAction(type: CancelMenuHelper.ActionType, bookmark: Bookmark) {
        when (type) {
            CancelMenuHelper.ActionType.library -> runAction(ActionCode.SHOW_LIBRARY)
            CancelMenuHelper.ActionType.previousBook -> openBook(
                Collection.getRecentBook(1),
                null,
                null,
                null
            )

            CancelMenuHelper.ActionType.returnTo -> {
                Collection.deleteBookmark(bookmark)
                gotoBookmark(bookmark, true)
            }

            CancelMenuHelper.ActionType.close -> closeWindow()
        }
    }

    @Synchronized
    private fun updateInvisibleBookmarksList(b: Bookmark?) {
        if (Model != null && Model!!.Book != null && b != null) {
            for (bm in invisibleBookmarks()) {
                if (b.equals(bm)) {
                    Collection.deleteBookmark(bm)
                }
            }
            Collection.saveBookmark(b)
            val bookmarks = invisibleBookmarks()
            for (i in 3..<bookmarks.size) {
                Collection.deleteBookmark(bookmarks.get(i))
            }
        }
    }

    fun addInvisibleBookmark(cursor: ZLTextWordCursor?) {
        var cursor = cursor
        if (cursor == null) {
            return
        }

        cursor = ZLTextWordCursor(cursor)
        if (cursor.isNull()) {
            return
        }

        val textView: ZLTextView = this.textView
        val textModel: ZLTextModel?
        val book: Book?
        val snippet: AutoTextSnippet
        // textView.model will not be changed inside synchronised block
        synchronized(textView) {
            textModel = textView.model
            val model = Model
            book = if (model != null) model.Book else null
            if (book == null || textView !== BookTextView || textModel == null) {
                return
            }
            snippet = AutoTextSnippet(cursor, 30)
        }

        updateInvisibleBookmarksList(
            Bookmark(
                Collection, book!!, textModel!!.getId(), snippet, false
            )
        )
    }

    fun addInvisibleBookmark() {
        if (Model!!.Book != null && this.textView === BookTextView) {
            updateInvisibleBookmarksList(createBookmark(30, false))
        }
    }

    fun createBookmark(maxChars: Int, visible: Boolean): Bookmark? {
        val view = this.textView
        val cursor = view.startCursor

        if (cursor.isNull()) {
            return null
        }

        return Bookmark(
            Collection,
            Model!!.Book,
            view.model!!.getId(),
            AutoTextSnippet(cursor, maxChars),
            visible
        )
    }

    open val currentTOCElement: TOCTree?
        get() {
            val cursor = BookTextView!!.startCursor
            if (Model == null || cursor == null) {
                return null
            }

            var index = cursor.paragraphIndex
            if (cursor.isEndOfParagraph) {
                ++index
            }
            var treeToSelect: TOCTree? = null
            for (tree in Model!!.TOCTree) {
                val reference =
                    tree.reference
                if (reference == null) {
                    continue
                }
                if (reference.ParagraphIndex > index) {
                    break
                }
                treeToSelect = tree
            }
            return treeToSelect
        }

    fun onBookUpdated(book: Book) {
        if (Model == null || Model!!.Book == null || !Collection.sameBook(Model!!.Book, book)) {
            return
        }

        val newEncoding = book.getEncodingNoDetection()
        val oldEncoding = Model!!.Book.getEncodingNoDetection()

        Model!!.Book.updateFrom(book)

        if (newEncoding != null && newEncoding != oldEncoding) {
            reloadBook()
        } else {
            ZLTextHyphenator.Instance().load(Model!!.Book.getLanguage())
            clearTextCaches()
            getViewWidget()!!.repaint()
        }
    }

    interface ExternalFileOpener {
        fun openFile(plugin: ExternalFormatPlugin?, book: Book?, bookmark: Bookmark?)
    }

    interface Notifier {
        fun showMissingBookNotification(info: MissingBookInfo?)
    }

    class MissingBookInfo

    private inner class PositionSaver(
        private val myBook: Book,
        private val myPosition: ZLTextPosition,
        private val myProgress: RationalNumber?
    ) : Runnable {
        override fun run() {
            Collection.storePosition(myBook.getId(), myPosition)
            myBook.setProgress(myProgress)
            Collection.saveBook(myBook)
        }
    }

    private inner class SaverThread : Thread() {
        private val myTasks: MutableList<Runnable?> = Collections.synchronizedList<Runnable?>(
            LinkedList<Runnable?>()
        )

        init {
            setPriority(MIN_PRIORITY)
        }

        fun add(task: Runnable?) {
            myTasks.add(task)
        }

        override fun run() {
            while (true) {
                synchronized(myTasks) {
                    while (!myTasks.isEmpty()) {
                        myTasks.removeAt(0)!!.run()
                    }
                }
                try {
                    sleep(500)
                } catch (e: InterruptedException) {
                }
            }
        }
    }
}
