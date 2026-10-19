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

package org.geometerplus.zlibrary.core.fonts

import org.fbreader.util.ComparisonUtil
import org.geometerplus.zlibrary.core.drm.FileEncryptionInfo

class FileInfo(
    @JvmField val Path: String,
    @JvmField val EncryptionInfo: FileEncryptionInfo?
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is FileInfo) {
            return false
        }
        return Path == other.Path && ComparisonUtil.equal(EncryptionInfo, other.EncryptionInfo)
    }

    override fun hashCode(): Int = Path.hashCode() + 23 * ComparisonUtil.hashCode(EncryptionInfo)
}
