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

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import org.geometerplus.android.fbreader.api.FBReaderIntents
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow
import org.geometerplus.fbreader.book.AbstractBook
import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.BookEvent
import org.geometerplus.fbreader.book.BookQuery
import org.geometerplus.fbreader.book.BookUtil
import org.geometerplus.fbreader.book.Filter
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.fbreader.fbreader.options.SyncOptions
import org.geometerplus.fbreader.network.sync.SyncData
import org.geometerplus.zlibrary.core.network.JsonRequest
import org.geometerplus.zlibrary.core.network.JsonRequest2
import org.geometerplus.zlibrary.core.network.ZLNetworkAuthenticationException
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest
import org.geometerplus.zlibrary.core.options.Config
import org.geometerplus.zlibrary.ui.android.network.SQLiteCookieDatabase
import org.json.simple.JSONValue
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.Collections
import java.util.LinkedList

class SyncService : Service(), IBookCollection.Listener<Book> {

    private val myCollection = BookCollectionShadow()
    private val mySyncOptions = SyncOptions()
    private val mySyncData = SyncData()

    private val myBookUploadContext = SyncNetworkContext(this, mySyncOptions, mySyncOptions.uploadAllBooks)
    private val mySyncPositionsContext = SyncNetworkContext(this, mySyncOptions, mySyncOptions.positions)
    private val mySyncBookmarksContext = SyncNetworkContext(this, mySyncOptions, mySyncOptions.bookmarks)

    private val myQuickSynchroniser = object : Runnable {
        @Synchronized
        override fun run() {
            if (!mySyncOptions.enabled.value) {
                return
            }
            mySyncPositionsContext.reloadCookie()

            if (ourQuickSynchronizationThread == null) {
                ourQuickSynchronizationThread = Thread {
                    try {
                        syncPositions()
                        syncCustomShelves()
                        BookmarkSyncUtil.sync(mySyncBookmarksContext, myCollection)
                    } finally {
                        ourQuickSynchronizationThread = null
                    }
                }
                ourQuickSynchronizationThread!!.priority = Thread.MAX_PRIORITY
                ourQuickSynchronizationThread!!.start()
            }
        }
    }

    private val myQueue = Collections.synchronizedList(LinkedList<Book>())
    private val myHashesFromServer = Hashes()

    private val myStandardSynchroniser = object : Runnable {
        @Synchronized
        override fun run() {
            if (!mySyncOptions.enabled.value) {
                return
            }
            myBookUploadContext.reloadCookie()

            myCollection.addListener(this@SyncService)
            if (ourSynchronizationThread == null) {
                ourSynchronizationThread = Thread {
                    val start = System.currentTimeMillis()
                    var count = 0

                    val statusCounts = HashMap<Status, Int>()
                    try {
                        myHashesFromServer.clear()
                        var q = BookQuery(Filter.Empty(), 20)
                        while (true) {
                            val books = myCollection.books(q)
                            if (books.isEmpty()) {
                                break
                            }
                            for (b in books) {
                                addBook(b)
                            }
                            q = q.next()
                        }
                        var status: Status? = null
                        while (myQueue.isNotEmpty() && status != Status.AuthenticationError) {
                            val book = myQueue.removeAt(0)
                            ++count
                            status = uploadBookToServer(book)
                            status.label?.let { label ->
                                for (l in Status.AllLabels) {
                                    if (label == l) {
                                        book.addNewLabel(l)
                                    } else {
                                        book.removeLabel(l)
                                    }
                                }
                                myCollection.saveBook(book)
                            }
                            val sc = statusCounts[status]
                            statusCounts[status] = (sc ?: 0) + 1
                        }
                    } finally {
                        log("SYNCHRONIZATION FINISHED IN ${System.currentTimeMillis() - start}msecs")
                        log("TOTAL BOOKS PROCESSED: $count")
                        for (value in Status.values()) {
                            log("STATUS $value: ${statusCounts[value]}")
                        }
                        ourSynchronizationThread = null
                    }
                }
                ourSynchronizationThread!!.priority = Thread.MIN_PRIORITY
                ourSynchronizationThread!!.start()
            }
        }
    }

