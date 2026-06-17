package org.geometerplus.fbreader.fbreader

import org.fbreader.util.ComparisonUtil
import org.geometerplus.fbreader.book.AbstractBook
import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.BookEvent
import org.geometerplus.fbreader.book.BookUtil
import org.geometerplus.fbreader.book.Bookmark
import org.geometerplus.fbreader.book.BookmarkQuery
import org.geometerplus.fbreader.book.BookmarkUtil
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.fbreader.bookmodel.BookModel
import org.geometerplus.fbreader.bookmodel.TOCTree
import org.geometerplus.fbreader.fbreader.options.CancelMenuHelper
import org.geometerplus.fbreader.fbreader.options.ImageOptions
import org.geometerplus.fbreader.fbreader.options.MiscOptions
import org.geometerplus.fbreader.fbreader.options.PageTurningOptions
import org.geometerplus.fbreader.fbreader.options.SyncOptions
import org.geometerplus.fbreader.fbreader.options.ViewOptions
import org.geometerplus.fbreader.formats.BookReadingException
import org.geometerplus.fbreader.formats.ExternalFormatPlugin
import org.geometerplus.fbreader.formats.FormatPlugin
import org.geometerplus.fbreader.formats.PluginCollection
import org.geometerplus.fbreader.network.sync.SyncData
import org.geometerplus.fbreader.util.AutoTextSnippet
import org.geometerplus.fbreader.util.EmptyTextSnippet
import org.geometerplus.zlibrary.core.application.ZLApplication
import org.geometerplus.zlibrary.core.application.ZLKeyBindings
import org.geometerplus.zlibrary.core.drm.EncryptionMethod
import org.geometerplus.zlibrary.core.util.RationalNumber
import org.geometerplus.zlibrary.core.util.SystemInfo
import org.geometerplus.zlibrary.core.view.ZLViewEnums.Direction
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

open class FBReaderApp(systemInfo: SystemInfo?, val collection: IBookCollection<Book>) : ZLApplication(systemInfo) {

    val miscOptions = MiscOptions()
    val imageOptions = ImageOptions()
    val viewOptions = ViewOptions()
    val pageTurningOptions = PageTurningOptions()
    val syncOptions = SyncOptions()
    private val bindings = ZLKeyBindings()
    private val syncData = SyncData()
    private val saverThread = SaverThread()
    var bookTextView: FBView
    var footnoteView: FBView
    @Volatile
    @JvmField
    var model: BookModel? = null
    @Volatile
    var externalBook: Book? = null
    private var externalFileOpener: ExternalFileOpener? = null
    private var footnoteModelId: String? = null
    private var jumpEndPosition: ZLTextPosition? = null
    private var jumpTimeStamp: Date? = null
    @Volatile
    private var storedPosition: ZLTextPosition? = null
    @Volatile
    private var storedPositionBook: Book? = null

    init {
        collection.addListener(object : IBookCollection.Listener<Book> {
            override fun onBookEvent(event: BookEvent, book: Book?) {
                when (event) {
                    BookEvent.BookmarkStyleChanged, BookEvent.BookmarksUpdated -> {
                        if (model != null && (book == null || collection.sameBook(book, model!!.book))) {
                            if (bookTextView.getModel() != null) {
                                setBookmarkHighlightings(bookTextView, null)
                            }
                            if (footnoteView.getModel() != null && footnoteModelId != null) {
                                setBookmarkHighlightings(footnoteView, footnoteModelId)
                            }
                        }
                    }
                    BookEvent.Updated -> {
                        if (book != null) {
                            onBookUpdated(book)
                        }
                    }
                    else -> {}
                }
            }

            override fun onBuildEvent(status: IBookCollection.Status) {}
        })

        addAction(ActionCode.INCREASE_FONT, ChangeFontSizeAction(this, +2))
        addAction(ActionCode.DECREASE_FONT, ChangeFontSizeAction(this, -2))

        addAction(ActionCode.FIND_NEXT, FindNextAction(this))
        addAction(ActionCode.FIND_PREVIOUS, FindPreviousAction(this))
        addAction(ActionCode.CLEAR_FIND_RESULTS, ClearFindResultsAction(this))

        addAction(ActionCode.SELECTION_CLEAR, SelectionClearAction(this))

        addAction(ActionCode.TEL_ABOUT_MISSPELL, SelectionClearAction(this))
        addAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD, SelectionClearAction(this))

