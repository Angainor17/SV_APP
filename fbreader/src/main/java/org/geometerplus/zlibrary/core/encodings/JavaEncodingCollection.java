package org.geometerplus.zlibrary.core.encodings;

import java.nio.charset.Charset;

public final class JavaEncodingCollection extends FilteredEncodingCollection {
    private volatile static JavaEncodingCollection ourInstance;

    private JavaEncodingCollection() {
        super();
    }

    public static JavaEncodingCollection Instance() {
        if (ourInstance == null) {
            ourInstance = new JavaEncodingCollection();
        }
        return ourInstance;
    }

    @Override
    public boolean isEncodingSupported(String name) {
        try {
            return Charset.forName(name) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
