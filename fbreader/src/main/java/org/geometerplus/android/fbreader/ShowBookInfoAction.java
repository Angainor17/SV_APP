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

import android.content.Intent;

import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.library.BookInfoActivity;
import org.geometerplus.android.util.OrientationUtil;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

class ShowBookInfoAction extends FBAndroidAction {
    ShowBookInfoAction(FBReader baseActivity, FBReaderApp fbreader) {
        super(baseActivity, fbreader);
    }

    @Override
    public boolean isVisible() {
        return Reader.Model != null;
    }

    @Override
    protected void run(Object... params) {
        final Intent intent =
                new Intent(BaseActivity.getApplicationContext(), BookInfoActivity.class)
                        .putExtra(BookInfoActivity.FROM_READING_MODE_KEY, true);
        FBReaderIntents.putBookExtra(intent, Reader.getCurrentBook());
        OrientationUtil.startActivity(BaseActivity, intent);
    }
}
