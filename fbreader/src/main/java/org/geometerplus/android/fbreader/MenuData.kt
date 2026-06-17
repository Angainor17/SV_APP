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

package org.geometerplus.android.fbreader

import org.geometerplus.R
import org.geometerplus.android.fbreader.api.MenuNode
import org.geometerplus.fbreader.fbreader.ActionCode
import org.geometerplus.zlibrary.core.library.ZLibrary

object MenuData {
    private var nodes: List<MenuNode>? = null

    private fun addToplevelNode(node: MenuNode) {
        nodes!!.toMutableList().add(node)
    }

    @Synchronized
    fun topLevelNodes(): List<MenuNode> {
        if (nodes == null) {
            val mutableNodes = mutableListOf<MenuNode>()
            mutableNodes.add(MenuNode.Item(ActionCode.SHOW_LIBRARY, R.drawable.ic_menu_library))
            mutableNodes.add(MenuNode.Item(ActionCode.SHOW_NETWORK_LIBRARY, R.drawable.ic_menu_networklibrary))
            mutableNodes.add(MenuNode.Item(ActionCode.SHOW_TOC, R.drawable.ic_menu_toc))
            mutableNodes.add(MenuNode.Item(ActionCode.SHOW_BOOKMARKS, R.drawable.ic_menu_bookmarks))
            mutableNodes.add(MenuNode.Item(ActionCode.SWITCH_TO_NIGHT_PROFILE, R.drawable.ic_menu_night))
            mutableNodes.add(MenuNode.Item(ActionCode.SWITCH_TO_DAY_PROFILE, R.drawable.ic_menu_day))
            mutableNodes.add(MenuNode.Item(ActionCode.SEARCH, R.drawable.ic_menu_search))
            mutableNodes.add(MenuNode.Item(ActionCode.SHARE_BOOK))
            mutableNodes.add(MenuNode.Item(ActionCode.SHOW_PREFERENCES))
            mutableNodes.add(MenuNode.Item(ActionCode.SHOW_BOOK_INFO))
            val orientations = MenuNode.Submenu("screenOrientation")
            orientations.children.add(MenuNode.Item(ActionCode.SET_SCREEN_ORIENTATION_SYSTEM))
            orientations.children.add(MenuNode.Item(ActionCode.SET_SCREEN_ORIENTATION_SENSOR))
            orientations.children.add(MenuNode.Item(ActionCode.SET_SCREEN_ORIENTATION_PORTRAIT))
            orientations.children.add(MenuNode.Item(ActionCode.SET_SCREEN_ORIENTATION_LANDSCAPE))
            if (ZLibrary.Instance().supportsAllOrientations()) {
                orientations.children.add(MenuNode.Item(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_PORTRAIT))
                orientations.children.add(MenuNode.Item(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_LANDSCAPE))
            }
            mutableNodes.add(orientations)
            mutableNodes.add(MenuNode.Item(ActionCode.INCREASE_FONT))
            mutableNodes.add(MenuNode.Item(ActionCode.DECREASE_FONT))
            mutableNodes.add(MenuNode.Item(ActionCode.SHOW_NAVIGATION))
            mutableNodes.add(MenuNode.Item(ActionCode.INSTALL_PLUGINS))
            mutableNodes.add(MenuNode.Item(ActionCode.OPEN_WEB_HELP))
            mutableNodes.add(MenuNode.Item(ActionCode.OPEN_START_SCREEN))
            nodes = mutableNodes.toList()
        }
        return nodes!!
    }
}
