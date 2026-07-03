package com.github.axet.bookreader.widgets

import android.graphics.Rect
import timber.log.Timber

/**
 * Helper class для работы с coordinate systems в SelectionView.
 *
 * ## Coordinate Systems
 *
 * В SelectionView используются три системы координат:
 *
 * 1. **Device Coordinates** - абсолютные координаты на экране устройства
 *    - Origin: (0, 0) в top-left corner экрана
 *    - Используются для: margin, handle positions, touch events
 *
 * 2. **Page Coordinates** - координаты относительно страницы PDF
 *    - Origin: (0, 0) в top-left corner страницы
 *    - Используются для: text bounds, selection points
 *
 * 3. **View Coordinates** - координаты относительно SelectionView
 *    - Origin: (0, 0) в top-left corner SelectionView
 *    - Используются для: drawing, child PageView positions
 *
 * ## Conversions
 *
 * - Device → Page: `deviceToPage(rect, pageRect)`
 * - Page → Device: `pageToDevice(rect, pageRect)`
 * - Device → View: `deviceToView(rect, viewRect)`
 * - View → Device: `viewToDevice(rect, viewRect)`
 */
object SelectionCoordinates {

    /**
     * Преобразует Device coordinates в Page coordinates.
     * Вычитает offset страницы из rect.
     *
     * @param rect rect в device coordinates (будет modified)
     * @param pageRect bounds страницы в device coordinates
     */
    fun deviceToPage(rect: Rect, pageRect: Rect) {
        rect.offset(-pageRect.left, -pageRect.top)
    }

    /**
     * Преобразует Page coordinates в Device coordinates.
     * Добавляет offset страницы к rect.
     *
     * @param rect rect в page coordinates (будет modified)
     * @param pageRect bounds страницы в device coordinates
     */
    fun pageToDevice(rect: Rect, pageRect: Rect) {
        rect.offset(pageRect.left, pageRect.top)
    }

    /**
     * Преобразует Device coordinates в View coordinates.
     * Вычитает offset SelectionView из rect.
     *
     * @param rect rect в device coordinates (будет modified)
     * @param viewRect bounds SelectionView в device coordinates (margin)
     */
    fun deviceToView(rect: Rect, viewRect: Rect) {
        rect.offset(-viewRect.left, -viewRect.top)
    }

    /**
     * Преобразует View coordinates в Device coordinates.
     * Добавляет offset SelectionView к rect.
     *
     * @param rect rect в view coordinates (будет modified)
     * @param viewRect bounds SelectionView в device coordinates (margin)
     */
    fun viewToDevice(rect: Rect, viewRect: Rect) {
        rect.offset(viewRect.left, viewRect.top)
    }

    /**
     * Преобразует touch coordinates в device coordinates.
     * Touch events приходят в view coordinates (относительно SelectionView).
     *
     * @param touchX x coordinate из MotionEvent (view relative)
     * @param touchY y coordinate из MotionEvent (view relative)
     * @param viewLeft left offset SelectionView в device coordinates
     * @param viewTop top offset SelectionView в device coordinates
     * @return Point в device coordinates
     */
    fun touchToDevice(
        touchX: Int,
        touchY: Int,
        viewLeft: Int,
        viewTop: Int
    ): Pair<Int, Int> {
        val deviceX = touchX + viewLeft
        val deviceY = touchY + viewTop
        Timber.d("Touch to Device: ($touchX, $touchY) → ($deviceX, $deviceY)")
        return Pair(deviceX, deviceY)
    }

    /**
     * Проверяет находится ли point внутри rect.
     *
     * @param x x coordinate в той же системе как rect
     * @param y y coordinate в той же системе как rect
     * @param rect bounds для проверки
     * @return true если point внутри rect
     */
    fun isPointInRect(x: Int, y: Int, rect: Rect): Boolean {
        return x >= rect.left && x < rect.right && y >= rect.top && y < rect.bottom
    }

    /**
     * Вычисляет center point rect.
     *
     * @param rect bounds
     * @return Pair(centerX, centerY)
     */
    fun getRectCenter(rect: Rect): Pair<Int, Int> {
        return Pair(rect.centerX(), rect.centerY())
    }

    /**
     * Создаёт Rect из point с padding.
     *
     * @param x center x
     * @param y center y
     * @param padding offset от center до edges
     * @return Rect с center в (x, y) и size (padding * 2)
     */
    fun createRectFromCenter(x: Int, y: Int, padding: Int): Rect {
        return Rect(
            x - padding,
            y - padding,
            x + padding,
            y + padding
        )
    }

    /**
     * Logs current coordinate system state для debugging.
     */
    fun logCoordinateState(
        tag: String = "SelectionCoordinates",
        margin: Rect?,
        startHandle: Rect?,
        endHandle: Rect?,
        touchX: Int? = null,
        touchY: Int? = null
    ) {
        Timber.tag(tag).d(
            "Coordinate state: margin=$margin, start=$startHandle, end=$endHandle, touch=($touchX, $touchY)"
        )
    }
}