    private fun syncIntent(): PendingIntent {
        return PendingIntent.getService(
            this, 0, Intent(this, this::class.java).setAction(FBReaderIntents.Action.SYNC_SYNC), 0
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: FBReaderIntents.Action.SYNC_SYNC
        if (FBReaderIntents.Action.SYNC_START == action) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(syncIntent())

            val config = Config.Instance()
            config?.runOnConnect {
                config.requestAllValuesForGroup("Sync")
                config.requestAllValuesForGroup("SyncData")

                if (!mySyncOptions.enabled.value) {
                    log("disabled")
                    return@runOnConnect
                }
                log("enabled")
                alarmManager.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime(),
                    AlarmManager.INTERVAL_HOUR,
                    syncIntent()
                )
                SQLiteCookieDatabase.init(this@SyncService)
                myCollection.bindToService(this@SyncService, myQuickSynchroniser)
            }
        } else if (FBReaderIntents.Action.SYNC_STOP == action) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(syncIntent())
            log("stopped")
            stopSelf()
        } else if (FBReaderIntents.Action.SYNC_SYNC == action) {
            SQLiteCookieDatabase.init(this)
            myCollection.bindToService(this, myQuickSynchroniser)
            myCollection.bindToService(this, myStandardSynchroniser)
        } else if (FBReaderIntents.Action.SYNC_QUICK_SYNC == action) {
            log("quick sync")
            SQLiteCookieDatabase.init(this)
            myCollection.bindToService(this, myQuickSynchroniser)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun addBook(book: Book) {
        if (BookUtil.fileByBook(book).physicalFile != null) {
            myQueue.add(book)
        }
    }

    @Synchronized
    private fun initHashTables() {
        if (myHashesFromServer.initialised) {
            return
        }

        try {
            myBookUploadContext.reloadCookie()
            val pageSize = 500
            val data = HashMap<String, String>()
            data["page_size"] = pageSize.toString()
            var pageNo = 0
            while (!myHashesFromServer.initialised) {
                data["page_no"] = pageNo.toString()
                myBookUploadContext.perform(object : PostRequest("all.hashes.paged", data) {
                    override fun processResponse(response: Any?) {
                        @Suppress("UNCHECKED_CAST")
                        val map = response as Map<String, List<String>>
                        val actualHashes = map["actual"]
                        val deletedHashes = map["deleted"]
                        myHashesFromServer.addAll(actualHashes, deletedHashes)
                        if ((actualHashes?.size ?: 0) < pageSize && (deletedHashes?.size ?: 0) < pageSize) {
                            myHashesFromServer.initialised = true
                        }
                    }
                })
                log("RECEIVED: $myHashesFromServer")
                pageNo++
            }
        } catch (e: SynchronizationDisabledException) {
            myHashesFromServer.clear()
            throw e
        } catch (e: Exception) {
            myHashesFromServer.clear()
            e.printStackTrace()
        }
    }

    private fun uploadBookToServer(book: Book): Status {
        return try {
            uploadBookToServerInternal(book)
        } catch (e: SynchronizationDisabledException) {
            Status.SynchronizationDisabled
        }
    }

    private fun uploadBookToServerInternal(book: Book): Status {
        val file = BookUtil.fileByBook(book).physicalFile!!.javaFile()
        val hash = myCollection.getHash(book, false)
        val force = book.hasLabel(AbstractBook.SYNC_TOSYNC_LABEL)
        if (hash == null) {
            return Status.HashNotComputed
        } else if (hash in myHashesFromServer.actual) {
            return Status.AlreadyUploaded
        } else if (!force && hash in myHashesFromServer.actual) {
            return Status.ToBeDeleted
        } else if (!force && book.hasLabel(AbstractBook.SYNC_FAILURE_LABEL)) {
            return Status.FailedPreviuousTime
        }
        if (file.length() > 120 * 1024 * 1024) {
            return Status.Failure
        }

        initHashTables()

        val result = HashMap<String, Any?>()
        val verificationRequest = object : PostRequest("book.status.by.hash", mapOf("sha1" to hash)) {
            override fun processResponse(response: Any?) {
                @Suppress("UNCHECKED_CAST")
                result.putAll(response as Map<String, Any?>)
            }
        }
        try {
            myBookUploadContext.perform(verificationRequest)
        } catch (e: ZLNetworkAuthenticationException) {
            e.printStackTrace()
            return Status.AuthenticationError
        } catch (e: ZLNetworkException) {
            e.printStackTrace()
            return Status.ServerError
        }

        val csrfToken = myBookUploadContext.getCookieValue(SyncOptions.DOMAIN, "csrftoken")
        try {
            val status = result["status"] as String?
            if ((force && status != "found") || status == "not found") {
                try {
                    val uploadRequest = UploadRequest(file, book, hash)
                    uploadRequest.addHeader("Referer", verificationRequest.url)
                    uploadRequest.addHeader("X-CSRFToken", csrfToken)
                    myBookUploadContext.perform(uploadRequest)
                    return uploadRequest.result
                } catch (e: ZLNetworkAuthenticationException) {
                    e.printStackTrace()
                    return Status.AuthenticationError
                } catch (e: ZLNetworkException) {
                    e.printStackTrace()
                    return Status.ServerError
                }
            } else {
                @Suppress("UNCHECKED_CAST")
                val hashes = result["hashes"] as List<String>?
                if (status == "found") {
                    myHashesFromServer.addAll(hashes, null)
                    return Status.AlreadyUploaded
                } else {
                    myHashesFromServer.addAll(null, hashes)
                    return Status.ToBeDeleted
                }
            }
        } catch (e: Exception) {
            log("UNEXPECTED RESPONSE: $result")
            return Status.ServerError
        }
    }

    private fun syncPositions() {
        try {
            mySyncPositionsContext.perform(object : JsonRequest2(
                SyncOptions.BASE_URL + "sync/position.exchange", mySyncData.data(myCollection)
            ) {
                override fun processResponse(response: Any?) {
                    @Suppress("UNCHECKED_CAST")
                    if (mySyncData.updateFromServer(response as Map<String, Any>)) {
                        sendBroadcast(Intent(FBReaderIntents.Event.SYNC_UPDATED))
                    }
                }
            })
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun syncCustomShelves() {
    }

    override fun onDestroy() {
        myCollection.removeListener(this)
        myCollection.unbind()
        super.onDestroy()
    }

    override fun onBookEvent(event: BookEvent, book: Book?) {
        when (event) {
            BookEvent.Added -> book?.let { addBook(it) }
            BookEvent.Opened -> SyncOperations.quickSync(this, mySyncOptions)
            else -> {}
        }
    }

    override fun onBuildEvent(status: IBookCollection.Status) {
    }

    private enum class Status(val label: String?) {
        AlreadyUploaded(AbstractBook.SYNCHRONISED_LABEL),
        Uploaded(AbstractBook.SYNCHRONISED_LABEL),
        ToBeDeleted(AbstractBook.SYNC_DELETED_LABEL),
        Failure(AbstractBook.SYNC_FAILURE_LABEL),
        AuthenticationError(null),
        ServerError(null),
        SynchronizationDisabled(null),
        FailedPreviuousTime(null),
        HashNotComputed(null);

        companion object {
            val AllLabels = listOf(
                AbstractBook.SYNCHRONISED_LABEL,
                AbstractBook.SYNC_FAILURE_LABEL,
                AbstractBook.SYNC_DELETED_LABEL,
                AbstractBook.SYNC_TOSYNC_LABEL
            )
        }
    }

    private class Hashes {
        val actual = HashSet<String>()
        val deleted = HashSet<String>()
        @Volatile var initialised = false

        fun clear() {
            actual.clear()
            deleted.clear()
            initialised = false
        }

        fun addAll(actual: Collection<String>?, deleted: Collection<String>?) {
            actual?.let { this.actual.addAll(it) }
            deleted?.let { this.deleted.addAll(it) }
        }

        override fun toString(): String {
            return String.format(
                "%s/%s HASHES (%s)",
                actual.size,
                deleted.size,
                if (initialised) "complete" else "partial"
            )
        }
    }

    private abstract class PostRequest(app: String, data: Map<String, String>?) : JsonRequest(SyncOptions.BASE_URL + "app/" + app) {
        init {
            data?.forEach { (key, value) -> addPostParameter(key, value) }
        }
    }

    private inner class UploadRequest(file: File, private val myBook: Book, private val myHash: String) : ZLNetworkRequest.FileUpload(SyncOptions.BASE_URL + "app/book.upload", file, false) {
        var result: Status = Status.Failure

        @Throws(IOException::class, ZLNetworkException::class)
        override fun handleStream(stream: java.io.InputStream, length: Int) {
            val response = JSONValue.parse(InputStreamReader(stream))
            var id: String? = null
            var hashes: List<String>? = null
            var error: String? = null
            var code: String? = null
            try {
                @Suppress("UNCHECKED_CAST")
                val responseList = response as List<Map<String, Any?>>
                if (responseList.size == 1) {
                    @Suppress("UNCHECKED_CAST")
                    val resultMap = responseList[0]["result"] as Map<String, Any?>?
                    resultMap?.let {
                        id = it["id"] as String?
                        @Suppress("UNCHECKED_CAST")
                        hashes = it["hashes"] as List<String>?
                        error = it["error"] as String?
                        code = it["code"] as String?
                    }
                }
            } catch (e: Exception) {
                // ignore
            }

            if (hashes != null && hashes!!.isNotEmpty()) {
                myHashesFromServer.addAll(hashes, null)
                if (myHash !in hashes!!) {
                    myCollection.setHash(myBook, hashes!![0])
                }
            }
            if (error != null) {
                log("UPLOAD FAILURE: $error")
                if ("ALREADY_UPLOADED" == code) {
                    result = Status.AlreadyUploaded
                }
            } else if (id != null) {
                log("UPLOADED SUCCESSFULLY: $id")
                result = Status.Uploaded
            } else {
                log("UNEXPECED RESPONSE: $response")
            }
        }
    }

    companion object {
        @Volatile private var ourSynchronizationThread: Thread? = null
        @Volatile private var ourQuickSynchronizationThread: Thread? = null

        private fun log(message: String) {
            Log.d("FBReader.Sync", message)
        }
    }
}
