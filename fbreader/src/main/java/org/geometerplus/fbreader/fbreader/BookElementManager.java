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

package org.geometerplus.fbreader.fbreader;

import org.geometerplus.fbreader.network.NetworkLibrary;
import org.geometerplus.fbreader.network.opds.OPDSBookItem;
import org.geometerplus.fbreader.network.opds.OPDSXMLReader;
import org.geometerplus.fbreader.network.opds.SimpleOPDSFeedHandler;
import org.geometerplus.zlibrary.core.network.QuietNetworkContext;
import org.geometerplus.zlibrary.core.network.ZLNetworkException;
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest;
import org.geometerplus.zlibrary.text.view.ExtensionElementManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

class BookElementManager extends ExtensionElementManager {
    private final FBView myView;
    private final Runnable myScreenRefresher;
    private final Map<Map<String, String>, List<BookElement>> myCache =
            new HashMap<Map<String, String>, List<BookElement>>();
    private Timer myTimer;

    BookElementManager(final FBView view) {
        myView = view;
        myScreenRefresher = new Runnable() {
            public void run() {
                view.Application.getViewWidget().reset();
                view.Application.getViewWidget().repaint();
            }
        };
    }

    @Override
    protected synchronized List<BookElement> getElements(String type, Map<String, String> data) {
        if (!"opds".equals(type)) {
            return Collections.emptyList();
        }

        List<BookElement> elements = myCache.get(data);
        if (elements == null) {
            try {
                final int count = Integer.valueOf(data.get("size"));
                elements = new ArrayList<BookElement>(count);
                for (int i = 0; i < count; ++i) {
                    elements.add(new BookElement(myView));
                }
                startLoading(data.get("src"), elements);
            } catch (Throwable t) {
                return Collections.emptyList();
            }
            myCache.put(data, elements);
        }
        return Collections.unmodifiableList(elements);
    }

    private void startLoading(final String url, final List<BookElement> elements) {
        final NetworkLibrary library = NetworkLibrary.Instance(myView.Application.SystemInfo);

        new Thread() {
            public void run() {
                final SimpleOPDSFeedHandler handler = new SimpleOPDSFeedHandler(library, url);
                try {
                    new QuietNetworkContext().perform(new ZLNetworkRequest.Get(url, true) {
                        @Override
                        public void handleStream(InputStream inputStream, int length) throws IOException, ZLNetworkException {
                            new OPDSXMLReader(library, handler, false).read(inputStream);
                        }
                    });
                    if (handler.books().isEmpty()) {
                        throw new RuntimeException();
                    }
                    myTimer = null;
                    final List<OPDSBookItem> items = handler.books();
                    int index = 0;
                    for (BookElement book : elements) {
                        book.setData(items.get(index));
                        index = (index + 1) % items.size();
                        myScreenRefresher.run();
                    }
                } catch (Exception e) {
                    if (myTimer == null) {
                        myTimer = new Timer();
                    }
                    myTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            startLoading(url, elements);
                        }
                    }, 10000);
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
