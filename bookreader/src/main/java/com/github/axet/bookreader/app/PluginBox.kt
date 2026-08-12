package com.github.axet.bookreader.app

import android.graphics.Rect

/**
 * Прямоугольник с координатами.
 *
 * Вынесен из [Plugin.Box] для уменьшения размера Plugin.kt.
 * [Plugin.Box] оставлен как backward-compatible враппер.
 */
open class PluginBox {
    @JvmField
    var x: Int = 0 // нижний левый x
    @JvmField
    var y: Int = 0 // нижний левый y
    @JvmField
    var w: Int = 0 // x + w = верхний правый x
    @JvmField
    var h: Int = 0 // y + h = верхний правый y

    constructor()

    constructor(r: PluginBox) {
        this.x = r.x
        this.y = r.y
        this.w = r.w
        this.h = r.h
    }

    constructor(x: Int, y: Int, w: Int, h: Int) {
        this.x = x
        this.y = y
        this.w = w
        this.h = h
    }

    /**
     * Преобразует в Rect.
     */
    fun toRect(canvasWidth: Int, canvasHeight: Int): Rect {
        return Rect(x, canvasHeight - this.h - y, x + this.w, canvasHeight - y)
    }
}

/**
 * Прямоугольник для рендеринга.
 *
 * Вынесен из [Plugin.RenderRect] для уменьшения размера Plugin.kt.
 */
open class PluginRenderRect : PluginBox() {
    @JvmField
    var src: Rect? = null
    @JvmField
    var dst: Rect? = null
}
