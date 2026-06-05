package com.github.axet.bookreader.app

import android.content.ContentResolver
import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.NonNull
import com.github.axet.androidlibrary.widgets.CacheImagesAdapter
import org.geometerplus.zlibrary.core.util.ZLTTFInfoDetector
import org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.TreeSet

/**
 * Менеджер TTF-шрифтов.
 * Поддерживает форматы .ttf, .otf, .ttc
 */
class TTFManager(val context: Context) {

    companion object {
        // Системные директории со шрифтами
        val SYSTEM_FONTS: Array<File> = arrayOf(
            File("/system/fonts"),
            File("/system/font"),
            File("/data/fonts")
        )
        val TAG: String = TTFManager::class.java.simpleName

        // Пользовательская директория со шрифтами: /sdcard/Fonts
        var USER_FONTS: File? = null

        init {
            val ext = Environment.getExternalStorageDirectory()
            if (ext != null)
                USER_FONTS = File(ext, "Fonts")
        }
    }

    // Директория шрифтов приложения: /sdcard/Android/data/.../files/Fonts
    var appFonts: File? = null

    // Файлы и content:// URI
    val uris: ArrayList<Uri> = ArrayList()

    // Кэш старых шрифтов
    val old: ArrayList<Font> = ArrayList()

    // Карта файлов шрифтов
    val ourFontFileMap: HashMap<File, Typeface> = HashMap()

    init {
        init()
    }

    /**
     * Перечисляет все доступные шрифты.
     */
    fun enumerateFonts(): ArrayList<Font>? {
        val ff = ArrayList<Font>()
        val a = TTFAnalyzer()
        for (uri in uris) {
            val s = uri.scheme
            if (s == ContentResolver.SCHEME_FILE) {
                val dir = Storage.getFile(uri)
                if (!dir.exists())
                    continue
                val files = dir.listFiles()
                if (files == null)
                    continue
                for (file in files) {
                    val nn = a.getNames(file)
                    if (nn != null) {
                        if (nn.size == 1) {
                            val name = nn[0]
                            if (name != null && name.isNotEmpty())
                                ff.add(Font(name, Uri.fromFile(file)))
                        } else {
                            for (i in nn.indices) {
                                val name = nn[i]
                                if (name != null && name.isNotEmpty())
                                    ff.add(Font(name, Uri.fromFile(file), i))
                            }
                        }
                    }
                }
            } else if (s == ContentResolver.SCHEME_CONTENT) {
                val resolver = context.contentResolver
                val nn = Storage.list(context, uri)
                for (n in nn) {
                    try {
                        val inputStream = resolver.openInputStream(n.uri)
                        if (inputStream == null) continue
                        val names = a.getNames(inputStream)
                        if (names != null) {
                            if (names.size == 1) {
                                val name = names[0]
                                if (name != null && name.isNotEmpty())
                                    ff.add(Font(name, n.uri))
                            } else {
                                for (i in names.indices) {
                                    val name = names[i]
                                    if (name != null && name.isNotEmpty())
                                        ff.add(Font(name, n.uri, i))
                                }
                            }
                        }
                    } catch (e: IOException) {
                        Log.w(TAG, e)
                    }
                }
            }
        }
        ff.sort()
        return if (ff.isEmpty()) null else ff
    }

    /**
     * Инициализирует пути к шрифтам.
     */
    fun init() {
        val fonts = ArrayList<File>(SYSTEM_FONTS.toList())
        if (USER_FONTS != null)
            fonts.add(USER_FONTS!!)
        var fl: File? = context.filesDir // /data/.../files/Fonts
        if (fl != null) {
            fl = File(fl, "Fonts")
            fonts.add(fl)
        }
        if (Build.VERSION.SDK_INT >= 19) {
            val fl2 = context.getExternalFilesDirs("Fonts")
            if (fl2 != null) {
                for (f in fl2) {
                    if (f != null)
                        fonts.add(f)
                }
                appFonts = fl2[0]
            }
        }
        if (appFonts == null)
            appFonts = fl
        uris.clear()
        for (f in fonts)
            uris.add(Uri.fromFile(f))
    }

    /**
     * Устанавливает дополнительную директорию со шрифтами.
     */
    fun setFolder(uri: Uri) {
        init()
        uris.add(uri)
    }