        addAction(ActionCode.MOVE_CURSOR_UP, MoveCursorAction(this, Direction.up))
        addAction(ActionCode.MOVE_CURSOR_DOWN, MoveCursorAction(this, Direction.down))
        addAction(ActionCode.MOVE_CURSOR_LEFT, MoveCursorAction(this, Direction.rightToLeft))
        addAction(ActionCode.MOVE_CURSOR_RIGHT, MoveCursorAction(this, Direction.leftToRight))

        addAction(ActionCode.VOLUME_KEY_SCROLL_FORWARD, VolumeKeyTurnPageAction(this, true))
        addAction(ActionCode.VOLUME_KEY_SCROLL_BACK, VolumeKeyTurnPageAction(this, false))

        addAction(ActionCode.EXIT, ExitAction(this))

        bookTextView = FBView(this)
        footnoteView = FBView(this)

        setView(bookTextView)
    }

    fun setExternalFileOpener(o: ExternalFileOpener?) {
        externalFileOpener = o
    }

    val currentBook: Book?
        get() {
            val m = model
            return m?.book ?: externalBook
        }

    fun openHelpBook() {
        openBook(collection.getBookByFile(BookUtil.getHelpFile().getPath()), null, null, null)
    }

    fun getCurrentServerBook(notifier: Notifier?): Book? {
        val info = syncData.serverBookInfo
        if (info == null) {
            return null
        }

        for (hash in info.hashes) {
            val book = collection.getBookByHash(hash)
            if (book != null) {
                return book
            }
        }
        notifier?.showMissingBookNotification(info)
        return null
    }

    fun openBook(book: Book?, bookmark: Bookmark?, postAction: Runnable?, notifier: Notifier?) {
        var bookToOpen = book
        if (model != null) {
            if (bookToOpen == null || bookmark == null && collection.sameBook(bookToOpen, model!!.book)) {
                return
            }
        }

        if (bookToOpen == null) {
            bookToOpen = getCurrentServerBook(notifier)
            if (bookToOpen == null) {
                bookToOpen = collection.getRecentBook(0)
            }
            if (bookToOpen == null || !BookUtil.fileByBook(bookToOpen).exists()) {
                bookToOpen = collection.getBookByFile(BookUtil.getHelpFile().getPath())
            }
            if (bookToOpen == null) {
                return
            }
        }
        bookToOpen.addNewLabel(AbstractBook.READ_LABEL)
        collection.saveBook(bookToOpen)

        val executor = createExecutor("loadingBook")
        executor.execute({
            openBookInternal(bookToOpen, bookmark, false)
        }, postAction)
    }

    private fun reloadBook() {
        val book = currentBook
        if (book != null) {
            val executor = createExecutor("loadingBook")
            executor.execute({
                openBookInternal(book, null, true)
            }, null)
        }
    }

    override fun keyBindings(): ZLKeyBindings = bindings

    fun getTextView(): FBView = currentView as FBView

    fun getFootnoteData(id: String): AutoTextSnippet? {
        if (model == null) {
            return null
        }
        val label = model!!.getLabel(id)
        if (label == null) {
            return null
        }
        val textModel: ZLTextModel? = if (label.modelId != null) {
            model!!.getFootnoteModel(label.modelId)
        } else {
            model!!.getTextModel()
        }
        if (textModel == null) {
            return null
        }
        val cursor = ZLTextWordCursor(ZLTextParagraphCursor(textModel, label.paragraphIndex))
        val longSnippet = AutoTextSnippet(cursor, 140)
        return if (longSnippet.isEndOfText) {
            longSnippet
        } else {
            AutoTextSnippet(cursor, 100)
        }
    }

    fun tryOpenFootnote(id: String) {
        if (model != null) {
            jumpEndPosition = null
            jumpTimeStamp = null
            val label = model!!.getLabel(id)
            if (label != null) {
                if (label.modelId == null) {
                    if (getTextView() == bookTextView) {
                        addInvisibleBookmark()
                        jumpEndPosition = ZLTextFixedPosition(label.paragraphIndex, 0, 0)
                        jumpTimeStamp = Date()
                    }
                    bookTextView.gotoPosition(label.paragraphIndex, 0, 0)
                    setView(bookTextView)
                } else {
                    setFootnoteModel(label.modelId)
                    setView(footnoteView)
                    footnoteView.gotoPosition(label.paragraphIndex, 0, 0)
                }
                viewWidget.repaint()
                storePosition()
            }
        }
    }

    fun clearTextCaches() {
        bookTextView.clearCaches()
        footnoteView.clearCaches()
    }

    fun addSelectionBookmark(): Bookmark? {
        val fbView = getTextView()
        val snippet = fbView.selectedSnippet
        if (snippet == null) {
            return null
        }

        val bookmark = Bookmark(
            collection,
            model!!.book,
            fbView.getModel()!!.id,
            snippet,
            true
        )
        collection.saveBookmark(bookmark)
        fbView.clearSelection()

        return bookmark
    }

    private fun setBookmarkHighlightings(view: ZLTextView, modelId: String?) {
        view.removeHighlightings(BookmarkHighlighting::class.java)
        var query = BookmarkQuery(model!!.book, 20)
        while (true) {
            val bookmarks = collection.bookmarks(query)
            if (bookmarks.isEmpty()) {
                break
            }
            for (b in bookmarks) {
                if (b.end == null) {
                    BookmarkUtil.findEnd(b, view)
                }
                if (ComparisonUtil.equal(modelId, b.modelId)) {
                    view.addHighlighting(BookmarkHighlighting(view, collection, b))
                }
            }
            query = query.next()
        }
    }

    private fun setFootnoteModel(modelId: String) {
        val textModel = model!!.getFootnoteModel(modelId)
        footnoteView.setModel(textModel)
        if (textModel != null) {
            footnoteModelId = modelId
            setBookmarkHighlightings(footnoteView, modelId)
        }
    }

    @Synchronized
    private fun openBookInternal(book: Book, bookmark: Bookmark?, force: Boolean) {
        if (!force && model != null && collection.sameBook(book, model!!.book)) {
            if (bookmark != null) {
                gotoBookmark(bookmark, false)
            }
            return
        }

        hideActivePopup()
        storePosition()

        bookTextView.setModel(null)
        footnoteView.setModel(null)
        clearTextCaches()
        model = null
        externalBook = null
        System.gc()
        System.gc()

        val pluginCollection = PluginCollection.Instance(SystemInfo)
        val plugin: FormatPlugin
        try {
            plugin = BookUtil.getPlugin(pluginCollection, book)
        } catch (e: BookReadingException) {
            processException(e)
            return
        }

        if (plugin is ExternalFormatPlugin) {
            externalBook = book
            val bm = bookmark ?: run {
                var pos = getStoredPosition(book)
                if (pos == null) {
                    pos = ZLTextFixedPosition(0, 0, 0)
                }
                Bookmark(collection, book, "", EmptyTextSnippet(pos), false)
            }
            externalFileOpener!!.openFile(plugin, book, bm)
            return
        }

        try {
            model = BookModel.createModel(book, plugin)
            collection.saveBook(book)
            ZLTextHyphenator.Instance().load(book.language)
            bookTextView.setModel(model!!.getTextModel())
            setBookmarkHighlightings(bookTextView, null)
            gotoStoredPosition()
            if (bookmark == null) {
                setView(bookTextView)
            } else {
                gotoBookmark(bookmark, false)
            }
            collection.addToRecentlyOpened(book)
            val title = StringBuilder(book.title)
            if (book.authors().isNotEmpty()) {
                var first = true
                for (a in book.authors()) {
                    title.append(if (first) " (" else ", ")
                    title.append(a.displayName)
                    first = false
                }
                title.append(")")
            }
            setTitle(title.toString())
        } catch (e: BookReadingException) {
            processException(e)
        }

        viewWidget.reset()
        viewWidget.repaint()

        for (info in plugin.readEncryptionInfos(book)) {
            if (info != null && !EncryptionMethod.isSupported(info.Method)) {
                showErrorMessage("unsupportedEncryptionMethod", book.getPath())
                break
            }
        }
    }

    private fun invisibleBookmarks(): List<Bookmark> {
        val bookmarks = collection.bookmarks(
            BookmarkQuery(model!!.book, 10, false)
        )
        Collections.sort(bookmarks, Bookmark.ByTimeComparator())
        return bookmarks
    }

    fun jumpBack(): Boolean {
        try {
            if (getTextView() != bookTextView) {
                showBookTextView()
                return true
            }

            if (jumpEndPosition == null || jumpTimeStamp == null) {
                return false
            }
            // more than 2 minutes ago
            if (jumpTimeStamp!!.time + 2 * 60 * 1000 < Date().time) {
                return false
            }
            if (jumpEndPosition != bookTextView.getStartCursor()) {
                return false
            }

            val bookmarks = invisibleBookmarks()
            if (bookmarks.isEmpty()) {
                return false
            }
            val b = bookmarks[0]
            collection.deleteBookmark(b)
            gotoBookmark(b, true)
            return true
        } finally {
            jumpEndPosition = null
            jumpTimeStamp = null
        }
    }

    private fun gotoBookmark(bookmark: Bookmark, exactly: Boolean) {
        val modelId = bookmark.modelId
        if (modelId == null) {
            addInvisibleBookmark()
            if (exactly) {
                bookTextView.gotoPosition(bookmark)
            } else {
                bookTextView.gotoHighlighting(
                    BookmarkHighlighting(bookTextView, collection, bookmark)
                )
            }
            setView(bookTextView)
        } else {
            setFootnoteModel(modelId)
            if (exactly) {
                footnoteView.gotoPosition(bookmark)
            } else {
                footnoteView.gotoHighlighting(
                    BookmarkHighlighting(footnoteView, collection, bookmark)
                )
            }
            setView(footnoteView)
        }
        viewWidget.repaint()
        storePosition()
    }

    fun showBookTextView() {
        setView(bookTextView)
    }

    override fun onWindowClosing() {
        storePosition()
    }

    fun useSyncInfo(openOtherBook: Boolean, notifier: Notifier?) {
        if (openOtherBook && syncOptions.changeCurrentBook.value) {
            val fromServer = getCurrentServerBook(notifier)
            val recentBook = collection.getRecentBook(0)
            if (fromServer != null && recentBook != null && !collection.sameBook(fromServer, recentBook)) {
                openBook(fromServer, null, null, notifier)
                return
            }
        }

        if (storedPositionBook != null &&
            syncData.hasPosition(collection.getHash(storedPositionBook!!, true)!!)) {
            gotoStoredPosition()
            storePosition()
        }
    }

    private fun getStoredPosition(book: Book): ZLTextFixedPosition {
        val fromServer = syncData.getAndCleanPosition(collection.getHash(book, true)!!)
        val local = collection.getStoredPosition(book.id)

        return when {
            local == null -> fromServer ?: ZLTextFixedPosition(0, 0, 0)
            fromServer == null -> local
            else -> if (fromServer.timestamp >= local.timestamp) fromServer else local
        }
    }

    private fun gotoStoredPosition() {
        storedPositionBook = model?.book
        if (storedPositionBook == null) {
            return
        }
        storedPosition = getStoredPosition(storedPositionBook!!)
        bookTextView.gotoPosition(storedPosition)
        savePosition()
    }

    fun storePosition() {
        val bk = model?.book
        if (bk != null && bk == storedPositionBook && storedPosition != null && bookTextView != null) {
            val position = ZLTextFixedPosition(bookTextView.getStartCursor())
            if (storedPosition != position) {
                storedPosition = position
                savePosition()
            }
        }
    }

    private fun savePosition() {
        val progress = bookTextView.progress
        synchronized(saverThread) {
            if (!saverThread.isAlive) {
                saverThread.start()
            }
            saverThread.add(PositionSaver(storedPositionBook!!, storedPosition!!, progress))
        }
    }

    fun hasCancelActions(): Boolean {
        return CancelMenuHelper().getActionsList(collection).size > 1
    }

    fun runCancelAction(type: CancelMenuHelper.ActionType, bookmark: Bookmark?) {
        when (type) {
            CancelMenuHelper.ActionType.library -> runAction(ActionCode.SHOW_LIBRARY)
            CancelMenuHelper.ActionType.networkLibrary -> runAction(ActionCode.SHOW_NETWORK_LIBRARY)
            CancelMenuHelper.ActionType.previousBook -> openBook(collection.getRecentBook(1), null, null, null)
            CancelMenuHelper.ActionType.returnTo -> {
                if (bookmark != null) {
                    collection.deleteBookmark(bookmark)
                    gotoBookmark(bookmark, true)
                }
            }
            CancelMenuHelper.ActionType.close -> closeWindow()
        }
    }

    @Synchronized
    private fun updateInvisibleBookmarksList(b: Bookmark) {
        if (model != null && model!!.book != null && b != null) {
            for (bm in invisibleBookmarks()) {
                if (b == bm) {
                    collection.deleteBookmark(bm)
                }
            }
            collection.saveBookmark(b)
            val bookmarks = invisibleBookmarks()
            for (i in 3 until bookmarks.size) {
                collection.deleteBookmark(bookmarks[i])
            }
        }
    }

    fun addInvisibleBookmark(cursor: ZLTextWordCursor?) {
        if (cursor == null) {
            return
        }

        val cursorCopy = ZLTextWordCursor(cursor)
        if (cursorCopy.isNull) {
            return
        }

        val textView = getTextView()
        val textModel: ZLTextModel?
        val book: Book?
        val snippet: AutoTextSnippet
        // textView.getModel() will not be changed inside synchronised block
        synchronized(textView) {
            textModel = textView.getModel()
            val bookModel = model
            book = bookModel?.book
            if (book == null || textView != bookTextView || textModel == null) {
                return
            }
            snippet = AutoTextSnippet(cursorCopy, 30)
        }

        updateInvisibleBookmarksList(Bookmark(
            collection, book!!, textModel!!.id, snippet, false
        ))
    }

    fun addInvisibleBookmark() {
        if (model!!.book != null && getTextView() == bookTextView) {
            updateInvisibleBookmarksList(createBookmark(30, false)!!)
        }
    }

    fun createBookmark(maxChars: Int, visible: Boolean): Bookmark? {
        val view = getTextView()
        val cursor = view.getStartCursor()

        if (cursor.isNull) {
            return null
        }

        return Bookmark(
            collection,
            model!!.book,
            view.getModel()!!.id,
            AutoTextSnippet(cursor, maxChars),
            visible
        )
    }

    open val currentTOCElement: TOCTree?
        get() {
            val cursor = bookTextView.getStartCursor()
            if (model == null || cursor == null) {
                return null
            }

            var index = cursor.paragraphIndex
            if (cursor.isEndOfParagraph) {
                ++index
            }
            var treeToSelect: TOCTree? = null
            for (tree in model!!.tocTree) {
                val ref = tree.reference
                if (ref == null) {
                    continue
                }
                if (ref.paragraphIndex > index) {
                    break
                }
                treeToSelect = tree
            }
            return treeToSelect
        }

    fun onBookUpdated(book: Book) {
        if (model == null || model!!.book == null || !collection.sameBook(model!!.book, book)) {
            return
        }

        val newEncoding = book.getEncodingNoDetection()
        val oldEncoding = model!!.book.getEncodingNoDetection()

        model!!.book.updateFrom(book)

        if (newEncoding != null && newEncoding != oldEncoding) {
            reloadBook()
        } else {
            ZLTextHyphenator.Instance().load(model!!.book.language)
            clearTextCaches()
            viewWidget.repaint()
        }
    }

    interface ExternalFileOpener {
        fun openFile(plugin: ExternalFormatPlugin, book: Book, bookmark: Bookmark)
    }

    interface Notifier {
        fun showMissingBookNotification(info: SyncData.ServerBookInfo)
    }

    private inner class PositionSaver(
        private val book: Book,
        private val position: ZLTextPosition,
        private val progress: RationalNumber
    ) : Runnable {

        override fun run() {
            collection.storePosition(book.id, position)
            book.progress = progress
            collection.saveBook(book)
        }
    }

    private inner class SaverThread : Thread() {
        private val tasks = Collections.synchronizedList(LinkedList<Runnable>())

        init {
            priority = MIN_PRIORITY
        }

        fun add(task: Runnable) {
            tasks.add(task)
        }

        override fun run() {
            while (true) {
                synchronized(tasks) {
                    while (tasks.isNotEmpty()) {
                        tasks.removeAt(0).run()
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
