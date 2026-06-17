/*
 * Copyright (C) 2011-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.fbreader.formats.fb2

import org.geometerplus.fbreader.book.AbstractBook
import org.geometerplus.fbreader.bookmodel.BookModel
import org.geometerplus.fbreader.formats.BookReadingException
import org.geometerplus.fbreader.formats.NativeFormatPlugin
import org.geometerplus.zlibrary.core.encodings.AutoEncodingCollection
import org.geometerplus.zlibrary.core.encodings.EncodingCollection
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.util.SystemInfo

import java.net.URLDecoder

class FB2NativePlugin(systemInfo: SystemInfo) : NativeFormatPlugin(systemInfo, "fb2") {

    override fun readModel(model: BookModel) {
        super.readModel(model)
        model.setLabelResolver(object : BookModel.LabelResolver {
            override fun getCandidates(id: String): List<String> {
                val candidates = mutableListOf<String>()
                try {
                    val c = URLDecoder.decode(id, "utf-8")
                    if (c != null && c != id) {
                        candidates.add(c)
                    }
                } catch (e: Exception) {
                    // ignore
                }
                try {
                    val c = URLDecoder.decode(id, "windows-1251")
                    if (c != null && c != id) {
                        candidates.add(c)
                    }
                } catch (e: Exception) {
                    // ignore
                }
                return candidates
            }
        })
    }

    override fun realBookFile(file: ZLFile): ZLFile {
        val realFile = getRealFB2File(file)
        if (realFile == null) {
            throw BookReadingException("incorrectFb2ZipFile", file)
        }
        return realFile
    }

    override fun supportedEncodings(): EncodingCollection = AutoEncodingCollection()

    override fun detectLanguageAndEncoding(book: AbstractBook) {
        book.encoding = "auto"
    }

    override fun readAnnotation(file: ZLFile): String? {
        return FB2AnnotationReader().readAnnotation(file)
    }

    override fun priority(): Int = 0

    companion object {
        private fun getRealFB2File(file: ZLFile): ZLFile? {
            val name = file.shortName.lowercase()
            if (name.endsWith(".fb2.zip") && file.isArchive) {
                val children = file.children()
                if (children == null) {
                    return null
                }
                var candidate: ZLFile? = null
                for (item in children) {
                    if ("fb2" == item.extension) {
                        if (candidate == null) {
                            candidate = item
                        } else {
                            return null
                        }
                    }
                }
                return candidate
            } else {
                return file
            }
        }
    }
}
