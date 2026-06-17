package org.geometerplus.zlibrary.core.fonts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FontManager {
    public final Map<String, FontEntry> Entries =
            Collections.synchronizedMap(new HashMap<String, FontEntry>());
    private final ArrayList<List<String>> myFamilyLists = new ArrayList<List<String>>();

    public synchronized int index(List<String> families) {
        for (int i = 0; i < myFamilyLists.size(); ++i) {
            if (myFamilyLists.get(i).equals(families)) {
                return i;
            }
        }
        myFamilyLists.add(new ArrayList<String>(families));
        return myFamilyLists.size() - 1;
    }

    public synchronized List<FontEntry> getFamilyEntries(int index) {
        try {
            final List<String> families = myFamilyLists.get(index);
            final ArrayList<FontEntry> entries = new ArrayList<FontEntry>(families.size());
            for (String f : families) {
                final FontEntry e = Entries.get(f);
                entries.add(e != null ? e : FontEntry.systemEntry(f));
            }
            return entries;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
