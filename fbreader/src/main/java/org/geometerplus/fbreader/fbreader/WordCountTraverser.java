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

import org.geometerplus.zlibrary.text.view.ZLTextControlElement;
import org.geometerplus.zlibrary.text.view.ZLTextTraverser;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.text.view.ZLTextWord;

class WordCountTraverser extends ZLTextTraverser {
    protected int myCount;

    WordCountTraverser(ZLTextView view) {
        super(view);
    }

    @Override
    protected void processWord(ZLTextWord word) {
        ++myCount;
    }

    @Override
    protected void processControlElement(ZLTextControlElement control) {
        // does nothing
    }

    @Override
    protected void processSpace() {
        // does nothing
    }

    @Override
    protected void processNbSpace() {
        // does nothing
    }

    @Override
    protected void processEndOfParagraph() {
        // does nothing
    }

    public int getCount() {
        return myCount;
    }
}
