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

package org.geometerplus.fbreader.network.sync

import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.fbreader.fbreader.options.SyncOptions
import org.geometerplus.zlibrary.core.options.Config
import org.geometerplus.zlibrary.core.options.ZLIntegerOption
import org.geometerplus.zlibrary.core.options.ZLStringListOption
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.json.simple.JSONValue

class SyncData {

    private val generation = ZLIntegerOption("SyncData", "Generation", -1)
    private val currentBookHash = ZLStringOption("SyncData", "CurrentBookHash", "")
    private val currentBookTimestamp = ZLStringOption("SyncData", "CurrentBookTimestamp", "")
    private val serverBook = ServerBook()

    private fun position2Map(pos: ZLTextFixedPosition.WithTimestamp): Map<String, Any> {
        return mapOf(
            "para" to pos.paragraphIndex,
            "elmt" to pos.elementIndex,
            "char" to pos.charIndex,
            "timestamp" to pos.timestamp
        )
    }

    private fun map2Position(map: Map<String, Any>): ZLTextFixedPosition.WithTimestamp {
        return ZLTextFixedPosition.WithTimestamp(
            (map["para"] as Long).toInt(),
            (map["elmt"] as Long).toInt(),
            (map["char"] as Long).toInt(),
            map["timestamp"] as Long
        )
    }

    private fun positionMap(collection: IBookCollection<Book>, book: Book?): Map<String, Any>? {
        if (book == null) return null
        val pos = collection.getStoredPosition(book.getId()) ?: return null
        return position2Map(pos)
    }

    fun data(collection: IBookCollection<Book>): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["generation"] = generation.value
        map["timestamp"] = System.currentTimeMillis()

        val currentBook = collection.getRecentBook(0)
        if (currentBook != null) {
            val oldHash = currentBookHash.value
            val newHash = collection.getHash(currentBook, true)
            if (newHash != null && newHash != oldHash) {
                currentBookHash.value = newHash
                if (oldHash.isNotEmpty()) {
                    currentBookTimestamp.value = System.currentTimeMillis().toString()
                    serverBook.reset()
                }
            }
            val currentBookHashValue = newHash ?: oldHash

            val currentBookMap = mutableMapOf<String, Any>()
            currentBookMap["hash"] = currentBookHashValue
            currentBookMap["title"] = currentBook.getTitle() ?: ""
            try {
                currentBookMap["timestamp"] = currentBookTimestamp.value.toLong()
            } catch (e: Exception) {
            }
            map["currentbook"] = currentBookMap

            val lst = mutableListOf<Map<String, Any>>()
            if (positionOption(currentBookHashValue).value.isEmpty()) {
                positionMap(collection, currentBook)?.let { posMap ->
                    val mutablePosMap = posMap.toMutableMap()
                    mutablePosMap["hash"] = currentBookHashValue
                    lst.add(mutablePosMap)
                }
            }
            if (currentBookHashValue != oldHash && positionOption(oldHash).value.isEmpty()) {
                positionMap(collection, collection.getBookByHash(oldHash))?.let { posMap ->
                    val mutablePosMap = posMap.toMutableMap()
                    mutablePosMap["hash"] = oldHash
                    lst.add(mutablePosMap)
                }
            }
            if (lst.isNotEmpty()) {
                map["positions"] = lst
            }
        }

        System.err.println("DATA = $map")
        return map
    }

    fun updateFromServer(data: Map<String, Any>): Boolean {
        System.err.println("RESPONSE = $data")
        generation.value = (data["generation"] as Long).toInt()

        @Suppress("UNCHECKED_CAST")
        val positions = data["positions"] as? List<Map<String, Any>>
        if (positions != null) {
            for (map in positions) {
                val pos = map2Position(map)
                @Suppress("UNCHECKED_CAST")
                for (hash in map["all_hashes"] as List<String>) {
                    savePosition(hash, pos)
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        serverBook.init(data["currentbook"] as? Map<String, Any>)

        return data.size > 1
    }

    private fun positionOption(hash: String): ZLStringOption = ZLStringOption("SyncData", "Pos:$hash", "")

    private fun savePosition(hash: String, pos: ZLTextFixedPosition.WithTimestamp?) {
        positionOption(hash).value = pos?.let { JSONValue.toJSONString(position2Map(it)) } ?: ""
    }

    fun hasPosition(hash: String): Boolean = positionOption(hash).value.isNotEmpty()

    val serverBookInfo: ServerBookInfo?
        get() = serverBook.info

    fun getAndCleanPosition(hash: String): ZLTextFixedPosition.WithTimestamp? {
        val option = positionOption(hash)
        return try {
            @Suppress("UNCHECKED_CAST")
            map2Position(JSONValue.parse(option.value) as Map<String, Any>)
        } catch (t: Throwable) {
            null
        } finally {
            option.value = ""
        }
    }

    fun reset() {
        Config.Instance()?.removeGroup("SyncData")
    }

    class ServerBookInfo(
        @JvmField val hashes: List<String>,
        @JvmField val title: String,
        @JvmField val downloadUrl: String?,
        @JvmField val mimetype: String,
        @JvmField val thumbnailUrl: String?,
        @JvmField val size: Int
    )

    private class ServerBook {
        val hashes = ZLStringListOption("SyncData", "ServerBookHashes", emptyList(), ";")
        val title = ZLStringOption("SyncData", "ServerBookTitle", "")
        val downloadUrl = ZLStringOption("SyncData", "ServerBookDownloadUrl", "")
        val mimetype = ZLStringOption("SyncData", "ServerBookMimetype", "")
        val thumbnailUrl = ZLStringOption("SyncData", "ServerBookThumbnailUrl", "")
        val size = ZLIntegerOption("SyncData", "ServerBookSize", 0)

        private fun fullUrl(option: ZLStringOption): String? {
            val value = option.value
            return if (value.isNotEmpty()) SyncOptions.BASE_URL + value else null
        }

        fun init(book: Map<String, Any>?) {
            if (book == null) {
                reset()
            } else {
                @Suppress("UNCHECKED_CAST")
                hashes.value = book["all_hashes"] as List<String>
                title.value = book["title"] as String

                val downloadUrlValue = book["download_url"] as? String
                downloadUrl.value = downloadUrlValue ?: ""
                val mimetypeValue = book["mimetype"] as? String
                mimetype.value = mimetypeValue ?: ""
                val thumbnailUrlValue = book["thumbnail_url"] as? String
                thumbnailUrl.value = thumbnailUrlValue ?: ""
                val sizeValue = book["size"] as? Long
                size.value = sizeValue?.toInt() ?: 0
            }
        }

        fun reset() {
            hashes.value = emptyList()
            title.value = ""
            downloadUrl.value = ""
            mimetype.value = ""
            thumbnailUrl.value = ""
            size.value = 0
        }

        val info: ServerBookInfo?
            get() {
                val hashesList = hashes.value
                if (hashesList.isEmpty()) return null
                return ServerBookInfo(
                    hashesList,
                    title.value,
                    fullUrl(downloadUrl),
                    mimetype.value,
                    fullUrl(thumbnailUrl),
                    size.value
                )
            }
    }
}
