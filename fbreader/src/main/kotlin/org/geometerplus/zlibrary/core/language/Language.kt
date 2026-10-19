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

package org.geometerplus.zlibrary.core.language

import org.geometerplus.zlibrary.core.resources.ZLResource
import java.text.Normalizer

class Language(code: String, root: ZLResource) : Comparable<Language> {

    @JvmField
    val Code: String = code

    @JvmField
    val Name: String = root.getResource(code).let { if (it.hasValue()) it.getValue() else code }

    private val mySortKey: String = Normalizer.normalize(Name, Normalizer.Form.NFKD)

    private val myOrder: Order = when {
        SYSTEM_CODE == code || ANY_CODE == code -> Order.Before
        MULTI_CODE == code || OTHER_CODE == code -> Order.After
        else -> Order.Normal
    }

    constructor(code: String) : this(code, ZLResource.resource("language"))

    override fun compareTo(other: Language): Int {
        val diff = myOrder.compareTo(other.myOrder)
        return if (diff != 0) diff else mySortKey.compareTo(other.mySortKey)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Language) {
            return false
        }
        return Code == other.Code
    }

    override fun hashCode(): Int = Code.hashCode()

    private enum class Order {
        Before,
        Normal,
        After
    }

    companion object {
        const val ANY_CODE = "any"
        const val OTHER_CODE = "other"
        const val MULTI_CODE = "multi"
        const val SYSTEM_CODE = "system"
    }
}
