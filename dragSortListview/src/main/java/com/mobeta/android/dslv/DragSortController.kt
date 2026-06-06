package com.mobeta.android.dslv

import android.graphics.Point
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.AdapterView
import com.mobeta.android.dslv.DragSortController.Companion.CLICK_REMOVE
import com.mobeta.android.dslv.DragSortController.Companion.FLING_REMOVE
import com.mobeta.android.dslv.DragSortController.Companion.ON_DOWN
import com.mobeta.android.dslv.DragSortController.Companion.ON_DRAG
import com.mobeta.android.dslv.DragSortController.Companion.ON_LONG_PRESS

/**
 * Class that starts and stops item drags on a [DragSortListView]
 * based on touch gestures. This class also inherits from
 * [SimpleFloatViewManager], which provides basic float View
 * creation.
 *
 *
 * An instance of this class is meant to be passed to the methods
 * [DragSortListView.setTouchListener] and
 * [DragSortListView.setFloatViewManager] of your
 * [DragSortListView] instance.
 */
open class DragSortController : SimpleFloatViewManager, View.OnTouchListener, GestureDetector.OnGestureListener {

    companion object {
        /**
         * Drag init mode enum.
         */
        const val ON_DOWN = 0
        const val ON_DRAG = 1
        const val ON_LONG_PRESS = 2
        /**
         * Remove mode enum.
         */
        const val CLICK_REMOVE = 0
        const val FLING_REMOVE = 1
        const val MISS = -1
    }

    private var dragInitMode = ON_DOWN
    private var sortEnabled = true
    /**
     * The current remove mode.
     */
    private var removeMode = 0
    private var removeEnabled = false
    private var isRemoving = false
    private var detector: GestureDetector
    private var flingRemoveDetector: GestureDetector
    private var touchSlop: Int
    private var hitPos = MISS
    private var flingHitPos = MISS

    private var clickRemoveHitPos = MISS

    private var tempLoc = IntArray(2)

    private var itemX = 0
    private var itemY = 0

    private var currX = 0
    private var currY = 0

    private var dragging = false

    private var flingSpeed = 500f

    private var dragHandleId: Int
    private var clickRemoveId: Int
    private var flingHandleId: Int
    private var canDrag = false

    private var dslv: DragSortListView
    private var positionX = 0

