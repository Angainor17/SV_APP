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

package org.geometerplus.android.fbreader;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import org.geometerplus.android.fbreader.api.PluginApi;
import org.geometerplus.android.util.OrientationUtil;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

class RunPluginAction extends FBAndroidAction {
    private final Uri myUri;

    RunPluginAction(FBReader baseActivity, FBReaderApp fbreader, Uri uri) {
        super(baseActivity, fbreader);
        myUri = uri;
    }

    @Override
    protected void run(Object... params) {
        if (myUri == null) {
            return;
        }
        try {
            OrientationUtil.startActivity(
                    BaseActivity, new Intent(PluginApi.ACTION_RUN, myUri)
            );
        } catch (ActivityNotFoundException e) {
        }
    }
}
