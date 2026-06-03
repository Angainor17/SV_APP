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
 * Утилитарный класс для сравнения объектов.
 *
 * Предоставляет методы для безопасного сравнения объектов,
 * включая обработку null-значений.
 *
 * ## Использование
 * ```kotlin
 * // Проверка равенства двух объектов (с учётом null)
 * val areEqual = ComparisonUtil.equal(obj1, obj2)
 *
 * // Получение хеш-кода объекта (безопасно для null)
 * val hash = ComparisonUtil.hashCode(obj)
 * ```
 */
object ComparisonUtil {

    /**
     * Проверяет равенство двух объектов с учётом null-значений.
     *
     * Два null-значения считаются равными. Если первый объект не null,
     * используется его метод [equals].
     *
     * @param o1 первый объект для сравнения (может быть null)
     * @param o2 второй объект для сравнения (может быть null)
     * @return `true`, если объекты равны или оба null; `false` в противном случае
     *
     * ## Примеры
     * ```kotlin
     * equal(null, null)      // true
     * equal("a", "a")        // true
     * equal("a", null)       // false
     * equal(null, "a")       // false
     * equal("a", "b")        // false
     * ```
     */
    @JvmStatic
    fun equal(o1: Any?, o2: Any?): Boolean =
        o1?.equals(o2) ?: (o2 == null)

    /**
     * Возвращает хеш-код объекта, безопасно обрабатывая null.
     *
     * @param o объект для получения хеш-кода (может быть null)
     * @return хеш-код объекта или 0, если объект null
     *
     * ## Примеры
     * ```kotlin
     * hashCode(null)         // 0
     * hashCode("string")     // хеш-код строки
     * hashCode(42)           // хеш-код числа
     * ```
     */
    @JvmStatic
    fun hashCode(o: Any?): Int = o?.hashCode() ?: 0
}