    /**
     * Предзагружает шрифты.
     */
    fun preloadFonts() {
        val files = ArrayList<File>()
        val ttc = HashMap<Font, File>()
        val ff = enumerateFonts()
        if (old == ff) {
            Log.d(TAG, "preloadFonts - no new items")
            return
        }
        old.clear()
        if (ff != null) old.addAll(ff)
        for (f in old) {
            if (f.index == -1 && f.uri.scheme == ContentResolver.SCHEME_FILE)
                files.add(Storage.getFile(f.uri))
            else
                ttc[f] = Storage.getFile(f.uri)
        }
        AndroidFontUtil.ourFileSet = TreeSet()
        AndroidFontUtil.ourFontFileMap = ZLTTFInfoDetector().collectFonts(files)
        ourFontFileMap.clear()
        if (Build.VERSION.SDK_INT >= 26) { // поддержка ttc индекса с API26
            for (f in ttc.keys) {
                try {
                    val tf = TTCFile(f.uri, f.index)
                    val ttf = load(tf)
                    AndroidFontUtil.ourTypefaces[f.name] = arrayOf(ttf, null, null, null)
                    AndroidFontUtil.ourFontFileMap[f.name] = arrayOf(tf, null, null, null)
                    ourFontFileMap[tf] = ttf
                } catch (e: Exception) {
                    Log.w(TAG, e)
                }
            }
        }
    }

