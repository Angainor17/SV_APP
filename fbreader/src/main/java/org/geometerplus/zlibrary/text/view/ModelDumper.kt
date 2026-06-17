package org.geometerplus.zlibrary.text.view

import org.geometerplus.zlibrary.text.model.ZLTextModel
import org.geometerplus.zlibrary.text.model.ZLTextParagraph

internal object ModelDumper {
    fun dump(model: ZLTextModel?) {
        System.err.println("+++ MODEL DUMP +++")
        if (model == null) {
            System.err.println("MODEL IS NULL")
        } else {
            System.err.println("PARAGRAPHS: ${model.paragraphsNumber}")
            for (i in 0 until model.paragraphsNumber) {
                val para = model.getParagraph(i)
                System.err.println("PARA NO $i")
                val it = para.iterator()
                while (it.next()) {
                    val elemType = it.type
                    when (elemType) {
                        ZLTextParagraph.Entry.TEXT ->
                            System.err.println("ELEM TEXT: ${String(it.textData, it.textOffset, it.textLength)}")
                        ZLTextParagraph.Entry.IMAGE ->
                            System.err.println("ELEM IMAGE")
                        ZLTextParagraph.Entry.CONTROL ->
                            System.err.println("ELEM CONTROL ${it.controlKind} ${it.controlIsStart}")
                        ZLTextParagraph.Entry.HYPERLINK_CONTROL ->
                            System.err.println("ELEM HYPERLINK_CONTROL")
                        ZLTextParagraph.Entry.STYLE_CSS ->
                            System.err.println("ELEM STYLE_CSS ${it.styleEntry}")
                        ZLTextParagraph.Entry.STYLE_OTHER ->
                            System.err.println("ELEM STYLE_OTHER ${it.styleEntry}")
                        ZLTextParagraph.Entry.STYLE_CLOSE ->
                            System.err.println("ELEM STYLE_CLOSE")
                        ZLTextParagraph.Entry.FIXED_HSPACE ->
                            System.err.println("ELEM FIXED_HSPACE")
                        else ->
                            System.err.println("ELEM $elemType")
                    }
                }
            }
        }
        System.err.println("--- MODEL DUMP ---")
    }
}
