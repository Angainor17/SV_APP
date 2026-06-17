package org.geometerplus.fbreader.fbreader

import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.opds.OPDSXMLReader
import org.geometerplus.fbreader.network.opds.SimpleOPDSFeedHandler
import org.geometerplus.zlibrary.core.network.QuietNetworkContext
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest
import org.geometerplus.zlibrary.text.view.ExtensionElementManager
import java.io.IOException
import java.io.InputStream
import java.util.Timer
import java.util.TimerTask

internal class BookElementManager(private val view: FBView) : ExtensionElementManager() {

    private val screenRefresher = Runnable {
        view.Application.viewWidget.reset()
        view.Application.viewWidget.repaint()
    }

    private val cache = mutableMapOf<Map<String, String>, MutableList<BookElement>>()
    private var timer: Timer? = null

    @Synchronized
    override fun getElements(type: String, data: Map<String, String>): List<BookElement> {
        if (type != "opds") {
            return emptyList()
        }

        var elements = cache[data]
        if (elements == null) {
            try {
                val count = data["size"]?.toInt() ?: 0
                elements = mutableListOf()
                for (i in 0 until count) {
                    elements.add(BookElement(view))
                }
                startLoading(data["src"] ?: "", elements)
            } catch (t: Throwable) {
                return emptyList()
            }
            cache[data] = elements
        }
        return elements.toList()
    }

    private fun startLoading(url: String, elements: MutableList<BookElement>) {
        val library = NetworkLibrary.Instance(view.Application.SystemInfo)

        Thread {
            val handler = SimpleOPDSFeedHandler(library, url)
            try {
                QuietNetworkContext().perform(object : ZLNetworkRequest.Get(url, true) {
                    @Throws(IOException::class, ZLNetworkException::class)
                    override fun handleStream(inputStream: InputStream, length: Int) {
                        OPDSXMLReader(library, handler, false).read(inputStream)
                    }
                })
                if (handler.books().isEmpty()) {
                    throw RuntimeException()
                }
                timer = null
                val items = handler.books()
                var index = 0
                for (book in elements) {
                    book.setData(items[index])
                    index = (index + 1) % items.size
                    screenRefresher.run()
                }
            } catch (e: Exception) {
                if (timer == null) {
                    timer = Timer()
                }
                timer?.schedule(object : TimerTask() {
                    override fun run() {
                        startLoading(url, elements)
                    }
                }, 10000)
                e.printStackTrace()
            }
        }.start()
    }
}
