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
package org.geometerplus.zlibrary.ui.android.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewConfiguration
import org.geometerplus.fbreader.Paths.systemInfo
import org.geometerplus.zlibrary.core.application.ZLApplication
import org.geometerplus.zlibrary.core.util.SystemInfo
import org.geometerplus.zlibrary.core.view.ZLView
import org.geometerplus.zlibrary.core.view.ZLViewEnums
import org.geometerplus.zlibrary.core.view.ZLViewEnums.PageIndex
import org.geometerplus.zlibrary.core.view.ZLViewWidget
import org.geometerplus.zlibrary.ui.android.view.ViewUtil.setColorLevel
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidPaintContext.Geometry
import org.geometerplus.zlibrary.ui.android.view.animation.AnimationProvider
import org.geometerplus.zlibrary.ui.android.view.animation.CurlAnimationProvider
import org.geometerplus.zlibrary.ui.android.view.animation.NoneAnimationProvider
import org.geometerplus.zlibrary.ui.android.view.animation.ShiftAnimationProvider
import org.geometerplus.zlibrary.ui.android.view.animation.SlideAnimationProvider
import org.geometerplus.zlibrary.ui.android.view.animation.SlideOldStyleAnimationProvider
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.Volatile
import kotlin.math.abs

open class ZLAndroidWidget : MainView, ZLViewWidget, OnLongClickListener {
    val PrepareService: ExecutorService = Executors.newSingleThreadExecutor()

    private val myPaint = Paint()

    private val myBitmapManager = BitmapManagerImpl(this)
    private val mySystemInfo: SystemInfo
    var ZLApplication: ZLApplicationInstance = ZLApplicationInstance()
    private var myFooterBitmap: Bitmap? = null
    private var myAnimationProvider: AnimationProvider? = null
    private var myAnimationType: ZLViewEnums.Animation? = null

    @Volatile
    private var myPendingLongClickRunnable: LongClickRunnable? = null

    @Volatile
    private var myLongClickPerformed = false

    @Volatile
    private var myPendingShortClickRunnable: ShortClickRunnable? = null

    @Volatile
    private var myPendingPress = false

