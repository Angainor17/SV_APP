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

package org.geometerplus.fbreader.network.opds

import org.geometerplus.zlibrary.core.util.ZLNetworkUtil
import org.geometerplus.zlibrary.core.xml.ZLStringMap
import java.util.regex.Pattern

internal class URLRewritingRule(map: ZLStringMap) {

    companion object {
        // rule types:
        const val ADD_URL_PARAMETER = 0
        const val REWRITE = 1
        const val UNKNOWN = 2

        // apply values:
        const val APPLY_EXTERNAL = 1
        const val APPLY_INTERNAL = 2
        const val APPLY_ALWAYS = APPLY_EXTERNAL or APPLY_INTERNAL
    }

    private val parameters = mutableMapOf<String, String>()
    private var type = UNKNOWN
    private var apply = APPLY_ALWAYS

    init {
        for (i in map.size - 1 downTo 0) {
            val key = map.getKey(i)
            val value = map.getValue(key)
            when (key) {
                "type" -> {
                    type = when (value) {
                        "addUrlParameter" -> ADD_URL_PARAMETER
                        "rewrite" -> REWRITE
                        else -> UNKNOWN
                    }
                }
                "apply" -> {
                    apply = when (value) {
                        "internal" -> APPLY_INTERNAL
                        "external" -> APPLY_EXTERNAL
                        else -> APPLY_ALWAYS
                    }
                }
                else -> parameters[key] = value
            }
        }
    }

    fun whereToApply(): Int = apply

    fun apply(url: String): String {
        return when (type) {
            ADD_URL_PARAMETER -> {
                val name = parameters["name"] ?: return url
                val value = parameters["value"] ?: return url
                ZLNetworkUtil.appendParameter(url, name, value)
            }
            REWRITE -> {
                val pattern = parameters["pattern"] ?: return url
                val replacement = parameters["replacement"] ?: return url
                val matcher = Pattern.compile(pattern).matcher(url)
                if (matcher.matches()) {
                    var result = replacement
                    for (i in matcher.groupCount() downTo 1) {
                        result = result.replace("%$i", matcher.group(1))
                    }
                    result
                } else {
                    url
                }
            }
            else -> url
        }
    }
}
