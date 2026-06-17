/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.fbreader.network.atom

import org.geometerplus.zlibrary.core.xml.ZLStringMap

abstract class ATOMDateConstruct(attributes: ZLStringMap) : ATOMCommonAttributes(attributes), Comparable<ATOMDateConstruct> {

    @JvmField
    var year: Int = 0

    @JvmField
    var month: Int = 0

    @JvmField
    var day: Int = 0

    @JvmField
    var hour: Int = 0

    @JvmField
    var minutes: Int = 0

    @JvmField
    var seconds: Int = 0

    @JvmField
    var secondFraction: Float = 0.0f

    @JvmField
    var tzHour: Int = 0

    @JvmField
    var tzMinutes: Int = 0

    companion object {
        private val DAYS_IN_MONTHS = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

        @JvmStatic
        fun parse(str: String?, dateTime: ATOMDateConstruct?): Boolean {
            dateTime?.year = 0
            dateTime?.month = 0
            dateTime?.day = 0
            dateTime?.hour = 0
            dateTime?.minutes = 0
            dateTime?.seconds = 0
            dateTime?.secondFraction = 0.0f
            dateTime?.tzHour = 0
            dateTime?.tzMinutes = 0

            if (str == null || dateTime == null) {
                return false
            }

            val len = str.length
            if (len != 4 && len != 7 && len != 10 && len != 17 && len != 20 && len < 22) {
                return false
            }

            var num = 0
            var sign = 1
            var fnum = 0.0f
            var fmult = 0.1f
            var start: Int
            var end: Int
            var log: Int
            var ch: Char
            end = 4
            start = 0
            log = 0
            while (start < len) {
                ch = str[start++]
                if (!Character.isDigit(ch)) {
                    return false
                }
                num = 10 * num + (ch.code - '0'.code)
                fnum += fmult * (ch.code - '0'.code)
                fmult *= 0.1f
                if (start == end) {
                    when (log) {
                        0 -> dateTime.year = num
                        1 -> dateTime.month = num
                        2 -> dateTime.day = num
                        3 -> dateTime.hour = num
                        4 -> dateTime.minutes = num
                        5 -> dateTime.seconds = num
                        6 -> dateTime.secondFraction = fnum
                        7 -> dateTime.tzHour = sign * num
                        8 -> dateTime.tzMinutes = sign * num
                        else -> return false
                    }
                    num = 0
                    fnum = 0.0f
                    fmult = 0.1f
                    if (start == len) return true
                    when (log) {
                        0, 1 -> {
                            if (str[start++] != '-') return false
                            end = start + 2
                        }
                        2 -> {
                            if (str[start++] != 'T') return false
                            end = start + 2
                        }
                        3, 7 -> {
                            if (str[start++] != ':') return false
                            end = start + 2
                        }
                        4 -> {
                            ch = str[start++]
                            if (ch == ':') {
                                end = start + 2
                            } else if (ch == '+' || ch == '-') {
                                sign = if (ch == '-') -1 else 1
                                log += 2
                                end = start + 2
                            } else if (ch == 'Z') {
                                return true
                            } else return false
                        }
                        5 -> {
                            ch = str[start++]
                            if (ch == '.') {
                                end = start
                                while (Character.isDigit(str[++end])) { /* NOP */ }
                            } else if (ch == '+' || ch == '-') {
                                sign = if (ch == '-') -1 else 1
                                log += 1
                                end = start + 2
                            } else if (ch == 'Z') {
                                return true
                            } else return false
                        }
                        6 -> {
                            ch = str[start++]
                            if (ch == '+' || ch == '-') {
                                sign = if (ch == '-') -1 else 1
                                end = start + 2
                            } else if (ch == 'Z') {
                                return true
                            } else return false
                        }
                        else -> return false
                    }
                    ++log
                }
            }
            return false
        }

        private fun appendChars(buffer: StringBuilder, ch: Char, count: Int) {
            var c = count
            while (c-- > 0) {
                buffer.append(ch)
            }
        }
    }

