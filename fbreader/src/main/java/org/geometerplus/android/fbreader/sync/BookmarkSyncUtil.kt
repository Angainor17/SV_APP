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

package org.geometerplus.android.fbreader.sync

import org.fbreader.util.ComparisonUtil
import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.Bookmark
import org.geometerplus.fbreader.book.BookmarkQuery
import org.geometerplus.fbreader.book.BookmarkUtil
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.fbreader.fbreader.options.SyncOptions
import org.geometerplus.zlibrary.core.network.JsonRequest2
import org.geometerplus.zlibrary.core.util.ZLColor
import java.util.LinkedList

internal object BookmarkSyncUtil {
    fun sync(context: SyncNetworkContext, collection: IBookCollection<Book>) {
        try {
            val actualServerInfos = HashMap<String, Info>()
            val deletedOnServerUids = HashSet<String>()
            val serverStyles = HashMap<Int, Map<String, Any?>>()

            var infoRequest: JsonRequest2? = null

            // Step 0: loading bookmarks info lists (actual & deleted bookmark ids)
            val data = HashMap<String, Any?>()
            val pageSize = 100
            data["page_size"] = pageSize
            val responseMap = HashMap<String, Any?>()

            var pageNo = 0
            while (true) {
                data["page_no"] = pageNo
                data["timestamp"] = System.currentTimeMillis()
                infoRequest = object : JsonRequest2(
                    SyncOptions.BASE_URL + "sync/bookmarks.lite.paged", data
                ) {
                    override fun processResponse(response: Any?) {
                        @Suppress("UNCHECKED_CAST")
                        responseMap.putAll(response as Map<String, Any?>)
                    }
                }
                context.perform(infoRequest)

                @Suppress("UNCHECKED_CAST")
                for (info in responseMap["actual"] as List<Map<String, Any?>>) {
                    val bmk = Info(info)
                    actualServerInfos[bmk.uid] = bmk
                }
                @Suppress("UNCHECKED_CAST")
                deletedOnServerUids.addAll(responseMap["deleted"] as List<String>)

                val count = (responseMap["count"] as Long).toInt()
                if (count <= (pageNo + 1) * pageSize) {
                    break
                }
                pageNo++
            }

            @Suppress("UNCHECKED_CAST")
            for (info in responseMap["styles"] as List<Map<String, Any?>>) {
                serverStyles[(info["style_id"] as Long).toInt()] = info
            }

            // Step 1: purge deleted bookmarks info already synced with server
            val deletedOnClientUids = HashSet(collection.deletedBookmarkUids())
            if (deletedOnClientUids.isNotEmpty()) {
                val toPurge = ArrayList(deletedOnClientUids)
                toPurge.removeAll(actualServerInfos.keys)
                if (toPurge.isNotEmpty()) {
                    collection.purgeBookmarks(toPurge)
                }
            }

            // Step 2a: prepare lists of bookmarks to create/delete/update on server/client
            val toSendToServer = LinkedList<Bookmark>()
            val toDeleteOnClient = LinkedList<Bookmark>()
            val toUpdateOnServer = LinkedList<Bookmark>()
            val toUpdateOnClient = LinkedList<Bookmark>()
            val toGetFromServer = LinkedList<String>()
            val toDeleteOnServer = LinkedList<String>()

            var q = BookmarkQuery(20)
            while (true) {
                val bmks = collection.bookmarks(q)
                if (bmks.isEmpty()) {
                    break
                }
                for (b in bmks) {
                    val info = actualServerInfos.remove(b.uid)
                    if (info != null) {
                        if (info.versionUid == null) {
                            if (b.getVersionUid() != null) {
                                toUpdateOnServer.add(b)
                            }
                        } else {
                            if (b.getVersionUid() == null) {
                                toUpdateOnClient.add(b)
                            } else if (info.versionUid != b.getVersionUid()) {
                                val ts = b.getTimestamp(Bookmark.DateType.Latest) ?: 0L
                                if (info.timestamp <= ts) {
                                    toUpdateOnServer.add(b)
                                } else {
                                    toUpdateOnClient.add(b)
                                }
                            }
                        }
                    } else if (b.uid in deletedOnServerUids) {
                        toDeleteOnClient.add(b)
                    } else {
                        toSendToServer.add(b)
                    }
                }
                q = q.next()
            }

            val leftUids = actualServerInfos.keys
            if (leftUids.isNotEmpty()) {
                toGetFromServer.addAll(leftUids)
                toGetFromServer.removeAll(deletedOnClientUids)

                toDeleteOnServer.addAll(leftUids)
                toDeleteOnServer.retainAll(deletedOnClientUids)
            }

            // collecting book hashes & removing bookmarks with unknown book hash
            val booksByHash = BooksByHash(collection)
            val iter = toGetFromServer.listIterator()
            while (iter.hasNext()) {
                val info = actualServerInfos[iter.next()] ?: continue
                if (booksByHash.getBook(info.bookHashes) == null) {
                    iter.remove()
                }
            }

            // Step 2b: update styles on client, prepare lists of styles to create/update on server
            val stylesToSend = ArrayList<Map<String, Any?>>()
            for (style in collection.highlightingStyles()) {
                val serverInfo = serverStyles[style.id]
                var doSend = false
                if (serverInfo == null) {
                    doSend = true
                } else {
                    var doUpdate = false

                    val clientName = BookmarkUtil.getStyleName(style)
                    val serverName = serverInfo["name"] as String?
                    if (clientName != serverName) {
                        doUpdate = true
                    }

                    val clientBg = style.getBackgroundColor()
                    val serverBgCode = serverInfo["bg_color"] as Long?
                    val serverBg = serverBgCode?.let { ZLColor(it.toInt()) }
                    if (!ComparisonUtil.equal(clientBg, serverBg)) {
                        doUpdate = true
                    }

                    val clientFg = style.getForegroundColor()
                    val serverFgCode = serverInfo["fg_color"] as Long?
                    val serverFg = serverFgCode?.let { ZLColor(it.toInt()) }
                    if (!ComparisonUtil.equal(clientFg, serverFg)) {
                        doUpdate = true
                    }

                    if (doUpdate) {
                        if (style.lastUpdateTimestamp < serverInfo["timestamp"] as Long) {
                            BookmarkUtil.setStyleName(style, serverName ?: "")
                            style.setBackgroundColor(serverBg)
                            style.setForegroundColor(serverFg)
                            collection.saveHighlightingStyle(style)
                        } else {
                            doSend = true
                        }
                    }
                }

                if (doSend) {
                    val styleMap = HashMap<String, Any?>()
                    styleMap["style_id"] = style.id
                    styleMap["timestamp"] = style.lastUpdateTimestamp
                    styleMap["name"] = BookmarkUtil.getStyleName(style)
                    style.getBackgroundColor()?.let { styleMap["bg_color"] = it.intValue() }
                    style.getForegroundColor()?.let { styleMap["fg_color"] = it.intValue() }
                    stylesToSend.add(styleMap)
                }
            }

            // Step 3a: deleting obsolete bookmarks on client
            for (b in toDeleteOnClient) {
                collection.deleteBookmark(b)
            }

            // Step 3b: getting new bookmarks from the server
            if (toGetFromServer.isNotEmpty()) {
                context.perform(object : JsonRequest2(
                    SyncOptions.BASE_URL + "sync/bookmarks", fullRequestData(toGetFromServer)
                ) {
                    override fun processResponse(response: Any?) {
                        @Suppress("UNCHECKED_CAST")
                        for (info in response as List<Map<String, Any?>>) {
                            val bookmark = newBookmarkFromData(info, booksByHash)
                            bookmark?.let { collection.saveBookmark(it) }
                        }
                    }
                })
            }

            // Step 3c: getting updated bookmarks from the server
            if (toUpdateOnClient.isNotEmpty()) {
                val bookmarksMap = HashMap<String, Bookmark>()
                for (b in toUpdateOnClient) {
                    bookmarksMap[b.uid] = b
                }
                context.perform(object : JsonRequest2(
                    SyncOptions.BASE_URL + "sync/bookmarks", fullRequestData(ids(toUpdateOnClient))
                ) {
                    override fun processResponse(response: Any?) {
                        @Suppress("UNCHECKED_CAST")
                        for (info in response as List<Map<String, Any?>>) {
                            val bookmark = bookmarkToUpdate(info, bookmarksMap)
                            bookmark?.let { collection.saveBookmark(it) }
                        }
                    }
                })
            }

            // Step 3d: sending locally updated information to the server
            class HashCache {
                private val myHashByBookId = HashMap<Long, String>()

                fun getHash(b: Bookmark): String? {
                    var hash = myHashByBookId[b.bookId]
                    if (hash == null) {
                        val book = collection.getBookById(b.bookId)
                        hash = book?.let { collection.getHash(it, false) } ?: ""
                        myHashByBookId[b.bookId] = hash
                    }
                    return if (hash.isEmpty()) null else hash
                }
            }

            val cache = HashCache()

            val requests = ArrayList<Request>()
            for (b in toSendToServer) {
                cache.getHash(b)?.let { requests.add(AddRequest(b, it)) }
            }
            for (b in toUpdateOnServer) {
                cache.getHash(b)?.let { requests.add(UpdateRequest(b, it)) }
            }
            for (uid in toDeleteOnServer) {
                requests.add(DeleteRequest(uid))
            }

            if (requests.isNotEmpty() || stylesToSend.isNotEmpty()) {
                val allDataToSend = HashMap<String, Any?>()
                allDataToSend["requests"] = requests
                allDataToSend["timestamp"] = System.currentTimeMillis()
                allDataToSend["styles"] = stylesToSend
                val serverUpdateRequest = object : JsonRequest2(
                    SyncOptions.BASE_URL + "sync/update.bookmarks", allDataToSend
                ) {
                    override fun processResponse(response: Any?) {
                        System.err.println("BMK UPDATED: $response")
                    }
                }
                val csrfToken = context.getCookieValue(SyncOptions.DOMAIN, "csrftoken")
                serverUpdateRequest.addHeader("Referer", infoRequest!!.url)
                serverUpdateRequest.addHeader("X-CSRFToken", csrfToken)
                context.perform(serverUpdateRequest)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private class BooksByHash(private val myCollection: IBookCollection<Book>) : HashMap<String, Book>() {
        fun getBook(hashes: List<String>?): Book? {
            if (hashes == null) return null
            var book: Book? = null
            for (h in hashes) {
                book = get(h)
                if (book != null) break
            }
            if (book == null) {
                for (h in hashes) {
                    book = myCollection.getBookByHash(h)
                    if (book != null) break
                }
            }
            if (book != null) {
                for (h in hashes) {
                    put(h, book)
                }
            }
            return book
        }

        fun getBook(hash: String): Book? {
            var book = get(hash)
            if (book == null) {
                book = myCollection.getBookByHash(hash)
                if (book != null) {
                    put(hash, book)
                }
            }
            return book
        }
    }

    private class Info(data: Map<String, Any?>) {
        val uid: String = data["uid"] as String
        val versionUid: String? = data["version_uid"] as String?
        @Suppress("UNCHECKED_CAST")
        val bookHashes: List<String> = data["book_hashes"] as List<String>
        val timestamp: Long = run {
            var ts = (data["access_timestamp"] as Long?) ?: 0L
            val mts = data["modification_timestamp"] as Long?
            if (mts != null && mts > ts) ts = mts
            ts
        }

        override fun toString(): String = "$uid ($versionUid); $timestamp"
    }

    private open class Request(action: String) : HashMap<String, Any?>() {
        init {
            put("action", action)
        }
    }

    private open class ChangeRequest(action: String, bookmark: Bookmark, bookHash: String) : Request(action) {
        init {
            val bmk = HashMap<String, Any?>()
            bmk["book_hash"] = bookHash
            bmk["uid"] = bookmark.uid!!
            bmk["version_uid"] = bookmark.getVersionUid()
            bmk["style_id"] = bookmark.getStyleId()
            bmk["text"] = bookmark.getText()
            bmk["original_text"] = bookmark.getOriginalText()
            bmk["model_id"] = bookmark.modelId
            bmk["para_start"] = bookmark.paragraphIndex
            bmk["elmt_start"] = bookmark.elementIndex
            bmk["char_start"] = bookmark.charIndex
            bmk["para_end"] = bookmark.getEnd()!!.paragraphIndex
            bmk["elmt_end"] = bookmark.getEnd()!!.elementIndex
            bmk["char_end"] = bookmark.getEnd()!!.charIndex
            bmk["creation_timestamp"] = bookmark.getTimestamp(Bookmark.DateType.Creation)
            bmk["modification_timestamp"] = bookmark.getTimestamp(Bookmark.DateType.Modification)
            bmk["access_timestamp"] = bookmark.getTimestamp(Bookmark.DateType.Access)

            put("bookmark", bmk)
        }
    }

    private class AddRequest(bookmark: Bookmark, bookHash: String) : ChangeRequest("add", bookmark, bookHash)
    private class UpdateRequest(bookmark: Bookmark, bookHash: String) : ChangeRequest("update", bookmark, bookHash)

    private class DeleteRequest(uid: String) : Request("delete") {
        init {
            put("uid", uid)
        }
    }

    private fun ids(bmks: List<Bookmark>): List<String> {
        val uids = ArrayList<String>(bmks.size)
        for (b in bmks) {
            uids.add(b.uid!!)
        }
        return uids
    }

    private fun getInt(data: Map<String, Any?>, key: String): Int = (data[key] as Long).toInt()

    private fun fullRequestData(uids: List<String>): Map<String, Any?> {
        val requestData = HashMap<String, Any?>()
        requestData["uids"] = uids
        requestData["timestamp"] = System.currentTimeMillis()
        return requestData
    }

    private fun bookmarkFromData(id: Long, data: Map<String, Any?>, bookId: Long, bookTitle: String): Bookmark {
        return Bookmark(
            id, data["uid"] as String, data["version_uid"] as String?,
            bookId, bookTitle,
            data["text"] as String,
            data["original_text"] as String?,
            data["creation_timestamp"] as Long,
            data["modification_timestamp"] as Long?,
            data["access_timestamp"] as Long?,
            data["model_id"] as String?,
            getInt(data, "para_start"), getInt(data, "elmt_start"), getInt(data, "char_start"),
            getInt(data, "para_end"), getInt(data, "elmt_end"), getInt(data, "char_end"),
            true,
            getInt(data, "style_id")
        )
    }

    private fun newBookmarkFromData(data: Map<String, Any?>, booksByHash: BooksByHash): Bookmark? {
        val book = booksByHash.getBook(data["book_hash"] as String) ?: return null
        return bookmarkFromData(-1, data, book.id, book.title ?: "")
    }

    private fun bookmarkToUpdate(data: Map<String, Any?>, bookmarksMap: Map<String, Bookmark>): Bookmark? {
        val oldBookmark = bookmarksMap[data["uid"] as String] ?: return null
        return bookmarkFromData(oldBookmark.id, data, oldBookmark.bookId, oldBookmark.bookTitle)
    }
}
