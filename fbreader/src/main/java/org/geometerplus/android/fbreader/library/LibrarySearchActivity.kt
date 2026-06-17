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

package org.geometerplus.android.fbreader.library

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle

class LibrarySearchActivity : Activity() {
    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        var intent = intent
        if (Intent.ACTION_SEARCH == intent.action) {
            val pattern = intent.getStringExtra(SearchManager.QUERY)
            if (!pattern.isNullOrEmpty()) {
                intent = Intent(LibraryActivity.START_SEARCH_ACTION, null, this, LibraryActivity::class.java)
                intent.putExtra(SearchManager.QUERY, pattern)
                startActivity(intent)
            }
        }
        finish()
    }
}
