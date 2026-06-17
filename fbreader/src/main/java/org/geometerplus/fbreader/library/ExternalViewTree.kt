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

package org.geometerplus.fbreader.library

import org.geometerplus.zlibrary.core.resources.ZLResource

class ExternalViewTree internal constructor(parent: RootTree) : LibraryTree(parent) {

    private val resource: ZLResource = resource().getResource(ROOT_EXTERNAL_VIEW)

    override val name: String get() = resource.value

    override val stringId: String get() = ROOT_EXTERNAL_VIEW

    override val summary: String get() = resource.getResource("summary").value
}
