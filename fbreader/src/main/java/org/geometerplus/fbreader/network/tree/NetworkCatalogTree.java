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

package org.geometerplus.fbreader.network.tree;

import org.fbreader.util.Boolean3;
import org.fbreader.util.Pair;
import org.geometerplus.fbreader.network.INetworkLink;
import org.geometerplus.fbreader.network.ISyncNetworkLink;
import org.geometerplus.fbreader.network.NetworkBookItem;
import org.geometerplus.fbreader.network.NetworkCatalogItem;
import org.geometerplus.fbreader.network.NetworkItem;
import org.geometerplus.fbreader.network.NetworkLibrary;
import org.geometerplus.fbreader.network.NetworkTree;
import org.geometerplus.fbreader.network.SearchItem;
import org.geometerplus.fbreader.network.SingleCatalogSearchItem;
import org.geometerplus.fbreader.network.urlInfo.UrlInfo;
import org.geometerplus.fbreader.tree.FBTree;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.network.QuietNetworkContext;
import org.geometerplus.zlibrary.core.network.ZLNetworkContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;

public class NetworkCatalogTree extends NetworkTree {
    public final NetworkCatalogItem Item;
    protected final ArrayList<NetworkCatalogItem> myChildrenItems =
            new ArrayList<NetworkCatalogItem>();
    private final INetworkLink myLink;
    private final Set<NetworkTree> myUnconfirmedTrees =
            Collections.synchronizedSet(new HashSet<NetworkTree>());
    private volatile int myLastTotalChildren = -1;
    private long myLoadedTime = -1;
    private SearchItem mySearchItem;

    public NetworkCatalogTree(NetworkTree parent, INetworkLink link, NetworkCatalogItem item, int position) {
        super(parent, position);
        myLink = link;
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        Item = item;
    }

    @Override
    public INetworkLink getLink() {
        return myLink;
    }

    public Boolean3 getVisibility() {
        return Item.getVisibility();
    }

    public final boolean canBeOpened() {
        return Item.canBeOpened();
    }

    protected void addSpecialTrees() {
        if ((Item.getFlags() & NetworkCatalogItem.FLAG_ADD_SEARCH_ITEM) != 0) {
            final INetworkLink link = getLink();
            if (link != null && link.getUrl(UrlInfo.Type.Search) != null) {
                if (mySearchItem == null) {
                    mySearchItem = new SingleCatalogSearchItem(link);
                }
                myChildrenItems.add(mySearchItem);
                new SearchCatalogTree(this, mySearchItem);
            }
        }
    }

    synchronized void addItem(final NetworkItem item) {
        if (!hasChildren() && !isSingleSyncItem(item)) {
            addSpecialTrees();
        }
        if (item instanceof NetworkCatalogItem) {
            myChildrenItems.add((NetworkCatalogItem) item);
        }
        myUnconfirmedTrees.add(NetworkTreeFactory.createNetworkTree(this, item));
        Library.fireModelChangedEvent(NetworkLibrary.ChangeListener.Code.SomeCode);
    }

    @Override
    public String getName() {
        final CharSequence title = Item.Title;
        return title != null ? String.valueOf(title) : "";
    }

    @Override
    public String getSummary() {
        final CharSequence summary = Item.getSummary();
        return summary != null ? summary.toString() : "";
    }

    @Override
    public Pair<String, String> getTreeTitle() {
        final INetworkLink link = getLink();
        return new Pair(getName(), link != null ? link.getTitle() : null);
    }

    @Override
    protected ZLImage createCover() {
        return createCoverForItem(Library, Item, true);
    }

    public boolean isContentValid() {
        if (myLoadedTime < 0) {
            return false;
        }
        final int reloadTime = 15 * 60 * 1000; // 15 minutes in milliseconds
        return System.currentTimeMillis() - myLoadedTime < reloadTime;
    }

    public void updateLoadedTime() {
        myLoadedTime = System.currentTimeMillis();
    }

