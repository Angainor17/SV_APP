/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader.libraryService

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDoneException
import android.database.sqlite.SQLiteStatement
import org.geometerplus.android.util.SQLiteUtil
import org.geometerplus.fbreader.book.Author
import org.geometerplus.fbreader.book.Bookmark
import org.geometerplus.fbreader.book.BookmarkQuery
import org.geometerplus.fbreader.book.BooksDatabase
import org.geometerplus.fbreader.book.DbBook
import org.geometerplus.fbreader.book.FileInfo
import org.geometerplus.fbreader.book.FileInfoSet
import org.geometerplus.fbreader.book.HighlightingStyle
import org.geometerplus.fbreader.book.Label
import org.geometerplus.fbreader.book.SeriesInfo
import org.geometerplus.fbreader.book.Tag
import org.geometerplus.fbreader.book.UID
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.options.Config
import org.geometerplus.zlibrary.core.options.ZLIntegerOption
import org.geometerplus.zlibrary.core.util.RationalNumber
import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextPosition
import java.util.LinkedList
import java.util.TreeSet
import java.util.UUID

internal class SQLiteBooksDatabase(context: Context) : BooksDatabase() {
    private val myDatabase: SQLiteDatabase = context.openOrCreateDatabase("books.db", Context.MODE_PRIVATE, null)
    private val myStatements = HashMap<String, SQLiteStatement>()
    private val myIdByTag = HashMap<Tag, Long>()
    private val myTagById = HashMap<Long, Tag>()
    private var myTagCacheIsInitialized = false

    init {
        migrate()
    }

    fun finalize() {
        myDatabase.close()
    }

    override fun executeAsTransaction(actions: Runnable) {
        var transactionStarted = false
        try {
            myDatabase.beginTransaction()
            transactionStarted = true
        } catch (t: Throwable) {
        }
        try {
            actions.run()
            if (transactionStarted) {
                myDatabase.setTransactionSuccessful()
            }
        } finally {
            if (transactionStarted) {
                myDatabase.endTransaction()
            }
        }
    }

    private fun migrate() {
        val version = myDatabase.version
        val currentVersion = 40
        if (version >= currentVersion) {
            return
        }

        myDatabase.beginTransaction()

        when (version) {
            0 -> createTables()
            1 -> updateTables1()
            2 -> updateTables2()
            3 -> updateTables3()
            4 -> updateTables4()
            5 -> updateTables5()
            6 -> updateTables6()
            7 -> updateTables7()
            8 -> updateTables8()
            9 -> updateTables9()
            10 -> updateTables10()
            11 -> updateTables11()
            12 -> updateTables12()
            13 -> updateTables13()
            14 -> updateTables14()
            15 -> updateTables15()
            16 -> updateTables16()
            17 -> updateTables17()
            18 -> updateTables18()
            19 -> updateTables19()
            20 -> updateTables20()
            21 -> updateTables21()
            22 -> updateTables22()
            23 -> updateTables23()
            24 -> updateTables24()
            25 -> updateTables25()
            26 -> updateTables26()
            27 -> updateTables27()
            28 -> updateTables28()
            29 -> updateTables29()
            30 -> updateTables30()
            31 -> updateTables31()
            32 -> updateTables32()
            33 -> updateTables33()
            34 -> updateTables34()
            35 -> updateTables35()
            36 -> updateTables36()
            37 -> updateTables37()
            38 -> updateTables38()
            39 -> updateTables39()
        }
        myDatabase.setTransactionSuccessful()
        myDatabase.setVersion(currentVersion)
        myDatabase.endTransaction()

        myDatabase.execSQL("VACUUM")
    }

    override fun getOptionValue(name: String): String? {
        val cursor = myDatabase.rawQuery(
            "SELECT value FROM Options WHERE name=?", arrayOf(name)
        )
        try {
            return if (cursor.moveToNext()) cursor.getString(0) else null
        } finally {
            cursor.close()
        }
    }

    override fun setOptionValue(name: String, value: String) {
        val statement = get(
            "INSERT OR REPLACE INTO Options (name,value) VALUES (?,?)"
        )
        synchronized(statement) {
            SQLiteUtil.bindString(statement, 1, name)
            SQLiteUtil.bindString(statement, 2, value)
            statement.execute()
        }
    }

