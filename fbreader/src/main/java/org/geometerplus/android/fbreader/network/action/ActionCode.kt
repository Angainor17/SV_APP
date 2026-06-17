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

package org.geometerplus.android.fbreader.network.action

interface ActionCode {
    companion object {
        const val TREE_SHOW_CONTEXT_MENU = -2
        const val TREE_NO_ACTION = -1

        const val SEARCH = 1
        const val REFRESH = 2
        const val LANGUAGE_FILTER = 3

        const val RELOAD_CATALOG = 11
        const val OPEN_CATALOG = 12
        const val OPEN_IN_BROWSER = 13
        const val OPEN_ROOT = 14

        const val SIGNUP = 21
        const val SIGNIN = 22
        const val SIGNOUT = 23
        const val TOPUP = 24

        const val CUSTOM_CATALOG_ADD = 31
        const val CUSTOM_CATALOG_EDIT = 32
        const val CUSTOM_CATALOG_REMOVE = 33
        const val MANAGE_CATALOGS = 34
        const val DISABLE_CATALOG = 35

        const val BASKET_CLEAR = 41
        const val BASKET_BUY_ALL_BOOKS = 42

        const val DOWNLOAD_BOOK = 51
        const val DOWNLOAD_DEMO = 52
        const val READ_BOOK = 53
        const val READ_DEMO = 54
        const val DELETE_BOOK = 55
        const val DELETE_DEMO = 56
        const val BUY_DIRECTLY = 57
        const val BUY_IN_BROWSER = 58
        const val SHOW_BOOK_ACTIVITY = 59
        const val SHOW_BOOKS = 60
        const val ADD_BOOK_TO_BASKET = 61
        const val REMOVE_BOOK_FROM_BASKET = 62
        const val OPEN_BASKET = 63
    }
}
