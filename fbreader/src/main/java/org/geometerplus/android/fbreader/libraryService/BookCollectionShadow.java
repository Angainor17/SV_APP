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

package org.geometerplus.android.fbreader.libraryService;

import org.geometerplus.fbreader.book.AbstractBookCollection;
import org.geometerplus.fbreader.book.Author;
import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.book.BookQuery;
import org.geometerplus.fbreader.book.Bookmark;
import org.geometerplus.fbreader.book.BookmarkQuery;
import org.geometerplus.fbreader.book.Filter;
import org.geometerplus.fbreader.book.HighlightingStyle;
import org.geometerplus.fbreader.book.Tag;
import org.geometerplus.fbreader.book.UID;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;

import java.util.Collections;
import java.util.List;

/**
 * A placeholder book collection.
 *
 * The original implementation proxied every call to a {@code LibraryService} via AIDL.
 * That service is no longer registered in the app, so at runtime every call already
 * returned an empty or default result. This class now returns those same results
 * directly, without the IPC layer.
 */
public class BookCollectionShadow extends AbstractBookCollection<Book> {
    @Override
    public Status status() {
        return Status.NotStarted;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public List<Book> books(BookQuery query) {
        return Collections.emptyList();
    }

    @Override
    public boolean hasBooks(Filter filter) {
        return false;
    }

    @Override
    public List<String> titles(BookQuery query) {
        return Collections.emptyList();
    }

    @Override
    public List<Book> recentlyOpenedBooks(int count) {
        return Collections.emptyList();
    }

    @Override
    public List<Book> recentlyAddedBooks(int count) {
        return Collections.emptyList();
    }

    @Override
    public Book getRecentBook(int index) {
        return null;
    }

    @Override
    public void addToRecentlyOpened(Book book) {
    }

    @Override
    public void removeFromRecentlyOpened(Book book) {
    }

    @Override
    public Book getBookByFile(String path) {
        return null;
    }

    @Override
    public Book getBookById(long id) {
        return null;
    }

    @Override
    public Book getBookByUid(UID uid) {
        return null;
    }

    @Override
    public Book getBookByHash(String hash) {
        return null;
    }

    @Override
    public List<String> labels() {
        return Collections.emptyList();
    }

    @Override
    public List<Author> authors() {
        return Collections.emptyList();
    }

    @Override
    public boolean hasSeries() {
        return false;
    }

    @Override
    public List<String> series() {
        return Collections.emptyList();
    }

    @Override
    public List<Tag> tags() {
        return Collections.emptyList();
    }

    @Override
    public List<String> firstTitleLetters() {
        return Collections.emptyList();
    }

    @Override
    public boolean saveBook(Book book) {
        return false;
    }

    @Override
    public boolean canRemoveBook(Book book, boolean deleteFromDisk) {
        return false;
    }

    @Override
    public void removeBook(Book book, boolean deleteFromDisk) {
    }

    @Override
    public String getHash(Book book, boolean force) {
        return null;
    }

    @Override
    public void setHash(Book book, String hash) {
    }

    @Override
    public ZLTextFixedPosition.WithTimestamp getStoredPosition(long bookId) {
        return null;
    }

    @Override
    public void storePosition(long bookId, ZLTextPosition position) {
    }

    @Override
    public boolean isHyperlinkVisited(Book book, String linkId) {
        return false;
    }

    @Override
    public void markHyperlinkAsVisited(Book book, String linkId) {
    }

    @Override
    public String getCoverUrl(Book book) {
        return null;
    }

    @Override
    public String getDescription(Book book) {
        return null;
    }

    @Override
    public List<Bookmark> bookmarks(BookmarkQuery query) {
        return Collections.emptyList();
    }

    @Override
    public void saveBookmark(Bookmark bookmark) {
    }

    @Override
    public void deleteBookmark(Bookmark bookmark) {
    }

    @Override
    public List<String> deletedBookmarkUids() {
        return Collections.emptyList();
    }

    @Override
    public void purgeBookmarks(List<String> uids) {
    }

    @Override
    public HighlightingStyle getHighlightingStyle(int styleId) {
        return null;
    }

    @Override
    public List<HighlightingStyle> highlightingStyles() {
        return Collections.emptyList();
    }

    @Override
    public void saveHighlightingStyle(HighlightingStyle style) {
    }

    @Override
    public int getDefaultHighlightingStyleId() {
        return 1;
    }

    @Override
    public void setDefaultHighlightingStyleId(int styleId) {
    }

    @Override
    public List<FormatDescriptor> formats() {
        return Collections.emptyList();
    }

    @Override
    public boolean setActiveFormats(List<String> formatIds) {
        return false;
    }

    @Override
    public void rescan(String path) {
    }

    public Book createBook(long id, String url, String title, String encoding, String language) {
        return new Book(id, url.substring("file://".length()), title, encoding, language);
    }
}