    private val flingRemoveListener: GestureDetector.OnGestureListener =
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                // Log.d("mobeta", "on fling remove called");
                if (removeEnabled && isRemoving) {
                    val w = dslv.width
                    val minPos = w / 5
                    if (velocityX > flingSpeed) {
                        if (positionX > -minPos) {
                            dslv.stopDragWithVelocity(true, velocityX)
                        }
                    } else if (velocityX < -flingSpeed) {
                        if (positionX < minPos) {
                            dslv.stopDragWithVelocity(true, velocityX)
                        }
                    }
                    isRemoving = false
                }
                return false
            }
        }

    /**
     * Calls [DragSortController.DragSortController] with a
     * 0 drag handle id, FLING_RIGHT_REMOVE remove mode,
     * and ON_DOWN drag init. By default, sorting is enabled, and
     * removal is disabled.
     *
     * @param dslv The DSLV instance
     */
    constructor(dslv: DragSortListView) : this(dslv, 0, ON_DOWN, FLING_REMOVE)

    constructor(dslv: DragSortListView, dragHandleId: Int, dragInitMode: Int, removeMode: Int) : this(dslv, dragHandleId, dragInitMode, removeMode, 0)

    constructor(dslv: DragSortListView, dragHandleId: Int, dragInitMode: Int, removeMode: Int, clickRemoveId: Int) : this(dslv, dragHandleId, dragInitMode, removeMode, clickRemoveId, 0)

    /**
     * By default, sorting is enabled, and removal is disabled.
     *
     * @param dslv         The DSLV instance
     * @param dragHandleId The resource id of the View that represents
     * the drag handle in a list item.
     */
    constructor(dslv: DragSortListView, dragHandleId: Int, dragInitMode: Int, removeMode: Int, clickRemoveId: Int, flingHandleId: Int) : super(dslv) {
        this.dslv = dslv
        detector = GestureDetector(dslv.context, this)
        flingRemoveDetector = GestureDetector(dslv.context, flingRemoveListener)
        flingRemoveDetector.setIsLongpressEnabled(false)
        touchSlop = ViewConfiguration.get(dslv.context).scaledTouchSlop
        this.dragHandleId = dragHandleId
        this.clickRemoveId = clickRemoveId
        this.flingHandleId = flingHandleId
        setRemoveMode(removeMode)
        setDragInitMode(dragInitMode)
    }

    open fun getDragInitMode(): Int {
        return dragInitMode
    }

    /**
     * Set how a drag is initiated. Needs to be one of
     * [ON_DOWN], [ON_DRAG], or [ON_LONG_PRESS].
     *
     * @param mode The drag init mode.
     */
    open fun setDragInitMode(mode: Int) {
        dragInitMode = mode
    }

    open fun isSortEnabled(): Boolean {
        return sortEnabled
    }

    /**
     * Enable/Disable list item sorting. Disabling is useful if only item
     * removal is desired. Prevents drags in the vertical direction.
     *
     * @param enabled Set `true` to enable list
     * item sorting.
     */
    open fun setSortEnabled(enabled: Boolean) {
        sortEnabled = enabled
    }

    open fun getRemoveMode(): Int {
        return removeMode
    }

    /**
     * One of [CLICK_REMOVE], [FLING_REMOVE].
     */
    open fun setRemoveMode(mode: Int) {
        removeMode = mode
    }

    open fun isRemoveEnabled(): Boolean {
        return removeEnabled
    }

    /**
     * Enable/Disable item removal without affecting remove mode.
     */
    open fun setRemoveEnabled(enabled: Boolean) {
        removeEnabled = enabled
    }

    /**
     * Set the resource id for the View that represents the drag
     * handle in a list item.
     *
     * @param id An android resource id.
     */
    open fun setDragHandleId(id: Int) {
        dragHandleId = id
    }

    /**
     * Set the resource id for the View that represents the fling
     * handle in a list item.
     *
     * @param id An android resource id.
     */
    open fun setFlingHandleId(id: Int) {
        flingHandleId = id
    }

    /**
     * Set the resource id for the View that represents click
     * removal button.
     *
     * @param id An android resource id.
     */
    open fun setClickRemoveId(id: Int) {
        clickRemoveId = id
    }

    /**
     * Sets flags to restrict certain motions of the floating View
     * based on DragSortController settings (such as remove mode).
     * Starts the drag on the DragSortListView.
     *
     * @param position The list item position (includes headers).
     * @param deltaX   Touch x-coord minus left edge of floating View.
     * @param deltaY   Touch y-coord minus top edge of floating View.
     * @return True if drag started, false otherwise.
     */
    open fun startDrag(position: Int, deltaX: Int, deltaY: Int): Boolean {
        var dragFlags = 0
        if (sortEnabled && !isRemoving) {
            dragFlags = dragFlags or DragSortListView.DRAG_POS_Y or DragSortListView.DRAG_NEG_Y
        }
        if (removeEnabled && isRemoving) {
            dragFlags = dragFlags or DragSortListView.DRAG_POS_X
            dragFlags = dragFlags or DragSortListView.DRAG_NEG_X
        }

        dragging = dslv.startDrag(position - dslv.headerViewsCount, dragFlags, deltaX, deltaY)
        return dragging
    }

    override fun onTouch(v: View, ev: MotionEvent): Boolean {
        if (!dslv.isDragEnabled || dslv.listViewIntercepted()) {
            return false
        }

        detector.onTouchEvent(ev)
        if (removeEnabled && dragging && removeMode == FLING_REMOVE) {
            flingRemoveDetector.onTouchEvent(ev)
        }

        val action = ev.action and MotionEvent.ACTION_MASK
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                currX = ev.x.toInt()
                currY = ev.y.toInt()
            }
            MotionEvent.ACTION_UP -> {
                if (removeEnabled && isRemoving) {
                    val x = if (positionX >= 0) positionX else -positionX
                    val removePoint = dslv.width / 2
                    if (x > removePoint) {
                        dslv.stopDragWithVelocity(true, 0f)
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                isRemoving = false
                dragging = false
            }
        }

        return false
    }

    /**
     * Overrides to provide fading when slide removal is enabled.
     */
    override fun onDragFloatView(floatView: View, position: Point, touch: Point) {
        if (removeEnabled && isRemoving) {
            positionX = position.x
        }
    }

    /**
     * Get the position to start dragging based on the ACTION_DOWN
     * MotionEvent. This function simply calls
     * [dragHandleHitPosition]. Override
     * to change drag handle behavior;
     * this function is called internally when an ACTION_DOWN
     * event is detected.
     *
     * @param ev The ACTION_DOWN MotionEvent.
     * @return The list position to drag if a drag-init gesture is
     * detected; MISS if unsuccessful.
     */
    open fun startDragPosition(ev: MotionEvent): Int {
        return dragHandleHitPosition(ev)
    }

    open fun startFlingPosition(ev: MotionEvent): Int {
        return if (removeMode == FLING_REMOVE) flingHandleHitPosition(ev) else MISS
    }

    /**
     * Checks for the touch of an item's drag handle (specified by
     * [setDragHandleId]), and returns that item's position
     * if a drag handle touch was detected.
     *
     * @param ev The ACTION_DOWN MotionEvent.
     * @return The list position of the item whose drag handle was
     * touched; MISS if unsuccessful.
     */
    open fun dragHandleHitPosition(ev: MotionEvent): Int {
        return viewIdHitPosition(ev, dragHandleId)
    }

    open fun flingHandleHitPosition(ev: MotionEvent): Int {
        return viewIdHitPosition(ev, flingHandleId)
    }

    open fun viewIdHitPosition(ev: MotionEvent, id: Int): Int {
        val x = ev.x.toInt()
        val y = ev.y.toInt()

        val touchPos = dslv.pointToPosition(x, y) // includes headers/footers

        val numHeaders = dslv.headerViewsCount
        val numFooters = dslv.footerViewsCount
        val count = dslv.count

        // Log.d("mobeta", "touch down on position " + itemnum);
        // We're only interested if the touch was on an
        // item that's not a header or footer.
        if (touchPos != AdapterView.INVALID_POSITION && touchPos >= numHeaders && touchPos < count - numFooters) {
            val item = dslv.getChildAt(touchPos - dslv.firstVisiblePosition)
            val rawX = ev.rawX.toInt()
            val rawY = ev.rawY.toInt()

            val dragBox: View? = if (id == 0) item else item.findViewById(id)
            if (dragBox != null) {
                dragBox.getLocationOnScreen(tempLoc)

                if (rawX > tempLoc[0] && rawY > tempLoc[1] &&
                    rawX < tempLoc[0] + dragBox.width &&
                    rawY < tempLoc[1] + dragBox.height
                ) {
                    itemX = item.left
                    itemY = item.top

                    return touchPos
                }
            }
        }

        return MISS
    }

    override fun onDown(ev: MotionEvent): Boolean {
        if (removeEnabled && removeMode == CLICK_REMOVE) {
            clickRemoveHitPos = viewIdHitPosition(ev, clickRemoveId)
        }

        hitPos = startDragPosition(ev)
        if (hitPos != MISS && dragInitMode == ON_DOWN) {
            startDrag(hitPos, ev.x.toInt() - itemX, ev.y.toInt() - itemY)
        }

        isRemoving = false
        canDrag = true
        positionX = 0
        flingHitPos = startFlingPosition(ev)

        return true
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (e1 == null) return false
        val x1 = e1.x.toInt()
        val y1 = e1.y.toInt()
        val x2 = e2.x.toInt()
        val y2 = e2.y.toInt()
        val deltaX = x2 - itemX
        val deltaY = y2 - itemY

        if (canDrag && !dragging && (hitPos != MISS || flingHitPos != MISS)) {
            if (hitPos != MISS) {
                if (dragInitMode == ON_DRAG && Math.abs(y2 - y1) > touchSlop && sortEnabled) {
                    startDrag(hitPos, deltaX, deltaY)
                } else if (dragInitMode != ON_DOWN && Math.abs(x2 - x1) > touchSlop && removeEnabled) {
                    isRemoving = true
                    startDrag(flingHitPos, deltaX, deltaY)
                }
            } else if (flingHitPos != MISS) {
                if (Math.abs(x2 - x1) > touchSlop && removeEnabled) {
                    isRemoving = true
                    startDrag(flingHitPos, deltaX, deltaY)
                } else if (Math.abs(y2 - y1) > touchSlop) {
                    canDrag = false // if started to scroll the list then
                    // don't allow sorting nor fling-removing
                }
            }
        }
        // return whatever
        return false
    }

    override fun onLongPress(e: MotionEvent) {
        // Log.d("mobeta", "lift listener long pressed");
        if (hitPos != MISS && dragInitMode == ON_LONG_PRESS) {
            dslv.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            startDrag(hitPos, currX - itemX, currY - itemY)
        }
    }

    // complete the OnGestureListener interface
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    // complete the OnGestureListener interface
    override fun onSingleTapUp(ev: MotionEvent): Boolean {
        if (removeEnabled && removeMode == CLICK_REMOVE) {
            if (clickRemoveHitPos != MISS) {
                dslv.removeItem(clickRemoveHitPos - dslv.headerViewsCount)
            }
        }
        return true
    }

    // complete the OnGestureListener interface
    override fun onShowPress(ev: MotionEvent) {
        // do nothing
    }
}
