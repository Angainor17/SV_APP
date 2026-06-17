package org.geometerplus.android.util

import android.view.View
import android.widget.ImageView
import android.widget.TextView

object ViewUtil {
    @JvmStatic
    fun findView(container: View, id: Int): View {
        var view = container.getTag(id) as? View
        if (view == null) {
            view = container.findViewById(id)
            container.setTag(id, view)
        }
        return view
    }

    @JvmStatic
    fun findTextView(container: View, id: Int): TextView = findView(container, id) as TextView

    @JvmStatic
    fun findImageView(container: View, id: Int): ImageView = findView(container, id) as ImageView

    @JvmStatic
    fun setSubviewText(view: View, resourceId: Int, text: String) {
        findTextView(view, resourceId).text = text
    }
}
