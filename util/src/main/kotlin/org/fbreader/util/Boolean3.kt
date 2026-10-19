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
package org.fbreader.util

/**
 * Трёхзначное логическое значение.
 *
 * Представляет логическое значение с тремя возможными состояниями:
 * истинно, ложно или не определено. Используется в случаях, когда
 * необходимо различать "ложь" и "неизвестно".
 *
 * ## Использование
 * ```kotlin
 * // Объявление переменной
 * var state: Boolean3 = Boolean3.UNDEFINED
 *
 * // Проверка значения
 * when (state) {
 *     Boolean3.TRUE -> println("Истина")
 *     Boolean3.FALSE -> println("Ложь")
 *     Boolean3.UNDEFINED -> println("Не определено")
 * }
 * ```
 *
 * ## Примеры использования
 * - Настройки, которые могут быть включены, выключены или не заданы
 * - Состояния синхронизации (успех, неудача, в процессе)
 * - Опциональные булевы флаги в конфигурации
 */
enum class Boolean3 {
    /**
     * Ложь — значение определено как ложное.
     */
    FALSE,

    /**
     * Истина — значение определено как истинное.
     */
    TRUE,

    /**
     * Не определено — значение неизвестно или не задано.
     */
    UNDEFINED
}
