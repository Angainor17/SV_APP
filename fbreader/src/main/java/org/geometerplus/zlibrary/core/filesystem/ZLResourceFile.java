package org.geometerplus.zlibrary.core.filesystem;

import org.geometerplus.zlibrary.core.library.ZLibrary;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class ZLResourceFile extends ZLFile {
    private static Map<String, ZLResourceFile> ourCache =
            Collections.synchronizedMap(new HashMap<String, ZLResourceFile>());
    private final String myPath;

    protected ZLResourceFile(String path) {
        myPath = path;
        init();
    }

    public static ZLResourceFile createResourceFile(String path) {
        ZLResourceFile file = ourCache.get(path);
        if (file == null) {
            file = ZLibrary.Instance().createResourceFile(path);
            ourCache.put(path, file);
        }
        return file;
    }

    static ZLResourceFile createResourceFile(ZLResourceFile parent, String name) {
        return ZLibrary.Instance().createResourceFile(parent, name);
    }

    @Override
    public String getPath() {
        return myPath;
    }

    @Override
    public String getLongName() {
        return myPath.substring(myPath.lastIndexOf('/') + 1);
    }

    @Override
    public ZLPhysicalFile getPhysicalFile() {
        return null;
    }
}
