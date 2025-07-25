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

package org.geometerplus.android.fbreader.network.action;

import android.app.Activity;
import android.content.Intent;

import org.geometerplus.android.fbreader.network.AddCustomCatalogActivity;
import org.geometerplus.android.fbreader.network.Util;
import org.geometerplus.fbreader.network.ICustomNetworkLink;
import org.geometerplus.fbreader.network.NetworkTree;
import org.geometerplus.fbreader.network.tree.NetworkCatalogRootTree;

public class EditCustomCatalogAction extends CatalogAction {
    public EditCustomCatalogAction(Activity activity) {
        super(activity, ActionCode.CUSTOM_CATALOG_EDIT, "editCustomCatalog");
    }

    @Override
    public boolean isVisible(NetworkTree tree) {
        return
                tree instanceof NetworkCatalogRootTree &&
                        tree.getLink() instanceof ICustomNetworkLink;
    }

    @Override
    public void run(NetworkTree tree) {
        final Intent intent = new Intent(myActivity, AddCustomCatalogActivity.class);
        Util.intentByLink(intent, tree.getLink());
        intent.setAction(Util.EDIT_CATALOG_ACTION);
        myActivity.startActivity(intent);
    }
}
