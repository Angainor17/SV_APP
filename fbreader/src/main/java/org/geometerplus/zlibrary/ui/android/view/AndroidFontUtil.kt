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

package org.geometerplus.zlibrary.ui.android.view

import android.graphics.Typeface
import org.geometerplus.fbreader.Paths
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.fonts.FileInfo
import org.geometerplus.zlibrary.core.fonts.FontEntry
import org.geometerplus.zlibrary.core.util.SystemInfo
import org.geometerplus.zlibrary.core.library.ZLibrary
import org.geometerplus.zlibrary.core.util.XmlUtil
import org.geometerplus.zlibrary.core.util.ZLTTFInfoDetector
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.FileOutputStream
import java.io.FilenameFilter
import java.io.InputStream
import java.io.OutputStream
import java.util.HashMap
import java.util.HashSet
import java.util.Locale
import java.util.TreeSet

object AndroidFontUtil {
    @JvmField
    val ourTypefaces: HashMap<String, Array<Typeface?>> = HashMap()

    private val ourCachedEmbeddedTypefaces = HashMap<Spec, Any>()
    private val NULL_OBJECT = Any()

    @JvmField
    @Volatile
    var ourFontFileMap: HashMap<String, Array<File?>>? = null

    @JvmField
    @Volatile
    var ourFileSet: Set<File>? = null

    @Volatile
    private var ourFontAssetMap: MutableMap<String, Array<String>>? = null

    @Volatile
    private var ourTimeStamp: Long = 0

    private fun getFontAssetMap(): MutableMap<String, Array<String>> {
        var map = ourFontAssetMap
        if (map == null) {
            map = HashMap()
            ourFontAssetMap = map
            XmlUtil.parseQuietly(
                ZLFile.createFileByPath("fonts/fonts.xml")!!,
                object : DefaultHandler() {
                    override fun startElement(
                        uri: String,
                        localName: String,
                        qName: String,
                        attributes: Attributes,
                    ) {
                        if ("font" == localName) {
                            map[attributes.getValue("family")!!] = arrayOf(
                                "fonts/" + attributes.getValue("regular"),
                                "fonts/" + attributes.getValue("bold"),
                                "fonts/" + attributes.getValue("italic"),
                                "fonts/" + attributes.getValue("boldItalic"),
                            )
                        }
                    }
                },
            )
        }
        return map
    }

    @Synchronized
    private fun getFontFileMap(forceReload: Boolean): HashMap<String, Array<File?>>? {
        var reload = forceReload
        val timeStamp = System.currentTimeMillis()
        if (reload && timeStamp < ourTimeStamp + 1000) {
            reload = false
        }
        ourTimeStamp = timeStamp
        if (ourFileSet == null || reload) {
            val fileSet = HashSet<File>()
            val filter = FilenameFilter { _, name ->
                if (name.startsWith(".")) {
                    false
                } else {
                    val lcName = name.lowercase(Locale.ROOT)
                    lcName.endsWith(".ttf") || lcName.endsWith(".otf")
                }
            }
            for (dir in Paths.FontPathOption.getValue()) {
                val fileList = File(dir).listFiles(filter)
                if (fileList != null) {
                    fileSet.addAll(fileList.toList())
                }
            }
            if (fileSet != ourFileSet) {
                ourFileSet = fileSet
                ourFontFileMap = ZLTTFInfoDetector().collectFonts(fileSet)
            }
        }
        return ourFontFileMap
    }

    @JvmStatic
    fun realFontFamilyName(fontFamily: String): String {
        for (name in getFontAssetMap().keys) {
            if (name.equals(fontFamily, ignoreCase = true)) {
                return name
            }
        }
        val fileMap = getFontFileMap(false)
        if (fileMap != null) {
            for (name in fileMap.keys) {
                if (name.equals(fontFamily, ignoreCase = true)) {
                    return name
                }
            }
        }
        if ("serif".equals(fontFamily, ignoreCase = true) ||
            "droid serif".equals(fontFamily, ignoreCase = true)
        ) {
            return "serif"
        }
        if ("sans-serif".equals(fontFamily, ignoreCase = true) ||
            "sans serif".equals(fontFamily, ignoreCase = true) ||
            "droid sans".equals(fontFamily, ignoreCase = true)
        ) {
            return "sans-serif"
        }
        if ("monospace".equals(fontFamily, ignoreCase = true) ||
            "droid mono".equals(fontFamily, ignoreCase = true)
        ) {
            return "monospace"
        }
        return "sans-serif"
    }

    @JvmStatic
    fun fillFamiliesList(families: ArrayList<String>) {
        val familySet = TreeSet<String>(getFontFileMap(true)?.keys ?: emptySet())
        familySet.addAll(getFontAssetMap().keys)
        familySet.add("Droid Sans")
        familySet.add("Droid Serif")
        familySet.add("Droid Mono")
        families.addAll(familySet)
    }

