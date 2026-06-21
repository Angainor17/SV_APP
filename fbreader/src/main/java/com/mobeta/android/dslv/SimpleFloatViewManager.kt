package com.mobeta.android.dslv

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView

/**
 * Simple implementation of the FloatViewManager class. Uses list
 * items as they appear in the ListView to create the floating View.
 */
open class SimpleFloatViewManager(private val listView: ListView) : DragSortListView.FloatViewManager {

    private var floatBitmap: Bitmap? = null
    private var imageView: ImageView? = null
    private var floatBGColor = Color.BLACK

    open fun setBackgroundColor(color: Int) {
        floatBGColor = color
    }

    /**
     * This simple implementation creates a Bitmap copy of the
     * list item currently shown at ListView `position`.
     */
    override fun onCreateFloatView(position: Int): View? {
        // Guaranteed that this will not be null? I think so. Nope, got
        // a NullPointerException once...
        val v = listView.getChildAt(position + listView.headerViewsCount - listView.firstVisiblePosition)

        if (v == null) {
            return null
        }

        v.isPressed = false

        // Create a copy of the drawing cache so that it does not get
        // recycled by the framework when the list tries to clean up memory
        v.isDrawingCacheEnabled = true
        floatBitmap = Bitmap.createBitmap(v.drawingCache)
        v.isDrawingCacheEnabled = false

        if (imageView == null) {
            imageView = ImageView(listView.context)
        }
        imageView!!.setBackgroundColor(floatBGColor)
        imageView!!.setPadding(0, 0, 0, 0)
        imageView!!.setImageBitmap(floatBitmap)
        imageView!!.layoutParams = ViewGroup.LayoutParams(v.width, v.height)

        return imageView
    }

    /**
     * This does nothing
     */
    override fun onDragFloatView(floatView: View, position: Point, touch: Point) {
        // do nothing
    }

    /**
     * Removes the Bitmap from the ImageView created in
     * onCreateFloatView() and tells the system to recycle it.
     */
    override fun onDestroyFloatView(floatView: View) {
        (floatView as ImageView).setImageDrawable(null)

        floatBitmap?.recycle()
        floatBitmap = null
    }
}