    public void updateVisibility() {
        final LinkedList<FBTree> toRemove = new LinkedList<FBTree>();

        ListIterator<FBTree> nodeIterator = subtrees().listIterator();
        FBTree currentTree = null;
        int nodeCount = 0;

        for (int i = 0; i < myChildrenItems.size(); ++i) {
            final NetworkCatalogItem currentItem = myChildrenItems.get(i);
            boolean processed = false;
            while (currentTree != null || nodeIterator.hasNext()) {
                if (currentTree == null) {
                    currentTree = nodeIterator.next();
                }
                if (!(currentTree instanceof NetworkCatalogTree)) {
                    currentTree = null;
                    ++nodeCount;
                    continue;
                }
                NetworkCatalogTree child = (NetworkCatalogTree) currentTree;
                if (child.Item == currentItem) {
                    switch (child.Item.getVisibility()) {
                        case TRUE:
                            child.updateVisibility();
                            break;
                        case FALSE:
                            toRemove.add(child);
                            break;
                        case UNDEFINED:
                            child.clearCatalog();
                            break;
                    }
                    currentTree = null;
                    ++nodeCount;
                    processed = true;
                    break;
                } else {
                    boolean found = false;
                    for (int j = i + 1; j < myChildrenItems.size(); ++j) {
                        if (child.Item == myChildrenItems.get(j)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        toRemove.add(currentTree);
                        currentTree = null;
                        ++nodeCount;
                    } else {
                        break;
                    }
                }
            }
            final int nextIndex = nodeIterator.nextIndex();
            if (!processed && NetworkTreeFactory.createNetworkTree(this, currentItem, nodeCount) != null) {
                ++nodeCount;
                nodeIterator = subtrees().listIterator(nextIndex + 1);
            }
        }

        while (currentTree != null || nodeIterator.hasNext()) {
            if (currentTree == null) {
                currentTree = nodeIterator.next();
            }
            if (currentTree instanceof NetworkCatalogTree) {
                toRemove.add(currentTree);
            }
            currentTree = null;
        }

        for (FBTree tree : toRemove) {
            tree.removeSelf();
        }
    }

    @Override
    public void removeTrees(Set<NetworkTree> trees) {
        for (NetworkTree t : trees) {
            if (t instanceof NetworkCatalogTree) {
                myChildrenItems.remove(((NetworkCatalogTree) t).Item);
            }
        }
        super.removeTrees(trees);
    }

    @Override
    protected String getStringId() {
        return Item.getStringId();
    }

    public void startItemsLoader(ZLNetworkContext nc, boolean authenticate, boolean resumeNotLoad) {
        new CatalogExpander(nc, this, authenticate, resumeNotLoad).start();
    }

    public synchronized void clearCatalog() {
        myChildrenItems.clear();
        myLastTotalChildren = -1;
        clear();
        Library.fireModelChangedEvent(NetworkLibrary.ChangeListener.Code.SomeCode);
    }

    public final void confirmAllItems() {
        myUnconfirmedTrees.clear();
    }

    public final void removeUnconfirmedItems() {
        synchronized (myUnconfirmedTrees) {
            removeTrees(myUnconfirmedTrees);
        }
    }

    public synchronized void loadMoreChildren(int currentTotal) {
        if (currentTotal == subtrees().size()
                && myLastTotalChildren < currentTotal
                && !Library.isLoadingInProgress(this)
                && Item.canResumeLoading()) {
            myLastTotalChildren = currentTotal;
            startItemsLoader(new QuietNetworkContext(), false, true);
        }
    }

    private boolean isSingleSyncItem(NetworkItem item) {
        if (!(item instanceof NetworkBookItem)) {
            return false;
        }
        final INetworkLink link = getLink();
        if (!(link instanceof ISyncNetworkLink)) {
            return false;
        }
        return "fbreader:book:network:description".equals(((NetworkBookItem) item).Id);
    }
}
