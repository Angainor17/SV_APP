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

package org.geometerplus.zlibrary.text.view

import org.geometerplus.zlibrary.text.model.ZLTextModel
import org.geometerplus.zlibrary.text.model.ZLTextParagraph

object ModelDumper {
    @JvmStatic
    fun dump(model: ZLTextModel?) {
        System.err.println("+++ MODEL DUMP +++")
        if (model == null) {
            System.err.println("MODEL IS NULL")
        } else {
            System.err.println("PARAGRAPHS: " + model.getParagraphsNumber())
            for (i in 0 until model.getParagraphsNumber()) {
                val para = model.getParagraph(i) ?: continue
                System.err.println("PARA NO $i")
                val it = para.iterator() ?: continue
                while (it.next()) {
                    val elemType = it.getType()
                    when (elemType) {
                        ZLTextParagraph.Entry.TEXT ->
                            System.err.println("ELEM TEXT: " + String(it.getTextData(), it.getTextOffset(), it.getTextLength()))
                        ZLTextParagraph.Entry.IMAGE ->
                            System.err.println("ELEM IMAGE")
                        ZLTextParagraph.Entry.CONTROL ->
                            System.err.println("ELEM CONTROL " + it.getControlKind() + " " + it.getControlIsStart())
                        ZLTextParagraph.Entry.HYPERLINK_CONTROL ->
                            System.err.println("ELEM HYPERLINK_CONTROL")
                        ZLTextParagraph.Entry.STYLE_CSS ->
                            System.err.println("ELEM STYLE_CSS " + it.getStyleEntry())
                        ZLTextParagraph.Entry.STYLE_OTHER ->
                            System.err.println("ELEM STYLE_OTHER " + it.getStyleEntry())
                        ZLTextParagraph.Entry.STYLE_CLOSE ->
                            System.err.println("ELEM STYLE_CLOSE")
                        ZLTextParagraph.Entry.FIXED_HSPACE ->
                            System.err.println("ELEM FIXED_HSPACE")
                        else ->
                            System.err.println("ELEM elemType")
                    }
                }
            }
        }
        System.err.println("--- MODEL DUMP ---")
    }
}
