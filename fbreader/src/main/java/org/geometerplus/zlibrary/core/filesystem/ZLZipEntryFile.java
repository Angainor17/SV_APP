package org.geometerplus.zlibrary.core.filesystem;

import org.amse.ys.zip.LocalFileHeader;
import org.amse.ys.zip.ZipFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

final class ZLZipEntryFile extends ZLArchiveEntryFile {
    private static HashMap<ZLFile, ZipFile> ourZipFileMap = new HashMap<ZLFile, ZipFile>();

    ZLZipEntryFile(ZLFile parent, String name) {
        super(parent, name);
    }

    static List<ZLFile> archiveEntries(ZLFile archive) {
        try {
            final ZipFile zf = ZLZipEntryFile.getZipFile(archive);
            final Collection<LocalFileHeader> headers = zf.headers();
            if (!headers.isEmpty()) {
                ArrayList<ZLFile> entries = new ArrayList<ZLFile>(headers.size());
                for (LocalFileHeader h : headers) {
                    entries.add(new ZLZipEntryFile(archive, h.FileName));
                }
                return entries;
            }
        } catch (IOException e) {
        }
        return Collections.emptyList();
    }

    private static ZipFile getZipFile(final ZLFile file) throws IOException {
        synchronized (ourZipFileMap) {
            ZipFile zf = file.isCached() ? ourZipFileMap.get(file) : null;
            if (zf == null) {
                zf = new ZipFile(file);
                if (file.isCached()) {
                    ourZipFileMap.put(file, zf);
                }
            }
            return zf;
        }
    }

    static void removeFromCache(ZLFile file) {
        ourZipFileMap.remove(file);
    }

    @Override
    public boolean exists() {
        try {
            return myParent.exists() && getZipFile(myParent).entryExists(myName);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public long size() {
        try {
            return getZipFile(myParent).getEntrySize(myName);
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return getZipFile(myParent).getInputStream(myName);
    }
}
