package org.geometerplus.zlibrary.core.library;

import org.geometerplus.zlibrary.core.filesystem.ZLResourceFile;
import org.geometerplus.zlibrary.core.options.ZLIntegerOption;
import org.geometerplus.zlibrary.core.options.ZLStringOption;

import java.util.List;

public abstract class ZLibrary {
    public static final String SCREEN_ORIENTATION_SYSTEM = "system";
    public static final String SCREEN_ORIENTATION_SENSOR = "sensor";
    public static final String SCREEN_ORIENTATION_PORTRAIT = "portrait";
    public static final String SCREEN_ORIENTATION_LANDSCAPE = "landscape";
    public static final String SCREEN_ORIENTATION_REVERSE_PORTRAIT = "reversePortrait";
    public static final String SCREEN_ORIENTATION_REVERSE_LANDSCAPE = "reverseLandscape";
    private static ZLibrary ourImplementation;
    public final ZLIntegerOption ScreenHintStageOption =
            new ZLIntegerOption("LookNFeel", "ScreenHintStage", 0);

    protected ZLibrary() {
        ourImplementation = this;
    }

    public static ZLibrary Instance() {
        return ourImplementation;
    }

    public final ZLStringOption getOrientationOption() {
        return new ZLStringOption("LookNFeel", "Orientation", "system");
    }

    abstract public ZLResourceFile createResourceFile(String path);

    abstract public ZLResourceFile createResourceFile(ZLResourceFile parent, String name);

    abstract public String getVersionName();

    abstract public String getFullVersionName();

    abstract public String getCurrentTimeString();

    abstract public int getDisplayDPI();

    abstract public int getWidthInPixels();

    abstract public int getHeightInPixels();

    abstract public List<String> defaultLanguageCodes();

    abstract public boolean supportsAllOrientations();

    public String[] allOrientations() {
        return supportsAllOrientations()
                ? new String[]{
                SCREEN_ORIENTATION_SYSTEM,
                SCREEN_ORIENTATION_SENSOR,
                SCREEN_ORIENTATION_PORTRAIT,
                SCREEN_ORIENTATION_LANDSCAPE,
                SCREEN_ORIENTATION_REVERSE_PORTRAIT,
                SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        }
                : new String[]{
                SCREEN_ORIENTATION_SYSTEM,
                SCREEN_ORIENTATION_SENSOR,
                SCREEN_ORIENTATION_PORTRAIT,
                SCREEN_ORIENTATION_LANDSCAPE
        };
    }
}
