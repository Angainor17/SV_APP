package org.geometerplus.zlibrary.core.xml;

import org.geometerplus.zlibrary.core.filesystem.ZLFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class ZLXMLProcessor {
    public static Map<String, char[]> getEntityMap(List<String> dtdList) {
        try {
            return ZLXMLParser.getDTDMap(dtdList);
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }

    public static void read(ZLXMLReader xmlReader, InputStream stream, int bufferSize) throws IOException {
        ZLXMLParser parser = null;
        try {
            parser = new ZLXMLParser(xmlReader, stream, bufferSize);
            xmlReader.startDocumentHandler();
            parser.doIt();
            xmlReader.endDocumentHandler();
        } finally {
            if (parser != null) {
                parser.finish();
            }
        }
    }

    public static void read(ZLXMLReader xmlReader, Reader reader, int bufferSize) throws IOException {
        ZLXMLParser parser = null;
        try {
            parser = new ZLXMLParser(xmlReader, reader, bufferSize);
            xmlReader.startDocumentHandler();
            parser.doIt();
            xmlReader.endDocumentHandler();
        } finally {
            if (parser != null) {
                parser.finish();
            }
        }
    }

    public static void read(ZLXMLReader xmlReader, ZLFile file) throws IOException {
        read(xmlReader, file, 65536);
    }

    public static void read(ZLXMLReader xmlReader, ZLFile file, int bufferSize) throws IOException {
        InputStream stream = file.getInputStream();
        try {
            read(xmlReader, stream, bufferSize);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }
}
