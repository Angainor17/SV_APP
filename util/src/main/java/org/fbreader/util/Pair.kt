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
 * Неизменяемая пара значений.
 *
 * Хранит два связанных значения разных типов. Пара является неизменяемой
 * после создания. Поддерживает обобщённые типы для типобезопасности.
 *
 * ## Использование из Kotlin
 * ```kotlin
 * val pair = Pair("ключ", 42)
 * println(pair.First)   // "ключ"
 * println(pair.Second)  // 42
 * ```
 *
 * ## Использование из Java
 * ```java
 * Pair<String, Integer> pair = new Pair<>("ключ", 42);
 * System.out.println(pair.First);   // "ключ"
 * System.out.println(pair.Second);  // 42
 * ```
 *
 * @param T1 тип первого значения
 * @param T2 тип второго значения
 * @property First первое значение пары
 * @property Second второе значение пары
 *
 * ## Примеры
 * ```kotlin
 * // Пара строк
 * val nameAndSurname = Pair("Иван", "Иванов")
 *
 * // Пара разных типов
 * val idAndName = Pair(123, "Продукт")
 *
 * // Пара с null-значением
 * val nullable = Pair<String, Int?>("значение", null)
 * ```
 */
class Pair<T1, T2>(
    /** Первое значение пары */
    @JvmField val First: T1,
    /** Второе значение пары */
    @JvmField val Second: T2
) {

    /**
     * Проверяет равенство этой пары с другим объектом.
     *
     * Две пары считаются равными, если равны их соответствующие элементы.
     * Сравнение учитывает null-значения через [ComparisonUtil.equal].
     *
     * @param other объект для сравнения
     * @return `true`, если объекты равны; `false` в противном случае
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Pair<*, *>) return false
        return ComparisonUtil.equal(First, other.First) &&
               ComparisonUtil.equal(Second, other.Second)
    }

    /**
     * Возвращает хеш-код пары.
     *
     * Хеш-код вычисляется на основе хеш-кодов обоих элементов.
     *
     * @return хеш-код пары
     */
    override fun hashCode(): Int =
        ComparisonUtil.hashCode(First) + 23 * ComparisonUtil.hashCode(Second)
}
