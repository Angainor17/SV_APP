package com.github.axet.bookreader.widgets

import android.graphics.Rect

/**
 * Callback interface для событий SelectionView.
 * Позволяет отделить логику callbacks от View.
 */
interface SelectionCallbacks {
    /**
     * Вызывается при начале drag операции.
     * Используется для скрытия панели действий.
     *
     * @param handle тип маркера который двигается
     */
    fun onDragStart(handle: HandleType)

    /**
     * Вызывается при завершении drag операции.
     * Используется для показа панели действий.
     *
     * @param handle тип маркера который двигался
     */
    fun onDragEnd(handle: HandleType)

    /**
     * Вызывается при изменении bounds выделения.
     *
     * @param startBounds новые bounds начала выделения
     * @param endBounds новые bounds конца выделения
     */
    fun onBoundsChanged(startBounds: Rect, endBounds: Rect)
}

/**
 * Пустая реализация callbacks для default behavior.
 */
class EmptySelectionCallbacks : SelectionCallbacks {
    override fun onDragStart(handle: HandleType) {}
    override fun onDragEnd(handle: HandleType) {}
    override fun onBoundsChanged(startBounds: Rect, endBounds: Rect) {}
}

/**
 * Тип маркера выделения.
 */
enum class HandleType {
    LEFT,
    RIGHT
}

/**
 * Состояние drag операции.
 */
sealed class DragState {
    /**
     * Нет активного drag.
     */
    data object Idle : DragState()

    /**
     * Активный drag маркера.
     *
     * @param handle какой маркер двигается
     * @param offsetX offset по X от точки касания до hot point
     * @param offsetY offset по Y от точки касания до hot point
     * @param startX начальная X координата при drag start
     * @param startY начальная Y координата при drag start
     */
    data class Dragging(
        val handle: HandleType,
        val offsetX: Int,
        val offsetY: Int,
        val startX: Int,
        val startY: Int
    ) : DragState()
}

/**
 * Результат проверки touch event на маркере.
 *
 * @param hit true если touch попал в маркер
 * @param handleType тип маркера если hit
 * @param offsetX offset X от touch point до hot point
 * @param offsetY offset Y от touch point до hot point
 */
data class HandleTouchResult(
    val hit: Boolean = false,
    val handleType: HandleType? = null,
    val offsetX: Int = 0,
    val offsetY: Int = 0
) {
    companion object {
        val NO_HIT = HandleTouchResult()

        fun hit(handle: HandleType, offsetX: Int, offsetY: Int): HandleTouchResult {
            return HandleTouchResult(
                hit = true,
                handleType = handle,
                offsetX = offsetX,
                offsetY = offsetY
            )
        }
    }
}