    fun getDateTime(brief: Boolean): String {
        val timezone = StringBuilder("Z")
        if (tzMinutes != 0 || tzHour != 0) {
            var tzminnum = tzMinutes
            var tzhournum = tzHour
            val sign: Char
            if (tzhournum == 0) {
                sign = if (tzminnum >= 0) '+' else '-'
            } else {
                sign = if (tzhournum > 0) '+' else '-'
                if (tzhournum > 0 && tzminnum < 0) {
                    --tzhournum
                    tzminnum = 60 + tzminnum
                } else if (tzhournum < 0 && tzminnum > 0) {
                    ++tzhournum
                    tzminnum = 60 - tzminnum
                }
            }
            val tzmin = (if (tzminnum < 0) -tzminnum else tzminnum).toString()
            val tzhour = (if (tzhournum < 0) -tzhournum else tzhournum).toString()
            timezone.append(sign)
            appendChars(timezone, '0', 2 - tzhour.length)
            timezone.append(tzhour)
            timezone.append(':')
            appendChars(timezone, '0', 2 - tzmin.length)
            timezone.append(tzmin)
        }

        val time = StringBuilder()
        val temp = StringBuilder()
        if (secondFraction >= 0.01f) {
            val sfrnum = Math.round(100 * secondFraction)
            val sfr = sfrnum.toString()
            time.append('.')
            appendChars(time, '0', 2 - sfr.length)
            time.append(sfr)
        }
        if (!brief || time.isNotEmpty() || seconds != 0) {
            val sec = seconds.toString()
            temp.append(':')
            appendChars(temp, '0', 2 - sec.length)
            temp.append(sec)
            time.insert(0, temp.toString())
            temp.clear()
        }
        if (!brief || time.isNotEmpty() || hour != 0 || minutes != 0 || timezone.length > 1) {
            val hourStr = hour.toString()
            val min = minutes.toString()
            appendChars(temp, '0', 2 - hourStr.length)
            temp.append(hourStr)
            temp.append(':')
            appendChars(temp, '0', 2 - min.length)
            temp.append(min)
            time.insert(0, temp.toString())
            temp.clear()
        }

        val date = StringBuilder()
        if (!brief || time.isNotEmpty() || day != 0) {
            val dayStr = day.toString()
            date.append('-')
            appendChars(date, '0', 2 - dayStr.length)
            date.append(dayStr)
        }
        if (!brief || date.isNotEmpty() || month != 0) {
            val monthStr = month.toString()
            temp.append('-')
            appendChars(temp, '0', 2 - monthStr.length)
            temp.append(monthStr)
            date.insert(0, temp.toString())
            temp.clear()
        }

        val yearStr = year.toString()
        appendChars(temp, '0', 4 - yearStr.length)
        temp.append(yearStr)
        date.insert(0, temp.toString())
        temp.clear()

        if (!brief || time.isNotEmpty()) {
            date.append('T')
            date.append(time.toString())
            date.append(timezone.toString())
        }
        return date.toString()
    }

    override fun toString(): String = getDateTime(false)

    private fun daysInMonth(month: Int, year: Int): Int {
        var m = month - 1
        while (m > 11) m -= 12
        while (m < 0) m += 12
        if (m == 1 && (year % 4 == 0 && year % 100 != 0 || year % 400 == 0)) {
            return DAYS_IN_MONTHS[1] + 1
        }
        return DAYS_IN_MONTHS[m]
    }

    override fun compareTo(other: ATOMDateConstruct): Int {
        var dateYear = other.year
        var dateMonth = other.month
        var dateDay = other.day
        var dateHour = other.hour
        var dateMinutes = other.minutes
        if (tzHour != other.tzHour || tzMinutes != other.tzMinutes) {
            dateMinutes += tzMinutes - other.tzMinutes
            while (dateMinutes < 0) {
                dateMinutes += 60
                --dateHour
            }
            while (dateMinutes > 59) {
                dateMinutes -= 60
                ++dateHour
            }
            dateHour += tzHour - other.tzHour
            while (dateHour < 0) {
                dateHour += 24
                --dateDay
            }
            while (dateHour > 23) {
                dateHour -= 24
                ++dateDay
            }
            while (dateDay < 1) dateDay += daysInMonth(--dateMonth, dateYear)
            while (dateDay > daysInMonth(dateMonth, dateYear))
                dateDay -= daysInMonth(dateMonth++, dateYear)
            while (dateMonth < 1) {
                dateMonth += 12
                --dateYear
            }
            while (dateMonth > 12) {
                dateMonth -= 12
                ++dateYear
            }
        }
        if (year != dateYear) return year - dateYear
        if (month != dateMonth) return month - dateMonth
        if (day != dateDay) return day - dateDay
        if (hour != dateHour) return hour - dateHour
        if (minutes != dateMinutes) return minutes - dateMinutes
        if (seconds != other.seconds) return seconds - other.seconds
        return Math.round(100 * secondFraction) - Math.round(100 * other.secondFraction)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ATOMDateConstruct) {
            return false
        }
        return compareTo(other) == 0
    }
}
