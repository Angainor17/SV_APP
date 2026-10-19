package com.github.axet.bookreader.widgets

import android.view.View

/**
 * Adapter for touch coordinates when FBReaderView is zoomed.
 *
 * When FBReaderView has scale applied (zoom), touch coordinates need to be
 * adjusted to account for the scale transformation. This adapter converts
 * screen coordinates to content coordinates.
 *
 * Formula:
 *   contentX = (screenX - pivotX) / scale + pivotX
 *   contentY = (screenY - pivotY) / scale + pivotY
 *
 * This ensures that touches at the same visual position map to the same
 * content position regardless of zoom level.
 */
class ZoomTouchAdapter(private val fbReaderView: FBReaderView) {

    /**
     * Adapt X coordinate from screen to content space.
     * @param rawX Screen X coordinate
     * @return Content X coordinate (adjusted for zoom)
     */
    fun adaptX(rawX: Float): Float {
        val scale = fbReaderView.scaleX
        if (scale == 1.0f) return rawX  // No adaptation needed at normal zoom

        val pivotX = fbReaderView.pivotX
        return (rawX - pivotX) / scale + pivotX
    }

    /**
     * Adapt Y coordinate from screen to content space.
     * @param rawY Screen Y coordinate
     * @return Content Y coordinate (adjusted for zoom)
     */
    fun adaptY(rawY: Float): Float {
        val scale = fbReaderView.scaleY
        if (scale == 1.0f) return rawY  // No adaptation needed at normal zoom

        val pivotY = fbReaderView.pivotY
        return (rawY - pivotY) / scale + pivotY
    }

    /**
     * Adapt X coordinate relative to a specific view.
     * @param rawX Screen X coordinate
     * @param view The target view to get relative coordinates for
     * @return Content X coordinate relative to view
     */
    fun adaptX(rawX: Float, view: View): Int {
        val adaptedX = adaptX(rawX)
        return (adaptedX - view.left).toInt()
    }

    /**
     * Adapt Y coordinate relative to a specific view.
     * @param rawY Screen Y coordinate
     * @param view The target view to get relative coordinates for
     * @return Content Y coordinate relative to view
     */
    fun adaptY(rawY: Float, view: View): Int {
        val adaptedY = adaptY(rawY)
        return (adaptedY - view.top).toInt()
    }

    /**
     * Check if currently in zoom mode
     */
    fun isInZoom(): Boolean = fbReaderView.scaleX > 1.0f

    /**
     * Get current zoom scale
     */
    fun getScale(): Float = fbReaderView.scaleX

    /**
     * Adapt a point (x, y) from screen to content space.
     * @param x Screen X coordinate
     * @param y Screen Y coordinate
     * @return Adapted point as Pair(adaptedX, adaptedY)
     */
    fun adaptPoint(x: Float, y: Float): Pair<Float, Float> {
        return Pair(adaptX(x), adaptY(y))
    }

    /**
     * Reverse adaptation: convert content coordinates back to screen coordinates.
     * Used for positioning overlays (bookmarks, selection) on screen.
     * @param contentX Content X coordinate
     * @return Screen X coordinate
     */
    fun reverseAdaptX(contentX: Float): Float {
        val scale = fbReaderView.scaleX
        if (scale == 1.0f) return contentX

        val pivotX = fbReaderView.pivotX
        return (contentX - pivotX) * scale + pivotX
    }

    /**
     * Reverse adaptation: convert content coordinates back to screen coordinates.
     * @param contentY Content Y coordinate
     * @return Screen Y coordinate
     */
    fun reverseAdaptY(contentY: Float): Float {
        val scale = fbReaderView.scaleY
        if (scale == 1.0f) return contentY

        val pivotY = fbReaderView.pivotY
        return (contentY - pivotY) * scale + pivotY
    }
}