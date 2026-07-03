package com.github.axet.bookreader.widgets

import android.graphics.Rect
import org.geometerplus.zlibrary.core.view.SelectionCursor

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
    RIGHT;

    fun toSelectionCursorWhich(): SelectionCursor.Which {
        return when (this) {
            LEFT -> SelectionCursor.Which.Left
            RIGHT -> SelectionCursor.Which.Right
        }
    }

    companion object {
        fun fromSelectionCursorWhich(which: SelectionCursor.Which): HandleType {
            return when (which) {
                SelectionCursor.Which.Left -> LEFT
                SelectionCursor.Which.Right -> RIGHT
            }
        }
    }
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

    /**
     * Проверяет, есть ли активный drag.
     */
    fun isDragging(): Boolean = this is Dragging

    /**
     * Возвращает активный handle если есть drag.
     */
    fun activeHandle(): HandleType? = (this as? Dragging)?.handle
}

/**
 * Состояние выделения текста.
 *
 * @param dragState состояние drag операции
 * @param startHandleBounds bounds начального маркера
 * @param endHandleBounds bounds конечного маркера
 * @param isValid true если выделение корректно (margin != null)
 */
data class SelectionState(
    val dragState: DragState = DragState.Idle,
    val startHandleBounds: Rect? = null,
    val endHandleBounds: Rect? = null,
    val isValid: Boolean = false
) {
    /**
     * Проверяет, нужно ли скрыть панель действий.
     * Панель скрывается при начале drag.
     */
    fun shouldHidePanel(): Boolean = dragState.isDragging() && startHandleBounds != null && endHandleBounds != null

    /**
     * Проверяет, нужно ли показать панель действий.
     * Панель показывается при завершении drag если выделение корректно.
     */
    fun shouldShowPanel(): Boolean = dragState == DragState.Idle && isValid

    /**
     * Возвращает bounds для указанного handle.
     */
    fun getHandleBounds(handle: HandleType): Rect? {
        return when (handle) {
            HandleType.LEFT -> startHandleBounds
            HandleType.RIGHT -> endHandleBounds
        }
    }
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