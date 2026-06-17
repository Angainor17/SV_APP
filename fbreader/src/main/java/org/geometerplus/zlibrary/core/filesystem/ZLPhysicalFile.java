package org.geometerplus.zlibrary.core.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ZLPhysicalFile extends ZLFile {
    private final File myFile;
    private Boolean myIsDirectory;
    private String myPath;

    ZLPhysicalFile(String path) {
        this(new File(path));
    }

    public ZLPhysicalFile(File file) {
        myFile = file;
        init();
    }

    @Override
    public boolean exists() {
        return myFile.exists();
    }

    @Override
    public long size() {
        return myFile.length();
    }

    @Override
    public long lastModified() {
        return myFile.lastModified();
    }

    @Override
    public boolean isDirectory() {
        if (myIsDirectory == null) {
            myIsDirectory = myFile.isDirectory();
        }
        return myIsDirectory;
    }

    @Override
    public boolean isReadable() {
        return myFile.canRead();
    }

    public boolean delete() {
        return myFile.delete();
    }

    public File javaFile() {
        return myFile;
    }

    @Override
    public String getPath() {
        if (myPath == null) {
            try {
                myPath = myFile.getCanonicalPath();
            } catch (Exception e) {
                // should be never thrown
                myPath = myFile.getPath();
            }
        }
        return myPath;
    }

    @Override
    public String getLongName() {
        return isDirectory() ? getPath() : myFile.getName();
    }

    @Override
    public ZLFile getParent() {
        return isDirectory() ? null : new ZLPhysicalFile(myFile.getParent());
    }

    @Override
    public ZLPhysicalFile getPhysicalFile() {
        return this;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(myFile);
    }

    protected List<ZLFile> directoryEntries() {
        File[] subFiles = myFile.listFiles();
        if (subFiles == null || subFiles.length == 0) {
            return Collections.emptyList();
        }

        ArrayList<ZLFile> entries = new ArrayList<ZLFile>(subFiles.length);
        for (File f : subFiles) {
            if (!f.getName().startsWith(".")) {
                entries.add(new ZLPhysicalFile(f));
            }
        }
        return entries;
    }
}