    private fun createTypefaceFromAsset(
        typefaces: Array<Typeface?>,
        family: String,
        style: Int,
    ): Typeface? {
        val assets = getFontAssetMap()[family]
        if (assets == null) {
            return null
        }
        return try {
            Typeface.createFromAsset(
                (ZLibrary.Instance() as ZLAndroidLibrary).getAssets(),
                assets[style],
            )
        } catch (t: Throwable) {
            null
        }
    }

    private fun createTypefaceFromFile(
        typefaces: Array<Typeface?>,
        family: String,
        style: Int,
    ): Typeface? {
        val files = getFontFileMap(false)?.get(family)
        if (files == null) {
            return null
        }
        try {
            val styledFile = files[style]
            if (styledFile != null) {
                return Typeface.createFromFile(styledFile)
            }
            for (i in 0 until 4) {
                val file = files[i]
                if (file != null) {
                    if (typefaces[i] == null) {
                        typefaces[i] = Typeface.createFromFile(file)
                    }
                    return typefaces[i]
                }
            }
        } catch (e: Throwable) {
            // ignore
        }
        return null
    }

    @JvmStatic
    fun typeface(systemInfo: SystemInfo, entry: FontEntry, bold: Boolean, italic: Boolean): Typeface? =
        if (entry.isSystem()) {
            systemTypeface(entry.Family, bold, italic)
        } else {
            embeddedTypeface(systemInfo, entry, bold, italic)
        }

    @JvmStatic
    fun systemTypeface(family0: String, bold: Boolean, italic: Boolean): Typeface {
        val family = realFontFamilyName(family0)
        val style = (if (bold) Typeface.BOLD else 0) or (if (italic) Typeface.ITALIC else 0)
        var typefaces = ourTypefaces[family]
        if (typefaces == null) {
            typefaces = arrayOfNulls(4)
            ourTypefaces[family] = typefaces
        }
        val tf = typefaces[style]
            ?: createTypefaceFromFile(typefaces, family, style)
            ?: createTypefaceFromAsset(typefaces, family, style)
            ?: Typeface.create(family, style)
        typefaces[style] = tf
        return tf
    }

    private fun alias(systemInfo: SystemInfo, family: String, bold: Boolean, italic: Boolean): String {
        val builder = StringBuilder(systemInfo.tempDirectory())
        builder.append("/")
        builder.append(family)
        if (bold) {
            builder.append("-bold")
        }
        if (italic) {
            builder.append("-italic")
        }
        return builder.append(".font").toString()
    }

    private fun copy(from: FileInfo, to: String): Boolean {
        var isStream: InputStream? = null
        var os: OutputStream? = null
        return try {
            isStream = ZLFile.createFileByPath(from.Path)?.getInputStream(from.EncryptionInfo)
            os = FileOutputStream(to)
            val buffer = ByteArray(8192)
            while (true) {
                val len = isStream!!.read(buffer)
                if (len <= 0) {
                    break
                }
                os!!.write(buffer, 0, len)
            }
            true
        } catch (e: Exception) {
            false
        } finally {
            try {
                os?.close()
            } catch (t: Throwable) {
                // ignore
            }
            try {
                isStream?.close()
            } catch (t: Throwable) {
                // ignore
            }
        }
    }

    private fun getOrCreateEmbeddedTypeface(
        systemInfo: SystemInfo,
        entry: FontEntry,
        bold: Boolean,
        italic: Boolean,
    ): Typeface? {
        val spec = Spec(entry, bold, italic)
        var cached: Any? = ourCachedEmbeddedTypefaces[spec]
        if (cached == null) {
            val fileInfo = entry.fileInfo(bold, italic)
            if (fileInfo != null) {
                val realFileName = alias(systemInfo, entry.Family, bold, italic)
                if (copy(fileInfo, realFileName)) {
                    try {
                        cached = Typeface.createFromFile(realFileName)
                    } catch (t: Throwable) {
                        // ignore
                    }
                }
                File(realFileName).delete()
            }
            ourCachedEmbeddedTypefaces[spec] = cached ?: NULL_OBJECT
        }
        return cached as? Typeface
    }

    private fun embeddedTypeface(
        systemInfo: SystemInfo,
        entry: FontEntry,
        bold: Boolean,
        italic: Boolean,
    ): Typeface? {
        getOrCreateEmbeddedTypeface(systemInfo, entry, bold, italic)?.let { return it }
        for (i in 0 until 4) {
            getOrCreateEmbeddedTypeface(
                systemInfo,
                entry,
                (i and 1) == 1,
                (i and 2) == 2,
            )?.let { return it }
        }
        return null
    }

    @JvmStatic
    fun clearFontCache() {
        ourTypefaces.clear()
        ourFileSet = null
        ourCachedEmbeddedTypefaces.clear()
    }

    private class Spec(val Entry: FontEntry, val Bold: Boolean, val Italic: Boolean) {
        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other !is Spec) {
                return false
            }
            return Bold == other.Bold && Italic == other.Italic && Entry == other.Entry
        }

        override fun hashCode(): Int =
            4 * Entry.hashCode() + (if (Bold) 2 else 0) + (if (Italic) 1 else 0)
    }
}