    override fun loadBook(bookId: Long): DbBook? {
        var book: DbBook? = null
        val cursor = myDatabase.rawQuery("SELECT file_id,title,encoding,language FROM Books WHERE book_id = $bookId", null)
        if (cursor.moveToNext()) {
            book = createBook(
                bookId, cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3)
            )
        }
        cursor.close()
        return book
    }

    override fun loadBookByFile(fileId: Long, file: ZLFile): DbBook? {
        if (fileId == -1L) {
            return null
        }
        var book: DbBook? = null
        val cursor = myDatabase.rawQuery("SELECT book_id,title,encoding,language FROM Books WHERE file_id = $fileId", null)
        if (cursor.moveToNext()) {
            book = createBook(
                cursor.getLong(0), file, cursor.getString(1), cursor.getString(2), cursor.getString(3)
            )
        }
        cursor.close()
        return book
    }

    private fun initTagCache() {
        if (myTagCacheIsInitialized) {
            return
        }
        myTagCacheIsInitialized = true

        val cursor = myDatabase.rawQuery("SELECT tag_id,parent_id,name FROM Tags ORDER BY tag_id", null)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            if (myTagById[id] == null) {
                val tag = Tag.getTag(myTagById[cursor.getLong(1)], cursor.getString(2))
                myIdByTag[tag] = id
                myTagById[id] = tag
            }
        }
        cursor.close()
    }

    override fun loadBooks(infos: FileInfoSet, existing: Boolean): Map<Long, DbBook> {
        var cursor = myDatabase.rawQuery(
            "SELECT book_id,file_id,title,encoding,language FROM Books WHERE `exists` = ${if (existing) 1 else 0}", null
        )
        val booksById = HashMap<Long, DbBook>()
        val booksByFileId = HashMap<Long, DbBook>()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val fileId = cursor.getLong(1)
            val book = createBook(
                id, infos.getFile(fileId), cursor.getString(2), cursor.getString(3), cursor.getString(4)
            )
            if (book != null) {
                booksById[id] = book
                booksByFileId[fileId] = book
            }
        }
        cursor.close()

        initTagCache()

        cursor = myDatabase.rawQuery(
            "SELECT author_id,name,sort_key FROM Authors", null
        )
        val authorById = HashMap<Long, Author>()
        while (cursor.moveToNext()) {
            authorById[cursor.getLong(0)] = Author(cursor.getString(1), cursor.getString(2))
        }
        cursor.close()

        cursor = myDatabase.rawQuery(
            "SELECT book_id,author_id FROM BookAuthor ORDER BY author_index", null
        )
        while (cursor.moveToNext()) {
            val book = booksById[cursor.getLong(0)]
            if (book != null) {
                val author = authorById[cursor.getLong(1)]
                if (author != null) {
                    addAuthor(book, author)
                }
            }
        }
        cursor.close()

        cursor = myDatabase.rawQuery("SELECT book_id,tag_id FROM BookTag", null)
        while (cursor.moveToNext()) {
            val book = booksById[cursor.getLong(0)]
            val tag = getTagById(cursor.getLong(1))
            if (book != null && tag != null) {
                addTag(book, tag)
            }
        }
        cursor.close()

        cursor = myDatabase.rawQuery(
            "SELECT series_id,name FROM Series", null
        )
        val seriesById = HashMap<Long, String>()
        while (cursor.moveToNext()) {
            seriesById[cursor.getLong(0)] = cursor.getString(1)
        }
        cursor.close()

        cursor = myDatabase.rawQuery(
            "SELECT book_id,series_id,book_index FROM BookSeries", null
        )
        while (cursor.moveToNext()) {
            val book = booksById[cursor.getLong(0)]
            if (book != null) {
                val series = seriesById[cursor.getLong(1)]
                if (series != null) {
                    setSeriesInfo(book, series, cursor.getString(2))
                }
            }
        }
        cursor.close()

        cursor = myDatabase.rawQuery(
            "SELECT book_id,type,uid FROM BookUid", null
        )
        while (cursor.moveToNext()) {
            val book = booksById[cursor.getLong(0)]
            if (book != null) {
                book.addUid(cursor.getString(1), cursor.getString(2))
            }
        }
        cursor.close()

        cursor = myDatabase.rawQuery(
            "SELECT BookLabel.book_id,Labels.name,BookLabel.uid FROM Labels" +
                    " INNER JOIN BookLabel ON BookLabel.label_id=Labels.label_id",
            null
        )
        while (cursor.moveToNext()) {
            val book = booksById[cursor.getLong(0)]
            if (book != null) {
                book.addLabel(Label(cursor.getString(2), cursor.getString(1)))
            }
        }
        cursor.close()

        cursor = myDatabase.rawQuery(
            "SELECT book_id,numerator,denominator FROM BookReadingProgress",
            null
        )
        while (cursor.moveToNext()) {
            val book = booksById[cursor.getLong(0)]
            if (book != null) {
                book.setProgress(RationalNumber.create(cursor.getLong(1), cursor.getLong(2)))
            }
        }
        cursor.close()

        cursor = myDatabase.rawQuery(
            "SELECT book_id FROM Bookmarks WHERE visible = 1 GROUP by book_id",
            null
        )
        while (cursor.moveToNext()) {
            val book = booksById[cursor.getLong(0)]
            if (book != null) {
                book.hasBookmark = true
            }
        }
        cursor.close()

        return booksByFileId
    }

    override fun setExistingFlag(books: Collection<DbBook>, flag: Boolean) {
        if (books.isEmpty()) {
            return
        }
        val bookSet = StringBuilder("(")
        var first = true
        for (b in books) {
            if (first) {
                first = false
            } else {
                bookSet.append(",")
            }
            bookSet.append(b.id)
        }
        bookSet.append(")")
        myDatabase.execSQL(
            "UPDATE Books SET `exists` = ${if (flag) 1 else 0} WHERE book_id IN $bookSet"
        )
    }

    internal override fun updateBookInfo(bookId: Long, fileId: Long, encoding: String?, language: String?, title: String?) {
        val statement = get(
            "UPDATE OR IGNORE Books SET file_id=?, encoding=?, language=?, title=? WHERE book_id=?"
        )
        synchronized(statement) {
            statement.bindLong(1, fileId)
            SQLiteUtil.bindString(statement, 2, encoding)
            SQLiteUtil.bindString(statement, 3, language)
            SQLiteUtil.bindString(statement, 4, title)
            statement.bindLong(5, bookId)
            statement.execute()
        }
    }

    internal override fun insertBookInfo(file: ZLFile, encoding: String?, language: String?, title: String?): Long {
        val statement = get(
            "INSERT OR IGNORE INTO Books (encoding,language,title,file_id) VALUES (?,?,?,?)"
        )
        synchronized(statement) {
            SQLiteUtil.bindString(statement, 1, encoding)
            SQLiteUtil.bindString(statement, 2, language)
            SQLiteUtil.bindString(statement, 3, title)
            val infoSet = FileInfoSet(this, file)
            statement.bindLong(4, infoSet.getId(file))
            return statement.executeInsert()
        }
    }

    override fun deleteAllBookAuthors(bookId: Long) {
        val statement = get("DELETE FROM BookAuthor WHERE book_id=?")
        synchronized(statement) {
            statement.bindLong(1, bookId)
            statement.execute()
        }
    }

    override fun saveBookAuthorInfo(bookId: Long, index: Long, author: Author) {
        val getAuthorIdStatement = get(
            "SELECT author_id FROM Authors WHERE name=? AND sort_key=?"
        )
        val insertAuthorStatement = get(
            "INSERT OR IGNORE INTO Authors (name,sort_key) VALUES (?,?)"
        )
        val insertBookAuthorStatement = get(
            "INSERT OR REPLACE INTO BookAuthor (book_id,author_id,author_index) VALUES (?,?,?)"
        )

        var authorId: Long
        try {
            getAuthorIdStatement.bindString(1, author.displayName)
            getAuthorIdStatement.bindString(2, author.sortKey)
            authorId = getAuthorIdStatement.simpleQueryForLong()
        } catch (e: SQLException) {
            insertAuthorStatement.bindString(1, author.displayName)
            insertAuthorStatement.bindString(2, author.sortKey)
            authorId = insertAuthorStatement.executeInsert()
        }
        insertBookAuthorStatement.bindLong(1, bookId)
        insertBookAuthorStatement.bindLong(2, authorId)
        insertBookAuthorStatement.bindLong(3, index)
        insertBookAuthorStatement.execute()
    }

    override fun listAuthors(bookId: Long): MutableList<Author>? {
        val cursor = myDatabase.rawQuery("SELECT Authors.name,Authors.sort_key FROM BookAuthor INNER JOIN Authors ON Authors.author_id = BookAuthor.author_id WHERE BookAuthor.book_id = ?", arrayOf(bookId.toString()))
        if (!cursor.moveToNext()) {
            cursor.close()
            return null
        }
        val list = ArrayList<Author>()
        do {
            list.add(Author(cursor.getString(0), cursor.getString(1)))
        } while (cursor.moveToNext())
        cursor.close()
        return list
    }

    private fun getTagId(tag: Tag): Long {
        val getTagIdStatement = get(
            "SELECT tag_id FROM Tags WHERE parent_id=? AND name=?"
        )
        myIdByTag[tag]?.let { return it }

        if (tag.parent != null) {
            getTagIdStatement.bindLong(1, getTagId(tag.parent!!))
        } else {
            getTagIdStatement.bindNull(1)
        }
        getTagIdStatement.bindString(2, tag.Name)
        var id: Long
        try {
            id = getTagIdStatement.simpleQueryForLong()
        } catch (e: SQLException) {
            val createTagIdStatement = get(
                "INSERT OR IGNORE INTO Tags (parent_id,name) VALUES (?,?)"
            )
            if (tag.parent != null) {
                createTagIdStatement.bindLong(1, getTagId(tag.parent!!))
            } else {
                createTagIdStatement.bindNull(1)
            }
            createTagIdStatement.bindString(2, tag.Name)
            id = createTagIdStatement.executeInsert()
        }
        myIdByTag[tag] = id
        myTagById[id] = tag
        return id
    }

    override fun deleteAllBookTags(bookId: Long) {
        val statement = get("DELETE FROM BookTag WHERE book_id=?")
        synchronized(statement) {
            statement.bindLong(1, bookId)
            statement.execute()
        }
    }

    override fun saveBookTagInfo(bookId: Long, tag: Tag) {
        val statement = get(
            "INSERT OR IGNORE INTO BookTag (book_id,tag_id) VALUES (?,?)"
        )
        synchronized(statement) {
            statement.bindLong(1, bookId)
            statement.bindLong(2, getTagId(tag))
            statement.execute()
        }
    }

    private fun getTagById(id: Long): Tag? {
        var tag = myTagById[id]
        if (tag == null) {
            val cursor = myDatabase.rawQuery("SELECT parent_id,name FROM Tags WHERE tag_id = ?", arrayOf(id.toString()))
            if (cursor.moveToNext()) {
                val parent = if (cursor.isNull(0)) null else getTagById(cursor.getLong(0))
                tag = Tag.getTag(parent, cursor.getString(1))
                myIdByTag[tag!!] = id
                myTagById[id] = tag
            }
            cursor.close()
        }
        return tag
    }

    override fun listTags(bookId: Long): MutableList<Tag>? {
        val cursor = myDatabase.rawQuery("SELECT Tags.tag_id FROM BookTag INNER JOIN Tags ON Tags.tag_id = BookTag.tag_id WHERE BookTag.book_id = ?", arrayOf(bookId.toString()))
        if (!cursor.moveToNext()) {
            cursor.close()
            return null
        }
        val list = ArrayList<Tag>()
        do {
            getTagById(cursor.getLong(0))?.let { list.add(it) }
        } while (cursor.moveToNext())
        cursor.close()
        return list
    }

    override fun listLabels(bookId: Long): MutableList<Label> {
        val cursor = myDatabase.rawQuery(
            "SELECT Labels.name,BookLabel.uid FROM Labels" +
                    " INNER JOIN BookLabel ON BookLabel.label_id=Labels.label_id" +
                    " WHERE BookLabel.book_id=?",
            arrayOf(bookId.toString())
        )
        val labels = LinkedList<Label>()
        while (cursor.moveToNext()) {
            labels.add(Label(cursor.getString(1), cursor.getString(0)))
        }
        cursor.close()
        return labels
    }

    override fun listLabels(): List<String> {
        val cursor = myDatabase.rawQuery(
            "SELECT DISTINCT(Labels.name) FROM Labels" +
                    " INNER JOIN BookLabel ON BookLabel.label_id=Labels.label_id" +
                    " INNER JOIN Books ON BookLabel.book_id=Books.book_id" +
                    " WHERE Books.`exists`=1",
            null
        )
        val names = LinkedList<String>()
        while (cursor.moveToNext()) {
            names.add(cursor.getString(0))
        }
        cursor.close()
        return names
    }

    override fun deleteAllBookUids(bookId: Long) {
        val statement = get("DELETE FROM BookUid WHERE book_id=?")
        synchronized(statement) {
            statement.bindLong(1, bookId)
            statement.execute()
        }
    }

    override fun saveBookUid(bookId: Long, uid: UID) {
        val statement = get(
            "INSERT OR IGNORE INTO BookUid (book_id,type,uid) VALUES (?,?,?)"
        )
        synchronized(statement) {
            statement.bindLong(1, bookId)
            statement.bindString(2, uid.type)
            statement.bindString(3, uid.id)
            statement.execute()
        }
    }

    override fun listUids(bookId: Long): MutableList<UID> {
        val list = ArrayList<UID>()
        val cursor = myDatabase.rawQuery("SELECT type,uid FROM BookUid WHERE book_id = ?", arrayOf(bookId.toString()))
        while (cursor.moveToNext()) {
            list.add(UID(cursor.getString(0), cursor.getString(1)))
        }
        cursor.close()
        return list
    }

    override fun bookIdByUid(uid: UID): Long? {
        var bookId: Long? = null
        val cursor = myDatabase.rawQuery("SELECT book_id FROM BookUid WHERE type = ? AND uid = ? LIMIT 1", arrayOf(uid.type, uid.id))
        if (cursor.moveToNext()) {
            bookId = cursor.getLong(0)
        }
        cursor.close()
        return bookId
    }

    override fun saveBookSeriesInfo(bookId: Long, seriesInfo: SeriesInfo?) {
        if (seriesInfo == null) {
            val statement = get("DELETE FROM BookSeries WHERE book_id=?")
            synchronized(statement) {
                statement.bindLong(1, bookId)
                statement.execute()
            }
        } else {
            var seriesId: Long
            try {
                val getSeriesIdStatement = get(
                    "SELECT series_id FROM Series WHERE name = ?"
                )
                synchronized(getSeriesIdStatement) {
                    getSeriesIdStatement.bindString(1, seriesInfo.series.getTitle())
                    seriesId = getSeriesIdStatement.simpleQueryForLong()
                }
            } catch (e: SQLException) {
                val insertSeriesStatement = get(
                    "INSERT OR IGNORE INTO Series (name) VALUES (?)"
                )
                synchronized(insertSeriesStatement) {
                    insertSeriesStatement.bindString(1, seriesInfo.series.getTitle())
                    seriesId = insertSeriesStatement.executeInsert()
                }
            }
            val insertBookSeriesStatement = get(
                "INSERT OR REPLACE INTO BookSeries (book_id,series_id,book_index) VALUES (?,?,?)"
            )
            synchronized(insertBookSeriesStatement) {
                insertBookSeriesStatement.bindLong(1, bookId)
                insertBookSeriesStatement.bindLong(2, seriesId)
                SQLiteUtil.bindString(
                    insertBookSeriesStatement, 3,
                    seriesInfo.index?.toPlainString()
                )
                insertBookSeriesStatement.execute()
            }
        }
    }

    override fun getSeriesInfo(bookId: Long): SeriesInfo? {
        val cursor = myDatabase.rawQuery("SELECT Series.name,BookSeries.book_index FROM BookSeries INNER JOIN Series ON Series.series_id = BookSeries.series_id WHERE BookSeries.book_id = ?", arrayOf(bookId.toString()))
        var info: SeriesInfo? = null
        if (cursor.moveToNext()) {
            info = SeriesInfo.createSeriesInfo(cursor.getString(0), cursor.getString(1))
        }
        cursor.close()
        return info
    }

    override fun removeFileInfo(fileId: Long) {
        if (fileId == -1L) {
            return
        }
        val statement = get("DELETE FROM Files WHERE file_id=?")
        synchronized(statement) {
            statement.bindLong(1, fileId)
            statement.execute()
        }
    }

    override fun saveFileInfo(fileInfo: FileInfo) {
        val id = fileInfo.id
        val statement: SQLiteStatement
        if (id == -1L) {
            statement = get(
                "INSERT OR IGNORE INTO Files (name,parent_id,size) VALUES (?,?,?)"
            )
        } else {
            statement = get(
                "UPDATE Files SET name=?, parent_id=?, size=? WHERE file_id=?"
            )
        }
        synchronized(statement) {
            statement.bindString(1, fileInfo.name)
            val parent = fileInfo.parent
            if (parent != null) {
                statement.bindLong(2, parent.id)
            } else {
                statement.bindNull(2)
            }
            val size = fileInfo.fileSize
            if (size != -1L) {
                statement.bindLong(3, size)
            } else {
                statement.bindNull(3)
            }
            if (id == -1L) {
                fileInfo.id = statement.executeInsert()
            } else {
                statement.bindLong(4, id)
                statement.execute()
            }
        }
    }

    override fun loadFileInfos(): Collection<FileInfo> {
        val cursor = myDatabase.rawQuery(
            "SELECT file_id,name,parent_id,size FROM Files", null
        )
        val infosById = HashMap<Long, FileInfo>()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val info = createFileInfo(id,
                cursor.getString(1),
                if (cursor.isNull(2)) null else infosById[cursor.getLong(2)]
            )
            if (!cursor.isNull(3)) {
                info.fileSize = cursor.getLong(3)
            }
            infosById[id] = info
        }
        cursor.close()
        return infosById.values
    }

    override fun loadFileInfos(file: ZLFile): Collection<FileInfo> {
        val fileStack = LinkedList<ZLFile>()
        var f: ZLFile? = file
        while (f != null) {
            fileStack.addFirst(f)
            f = f.parent
        }

        val infos = ArrayList<FileInfo>(fileStack.size)
        val parameters = arrayOfNulls<String>(1)
        var current: FileInfo? = null
        for (fileItem in fileStack) {
            parameters[0] = fileItem.longName
            val cursor = myDatabase.rawQuery(
                if (current == null) {
                    "SELECT file_id,size FROM Files WHERE name = ?"
                } else {
                    "SELECT file_id,size FROM Files WHERE parent_id = ${current.id} AND name = ?"
                },
                parameters
            )
            if (cursor.moveToNext()) {
                current = createFileInfo(cursor.getLong(0), parameters[0]!!, current)
                if (!cursor.isNull(1)) {
                    current.fileSize = cursor.getLong(1)
                }
                infos.add(current)
                cursor.close()
            } else {
                cursor.close()
                break
            }
        }

        return infos
    }

    override fun loadFileInfos(fileId: Long): Collection<FileInfo> {
        val infos = ArrayList<FileInfo>()
        var currentFileId = fileId
        while (currentFileId != -1L) {
            val cursor = myDatabase.rawQuery(
                "SELECT name,size,parent_id FROM Files WHERE file_id = $currentFileId", null
            )
            if (cursor.moveToNext()) {
                val info = createFileInfo(currentFileId, cursor.getString(0), null)
                if (!cursor.isNull(1)) {
                    info.fileSize = cursor.getLong(1)
                }
                infos.add(0, info)
                currentFileId = if (cursor.isNull(2)) -1 else cursor.getLong(2)
            } else {
                currentFileId = -1
            }
            cursor.close()
        }
        for (i in 1 until infos.size) {
            val oldInfo = infos[i]
            val newInfo = createFileInfo(oldInfo.id, oldInfo.name, infos[i - 1])
            newInfo.fileSize = oldInfo.fileSize
            infos[i] = newInfo
        }
        return infos
    }

    override fun addBookHistoryEvent(bookId: Long, event: Int) {
        val statement = get(
            "INSERT INTO BookHistory (book_id,timestamp,event) VALUES (?,?,?)"
        )
        synchronized(statement) {
            statement.bindLong(1, bookId)
            statement.bindLong(2, System.currentTimeMillis())
            statement.bindLong(3, event.toLong())
            statement.executeInsert()
        }
    }

    override fun removeBookHistoryEvents(bookId: Long, event: Int) {
        val statement = get(
            "DELETE FROM BookHistory WHERE book_id=? and event=?"
        )
        synchronized(statement) {
            statement.bindLong(1, bookId)
            statement.bindLong(2, event.toLong())
            statement.executeInsert()
        }
    }

    override fun loadRecentBookIds(event: Int, limit: Int): List<Long> {
        val cursor = myDatabase.rawQuery(
            "SELECT book_id FROM BookHistory WHERE event=? GROUP BY book_id ORDER BY timestamp DESC LIMIT ?",
            arrayOf(event.toString(), limit.toString())
        )
        val ids = LinkedList<Long>()
        while (cursor.moveToNext()) {
            ids.add(cursor.getLong(0))
        }
        cursor.close()
        return ids
    }

    override fun addLabel(bookId: Long, label: Label) {
        myDatabase.execSQL("INSERT OR IGNORE INTO Labels (name) VALUES (?)", arrayOf(label.name))
        val statement = get(
            "INSERT OR IGNORE INTO BookLabel(label_id,book_id,uid,timestamp)" +
                    " SELECT label_id,?,?,? FROM Labels WHERE name=?"
        )
        synchronized(statement) {
            statement.bindLong(1, bookId)
            statement.bindString(2, label.uid)
            statement.bindLong(3, System.currentTimeMillis())
            statement.bindString(4, label.name)
            statement.execute()
        }
    }

    override fun removeLabel(bookId: Long, label: Label) {
        val count = myDatabase.delete(
            "BookLabel",
            "book_id=? AND uid=?",
            arrayOf(bookId.toString(), label.uid)
        )

        if (count > 0) {
            val statement = get(
                "INSERT OR IGNORE INTO DeletedBookLabelIds (uid) VALUES (?)"
            )
            synchronized(statement) {
                statement.bindString(1, label.uid)
                statement.execute()
            }
        }
    }

    override fun hasVisibleBookmark(bookId: Long): Boolean {
        val cursor = myDatabase.rawQuery(
            "SELECT bookmark_id FROM Bookmarks WHERE book_id = $bookId" +
                    " AND visible = 1 LIMIT 1", null
        )
        val result = cursor.moveToNext()
        cursor.close()
        return result
    }

    override fun loadBookmarks(query: BookmarkQuery): List<Bookmark> {
        val list = LinkedList<Bookmark>()
        val sql = StringBuilder("SELECT")
            .append(" bm.bookmark_id,bm.uid,bm.version_uid,")
            .append("bm.book_id,b.title,bm.bookmark_text,bm.original_text,")
            .append("bm.creation_time,bm.modification_time,bm.access_time,")
            .append("bm.model_id,bm.paragraph,bm.word,bm.char,")
            .append("bm.end_paragraph,bm.end_word,bm.end_character,")
            .append("bm.style_id")
            .append(" FROM Bookmarks AS bm")
            .append(" INNER JOIN Books AS b ON b.book_id = bm.book_id")
            .append(" WHERE")
        if (query.book != null) {
            sql.append(" b.book_id = ${query.book!!.id} AND")
        }
        sql
            .append(" bm.visible = ${if (query.visible) 1 else 0}")
            .append(" ORDER BY bm.bookmark_id")
            .append(" LIMIT ${query.limit * query.page},${query.limit}")
        val cursor = myDatabase.rawQuery(sql.toString(), null)
        while (cursor.moveToNext()) {
            list.add(createBookmark(
                cursor.getLong(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getLong(3),
                cursor.getString(4),
                cursor.getString(5),
                if (cursor.isNull(6)) null else cursor.getString(6),
                cursor.getLong(7),
                if (cursor.isNull(8)) null else cursor.getLong(8),
                if (cursor.isNull(9)) null else cursor.getLong(9),
                cursor.getString(10),
                cursor.getLong(11).toInt(),
                cursor.getLong(12).toInt(),
                cursor.getLong(13).toInt(),
                cursor.getLong(14).toInt(),
                if (cursor.isNull(15)) -1 else cursor.getLong(15).toInt(),
                if (cursor.isNull(16)) -1 else cursor.getLong(16).toInt(),
                query.visible,
                cursor.getLong(17).toInt()
            ))
        }
        cursor.close()
        return list
    }

    override fun loadStyles(): List<HighlightingStyle> {
        val list = LinkedList<HighlightingStyle>()
        val sql = "SELECT style_id,timestamp,name,bg_color,fg_color FROM HighlightingStyle"
        val cursor = myDatabase.rawQuery(sql, null)
        while (cursor.moveToNext()) {
            val name = cursor.getString(2)
            val bgColor = cursor.getLong(3).toInt()
            val fgColor = cursor.getLong(4).toInt()
            list.add(createStyle(
                cursor.getLong(0).toInt(),
                cursor.getLong(1),
                if (name.isNotEmpty()) name else null,
                if (bgColor != -1) ZLColor(bgColor) else null,
                if (fgColor != -1) ZLColor(fgColor) else null
            ))
        }
        cursor.close()
        return list
    }

    override fun saveStyle(style: HighlightingStyle) {
        val statement = get(
            "INSERT OR REPLACE INTO HighlightingStyle (style_id,name,bg_color,fg_color,timestamp) VALUES (?,?,?,?,?)"
        )
        synchronized(statement) {
            statement.bindLong(1, style.id.toLong())
            val name = style.getNameOrNull()
            statement.bindString(2, name ?: "")
            val bgColor = style.getBackgroundColor()
            statement.bindLong(3, bgColor?.intValue()?.toLong() ?: -1)
            val fgColor = style.getForegroundColor()
            statement.bindLong(4, fgColor?.intValue()?.toLong() ?: -1)
            statement.bindLong(5, System.currentTimeMillis())
            statement.execute()
        }
    }

    private fun uid(bookmark: Bookmark): String {
        bookmark.uid?.let { return it }
        if (bookmark.id == -1L) {
            return UUID.randomUUID().toString()
        }

        val cursor = myDatabase.rawQuery(
            "SELECT uid FROM Bookmarks WHERE bookmark_id = ${bookmark.id}", null
        )
        try {
            if (cursor.moveToNext()) {
                return cursor.getString(0)
            }
        } finally {
            cursor.close()
        }

        return UUID.randomUUID().toString()
    }

    override fun saveBookmark(bookmark: Bookmark): Long {
        val statement: SQLiteStatement
        val bookmarkId = bookmark.id

        if (bookmarkId == -1L) {
            statement = get(
                "INSERT INTO Bookmarks (uid,version_uid,book_id,bookmark_text,original_text,creation_time,modification_time,access_time,model_id,paragraph,word,char,end_paragraph,end_word,end_character,visible,style_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
            )
        } else {
            statement = get(
                "UPDATE Bookmarks SET uid=?,version_uid=?,book_id=?,bookmark_text=?,original_text=?,creation_time=?,modification_time=?,access_time=?,model_id=?,paragraph=?,word=?,char=?,end_paragraph=?,end_word=?,end_character=?,visible=?,style_id=? WHERE bookmark_id=?"
            )
        }

        synchronized(statement) {
            var fieldCount = 0
            SQLiteUtil.bindString(statement, ++fieldCount, uid(bookmark))
            SQLiteUtil.bindString(statement, ++fieldCount, bookmark.getVersionUid())
            statement.bindLong(++fieldCount, bookmark.bookId)
            statement.bindString(++fieldCount, bookmark.getText())
            SQLiteUtil.bindString(statement, ++fieldCount, bookmark.getOriginalText())
            SQLiteUtil.bindLong(statement, ++fieldCount, bookmark.getTimestamp(Bookmark.DateType.Creation))
            SQLiteUtil.bindLong(statement, ++fieldCount, bookmark.getTimestamp(Bookmark.DateType.Modification))
            SQLiteUtil.bindLong(statement, ++fieldCount, bookmark.getTimestamp(Bookmark.DateType.Access))
            SQLiteUtil.bindString(statement, ++fieldCount, bookmark.modelId)
            statement.bindLong(++fieldCount, bookmark.paragraphIndex.toLong())
            statement.bindLong(++fieldCount, bookmark.elementIndex.toLong())
            statement.bindLong(++fieldCount, bookmark.charIndex.toLong())
            val end = bookmark.getEnd()
            if (end != null) {
                statement.bindLong(++fieldCount, end.paragraphIndex.toLong())
                statement.bindLong(++fieldCount, end.elementIndex.toLong())
                statement.bindLong(++fieldCount, end.charIndex.toLong())
            } else {
                statement.bindLong(++fieldCount, bookmark.length.toLong())
                statement.bindNull(++fieldCount)
                statement.bindNull(++fieldCount)
            }
            statement.bindLong(++fieldCount, if (bookmark.isVisible) 1 else 0)
            statement.bindLong(++fieldCount, bookmark.getStyleId().toLong())

            if (bookmarkId == -1L) {
                return statement.executeInsert()
            } else {
                statement.bindLong(++fieldCount, bookmarkId)
                statement.execute()
                return bookmarkId
            }
        }
    }

    override fun deleteBookmark(bookmark: Bookmark) {
        val uuid = uid(bookmark)
        var statement = get("DELETE FROM Bookmarks WHERE uid=?")
        synchronized(statement) {
            statement.bindString(1, uuid)
            statement.execute()
        }
        statement = get("INSERT OR IGNORE INTO DeletedBookmarkIds (uid) VALUES (?)")
        synchronized(statement) {
            statement.bindString(1, uuid)
            statement.execute()
        }
    }

    override fun deletedBookmarkUids(): List<String> {
        val cursor = myDatabase.rawQuery("SELECT uid FROM DeletedBookmarkIds", null)
        val uids = LinkedList<String>()
        while (cursor.moveToNext()) {
            uids.add(cursor.getString(0))
        }
        cursor.close()
        return uids
    }

    override fun purgeBookmarks(uids: List<String>) {
        val statement = get("DELETE FROM DeletedBookmarkIds WHERE uid=?")
        synchronized(statement) {
            for (u in uids) {
                statement.bindString(1, u)
                statement.execute()
            }
        }
    }

    override fun getStoredPosition(bookId: Long): ZLTextFixedPosition.WithTimestamp? {
        var position: ZLTextFixedPosition.WithTimestamp? = null
        val cursor = myDatabase.rawQuery(
            "SELECT paragraph,word,char,timestamp FROM BookState WHERE book_id = $bookId", null
        )
        if (cursor.moveToNext()) {
            position = ZLTextFixedPosition.WithTimestamp(
                cursor.getLong(0).toInt(),
                cursor.getLong(1).toInt(),
                cursor.getLong(2).toInt(),
                cursor.getLong(3)
            )
        }
        cursor.close()
        return position
    }

    override fun storePosition(bookId: Long, position: ZLTextPosition) {
        val statement = get(
            "INSERT OR REPLACE INTO BookState (book_id,paragraph,word,char,timestamp) VALUES (?,?,?,?,?)"
        )
        synchronized(statement) {
            statement.bindLong(1, bookId)
            statement.bindLong(2, position.paragraphIndex.toLong())
            statement.bindLong(3, position.elementIndex.toLong())
            statement.bindLong(4, position.charIndex.toLong())

            var timestamp = -1L
            if (position is ZLTextFixedPosition.WithTimestamp) {
                timestamp = position.timestamp
            }
            if (timestamp == -1L) {
                timestamp = System.currentTimeMillis()
            }
            statement.bindLong(5, timestamp)

            statement.execute()
        }
    }

    private fun deleteVisitedHyperlinks(bookId: Long) {
        val statement = get("DELETE FROM VisitedHyperlinks WHERE book_id=?")
        synchronized(statement) {
            statement.bindLong(1, bookId)
            statement.execute()
        }
    }

    override fun addVisitedHyperlink(bookId: Long, hyperlinkId: String) {
        val statement = get(
            "INSERT OR IGNORE INTO VisitedHyperlinks(book_id,hyperlink_id) VALUES (?,?)"
        )
        synchronized(statement) {
            statement.bindLong(1, bookId)
            statement.bindString(2, hyperlinkId)
            statement.execute()
        }
    }

    override fun loadVisitedHyperlinks(bookId: Long): Collection<String> {
        val links = TreeSet<String>()
        val cursor = myDatabase.rawQuery("SELECT hyperlink_id FROM VisitedHyperlinks WHERE book_id = ?", arrayOf(bookId.toString()))
        while (cursor.moveToNext()) {
            links.add(cursor.getString(0))
        }
        cursor.close()
        return links
    }

    override fun saveBookProgress(bookId: Long, progress: RationalNumber) {
        val statement = get(
            "INSERT OR REPLACE INTO BookReadingProgress (book_id,numerator,denominator) VALUES (?,?,?)"
        )
        synchronized(statement) {
            statement.bindLong(1, bookId)
            statement.bindLong(2, progress.numerator.toLong())
            statement.bindLong(3, progress.denominator.toLong())
            statement.execute()
        }
    }

    override fun getProgress(bookId: Long): RationalNumber? {
        val progress: RationalNumber?
        val cursor = myDatabase.rawQuery(
            "SELECT numerator,denominator FROM BookReadingProgress WHERE book_id=$bookId", null
        )
        if (cursor.moveToNext()) {
            progress = RationalNumber.create(cursor.getLong(0), cursor.getLong(1))
        } else {
            progress = null
        }
        cursor.close()
        return progress
    }

    override fun getHash(bookId: Long, lastModified: Long): String? {
        try {
            val statement = get(
                "SELECT hash FROM BookHash WHERE book_id=? AND timestamp>?"
            )
            synchronized(statement) {
                statement.bindLong(1, bookId)
                statement.bindLong(2, lastModified)
                try {
                    return statement.simpleQueryForString()
                } catch (e: SQLiteDoneException) {
                    return null
                }
            }
        } catch (t: Throwable) {
            throw NotAvailable()
        }
    }

    override fun setHash(bookId: Long, hash: String) {
        try {
            val statement = get(
                "INSERT OR REPLACE INTO BookHash (book_id,timestamp,hash) VALUES (?,?,?)"
            )
            synchronized(statement) {
                statement.bindLong(1, bookId)
                statement.bindLong(2, System.currentTimeMillis())
                statement.bindString(3, hash)
                statement.execute()
            }
        } catch (t: Throwable) {
            throw NotAvailable()
        }
    }

    override fun bookIdsByHash(hash: String): List<Long> {
        val cursor = myDatabase.rawQuery(
            "SELECT book_id FROM BookHash WHERE hash=?", arrayOf(hash)
        )
        val bookIds = LinkedList<Long>()
        while (cursor.moveToNext()) {
            bookIds.add(cursor.getLong(0))
        }
        cursor.close()
        return bookIds
    }

    override fun deleteBook(bookId: Long) {
        myDatabase.beginTransaction()
        myDatabase.execSQL("DELETE FROM BookHistory WHERE book_id=$bookId")
        myDatabase.execSQL("DELETE FROM BookHash WHERE book_id=$bookId")
        myDatabase.execSQL("DELETE FROM BookAuthor WHERE book_id=$bookId")
        myDatabase.execSQL("DELETE FROM BookLabel WHERE book_id=$bookId")
        myDatabase.execSQL("DELETE FROM BookReadingProgress WHERE book_id=$bookId")
        myDatabase.execSQL("DELETE FROM BookSeries WHERE book_id=$bookId")
        myDatabase.execSQL("DELETE FROM BookState WHERE book_id=$bookId")
        myDatabase.execSQL("DELETE FROM BookTag WHERE book_id=$bookId")
        myDatabase.execSQL("DELETE FROM BookUid WHERE book_id=$bookId")
        myDatabase.execSQL("DELETE FROM Bookmarks WHERE book_id=$bookId")
        myDatabase.execSQL("DELETE FROM VisitedHyperlinks WHERE book_id=$bookId")
        myDatabase.execSQL("DELETE FROM Books WHERE book_id=$bookId")
        myDatabase.setTransactionSuccessful()
        myDatabase.endTransaction()
    }

    private fun createTables() {
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS Books(" +
                    "book_id INTEGER PRIMARY KEY," +
                    "encoding TEXT," +
                    "language TEXT," +
                    "title TEXT NOT NULL," +
                    "file_name TEXT UNIQUE NOT NULL)"
        )
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS Authors(" +
                    "author_id INTEGER PRIMARY KEY," +
                    "name TEXT NOT NULL," +
                    "sort_key TEXT NOT NULL," +
                    "CONSTRAINT Authors_Unique UNIQUE (name, sort_key))"
        )
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS BookAuthor(" +
                    "author_id INTEGER NOT NULL REFERENCES Authors(author_id)," +
                    "book_id INTEGER NOT NULL REFERENCES Books(book_id)," +
                    "author_index INTEGER NOT NULL," +
                    "CONSTRAINT BookAuthor_Unique0 UNIQUE (author_id, book_id)," +
                    "CONSTRAINT BookAuthor_Unique1 UNIQUE (book_id, author_index))"
        )
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS Series(" +
                    "series_id INTEGER PRIMARY KEY," +
                    "name TEXT UNIQUE NOT NULL)"
        )
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS BookSeries(" +
                    "series_id INTEGER NOT NULL REFERENCES Series(series_id)," +
                    "book_id INTEGER NOT NULL UNIQUE REFERENCES Books(book_id)," +
                    "book_index INTEGER)"
        )
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS Tags(" +
                    "tag_id INTEGER PRIMARY KEY," +
                    "name TEXT NOT NULL," +
                    "parent INTEGER REFERENCES Tags(tag_id)," +
                    "CONSTRAINT Tags_Unique UNIQUE (name, parent))"
        )
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS BookTag(" +
                    "tag_id INTEGER REFERENCES Tags(tag_id)," +
                    "book_id INTEGER REFERENCES Books(book_id)," +
                    "CONSTRAINT BookTag_Unique UNIQUE (tag_id, book_id))"
        )
    }

    private fun updateTables1() {
        myDatabase.execSQL("ALTER TABLE Tags RENAME TO Tags_Obsolete")
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS Tags(" +
                    "tag_id INTEGER PRIMARY KEY," +
                    "name TEXT NOT NULL," +
                    "parent_id INTEGER REFERENCES Tags(tag_id)," +
                    "CONSTRAINT Tags_Unique UNIQUE (name, parent_id))"
        )
        myDatabase.execSQL("INSERT INTO Tags (tag_id,name,parent_id) SELECT tag_id,name,parent FROM Tags_Obsolete")
        myDatabase.execSQL("DROP TABLE IF EXISTS Tags_Obsolete")

        myDatabase.execSQL("ALTER TABLE BookTag RENAME TO BookTag_Obsolete")
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS BookTag(" +
                    "tag_id INTEGER NOT NULL REFERENCES Tags(tag_id)," +
                    "book_id INTEGER NOT NULL REFERENCES Books(book_id)," +
                    "CONSTRAINT BookTag_Unique UNIQUE (tag_id, book_id))"
        )
        myDatabase.execSQL("INSERT INTO BookTag (tag_id,book_id) SELECT tag_id,book_id FROM BookTag_Obsolete")
        myDatabase.execSQL("DROP TABLE IF EXISTS BookTag_Obsolete")
    }

    private fun updateTables2() {
        myDatabase.execSQL("CREATE INDEX BookAuthor_BookIndex ON BookAuthor (book_id)")
        myDatabase.execSQL("CREATE INDEX BookTag_BookIndex ON BookTag (book_id)")
        myDatabase.execSQL("CREATE INDEX BookSeries_BookIndex ON BookSeries (book_id)")
    }

    private fun updateTables3() {
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS Files(" +
                    "file_id INTEGER PRIMARY KEY," +
                    "name TEXT NOT NULL," +
                    "parent_id INTEGER REFERENCES Files(file_id)," +
                    "size INTEGER," +
                    "CONSTRAINT Files_Unique UNIQUE (name, parent_id))"
        )
    }

    private fun updateTables4() {
        val fileInfos = FileInfoSet(this)
        val cursor = myDatabase.rawQuery(
            "SELECT file_name FROM Books", null
        )
        while (cursor.moveToNext()) {
            fileInfos.check(ZLFile.createFileByPath(cursor.getString(0))?.physicalFile, false)
        }
        cursor.close()
        fileInfos.save()

        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS RecentBooks(" +
                    "book_index INTEGER PRIMARY KEY," +
                    "book_id INTEGER REFERENCES Books(book_id))"
        )
    }

    private fun updateTables5() {
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS Bookmarks(" +
                    "bookmark_id INTEGER PRIMARY KEY," +
                    "book_id INTEGER NOT NULL REFERENCES Books(book_id)," +
                    "bookmark_text TEXT NOT NULL," +
                    "creation_time INTEGER NOT NULL," +
                    "modification_time INTEGER," +
                    "access_time INTEGER," +
                    "access_counter INTEGER NOT NULL," +
                    "paragraph INTEGER NOT NULL," +
                    "word INTEGER NOT NULL," +
                    "char INTEGER NOT NULL)"
        )

        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS BookState(" +
                    "book_id INTEGER UNIQUE NOT NULL REFERENCES Books(book_id)," +
                    "paragraph INTEGER NOT NULL," +
                    "word INTEGER NOT NULL," +
                    "char INTEGER NOT NULL)"
        )
        val cursor = myDatabase.rawQuery(
            "SELECT book_id,file_name FROM Books", null
        )
        val statement = myDatabase.compileStatement("INSERT INTO BookState (book_id,paragraph,word,char) VALUES (?,?,?,?)")
        while (cursor.moveToNext()) {
            val bookId = cursor.getLong(0)
            val fileName = cursor.getString(1)
            val position = ZLIntegerOption(fileName, "PositionInBuffer", 0).value
            val paragraph = ZLIntegerOption(fileName, "Paragraph_$position", 0).value
            val word = ZLIntegerOption(fileName, "Word_$position", 0).value
            val chr = ZLIntegerOption(fileName, "Char_$position", 0).value
            if ((paragraph != 0) || (word != 0) || (chr != 0)) {
                statement.bindLong(1, bookId)
                statement.bindLong(2, paragraph.toLong())
                statement.bindLong(3, word.toLong())
                statement.bindLong(4, chr.toLong())
                statement.execute()
            }
            Config.Instance()?.removeGroup(fileName)
        }
        cursor.close()
    }

    private fun updateTables6() {
        myDatabase.execSQL(
            "ALTER TABLE Bookmarks ADD COLUMN model_id TEXT"
        )

        myDatabase.execSQL(
            "ALTER TABLE Books ADD COLUMN file_id INTEGER"
        )

        myDatabase.execSQL("DELETE FROM Files")
        val infoSet = FileInfoSet(this)
        var cursor = myDatabase.rawQuery(
            "SELECT file_name FROM Books", null
        )
        while (cursor.moveToNext()) {
            infoSet.check(ZLFile.createFileByPath(cursor.getString(0))?.physicalFile, false)
        }
        cursor.close()
        infoSet.save()

        cursor = myDatabase.rawQuery(
            "SELECT book_id,file_name FROM Books", null
        )
        val deleteStatement = myDatabase.compileStatement("DELETE FROM Books WHERE book_id=?")
        val updateStatement = myDatabase.compileStatement("UPDATE OR IGNORE Books SET file_id=? WHERE book_id=?")
        while (cursor.moveToNext()) {
            val bookId = cursor.getLong(0)
            val fileId = infoSet.getId(ZLFile.createFileByPath(cursor.getString(1)))

            if (fileId == -1L) {
                deleteStatement.bindLong(1, bookId)
                deleteStatement.execute()
            } else {
                updateStatement.bindLong(1, fileId)
                updateStatement.bindLong(2, bookId)
                updateStatement.execute()
            }
        }
        cursor.close()

        myDatabase.execSQL("ALTER TABLE Books RENAME TO Books_Obsolete")
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS Books(" +
                    "book_id INTEGER PRIMARY KEY," +
                    "encoding TEXT," +
                    "language TEXT," +
                    "title TEXT NOT NULL," +
                    "file_id INTEGER UNIQUE NOT NULL REFERENCES Files(file_id))"
        )
        myDatabase.execSQL("INSERT INTO Books (book_id,encoding,language,title,file_id) SELECT book_id,encoding,language,title,file_id FROM Books_Obsolete")
        myDatabase.execSQL("DROP TABLE IF EXISTS Books_Obsolete")
    }

    private fun updateTables7() {
        val seriesIDs = ArrayList<Long>()
        var cursor = myDatabase.rawQuery(
            "SELECT series_id,name FROM Series", null
        )
        while (cursor.moveToNext()) {
            if (cursor.getString(1).length > 200) {
                seriesIDs.add(cursor.getLong(0))
            }
        }
        cursor.close()
        if (seriesIDs.isEmpty()) {
            return
        }

        val bookIDs = ArrayList<Long>()
        for (id in seriesIDs) {
            cursor = myDatabase.rawQuery(
                "SELECT book_id FROM BookSeries WHERE series_id=$id", null
            )
            while (cursor.moveToNext()) {
                bookIDs.add(cursor.getLong(0))
            }
            cursor.close()
            myDatabase.execSQL("DELETE FROM BookSeries WHERE series_id=$id")
            myDatabase.execSQL("DELETE FROM Series WHERE series_id=$id")
        }

        for (id in bookIDs) {
            myDatabase.execSQL("DELETE FROM Books WHERE book_id=$id")
            myDatabase.execSQL("DELETE FROM BookAuthor WHERE book_id=$id")
            myDatabase.execSQL("DELETE FROM BookTag WHERE book_id=$id")
        }
    }

    private fun updateTables8() {
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS BookList ( " +
                    "book_id INTEGER UNIQUE NOT NULL REFERENCES Books (book_id))"
        )
    }

    private fun updateTables9() {
        myDatabase.execSQL("CREATE INDEX BookList_BookIndex ON BookList (book_id)")
    }

    private fun updateTables10() {
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS Favorites(" +
                    "book_id INTEGER UNIQUE NOT NULL REFERENCES Books(book_id))"
        )
    }

    private fun updateTables11() {
        myDatabase.execSQL("UPDATE Files SET size = size + 1")
    }

    private fun updateTables12() {
        myDatabase.execSQL("DELETE FROM Files WHERE parent_id IN (SELECT file_id FROM Files WHERE name LIKE '%.epub')")
    }

    private fun updateTables13() {
        myDatabase.execSQL(
            "ALTER TABLE Bookmarks ADD COLUMN visible INTEGER DEFAULT 1"
        )
    }

    private fun updateTables14() {
        myDatabase.execSQL("ALTER TABLE BookSeries RENAME TO BookSeries_Obsolete")
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS BookSeries(" +
                    "series_id INTEGER NOT NULL REFERENCES Series(series_id)," +
                    "book_id INTEGER NOT NULL UNIQUE REFERENCES Books(book_id)," +
                    "book_index REAL)"
        )
        myDatabase.execSQL("INSERT INTO BookSeries (series_id,book_id,book_index) SELECT series_id,book_id,book_index FROM BookSeries_Obsolete")
        myDatabase.execSQL("DROP TABLE IF EXISTS BookSeries_Obsolete")
    }

    private fun updateTables15() {
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS VisitedHyperlinks(" +
                    "book_id INTEGER NOT NULL REFERENCES Books(book_id)," +
                    "hyperlink_id TEXT NOT NULL," +
                    "CONSTRAINT VisitedHyperlinks_Unique UNIQUE (book_id, hyperlink_id))"
        )
    }

    private fun updateTables16() {
        myDatabase.execSQL(
            "ALTER TABLE Books ADD COLUMN `exists` INTEGER DEFAULT 1"
        )
    }

    private fun updateTables17() {
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS BookStatus(" +
                    "book_id INTEGER NOT NULL REFERENCES Books(book_id) PRIMARY KEY," +
                    "access_time INTEGER NOT NULL," +
                    "pages_full INTEGER NOT NULL," +
                    "page_current INTEGER NOT NULL)"
        )
    }

    private fun updateTables18() {
        myDatabase.execSQL("ALTER TABLE BookSeries RENAME TO BookSeries_Obsolete")
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS BookSeries(" +
                    "series_id INTEGER NOT NULL REFERENCES Series(series_id)," +
                    "book_id INTEGER NOT NULL UNIQUE REFERENCES Books(book_id)," +
                    "book_index TEXT)"
        )
        val insert = myDatabase.compileStatement(
            "INSERT INTO BookSeries (series_id,book_id,book_index) VALUES (?,?,?)"
        )
        val cursor = myDatabase.rawQuery("SELECT series_id,book_id,book_index FROM BookSeries_Obsolete", null)
        while (cursor.moveToNext()) {
            insert.bindLong(1, cursor.getLong(0))
            insert.bindLong(2, cursor.getLong(1))
            val index = cursor.getFloat(2)
            val stringIndex: String?
            if (index == 0.0f) {
                stringIndex = null
            } else {
                stringIndex = if (Math.abs(index - Math.round(index).toFloat()) < 0.01f) {
                    Math.round(index).toString()
                } else {
                    String.format("%.1f", index)
                }
            }
            val bdIndex = SeriesInfo.createIndex(stringIndex)
            SQLiteUtil.bindString(insert, 3, bdIndex?.toString())
            insert.executeInsert()
        }
        cursor.close()
        myDatabase.execSQL("DROP TABLE IF EXISTS BookSeries_Obsolete")
    }

    private fun updateTables19() {
        myDatabase.execSQL("DROP TABLE IF EXISTS BookList")
    }

    private fun updateTables20() {
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS Labels(" +
                    "label_id INTEGER PRIMARY KEY," +
                    "name TEXT NOT NULL UNIQUE)"
        )
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS BookLabel(" +
                    "label_id INTEGER NOT NULL REFERENCES Labels(label_id)," +
                    "book_id INTEGER NOT NULL REFERENCES Books(book_id)," +
                    "CONSTRAINT BookLabel_Unique UNIQUE (label_id,book_id))"
        )
        val insert = myDatabase.compileStatement(
            "INSERT INTO Labels (name) VALUES ('favorite')"
        )
        val id = insert.executeInsert()
        myDatabase.execSQL("INSERT INTO BookLabel (label_id,book_id) SELECT $id,book_id FROM Favorites")
        myDatabase.execSQL("DROP TABLE IF EXISTS Favorites")
    }

    private fun updateTables21() {
        myDatabase.execSQL("DROP TABLE IF EXISTS BookUid")
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS BookUid(" +
                    "book_id INTEGER NOT NULL UNIQUE REFERENCES Books(book_id)," +
                    "type TEXT NOT NULL," +
                    "uid TEXT NOT NULL," +
                    "CONSTRAINT BookUid_Unique UNIQUE (book_id,type,uid))"
        )
    }

    private fun updateTables22() {
        myDatabase.execSQL("ALTER TABLE Bookmarks ADD COLUMN end_paragraph INTEGER")
        myDatabase.execSQL("ALTER TABLE Bookmarks ADD COLUMN end_word INTEGER")
        myDatabase.execSQL("ALTER TABLE Bookmarks ADD COLUMN end_character INTEGER")
    }

    private fun updateTables23() {
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS HighlightingStyle(" +
                    "style_id INTEGER PRIMARY KEY," +
                    "name TEXT NOT NULL," +
                    "bg_color INTEGER NOT NULL)"
        )
        myDatabase.execSQL("ALTER TABLE Bookmarks ADD COLUMN style_id INTEGER NOT NULL REFERENCES HighlightingStyle(style_id) DEFAULT 1")
        myDatabase.execSQL("UPDATE Bookmarks SET end_paragraph = LENGTH(bookmark_text)")
    }

    private fun updateTables24() {
        myDatabase.execSQL("INSERT OR REPLACE INTO HighlightingStyle (style_id, name, bg_color) VALUES (1, '', 136*256*256 + 138*256 + 133)")
        myDatabase.execSQL("INSERT OR REPLACE INTO HighlightingStyle (style_id, name, bg_color) VALUES (2, '', 245*256*256 + 121*256 + 0)")
        myDatabase.execSQL("INSERT OR REPLACE INTO HighlightingStyle (style_id, name, bg_color) VALUES (3, '', 114*256*256 + 159*256 + 207)")
    }

    private fun updateTables25() {
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS BookReadingProgress(" +
                    "book_id INTEGER PRIMARY KEY REFERENCES Books(book_id)," +
                    "numerator INTEGER NOT NULL," +
                    "denominator INTEGER NOT NULL)"
        )
    }

    private fun updateTables26() {
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS BookHash(" +
                    "book_id INTEGER PRIMARY KEY REFERENCES Books(book_id)," +
                    "timestamp INTEGER NOT NULL," +
                    "hash TEXT(40) NOT NULL)"
        )
    }

    private fun updateTables27() {
        myDatabase.execSQL("ALTER TABLE BookState ADD COLUMN timestamp INTEGER")
    }

    private fun updateTables28() {
        myDatabase.execSQL("ALTER TABLE HighlightingStyle ADD COLUMN fg_color INTEGER NOT NULL DEFAULT -1")
    }

    private fun updateTables29() {
        myDatabase.execSQL("DROP TABLE IF EXISTS BookHistory")
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS BookHistory(" +
                    "book_id INTEGER REFERENCES Books(book_id)," +
                    "timestamp INTEGER NOT NULL," +
                    "event INTEGER NOT NULL)"
        )

        var cursor = myDatabase.rawQuery(
            "SELECT book_id FROM RecentBooks ORDER BY book_index", null
        )
        var insert = myDatabase.compileStatement(
            "INSERT OR IGNORE INTO BookHistory(book_id,timestamp,event) VALUES (?,?,?)"
        )
        insert.bindLong(3, HistoryEvent.Opened.toLong())
        var count = -1
        while (cursor.moveToNext()) {
            insert.bindLong(1, cursor.getLong(0))
            insert.bindLong(2, count.toLong())
            try {
                insert.executeInsert()
            } catch (t: Throwable) {
                // ignore
            }
            --count
        }
        cursor.close()

        cursor = myDatabase.rawQuery(
            "SELECT book_id FROM Books ORDER BY book_id DESC", null
        )
        insert = myDatabase.compileStatement(
            "INSERT OR IGNORE INTO BookHistory(book_id,timestamp,event) VALUES (?,?,?)"
        )
        insert.bindLong(3, HistoryEvent.Added.toLong())
        while (cursor.moveToNext()) {
            insert.bindLong(1, cursor.getLong(0))
            insert.bindLong(2, count.toLong())
            try {
                insert.executeInsert()
            } catch (t: Throwable) {
                // ignore
            }
            --count
        }
        cursor.close()
    }

    private fun updateTables30() {
        myDatabase.execSQL("DROP TABLE IF EXISTS RecentBooks")
    }

    private fun updateTables31() {
        myDatabase.execSQL("ALTER TABLE BookLabel ADD COLUMN timestamp INTEGER NOT NULL DEFAULT -1")
    }

    private fun updateTables32() {
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS Options(name TEXT PRIMARY KEY, value TEXT)")
    }

    private fun updateTables33() {
        myDatabase.execSQL("ALTER TABLE Bookmarks ADD COLUMN uid TEXT(36)")
        val cursor = myDatabase.rawQuery("SELECT bookmark_id FROM Bookmarks", null)
        val statement = get("UPDATE Bookmarks SET uid=? WHERE bookmark_id=?")
        while (cursor.moveToNext()) {
            statement.bindString(1, UUID.randomUUID().toString())
            statement.bindLong(2, cursor.getLong(0))
            statement.execute()
        }
        cursor.close()

        myDatabase.execSQL("ALTER TABLE Bookmarks RENAME TO Bookmarks_Obsolete")
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS Bookmarks(" +
                    "bookmark_id INTEGER PRIMARY KEY," +
                    "uid TEXT(36) NOT NULL UNIQUE," +
                    "version_uid TEXT(36)," +
                    "book_id INTEGER NOT NULL REFERENCES Books(book_id)," +
                    "visible INTEGER DEFAULT 1," +
                    "style_id INTEGER NOT NULL REFERENCES HighlightingStyle(style_id) DEFAULT 1," +
                    "bookmark_text TEXT NOT NULL," +
                    "creation_time INTEGER NOT NULL," +
                    "modification_time INTEGER," +
                    "access_time INTEGER," +
                    "model_id TEXT," +
                    "paragraph INTEGER NOT NULL," +
                    "word INTEGER NOT NULL," +
                    "char INTEGER NOT NULL," +
                    "end_paragraph INTEGER," +
                    "end_word INTEGER," +
                    "end_character INTEGER)"
        )
        val fields = "bookmark_id,uid,book_id,visible,style_id,bookmark_text,creation_time,modification_time,access_time,model_id,paragraph,word,char,end_paragraph,end_word,end_character"
        myDatabase.execSQL("INSERT INTO Bookmarks ($fields) SELECT $fields FROM Bookmarks_Obsolete")
        myDatabase.execSQL("DROP TABLE IF EXISTS Bookmarks_Obsolete")
    }

    private fun updateTables34() {
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS DeletedBookmarkIds(uid TEXT(36) PRIMARY KEY)")
    }

    private fun updateTables35() {
        myDatabase.execSQL("ALTER TABLE Bookmarks ADD COLUMN original_text TEXT DEFAULT NULL")
    }

    private fun styleBg(styleId: Int): Int {
        return when (styleId) {
            1 -> 0x888a85
            2 -> 0xf57900
            3 -> 0x729fcf
            else -> 0
        }
    }

    private fun updateTables36() {
        myDatabase.execSQL("ALTER TABLE HighlightingStyle ADD COLUMN timestamp INTEGER DEFAULT 0")

        val sql = "SELECT style_id,name,bg_color FROM HighlightingStyle"
        val cursor = myDatabase.rawQuery(sql, null)
        val statement = get("UPDATE HighlightingStyle SET timestamp=? WHERE style_id=?")
        while (cursor.moveToNext()) {
            val styleId = cursor.getLong(0).toInt()
            if ((!cursor.isNull(1) && cursor.getString(1).isNotEmpty()) ||
                styleBg(styleId) != cursor.getLong(2).toInt()) {
                statement.bindLong(1, System.currentTimeMillis())
                statement.bindLong(2, styleId.toLong())
                statement.execute()
            }
        }
        cursor.close()
    }

    private fun updateTables37() {
        myDatabase.execSQL("ALTER TABLE Bookmarks RENAME TO Bookmarks_Obsolete")
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS Bookmarks(" +
                    "bookmark_id INTEGER PRIMARY KEY," +
                    "uid TEXT(36) NOT NULL UNIQUE," +
                    "version_uid TEXT(36)," +
                    "book_id INTEGER NOT NULL REFERENCES Books(book_id)," +
                    "visible INTEGER DEFAULT 1," +
                    "style_id INTEGER NOT NULL REFERENCES HighlightingStyle(style_id) DEFAULT 1," +
                    "bookmark_text TEXT NOT NULL," +
                    "creation_time INTEGER NOT NULL," +
                    "modification_time INTEGER," +
                    "access_time INTEGER," +
                    "model_id TEXT," +
                    "paragraph INTEGER NOT NULL," +
                    "word INTEGER NOT NULL," +
                    "char INTEGER NOT NULL," +
                    "end_paragraph INTEGER," +
                    "end_word INTEGER," +
                    "end_character INTEGER)"
        )
        val fields = "bookmark_id,uid,version_uid,book_id,visible,style_id,bookmark_text,creation_time,modification_time,access_time,model_id,paragraph,word,char,end_paragraph,end_word,end_character"
        myDatabase.execSQL("INSERT INTO Bookmarks ($fields) SELECT $fields FROM Bookmarks_Obsolete")
        myDatabase.execSQL("DROP TABLE IF EXISTS Bookmarks_Obsolete")
    }

    private fun updateTables38() {
        myDatabase.execSQL("ALTER TABLE Bookmarks ADD COLUMN original_text TEXT DEFAULT NULL")
    }

    private fun updateTables39() {
        myDatabase.execSQL("ALTER TABLE BookLabel RENAME TO BookLabel_Obsolete")
        myDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS BookLabel(" +
                    "label_id INTEGER NOT NULL REFERENCES Labels(label_id)," +
                    "book_id INTEGER NOT NULL REFERENCES Books(book_id)," +
                    "timestamp INTEGER NOT NULL DEFAULT -1," +
                    "uid TEXT(36) NOT NULL UNIQUE," +
                    "CONSTRAINT BookLabel_Unique UNIQUE (label_id,book_id))"
        )
        val cursor = myDatabase.rawQuery("SELECT label_id,book_id,timestamp FROM BookLabel_Obsolete", null)
        val statement = get("INSERT INTO BookLabel (label_id,book_id,timestamp,uid) VALUES (?,?,?,?)")
        while (cursor.moveToNext()) {
            statement.bindLong(1, cursor.getLong(0))
            statement.bindLong(2, cursor.getLong(1))
            statement.bindLong(3, cursor.getLong(2))
            statement.bindString(4, UUID.randomUUID().toString())
            statement.execute()
        }
        cursor.close()
        myDatabase.execSQL("DROP TABLE IF EXISTS BookLabel_Obsolete")

        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS DeletedBookLabelIds(uid TEXT(36) PRIMARY KEY)")
    }

    private fun get(sql: String): SQLiteStatement {
        var statement = myStatements[sql]
        if (statement == null) {
            statement = myDatabase.compileStatement(sql)
            myStatements[sql] = statement
        }
        return statement
    }
}
