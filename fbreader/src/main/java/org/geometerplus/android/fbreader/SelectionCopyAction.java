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

package org.geometerplus.android.fbreader;

import android.app.Application;
import android.text.ClipboardManager;

import org.geometerplus.android.util.UIMessageUtil;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.FBView;
import org.geometerplus.fbreader.util.TextSnippet;
import org.geometerplus.zlibrary.core.resources.ZLResource;

public class SelectionCopyAction extends FBAndroidAction {
    SelectionCopyAction(FBReader baseActivity, FBReaderApp fbreader) {
        super(baseActivity, fbreader);
    }

    @Override
    protected void run(Object... params) {
        final FBView fbview = Reader.getTextView();
        final TextSnippet snippet = fbview.getSelectedSnippet();
        if (snippet == null) {
            return;
        }

        final String text = snippet.getText();
        fbview.clearSelection();

        final ClipboardManager clipboard =
                (ClipboardManager) BaseActivity.getApplication().getSystemService(Application.CLIPBOARD_SERVICE);
        clipboard.setText(text);
        UIMessageUtil.showMessageText(
                BaseActivity,
                ZLResource.resource("selection").getResource("textInBuffer").getValue().replace("%s", clipboard.getText())
        );
    }
}