    /**
     * Загружает шрифт из файла.
     */
    fun load(file: File): Typeface {
        var tf = ourFontFileMap[file]
        if (tf != null)
            return tf
        if (Build.VERSION.SDK_INT >= 26 && file is TTCFile) {
            val tc = file
            val s = tc.uri.scheme
            if (s == ContentResolver.SCHEME_FILE) {
                return Typeface.Builder(Storage.getFile(tc.uri)).setTtcIndex(tc.index).build()
            } else if (s == ContentResolver.SCHEME_CONTENT) {
                val resolver = context.contentResolver
                try {
                    val fd = resolver.openFileDescriptor(tc.uri, "r")
                    tf = if (tc.index == -1)
                        Typeface.Builder(fd!!.fileDescriptor).build()
                    else
                        Typeface.Builder(fd!!.fileDescriptor).setTtcIndex(tc.index).build()
                    ourFontFileMap[tc] = tf
                    return tf
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            } else {
                throw RuntimeException("Unknown URI scheme")
            }
        } else {
            tf = Typeface.createFromFile(file)
            ourFontFileMap[file] = tf
            return tf
        }
    }

    /**
     * Класс, описывающий шрифт.
     */
    class Font(
        val name: String,
        val uri: Uri,
        val index: Int = -1 // индекс ttc
    ) : Comparable<Font> {

        constructor(n: String, f: Uri) : this(n, f, -1)

        override fun compareTo(other: Font): Int {
            val i = uri.compareTo(other.uri)
            if (i != 0)
                return i
            return index.compareTo(other.index)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val font = other as Font
            return index == font.index && uri == font.uri
        }

        override fun hashCode(): Int {
            return arrayOf<Any?>(uri, index).contentHashCode()
        }
    }

    /**
     * Файл TTC (TrueType Collection).
     */
    class TTCFile : File {
        var uri: Uri
        var index: Int

        constructor(@NonNull pathname: String) : super(pathname) {
            uri = Uri.EMPTY
            index = -1
        }

        constructor(f: Uri, i: Int) : super(f.path) {
            uri = f
            index = i
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val ttcFile = other as TTCFile
            return index == ttcFile.index && uri == ttcFile.uri
        }

        override fun hashCode(): Int {
            return arrayOf<Any?>(super.hashCode(), uri, index).contentHashCode()
        }
    }

    /**
     * Анализатор TTF-файлов.
     * http://www.ulduzsoft.com/2012/01/enumerating-the-fonts-on-android-platform/
     */
    class TTFAnalyzer {
        private var mFile: CacheImagesAdapter.SeekInputStream? = null // Файл шрифта; должен поддерживать seek

        /**
         * Парсит TTF-файл и возвращает имя шрифта.
         */
        fun getTtfFontName(): String? {
            try {
                // TTF-файл состоит из нескольких секций "tables", нужно знать их количество
                val numTables = readWord()

                // Пропускаем остальное в заголовке
                readWord() // skip searchRange
                readWord() // skip entrySelector
                readWord() // skip rangeShift

                // Читаем таблицы
                for (i in 0 until numTables) {
                    // Читаем запись таблицы
                    val tag = readDword()
                    readDword() // skip checksum
                    val offset = readDword()
                    val length = readDword()

                    // Поле 'name' содержит текстовое имя шрифта
                    // Строка 'name' в символах равна 0x6E616D65
                    if (tag == 0x6E616D65) {
                        // Читаем секцию имени полностью
                        val table = ByteArray(length)

                        mFile!!.seek(offset.toLong())
                        read(table)

                        // Это тоже таблица. См. http://developer.apple.com/fonts/ttrefman/rm06/Chap6name.html
                        // Согласно Table 36, общее количество записей хранится во втором слове, смещение 2
                        val count = getWord(table, 2)
                        val stringOffset = getWord(table, 4)

                        // Записи начинаются со смещения 6
                        for (record in 0 until count) {
                            // Table 37: каждая запись 6 слов -> 12 байт, nameID - 4-е слово, смещение 6
                            // Учитываем первые 6 байт заголовка (Table 36)
                            val nameidOffset = record * 12 + 6
                            val platformID = getWord(table, nameidOffset)
                            val nameidValue = getWord(table, nameidOffset + 6)

                            // Table 42: интересует ID 4, но не в Unicode кодировке
                            // Кодировка хранится как PlatformID, интересует Mac кодировка
                            if (nameidValue == 4 && platformID == 1) {
                                // Нужны смещение и длина строки, слова 6 и 5 соответственно
                                val nameLength = getWord(table, nameidOffset + 8)
                                var nameOffset = getWord(table, nameidOffset + 10)

                                // Реальное смещение строки
                                nameOffset = nameOffset + stringOffset

                                // Проверяем, что внутри массива
                                if (nameOffset >= 0 && nameOffset + nameLength < table.size)
                                    return String(table, nameOffset, nameLength)
                            }
                        }
                    }
                }

                return null
            } catch (e: FileNotFoundException) { // Разрешения?
                return null
            } catch (e: IOException) { // Скорее всего повреждённый файл шрифта
                return null
            }
        }

        fun getTtfFontName(file: File): String? {
            var tag = 0
            try {
                mFile = CacheImagesAdapter.SeekInputStream(FileInputStream(file))
                tag = readDword()
            } catch (e: IOException) {
                return null
            }
            when (tag) {
                0x74727565, 0x00010000, 0x4F54544F -> return getTtfFontName()
            }
            return null
        }

        fun getTTCFontNames(): Array<String>? {
            try {
                val major = readWord()
                val min = readWord()
                val num = readDword()
                val nn = IntArray(num)
                for (i in 0 until num)
                    nn[i] = readDword()
                val ss = arrayOfNulls<String>(num)
                for (i in 0 until num) {
                    mFile!!.seek(nn[i].toLong())
                    val tag = readDword()
                    when (tag) {
                        0x74727565, 0x00010000, 0x4F54544F -> ss[i] = getTtfFontName()
                    }
                }
                return ss.mapNotNull { it }.toTypedArray()
            } catch (e: Exception) {
                return null
            }
        }

        fun getNames(file: File): Array<String>? {
            return try {
                getNames(FileInputStream(file))
            } catch (e: Exception) {
                null
            }
        }

        fun getNames(`is`: InputStream): Array<String>? {
            try {
                mFile = CacheImagesAdapter.SeekInputStream(`is`)
                val tag = readDword()
                when (tag) {
                    0x74746366 -> return getTTCFontNames() // 'ttcf'
                    0x74727565, 0x00010000, 0x4F54544F -> return arrayOf(getTtfFontName() ?: return null)
                }
            } catch (e: Exception) {
                return null
            }
            return null
        }

        // Вспомогательные функции I/O
        @Throws(IOException::class)
        private fun readByte(): Int {
            return mFile!!.read() and 0xFF
        }

        @Throws(IOException::class)
        private fun readWord(): Int {
            val b1 = readByte()
            val b2 = readByte()
            return b1 shl 8 or b2
        }

        @Throws(IOException::class)
        private fun readDword(): Int {
            val b1 = readByte()
            val b2 = readByte()
            val b3 = readByte()
            val b4 = readByte()
            return b1 shl 24 or (b2 shl 16) or (b3 shl 8) or b4
        }

        @Throws(IOException::class)
        private fun read(array: ByteArray) {
            if (mFile!!.read(array) != array.size)
                throw IOException()
        }

        // Вспомогательная функция
        private fun getWord(array: ByteArray, offset: Int): Int {
            val b1 = array[offset].toInt() and 0xFF
            val b2 = array[offset + 1].toInt() and 0xFF
            return b1 shl 8 or b2
        }
    }
}
