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

package org.geometerplus.zlibrary.core.filesystem.tar;

import org.geometerplus.zlibrary.core.filesystem.ZLArchiveEntryFile;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public final class ZLTarEntryFile extends ZLArchiveEntryFile {
    public ZLTarEntryFile(ZLFile parent, String name) {
        super(parent, name);
    }

    public static List<ZLFile> archiveEntries(ZLFile archive) {
        try {
            InputStream stream = archive.getInputStream();
            if (stream != null) {
                LinkedList<ZLFile> entries = new LinkedList<ZLFile>();
                ZLTarHeader header = new ZLTarHeader();
                while (header.read(stream)) {
                    if (header.IsRegularFile) {
                        entries.add(new ZLTarEntryFile(archive, header.Name));
                    }
                    final int lenToSkip = (header.Size + 0x1ff) & -0x200;
                    if (lenToSkip < 0) {
                        break;
                    }
                    if (stream.skip(lenToSkip) != lenToSkip) {
                        break;
                    }
                    header.erase();
                }
                stream.close();
                return entries;
            }
        } catch (IOException e) {
        }
        return Collections.emptyList();
    }

    @Override
    public boolean exists() {
        // TODO: optimize
        return myParent.exists() && archiveEntries(myParent).contains(this);
    }

    @Override
    public long size() {
        throw new RuntimeException("Not implemented yet.");
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ZLTarInputStream(myParent.getInputStream(), myName);
    }
}
