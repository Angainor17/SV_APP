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

package org.geometerplus.fbreader.network.authentication.litres;

import org.geometerplus.zlibrary.core.network.ZLNetworkException;
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest;

import java.io.IOException;
import java.io.InputStream;

public class LitResNetworkRequest extends ZLNetworkRequest.PostWithMap {
    public final LitResAuthenticationXMLReader Reader;

    public LitResNetworkRequest(String url, LitResAuthenticationXMLReader reader) {
        super(clean(url));
        final int index = url.indexOf('?');
        if (index != -1) {
            for (String param : url.substring(index + 1).split("&")) {
                String[] pp = param.split("=");
                if (pp.length == 2) {
                    addPostParameter(pp[0], pp[1]);
                }
            }
        }
        Reader = reader;
    }

    static String clean(String url) {
        final int index = url.indexOf('?');
        return index != -1 ? url.substring(0, index) : url;
    }

    @Override
    public void handleStream(InputStream inputStream, int length) throws IOException, ZLNetworkException {
        Reader.read(inputStream);
        ZLNetworkException e = Reader.getException();
        if (e != null) {
            throw e;
        }
    }
}
