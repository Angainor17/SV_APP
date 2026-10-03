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

package org.geometerplus.zlibrary.core.drm.embedding

import org.geometerplus.zlibrary.core.util.InputStreamWithOffset
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

class EmbeddingInputStream(base: InputStream, uid: String) : InputStreamWithOffset(base) {
    private val myKey: ByteArray = try {
        MessageDigest.getInstance("SHA").digest(uid.toByteArray(Charsets.UTF_8))
    } catch (e: Exception) {
        throw IOException(e)
    }

    override fun read(): Int {
        val o = offset()
        val bt = super.read()
        if (bt == -1) {
            return -1
        }
        return if (o > 1040) bt else ((bt xor myKey[o % myKey.size].toInt()) and 0xFF)
    }

    override fun read(buffer: ByteArray, bOffset: Int, bCount: Int): Int {
        val o = offset()
        val len = super.read(buffer, bOffset, bCount)
        if (o < 1040) {
            val e = minOf(1040 - o, len)
            for (c in 0 until e) {
                buffer[bOffset + c] =
                    (buffer[bOffset + c].toInt() xor myKey[(o + c) % myKey.size].toInt()).toByte()
            }
        }
        return len
    }

    override fun read(buffer: ByteArray): Int = read(buffer, 0, buffer.size)
}
