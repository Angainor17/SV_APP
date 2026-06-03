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

    private var mDragInitMode = ON_DOWN
    private var mSortEnabled = true
    /**
     * The current remove mode.
     */
    private var mRemoveMode = 0
    private var mRemoveEnabled = false
    private var mIsRemoving = false
    private var mDetector: GestureDetector
    private var mFlingRemoveDetector: GestureDetector
    private var mTouchSlop: Int
    private var mHitPos = MISS
    private var mFlingHitPos = MISS

    private var mClickRemoveHitPos = MISS

    private var mTempLoc = IntArray(2)

    private var mItemX = 0
    private var mItemY = 0

    private var mCurrX = 0
    private var mCurrY = 0

    private var mDragging = false

    private var mFlingSpeed = 500f

    private var mDragHandleId: Int
    private var mClickRemoveId: Int
    private var mFlingHandleId: Int
    private var mCanDrag = false

    private var mDslv: DragSortListView
    private var mPositionX = 0

    private val mFlingRemoveListener: GestureDetector.OnGestureListener =
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                // Log.d("mobeta", "on fling remove called");
                if (mRemoveEnabled && mIsRemoving) {
                    val w = mDslv.width
                    val minPos = w / 5
                    if (velocityX > mFlingSpeed) {
                        if (mPositionX > -minPos) {
                            mDslv.stopDragWithVelocity(true, velocityX)
                        }
                    } else if (velocityX < -mFlingSpeed) {
                        if (mPositionX < minPos) {
                            mDslv.stopDragWithVelocity(true, velocityX)
                        }
                    }
                    mIsRemoving = false
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
        mDslv = dslv
        mDetector = GestureDetector(dslv.context, this)
        mFlingRemoveDetector = GestureDetector(dslv.context, mFlingRemoveListener)
        mFlingRemoveDetector.setIsLongpressEnabled(false)
        mTouchSlop = ViewConfiguration.get(dslv.context).scaledTouchSlop
        mDragHandleId = dragHandleId
        mClickRemoveId = clickRemoveId
        mFlingHandleId = flingHandleId
        setRemoveMode(removeMode)
        setDragInitMode(dragInitMode)
    }

    open fun getDragInitMode(): Int {
        return mDragInitMode
    }

    /**
     * Set how a drag is initiated. Needs to be one of
     * [ON_DOWN], [ON_DRAG], or [ON_LONG_PRESS].
     *
     * @param mode The drag init mode.
     */
    open fun setDragInitMode(mode: Int) {
        mDragInitMode = mode
    }

    open fun isSortEnabled(): Boolean {
        return mSortEnabled
    }

    /**
     * Enable/Disable list item sorting. Disabling is useful if only item
     * removal is desired. Prevents drags in the vertical direction.
     *
     * @param enabled Set `true` to enable list
     * item sorting.
     */
    open fun setSortEnabled(enabled: Boolean) {
        mSortEnabled = enabled
    }

    open fun getRemoveMode(): Int {
        return mRemoveMode
    }

    /**
     * One of [CLICK_REMOVE], [FLING_REMOVE].
     */
    open fun setRemoveMode(mode: Int) {
        mRemoveMode = mode
    }

    open fun isRemoveEnabled(): Boolean {
        return mRemoveEnabled
    }

    /**
     * Enable/Disable item removal without affecting remove mode.
     */
    open fun setRemoveEnabled(enabled: Boolean) {
        mRemoveEnabled = enabled
    }

    /**
     * Set the resource id for the View that represents the drag
     * handle in a list item.
     *
     * @param id An android resource id.
     */
    open fun setDragHandleId(id: Int) {
        mDragHandleId = id
    }

    /**
     * Set the resource id for the View that represents the fling
     * handle in a list item.
     *
     * @param id An android resource id.
     */
    open fun setFlingHandleId(id: Int) {
        mFlingHandleId = id
    }

    /**
     * Set the resource id for the View that represents click
     * removal button.
     *
     * @param id An android resource id.
     */
    open fun setClickRemoveId(id: Int) {
        mClickRemoveId = id
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
        if (mSortEnabled && !mIsRemoving) {
            dragFlags = dragFlags or DragSortListView.DRAG_POS_Y or DragSortListView.DRAG_NEG_Y
        }
        if (mRemoveEnabled && mIsRemoving) {
            dragFlags = dragFlags or DragSortListView.DRAG_POS_X
            dragFlags = dragFlags or DragSortListView.DRAG_NEG_X
        }

        mDragging = mDslv.startDrag(position - mDslv.headerViewsCount, dragFlags, deltaX, deltaY)
        return mDragging
    }

    override fun onTouch(v: View, ev: MotionEvent): Boolean {
        if (!mDslv.isDragEnabled || mDslv.listViewIntercepted()) {
            return false
        }

        mDetector.onTouchEvent(ev)
        if (mRemoveEnabled && mDragging && mRemoveMode == FLING_REMOVE) {
            mFlingRemoveDetector.onTouchEvent(ev)
        }

        val action = ev.action and MotionEvent.ACTION_MASK
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mCurrX = ev.x.toInt()
                mCurrY = ev.y.toInt()
            }
            MotionEvent.ACTION_UP -> {
                if (mRemoveEnabled && mIsRemoving) {
                    val x = if (mPositionX >= 0) mPositionX else -mPositionX
                    val removePoint = mDslv.width / 2
                    if (x > removePoint) {
                        mDslv.stopDragWithVelocity(true, 0f)
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                mIsRemoving = false
                mDragging = false
            }
        }

        return false
    }

    /**
     * Overrides to provide fading when slide removal is enabled.
     */
    override fun onDragFloatView(floatView: View, position: Point, touch: Point) {
        if (mRemoveEnabled && mIsRemoving) {
            mPositionX = position.x
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
        return if (mRemoveMode == FLING_REMOVE) flingHandleHitPosition(ev) else MISS
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
        return viewIdHitPosition(ev, mDragHandleId)
    }

    open fun flingHandleHitPosition(ev: MotionEvent): Int {
        return viewIdHitPosition(ev, mFlingHandleId)
    }

    open fun viewIdHitPosition(ev: MotionEvent, id: Int): Int {
        val x = ev.x.toInt()
        val y = ev.y.toInt()

        val touchPos = mDslv.pointToPosition(x, y) // includes headers/footers

        val numHeaders = mDslv.headerViewsCount
        val numFooters = mDslv.footerViewsCount
        val count = mDslv.count

        // Log.d("mobeta", "touch down on position " + itemnum);
        // We're only interested if the touch was on an
        // item that's not a header or footer.
        if (touchPos != AdapterView.INVALID_POSITION && touchPos >= numHeaders && touchPos < count - numFooters) {
            val item = mDslv.getChildAt(touchPos - mDslv.firstVisiblePosition)
            val rawX = ev.rawX.toInt()
            val rawY = ev.rawY.toInt()

            val dragBox: View? = if (id == 0) item else item.findViewById(id)
            if (dragBox != null) {
                dragBox.getLocationOnScreen(mTempLoc)

                if (rawX > mTempLoc[0] && rawY > mTempLoc[1] &&
                    rawX < mTempLoc[0] + dragBox.width &&
                    rawY < mTempLoc[1] + dragBox.height
                ) {
                    mItemX = item.left
                    mItemY = item.top

                    return touchPos
                }
            }
        }

        return MISS
    }

    override fun onDown(ev: MotionEvent): Boolean {
        if (mRemoveEnabled && mRemoveMode == CLICK_REMOVE) {
            mClickRemoveHitPos = viewIdHitPosition(ev, mClickRemoveId)
        }

        mHitPos = startDragPosition(ev)
        if (mHitPos != MISS && mDragInitMode == ON_DOWN) {
            startDrag(mHitPos, ev.x.toInt() - mItemX, ev.y.toInt() - mItemY)
        }

        mIsRemoving = false
        mCanDrag = true
        mPositionX = 0
        mFlingHitPos = startFlingPosition(ev)

        return true
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (e1 == null) return false
        val x1 = e1.x.toInt()
        val y1 = e1.y.toInt()
        val x2 = e2.x.toInt()
        val y2 = e2.y.toInt()
        val deltaX = x2 - mItemX
        val deltaY = y2 - mItemY

        if (mCanDrag && !mDragging && (mHitPos != MISS || mFlingHitPos != MISS)) {
            if (mHitPos != MISS) {
                if (mDragInitMode == ON_DRAG && Math.abs(y2 - y1) > mTouchSlop && mSortEnabled) {
                    startDrag(mHitPos, deltaX, deltaY)
                } else if (mDragInitMode != ON_DOWN && Math.abs(x2 - x1) > mTouchSlop && mRemoveEnabled) {
                    mIsRemoving = true
                    startDrag(mFlingHitPos, deltaX, deltaY)
                }
            } else if (mFlingHitPos != MISS) {
                if (Math.abs(x2 - x1) > mTouchSlop && mRemoveEnabled) {
                    mIsRemoving = true
                    startDrag(mFlingHitPos, deltaX, deltaY)
                } else if (Math.abs(y2 - y1) > mTouchSlop) {
                    mCanDrag = false // if started to scroll the list then
                    // don't allow sorting nor fling-removing
                }
            }
        }
        // return whatever
        return false
    }

    override fun onLongPress(e: MotionEvent) {
        // Log.d("mobeta", "lift listener long pressed");
        if (mHitPos != MISS && mDragInitMode == ON_LONG_PRESS) {
            mDslv.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            startDrag(mHitPos, mCurrX - mItemX, mCurrY - mItemY)
        }
    }

    // complete the OnGestureListener interface
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    // complete the OnGestureListener interface
    override fun onSingleTapUp(ev: MotionEvent): Boolean {
        if (mRemoveEnabled && mRemoveMode == CLICK_REMOVE) {
            if (mClickRemoveHitPos != MISS) {
                mDslv.removeItem(mClickRemoveHitPos - mDslv.headerViewsCount)
            }
        }
        return true
    }

    // complete the OnGestureListener interface
    override fun onShowPress(ev: MotionEvent) {
        // do nothing
    }
}