    @Volatile
    private var myPendingDoubleTap = false
    private var myPressedX = 0
    private var myPressedY = 0
    private var myScreenIsTouched = false
    private var myKeyUnderTracking = -1
    private var myTrackingStartTime: Long = 0

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        mySystemInfo = systemInfo(context)
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        mySystemInfo = systemInfo(context)
        init()
    }

    constructor(context: Context) : super(context) {
        mySystemInfo = systemInfo(context)
        init()
    }

    private fun init() {
        // next line prevent ignoring first onKeyDown DPad event
        // after any dialog was closed
        setFocusableInTouchMode(true)
        setDrawingCacheEnabled(false)
        setOnLongClickListener(this)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        this.animationProvider!!.terminate()
        if (myScreenIsTouched) {
            val view = ZLApplication.Instance().getCurrentView()!!
            myScreenIsTouched = false
            view.onScrollingFinished(PageIndex.current)
        }
    }

    override fun onDraw(canvas: Canvas) {
        // WakeLock handling removed - FBReader activity deleted
        super.onDraw(canvas)

        //		final int w = getWidth();
//		final int h = getMainAreaHeight();
        myBitmapManager.setSize(getWidth(), this.mainAreaHeight)
        if (this.animationProvider!!.inProgress()) {
            onDrawInScrolling(canvas)
        } else {
            onDrawStatic(canvas)
            ZLApplication.Instance().onRepaintFinished()
        }
    }

    private val animationProvider: AnimationProvider?
        get() {
            val type =
                ZLApplication.Instance().getCurrentView()!!.getAnimationType()
            if (myAnimationProvider == null || myAnimationType != type) {
                myAnimationType = type
                when (type) {
                    ZLViewEnums.Animation.none -> myAnimationProvider =
                        NoneAnimationProvider(myBitmapManager)

                    ZLViewEnums.Animation.curl -> myAnimationProvider =
                        CurlAnimationProvider(myBitmapManager)

                    ZLViewEnums.Animation.slide -> myAnimationProvider =
                        SlideAnimationProvider(myBitmapManager)

                    ZLViewEnums.Animation.slideOldStyle -> myAnimationProvider =
                        SlideOldStyleAnimationProvider(myBitmapManager)

                    ZLViewEnums.Animation.shift -> myAnimationProvider =
                        ShiftAnimationProvider(myBitmapManager)
                }
            }
            return myAnimationProvider
        }

    private fun onDrawInScrolling(canvas: Canvas) {
        val view = ZLApplication.Instance().getCurrentView()!!

        val animator = this.animationProvider
        val oldMode = animator!!.getMode()
        animator.doStep()
        if (animator.inProgress()) {
            animator.draw(canvas)
            if (animator.getMode().Auto) {
                postInvalidate()
            }
            drawFooter(canvas, animator)
        } else {
            when (oldMode) {
                AnimationProvider.Mode.AnimatedScrollingForward -> {
                    val index = animator.getPageToScrollTo()
                    myBitmapManager.shift(index == PageIndex.next)
                    view.onScrollingFinished(index)
                    ZLApplication.Instance().onRepaintFinished()
                }

                AnimationProvider.Mode.AnimatedScrollingBackward -> view.onScrollingFinished(PageIndex.current)

                else -> {}
            }
            onDrawStatic(canvas)
        }
    }

    override fun reset() {
        myBitmapManager.reset()
    }

    override fun repaint() {
        postInvalidate()
    }

    override fun startManualScrolling(x: Int, y: Int, direction: ZLViewEnums.Direction) {
        val animator = this.animationProvider
        animator!!.setup(direction, getWidth(), this.mainAreaHeight, myColorLevel)
        animator.startManualScrolling(x, y)
    }

    override fun scrollManuallyTo(x: Int, y: Int) {
        val view = ZLApplication.Instance().getCurrentView()!!
        val animator = this.animationProvider
        if (view.canScroll(animator!!.getPageToScrollTo(x, y))) {
            animator.scrollTo(x, y)
            postInvalidate()
        }
    }

    override fun startAnimatedScrolling(
        pageIndex: PageIndex,
        x: Int,
        y: Int,
        direction: ZLViewEnums.Direction,
        speed: Int
    ) {
        val view = ZLApplication.Instance().getCurrentView()!!
        if (pageIndex == PageIndex.current || !view.canScroll(pageIndex)) {
            return
        }
        val animator = this.animationProvider
        animator!!.setup(direction, getWidth(), this.mainAreaHeight, myColorLevel)
        animator.startAnimatedScrolling(pageIndex, x, y, speed)
        if (animator.getMode().Auto) {
            postInvalidate()
        }
    }

    override fun startAnimatedScrolling(
        pageIndex: PageIndex,
        direction: ZLViewEnums.Direction,
        speed: Int
    ) {
        val view = ZLApplication.Instance().getCurrentView()!!
        if (pageIndex == PageIndex.current || !view.canScroll(pageIndex)) {
            return
        }
        val animator = this.animationProvider
        animator!!.setup(direction, getWidth(), this.mainAreaHeight, myColorLevel)
        animator.startAnimatedScrolling(pageIndex, null, null, speed)
        if (animator.getMode().Auto) {
            postInvalidate()
        }
    }

    override fun startAnimatedScrolling(x: Int, y: Int, speed: Int) {
        val view = ZLApplication.Instance().getCurrentView()!!
        val animator = this.animationProvider
        if (!view.canScroll(animator!!.getPageToScrollTo(x, y))) {
            animator.terminate()
            return
        }
        animator.startAnimatedScrolling(x, y, speed)
        postInvalidate()
    }

    open fun drawOnBitmap(bitmap: Bitmap, index: PageIndex) {
        val view = ZLApplication.Instance().getCurrentView()!! ?: return

        val context = ZLAndroidPaintContext(
            mySystemInfo,
            Canvas(bitmap),
            Geometry(
                getWidth(),
                getHeight(),
                getWidth(),
                this.mainAreaHeight,
                0,
                0
            ),
            if (view.isScrollbarShown()) getVerticalScrollbarWidth() else 0
        )
        view.paint(context, index)
    }

    private fun drawFooter(canvas: Canvas, animator: AnimationProvider?) {
        val view = ZLApplication.Instance().getCurrentView()!!
        val footer = view.footerArea

        if (footer == null) {
            myFooterBitmap = null
            return
        }

        if (myFooterBitmap != null &&
            (myFooterBitmap!!.getWidth() != getWidth() ||
                    myFooterBitmap!!.getHeight() != footer.height)
        ) {
            myFooterBitmap = null
        }
        if (myFooterBitmap == null) {
            myFooterBitmap = Bitmap.createBitmap(getWidth(), footer.height, Bitmap.Config.RGB_565)
        }
        val context = ZLAndroidPaintContext(
            mySystemInfo,
            Canvas(myFooterBitmap!!),
            Geometry(
                getWidth(),
                getHeight(),
                getWidth(),
                footer.height,
                0,
                this.mainAreaHeight
            ),
            if (view.isScrollbarShown()) getVerticalScrollbarWidth() else 0
        )
        footer.paint(context)
        val voffset = getHeight() - footer.height
        if (animator != null) {
            animator.drawFooterBitmap(canvas, myFooterBitmap!!, voffset)
        } else {
            canvas.drawBitmap(myFooterBitmap!!, 0f, voffset.toFloat(), myPaint)
        }
    }

    private fun onDrawStatic(canvas: Canvas) {
        canvas.drawBitmap(myBitmapManager.getBitmap(PageIndex.current), 0f, 0f, myPaint)
        drawFooter(canvas, null)
        post(object : Runnable {
            override fun run() {
                PrepareService.execute(object : Runnable {
                    override fun run() {
                        val view = ZLApplication.Instance().getCurrentView()!!
                        val context = ZLAndroidPaintContext(
                            mySystemInfo,
                            canvas,
                            Geometry(
                                getWidth(),
                                getHeight(),
                                getWidth(),
                                this@ZLAndroidWidget.mainAreaHeight,
                                0,
                                0
                            ),
                            if (view.isScrollbarShown()) getVerticalScrollbarWidth() else 0
                        )
                        view.preparePage(context, PageIndex.next)
                    }
                })
            }
        })
    }

    override fun onTrackballEvent(event: MotionEvent): Boolean {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            onKeyDown(KeyEvent.KEYCODE_DPAD_CENTER, null)
        } else {
            ZLApplication.Instance().getCurrentView()!!
                .onTrackballRotated((10 * event.getX()).toInt(), (10 * event.getY()).toInt())
        }
        return true
    }

    private fun postLongClickRunnable() {
        myLongClickPerformed = false
        myPendingPress = false
        if (myPendingLongClickRunnable == null) {
            myPendingLongClickRunnable = LongClickRunnable()
        }
        postDelayed(
            myPendingLongClickRunnable,
            (2 * ViewConfiguration.getLongPressTimeout()).toLong()
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.getX().toInt()
        val y = event.getY().toInt()

        val view = ZLApplication.Instance().getCurrentView()!!
        when (event.getAction()) {
            MotionEvent.ACTION_CANCEL -> {
                myPendingDoubleTap = false
                myPendingPress = false
                myScreenIsTouched = false
                myLongClickPerformed = false
                if (myPendingShortClickRunnable != null) {
                    removeCallbacks(myPendingShortClickRunnable)
                    myPendingShortClickRunnable = null
                }
                if (myPendingLongClickRunnable != null) {
                    removeCallbacks(myPendingLongClickRunnable)
                    myPendingLongClickRunnable = null
                }
                view.onFingerEventCancelled()
            }

            MotionEvent.ACTION_UP -> {
                if (myPendingDoubleTap) {
                    view.onFingerDoubleTap(x, y)
                } else if (myLongClickPerformed) {
                    view.onFingerReleaseAfterLongPress(x, y)
                } else {
                    if (myPendingLongClickRunnable != null) {
                        removeCallbacks(myPendingLongClickRunnable)
                        myPendingLongClickRunnable = null
                    }
                    if (myPendingPress) {
                        if (view.isDoubleTapSupported()) {
                            if (myPendingShortClickRunnable == null) {
                                myPendingShortClickRunnable = ShortClickRunnable()
                            }
                            postDelayed(
                                myPendingShortClickRunnable,
                                ViewConfiguration.getDoubleTapTimeout().toLong()
                            )
                        } else {
                            view.onFingerSingleTap(x, y)
                        }
                    } else {
                        view.onFingerRelease(x, y)
                    }
                }
                myPendingDoubleTap = false
                myPendingPress = false
                myScreenIsTouched = false
            }

            MotionEvent.ACTION_DOWN -> {
                if (myPendingShortClickRunnable != null) {
                    removeCallbacks(myPendingShortClickRunnable)
                    myPendingShortClickRunnable = null
                    myPendingDoubleTap = true
                } else {
                    postLongClickRunnable()
                    myPendingPress = true
                }
                myScreenIsTouched = true
                myPressedX = x
                myPressedY = y
            }

            MotionEvent.ACTION_MOVE -> {
                val slop = ViewConfiguration.get(getContext()).getScaledTouchSlop()
                val isAMove =
                    abs(myPressedX - x) > slop || abs(myPressedY - y) > slop
                if (isAMove) {
                    myPendingDoubleTap = false
                }
                if (myLongClickPerformed) {
                    view.onFingerMoveAfterLongPress(x, y)
                } else {
                    if (myPendingPress) {
                        if (isAMove) {
                            if (myPendingShortClickRunnable != null) {
                                removeCallbacks(myPendingShortClickRunnable)
                                myPendingShortClickRunnable = null
                            }
                            if (myPendingLongClickRunnable != null) {
                                removeCallbacks(myPendingLongClickRunnable)
                            }
                            view.onFingerPress(myPressedX, myPressedY)
                            myPendingPress = false
                        }
                    }
                    if (!myPendingPress) {
                        view.onFingerMove(x, y)
                    }
                }
            }
        }

        return true
    }

    override fun onLongClick(v: View?): Boolean {
        val view = ZLApplication.Instance().getCurrentView()!!
        return view.onFingerLongPress(myPressedX, myPressedY)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val application = ZLApplication.Instance()
        val bindings = application.keyBindings()

        if (bindings.hasBinding(keyCode, true) ||
            bindings.hasBinding(keyCode, false)
        ) {
            if (myKeyUnderTracking != -1) {
                if (myKeyUnderTracking == keyCode) {
                    return true
                } else {
                    myKeyUnderTracking = -1
                }
            }
            if (bindings.hasBinding(keyCode, true)) {
                myKeyUnderTracking = keyCode
                myTrackingStartTime = System.currentTimeMillis()
                return true
            } else {
                return application.runActionByKey(keyCode, false)
            }
        } else {
            return false
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (myKeyUnderTracking != -1) {
            if (myKeyUnderTracking == keyCode) {
                val longPress = System.currentTimeMillis() >
                        myTrackingStartTime + ViewConfiguration.getLongPressTimeout()
                ZLApplication.Instance().runActionByKey(keyCode, longPress)
            }
            myKeyUnderTracking = -1
            return true
        } else {
            val bindings = ZLApplication.Instance().keyBindings()
            return bindings.hasBinding(keyCode, false) ||
                    bindings.hasBinding(keyCode, true)
        }
    }

    override fun computeVerticalScrollExtent(): Int {
        val view = ZLApplication.Instance().getCurrentView()!!
        if (!view.isScrollbarShown()) {
            return 0
        }
        val animator = this.animationProvider
        if (animator!!.inProgress()) {
            val from = view.getScrollbarThumbLength(PageIndex.current)
            val to = view.getScrollbarThumbLength(animator.getPageToScrollTo())
            val percent = animator.getScrolledPercent()
            return (from * (100 - percent) + to * percent) / 100
        } else {
            return view.getScrollbarThumbLength(PageIndex.current)
        }
    }

    override fun computeVerticalScrollOffset(): Int {
        val view = ZLApplication.Instance().getCurrentView()!!
        if (!view.isScrollbarShown()) {
            return 0
        }
        val animator = this.animationProvider
        if (animator!!.inProgress()) {
            val from = view.getScrollbarThumbPosition(PageIndex.current)
            val to = view.getScrollbarThumbPosition(animator.getPageToScrollTo())
            val percent = animator.getScrolledPercent()
            return (from * (100 - percent) + to * percent) / 100
        } else {
            return view.getScrollbarThumbPosition(PageIndex.current)
        }
    }

    override fun computeVerticalScrollRange(): Int {
        val view = ZLApplication.Instance().getCurrentView()!!
        if (!view.isScrollbarShown()) {
            return 0
        }
        return view.getScrollbarFullSize()
    }

    val mainAreaHeight: Int
        get() {
            val footer = ZLApplication.Instance().getCurrentView()!!.footerArea
            return if (footer != null) getHeight() - footer.height else getHeight()
        }

    override fun updateColorLevel() {
        setColorLevel(myPaint, myColorLevel)
    }

    open class ZLApplicationInstance {
        open fun Instance(): ZLApplication {
            return ZLApplication.Instance()
        }
    }

    private inner class LongClickRunnable : Runnable {
        override fun run() {
            if (performLongClick()) {
                myLongClickPerformed = true
            }
        }
    }

    private inner class ShortClickRunnable : Runnable {
        override fun run() {
            val view = ZLApplication.Instance().getCurrentView()!!
            view.onFingerSingleTap(myPressedX, myPressedY)
            myPendingPress = false
            myPendingShortClickRunnable = null
        }
    }
}
