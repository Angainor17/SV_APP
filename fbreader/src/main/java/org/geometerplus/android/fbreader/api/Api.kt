/*
 * This code is in the public domain.
 */

package org.geometerplus.android.fbreader.api

import android.graphics.Bitmap
import java.util.Date

interface Api {
    // information about fbreader
    @Throws(ApiException::class)
    fun getFBReaderVersion(): String

    // preferences information
    @Throws(ApiException::class)
    fun getOptionGroups(): List<String>

    @Throws(ApiException::class)
    fun getOptionNames(group: String): List<String>

    @Throws(ApiException::class)
    fun getOptionValue(group: String, name: String): String

    @Throws(ApiException::class)
    fun setOptionValue(group: String, name: String, value: String)

    // book information for current book
    @Throws(ApiException::class)
    fun getBookLanguage(): String

    @Throws(ApiException::class)
    fun getBookTitle(): String

    @Throws(ApiException::class)
    fun getBookAuthors(): List<String>

    @Throws(ApiException::class)
    fun getBookTags(): List<String>

    @Throws(ApiException::class)
    fun getBookFilePath(): String

    @Throws(ApiException::class)
    fun getBookHash(): String

    @Throws(ApiException::class)
    fun getBookUniqueId(): String

    @Throws(ApiException::class)
    fun getBookLastTurningTime(): Date

    // book information for book defined by id
    @Throws(ApiException::class)
    fun getBookLanguage(id: Long): String

    @Throws(ApiException::class)
    fun getBookTitle(id: Long): String

    @Throws(ApiException::class)
    fun getBookTags(id: Long): List<String>

    @Throws(ApiException::class)
    fun getBookFilePath(id: Long): String

    @Throws(ApiException::class)
    fun getBookHash(id: Long): String

    @Throws(ApiException::class)
    fun getBookUniqueId(id: Long): String

    @Throws(ApiException::class)
    fun getBookLastTurningTime(id: Long): Date

    @Throws(ApiException::class)
    fun getBookProgress(): Float

    // text information
    @Throws(ApiException::class)
    fun getParagraphsNumber(): Int

    @Throws(ApiException::class)
    fun getParagraphElementsCount(paragraphIndex: Int): Int

    @Throws(ApiException::class)
    fun getParagraphText(paragraphIndex: Int): String

    @Throws(ApiException::class)
    fun getParagraphWords(paragraphIndex: Int): List<String>

    @Throws(ApiException::class)
    fun getParagraphWordIndices(paragraphIndex: Int): List<Int>

    // page information
    @Throws(ApiException::class)
    fun getPageStart(): TextPosition

    // manage view
    @Throws(ApiException::class)
    fun setPageStart(position: TextPosition)

    @Throws(ApiException::class)
    fun getPageEnd(): TextPosition

    @Throws(ApiException::class)
    fun isPageEndOfSection(): Boolean

    @Throws(ApiException::class)
    fun isPageEndOfText(): Boolean

    @Throws(ApiException::class)
    fun highlightArea(start: TextPosition, end: TextPosition)

    @Throws(ApiException::class)
    fun clearHighlighting()

    @Throws(ApiException::class)
    fun getBottomMargin(): Int

    @Throws(ApiException::class)
    fun setBottomMargin(value: Int)

    @Throws(ApiException::class)
    fun getTopMargin(): Int

    @Throws(ApiException::class)
    fun setTopMargin(value: Int)

    @Throws(ApiException::class)
    fun getLeftMargin(): Int

    @Throws(ApiException::class)
    fun setLeftMargin(value: Int)

    @Throws(ApiException::class)
    fun getRightMargin(): Int

    @Throws(ApiException::class)
    fun setRightMargin(value: Int)

    // action control
    @Throws(ApiException::class)
    fun listActions(): List<String>

    @Throws(ApiException::class)
    fun listActionNames(actions: List<String>): List<String>

    @Throws(ApiException::class)
    fun getKeyAction(key: Int, longPress: Boolean): String

    @Throws(ApiException::class)
    fun setKeyAction(key: Int, longPress: Boolean, action: String)

    @Throws(ApiException::class)
    fun listZoneMaps(): List<String>

    @Throws(ApiException::class)
    fun getZoneMap(): String

    @Throws(ApiException::class)
    fun setZoneMap(name: String)

    @Throws(ApiException::class)
    fun getZoneMapHeight(name: String): Int

    @Throws(ApiException::class)
    fun getZoneMapWidth(name: String): Int

    @Throws(ApiException::class)
    fun createZoneMap(name: String, width: Int, height: Int)

    @Throws(ApiException::class)
    fun isZoneMapCustom(name: String): Boolean

    @Throws(ApiException::class)
    fun deleteZoneMap(name: String)

    @Throws(ApiException::class)
    fun getTapZoneAction(name: String, h: Int, v: Int, singleTap: Boolean): String

    @Throws(ApiException::class)
    fun setTapZoneAction(name: String, h: Int, v: Int, singleTap: Boolean, action: String)

    @Throws(ApiException::class)
    fun getTapActionByCoordinates(name: String, x: Int, y: Int, width: Int, height: Int, tap: String): String

    @Throws(ApiException::class)
    fun getMainMenuContent(): List<MenuNode>

    @Throws(ApiException::class)
    fun getResourceString(vararg keys: String): String

    @Throws(ApiException::class)
    fun getBitmap(resourceId: Int): Bitmap
}
