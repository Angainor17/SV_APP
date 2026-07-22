package com.github.axet.bookreader.widgets

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import timber.log.Timber
import kotlin.math.abs

/**
 * Handler for zoom/pinch gestures.
 * Detects pinch gestures using ScaleGestureDetector and notifies listener about zoom changes.
 *
 * Unlike the old PinchGesture, this handler does NOT intercept touch events.
 * It detects zoom gestures but lets other handlers (scroll, long press, selection) work too.
 *
 * Also handles:
 * - Double tap for fit-width zoom toggle
 * - Pan gesture for horizontal movement when zoomed
 */
class ZoomGestureHandler(
    context: Context,
    private val listener: ZoomListener
) {
    private val scaleDetector: ScaleGestureDetector
    private val tapDetector: GestureDetector
    private val panDetector: GestureDetector

    var currentZoom: Float = 1.0f
        private set

    var pivotX: Float = 0f
        private set

    var pivotY: Float = 0f
        private set

    // Translation offset for pan when zoomed
    var translationX: Float = 0f
        private set

    var translationY: Float = 0f
        private set

    var isInZoom: Boolean = false
        private set

    // Zoom limits
    private val minZoom = 1.0f
    private val maxZoom = 3.0f
    private val maxFitWidthZoom = 1.25f  // Max for double tap fit-width (reduced)

    // Fit-width zoom (calculated based on page width)
    private var fitWidthZoom: Float = 1.5f  // Default, will be calculated

    // Pan tracking
    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    private var isPanning: Boolean = false

    interface ZoomListener {
        /**
         * Called when zoom scale changes.
         * @param scale The new zoom scale (1.0 = normal, >1.0 = zoomed in)
         * @param pivotX The X pivot point for zoom (focus point of pinch)
         * @param pivotY The Y pivot point for zoom
         */
        fun onZoomChange(scale: Float, pivotX: Float, pivotY: Float)

        /**
         * Called when zoom mode ends (scale returns to 1.0)
         */
        fun onZoomEnd()

        /**
         * Called to get page content width for fit-width zoom calculation.
         * @return Width of page content area (pageBox.w), or null if not available
         */
        fun getPageContentWidth(): Int?

        /**
         * Called to get screen width for fit-width zoom calculation.
         * @return Width of screen/view
         */
        fun getScreenWidth(): Int

        /**
         * Called to get screen height for centering calculation.
         * @return Height of screen/view
         */
        fun getScreenHeight(): Int

        /**
         * Called when pan offset changes (for horizontal movement when zoomed).
         * @param offsetX X offset (translation)
         * @param offsetY Y offset (translation)
         */
        fun onPanChange(offsetX: Float, offsetY: Float)
    }

    init {
        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                Timber.tag("voronin").d("ZoomGestureHandler: onScaleBegin focus=${detector.focusX},${detector.focusY}")
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                val newZoom = (currentZoom * scaleFactor).coerceIn(minZoom, maxZoom)

                if (newZoom != currentZoom) {
                    currentZoom = newZoom
                    pivotX = detector.focusX
                    pivotY = detector.focusY
                    isInZoom = currentZoom > minZoom

                    Timber.tag("voronin").d("ZoomGestureHandler: onScale zoom=$currentZoom pivot=$pivotX,$pivotY")
                    listener.onZoomChange(currentZoom, pivotX, pivotY)

                    // If zoom returns to 1.0, notify end
                    if (currentZoom == minZoom && isInZoom) {
                        isInZoom = false
                        listener.onZoomEnd()
                    }
                }
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                Timber.tag("voronin").d("ZoomGestureHandler: onScaleEnd finalZoom=$currentZoom")
                // If user ended gesture at zoom > 1.0, keep zoom state
                // Zoom will be reset via back button or explicit reset
            }
        })

        // Quick scaling mode for smoother zoom
        scaleDetector.isQuickScaleEnabled = true

        // Double tap detector for fit-width zoom toggle
        tapDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                Timber.tag("voronin").d("ZoomGestureHandler: onDoubleTap at ${e.x},${e.y}")

                if (currentZoom > minZoom) {
                    // Already zoomed - reset to normal
                    resetZoom()
                } else {
                    // Calculate fit-width zoom
                    calculateAndApplyFitWidthZoom(e.x, e.y)
                }
                return true
            }
        })

        // Pan detector for horizontal movement when zoomed
        panDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                // Only pan when zoomed and primarily horizontal movement
                if (currentZoom > minZoom && abs(distanceX) > abs(distanceY) * 2) {
                    // Direct pan - no division by zoom (full sensitivity)
                    translationX -= distanceX
                    translationY -= distanceY

                    Timber.tag("voronin").d("ZoomGestureHandler: pan translation=$translationX,$translationY")
                    listener.onPanChange(translationX, translationY)
                    return true
                }
                return false  // Let vertical scroll work normally
            }
        })
    }

    /**
     * Calculate fit-width zoom factor to remove margins and fill screen with content.
     */
    private fun calculateAndApplyFitWidthZoom(tapX: Float, tapY: Float) {
        val screenWidth = listener.getScreenWidth()
        val pageContentWidth = listener.getPageContentWidth()

        // Reset translation when applying fit-width zoom (center the page)
        translationX = 0f
        translationY = 0f
        listener.onPanChange(0f, 0f)

        if (pageContentWidth != null && pageContentWidth > 0) {
            val actualPageWidth = pageContentWidth

            // If pageWidth < screenWidth, page is rendered smaller than screen
            // This means there are visible margins - zoom to fill screen
            if (actualPageWidth < screenWidth) {
                // Simple formula: zoom to make page fill screen
                fitWidthZoom = screenWidth.toFloat() / actualPageWidth.toFloat()
                Timber.tag("voronin").d("ZoomGestureHandler: page smaller than screen, zoom=$fitWidthZoom")
            } else {
                // Page fills or exceeds screen width
                // Use content ratio to estimate actual text area
                val contentRatio = 0.9f  // Assume 90% is content (small margins)
                val contentWidth = actualPageWidth * contentRatio
                fitWidthZoom = screenWidth.toFloat() / contentWidth.toFloat()
                Timber.tag("voronin").d("ZoomGestureHandler: page larger than screen, zoom=$fitWidthZoom contentRatio=$contentRatio")
            }

            // Clamp to reasonable range
            fitWidthZoom = fitWidthZoom.coerceIn(1.05f, maxFitWidthZoom)

            Timber.tag("voronin").d("ZoomGestureHandler: fitWidthZoom=$fitWidthZoom (screen=$screenWidth, pageWidth=$actualPageWidth)")
        } else {
            // Default zoom when page width not available
            fitWidthZoom = 1.15f
        }

        // Center pivot point (center of screen for centered zoom)
        val centerPivotX = screenWidth / 2f
        val screenHeight = listener.getScreenHeight()
        val centerPivotY = screenHeight / 2f

        Timber.tag("voronin").d("ZoomGestureHandler: centerPivot=$centerPivotX,$centerPivotY screen=$screenWidth,$screenHeight")
        setZoom(fitWidthZoom, centerPivotX, centerPivotY)
    }

    /**
     * Process touch event to detect zoom gestures.
     * Returns false always - does NOT intercept touch events.
     * Other handlers (scroll, long press) will still receive events.
     */
    fun onTouchEvent(e: MotionEvent): Boolean {
        // Process scale gesture (pinch)
        scaleDetector.onTouchEvent(e)
        // Process tap gesture (double tap)
        tapDetector.onTouchEvent(e)
        // Process pan gesture (horizontal scroll when zoomed)
        if (currentZoom > minZoom) {
            panDetector.onTouchEvent(e)
        }
        // Always return false - let other handlers work too!
        return false
    }

    /**
     * Reset zoom to normal (1.0)
     */
    fun resetZoom() {
        if (currentZoom > minZoom) {
            currentZoom = minZoom
            pivotX = 0f
            pivotY = 0f
            translationX = 0f
            translationY = 0f
            isInZoom = false
            listener.onZoomEnd()
            Timber.tag("voronin").d("ZoomGestureHandler: resetZoom")
        }
    }

    /**
     * Set zoom to a specific value (for double tap zoom)
     */
    fun setZoom(scale: Float, pivotX: Float, pivotY: Float) {
        val newZoom = scale.coerceIn(minZoom, maxZoom)
        currentZoom = newZoom
        this.pivotX = pivotX
        this.pivotY = pivotY
        isInZoom = currentZoom > minZoom
        listener.onZoomChange(currentZoom, pivotX, pivotY)
        Timber.tag("voronin").d("ZoomGestureHandler: setZoom zoom=$currentZoom pivot=$pivotX,$pivotY")
    }
}