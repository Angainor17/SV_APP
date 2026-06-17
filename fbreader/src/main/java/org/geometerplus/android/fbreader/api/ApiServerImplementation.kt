/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader.api

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.geometerplus.android.fbreader.MenuData
import org.geometerplus.fbreader.book.BookUtil
import org.geometerplus.fbreader.fbreader.FBReaderApp
import org.geometerplus.fbreader.fbreader.TapZoneMap
import org.geometerplus.zlibrary.core.application.ZLApplication
import org.geometerplus.zlibrary.core.application.ZLKeyBindings
import org.geometerplus.zlibrary.core.library.ZLibrary
import org.geometerplus.zlibrary.core.options.Config
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextWord
import org.geometerplus.zlibrary.text.view.ZLTextWordCursor
import java.util.Date

internal class ApiServerImplementation(private val myContext: Context) : ApiInterface.Stub(), Api {

    private val myBindings = ZLKeyBindings()
    @Volatile
    private var myReader: FBReaderApp? = null

    @Synchronized
    private fun getReader(): FBReaderApp {
        if (myReader == null) {
            myReader = ZLApplication.Instance() as FBReaderApp
        }
        return myReader!!
    }

    private fun unsupportedMethodError(method: Int): ApiObject.Error =
        ApiObject.Error("Unsupported method code: $method")

    private fun exceptionInMethodError(method: Int, e: Throwable): ApiObject.Error =
        ApiObject.Error("Exception in method $method: $e")

    override fun request(method: Int, parameters: Array<ApiObject>): ApiObject {
        return try {
            when (method) {
                ApiMethods.GET_FBREADER_VERSION -> ApiObject.envelope(getFBReaderVersion())
                ApiMethods.GET_OPTION_VALUE -> ApiObject.envelope(
                    getOptionValue(
                        (parameters[0] as ApiObject.String).value,
                        (parameters[1] as ApiObject.String).value
                    )
                )
                ApiMethods.SET_OPTION_VALUE -> {
                    setOptionValue(
                        (parameters[0] as ApiObject.String).value,
                        (parameters[1] as ApiObject.String).value,
                        (parameters[2] as ApiObject.String).value
                    )
                    ApiObject.Void.Instance
                }
                ApiMethods.GET_BOOK_LANGUAGE -> if (parameters.isEmpty()) {
                    ApiObject.envelope(getBookLanguage())
                } else {
                    ApiObject.envelope(getBookLanguage((parameters[0] as ApiObject.Long).value))
                }
                ApiMethods.GET_BOOK_TITLE -> if (parameters.isEmpty()) {
                    ApiObject.envelope(getBookTitle())
                } else {
                    ApiObject.envelope(getBookTitle((parameters[0] as ApiObject.Long).value))
                }
                ApiMethods.GET_BOOK_FILE_PATH -> if (parameters.isEmpty()) {
                    ApiObject.envelope(getBookFilePath())
                } else {
                    ApiObject.envelope(getBookFilePath((parameters[0] as ApiObject.Long).value))
                }
                ApiMethods.GET_BOOK_HASH -> if (parameters.isEmpty()) {
                    ApiObject.envelope(getBookHash())
                } else {
                    ApiObject.envelope(getBookHash((parameters[0] as ApiObject.Long).value))
                }
                ApiMethods.GET_BOOK_UNIQUE_ID -> if (parameters.isEmpty()) {
                    ApiObject.envelope(getBookUniqueId())
                } else {
                    ApiObject.envelope(getBookUniqueId((parameters[0] as ApiObject.Long).value))
                }
                ApiMethods.GET_BOOK_LAST_TURNING_TIME -> if (parameters.isEmpty()) {
                    ApiObject.envelope(getBookLastTurningTime())
                } else {
                    ApiObject.envelope(getBookLastTurningTime((parameters[0] as ApiObject.Long).value))
                }
                ApiMethods.GET_BOOK_PROGRESS -> ApiObject.envelope(getBookProgress())
                ApiMethods.GET_PARAGRAPHS_NUMBER -> ApiObject.envelope(getParagraphsNumber())
                ApiMethods.GET_PARAGRAPH_ELEMENTS_COUNT -> ApiObject.envelope(
                    getParagraphElementsCount((parameters[0] as ApiObject.Integer).value)
                )
                ApiMethods.GET_PARAGRAPH_TEXT -> ApiObject.envelope(
                    getParagraphText((parameters[0] as ApiObject.Integer).value)
                )
                ApiMethods.GET_PAGE_START -> getPageStart()
                ApiMethods.GET_PAGE_END -> getPageEnd()
                ApiMethods.IS_PAGE_END_OF_SECTION -> ApiObject.envelope(isPageEndOfSection())
                ApiMethods.IS_PAGE_END_OF_TEXT -> ApiObject.envelope(isPageEndOfText())
                ApiMethods.SET_PAGE_START -> {
                    setPageStart(parameters[0] as TextPosition)
                    ApiObject.Void.Instance
                }
                ApiMethods.HIGHLIGHT_AREA -> {
                    highlightArea(parameters[0] as TextPosition, parameters[1] as TextPosition)
                    ApiObject.Void.Instance
                }
                ApiMethods.CLEAR_HIGHLIGHTING -> {
                    clearHighlighting()
                    ApiObject.Void.Instance
                }
                ApiMethods.GET_BOTTOM_MARGIN -> ApiObject.envelope(getBottomMargin())
                ApiMethods.SET_BOTTOM_MARGIN -> {
                    setBottomMargin((parameters[0] as ApiObject.Integer).value)
                    ApiObject.Void.Instance
                }
                ApiMethods.GET_TOP_MARGIN -> ApiObject.envelope(getTopMargin())
                ApiMethods.SET_TOP_MARGIN -> {
                    setTopMargin((parameters[0] as ApiObject.Integer).value)
                    ApiObject.Void.Instance
                }
                ApiMethods.GET_LEFT_MARGIN -> ApiObject.envelope(getLeftMargin())
                ApiMethods.SET_LEFT_MARGIN -> {
                    setLeftMargin((parameters[0] as ApiObject.Integer).value)
                    ApiObject.Void.Instance
                }
                ApiMethods.GET_RIGHT_MARGIN -> ApiObject.envelope(getRightMargin())
                ApiMethods.SET_RIGHT_MARGIN -> {
                    setRightMargin((parameters[0] as ApiObject.Integer).value)
                    ApiObject.Void.Instance
                }
                ApiMethods.GET_KEY_ACTION -> ApiObject.envelope(
                    getKeyAction(
                        (parameters[0] as ApiObject.Integer).value,
                        (parameters[1] as ApiObject.Boolean).value
                    )
                )
                ApiMethods.SET_KEY_ACTION -> {
                    setKeyAction(
                        (parameters[0] as ApiObject.Integer).value,
                        (parameters[1] as ApiObject.Boolean).value,
                        (parameters[2] as ApiObject.String).value
                    )
                    ApiObject.Void.Instance
                }
                ApiMethods.GET_ZONEMAP -> ApiObject.envelope(getZoneMap())
                ApiMethods.SET_ZONEMAP -> {
                    setZoneMap((parameters[0] as ApiObject.String).value)
                    ApiObject.Void.Instance
                }
                ApiMethods.GET_ZONEMAP_HEIGHT -> ApiObject.envelope(getZoneMapHeight((parameters[0] as ApiObject.String).value))
                ApiMethods.GET_ZONEMAP_WIDTH -> ApiObject.envelope(getZoneMapWidth((parameters[0] as ApiObject.String).value))
                ApiMethods.GET_TAPZONE_ACTION -> ApiObject.envelope(
                    getTapZoneAction(
                        (parameters[0] as ApiObject.String).value,
                        (parameters[1] as ApiObject.Integer).value,
                        (parameters[2] as ApiObject.Integer).value,
                        (parameters[3] as ApiObject.Boolean).value
                    )
                )
                ApiMethods.GET_TAP_ACTION_BY_COORDINATES -> ApiObject.envelope(
                    getTapActionByCoordinates(
                        (parameters[0] as ApiObject.String).value,
                        (parameters[1] as ApiObject.Integer).value,
                        (parameters[2] as ApiObject.Integer).value,
                        (parameters[3] as ApiObject.Integer).value,
                        (parameters[4] as ApiObject.Integer).value,
                        (parameters[5] as ApiObject.String).value
                    )
                )
                ApiMethods.SET_TAPZONE_ACTION -> {
                    setTapZoneAction(
                        (parameters[0] as ApiObject.String).value,
                        (parameters[1] as ApiObject.Integer).value,
                        (parameters[2] as ApiObject.Integer).value,
                        (parameters[3] as ApiObject.Boolean).value,
                        (parameters[4] as ApiObject.String).value
                    )
                    ApiObject.Void.Instance
                }
                ApiMethods.CREATE_ZONEMAP -> {
                    createZoneMap(
                        (parameters[0] as ApiObject.String).value,
                        (parameters[1] as ApiObject.Integer).value,
                        (parameters[2] as ApiObject.Integer).value
                    )
                    ApiObject.Void.Instance
                }
                ApiMethods.IS_ZONEMAP_CUSTOM -> ApiObject.envelope(isZoneMapCustom((parameters[0] as ApiObject.String).value))
                ApiMethods.DELETE_ZONEMAP -> {
                    deleteZoneMap((parameters[0] as ApiObject.String).value)
                    ApiObject.Void.Instance
                }
                ApiMethods.GET_RESOURCE_STRING -> {
                    val stringParams = Array(parameters.size) { i ->
                        (parameters[i] as ApiObject.String).value
                    }
                    ApiObject.envelope(getResourceString(*stringParams))
                }
                ApiMethods.GET_BITMAP -> ApiObject.envelope(getBitmap((parameters[0] as ApiObject.Integer).value))
                else -> unsupportedMethodError(method)
            }
        } catch (e: Throwable) {
            ApiObject.Error("Exception in method $method: $e")
        }
    }

    override fun requestList(method: Int, parameters: Array<ApiObject>): List<ApiObject> {
        return try {
            when (method) {
                ApiMethods.LIST_OPTION_GROUPS -> ApiObject.envelopeStringList(getOptionGroups())
                ApiMethods.LIST_OPTION_NAMES -> ApiObject.envelopeStringList(
                    getOptionNames((parameters[0] as ApiObject.String).value)
                )
                ApiMethods.LIST_BOOK_TAGS -> ApiObject.envelopeStringList(getBookTags())
                ApiMethods.LIST_BOOK_AUTHORS -> ApiObject.envelopeStringList(getBookAuthors())
                ApiMethods.LIST_ACTIONS -> ApiObject.envelopeStringList(listActions())
                ApiMethods.LIST_ACTION_NAMES -> {
                    val actions = ArrayList<String>(parameters.size)
                    for (o in parameters) {
                        actions.add((o as ApiObject.String).value)
                    }
                    ApiObject.envelopeStringList(listActionNames(actions))
                }
                ApiMethods.LIST_ZONEMAPS -> ApiObject.envelopeStringList(listZoneMaps())
                ApiMethods.GET_PARAGRAPH_WORDS -> ApiObject.envelopeStringList(
                    getParagraphWords((parameters[0] as ApiObject.Integer).value)
                )
                ApiMethods.GET_PARAGRAPH_WORD_INDICES -> ApiObject.envelopeIntegerList(
                    getParagraphWordIndices((parameters[0] as ApiObject.Integer).value)
                )
                ApiMethods.GET_MAIN_MENU_CONTENT -> ApiObject.envelopeSerializableList(getMainMenuContent())
                else -> listOf(unsupportedMethodError(method))
            }
        } catch (e: Throwable) {
            listOf(exceptionInMethodError(method, e))
        }
    }

    override fun requestMap(method: Int, parameters: Array<ApiObject>): Map<ApiObject, ApiObject> {
        return try {
            when (method) {
                else -> mapOf(unsupportedMethodError(method) to unsupportedMethodError(method))
            }
        } catch (e: Throwable) {
            mapOf(exceptionInMethodError(method, e) to exceptionInMethodError(method, e))
        }
    }

    // Api interface implementation
    override fun getFBReaderVersion(): String = ZLibrary.Instance().versionName

    override fun getOptionGroups(): List<String> = Config.Instance()?.listGroups()?.toList() ?: emptyList()

    override fun getOptionNames(group: String): List<String> = Config.Instance()?.listNames(group)?.toList() ?: emptyList()

    override fun getOptionValue(group: String, name: String): String = ZLStringOption(group, name, null).value ?: ""

    override fun setOptionValue(group: String, name: String, value: String) {
        ZLStringOption(group, name, null).value = value
    }

    override fun getBookLanguage(): String = getReader().currentBook?.language ?: ""

    override fun getBookTitle(): String = getReader().currentBook?.title ?: ""

    override fun getBookTags(): List<String> = emptyList() // TODO: implement

    override fun getBookProgress(): Float {
        val book = getReader().currentBook ?: return -1.0f
        val progress = book.progress ?: return -1.0f
        return progress.toFloat()
    }

    override fun getBookAuthors(): List<String> {
        val book = getReader().currentBook ?: return emptyList()
        return book.authors().map { it.displayName }
    }

    override fun getBookFilePath(): String = getReader().currentBook?.getPath() ?: ""

    override fun getBookHash(): String {
        val book = getReader().currentBook ?: return ""
        val uid = BookUtil.createUid(book, "SHA-256") ?: return ""
        return uid.id
    }

    override fun getBookUniqueId(): String = "" // TODO: implement

    override fun getBookLastTurningTime(): Date = Date(0) // TODO: implement

    override fun getBookLanguage(id: Long): String = "" // TODO: implement

    override fun getBookTitle(id: Long): String = "" // TODO: implement

    override fun getBookTags(id: Long): List<String> = emptyList() // TODO: implement

    override fun getBookFilePath(id: Long): String = "" // TODO: implement

    override fun getBookHash(id: Long): String = "" // TODO: implement

    override fun getBookUniqueId(id: Long): String = "" // TODO: implement

    override fun getBookLastTurningTime(id: Long): Date = Date(0) // TODO: implement

    override fun getPageStart(): TextPosition = getTextPosition(getReader().getTextView().getStartCursor())

    override fun setPageStart(position: TextPosition) {
        getReader().getTextView().gotoPosition(position.paragraphIndex, position.elementIndex, position.charIndex)
        getReader().viewWidget.repaint()
        getReader().storePosition()
    }

    override fun getPageEnd(): TextPosition = getTextPosition(getReader().getTextView().getEndCursor())

    override fun isPageEndOfSection(): Boolean {
        val cursor = getReader().getTextView().getEndCursor()
        return cursor.isEndOfParagraph && cursor.paragraphCursor?.isEndOfSection == true
    }

    override fun isPageEndOfText(): Boolean {
        val cursor = getReader().getTextView().getEndCursor()
        return cursor.isEndOfParagraph && cursor.paragraphCursor?.isLast == true
    }

    private fun getTextPosition(cursor: ZLTextWordCursor): TextPosition =
        TextPosition(cursor.paragraphIndex, cursor.elementIndex, cursor.charIndex)

    private fun getZLTextPosition(position: TextPosition): ZLTextFixedPosition =
        ZLTextFixedPosition(position.paragraphIndex, position.elementIndex, position.charIndex)

    override fun highlightArea(start: TextPosition, end: TextPosition) {
        getReader().getTextView().highlight(getZLTextPosition(start), getZLTextPosition(end))
    }

    override fun clearHighlighting() {
        getReader().getTextView().clearHighlighting()
    }

    override fun getBottomMargin(): Int = getReader().viewOptions.bottomMargin.value

    override fun setBottomMargin(value: Int) {
        getReader().viewOptions.bottomMargin.value = value
    }

    override fun getTopMargin(): Int = getReader().viewOptions.topMargin.value

    override fun setTopMargin(value: Int) {
        getReader().viewOptions.topMargin.value = value
    }

    override fun getLeftMargin(): Int = getReader().viewOptions.leftMargin.value

    override fun setLeftMargin(value: Int) {
        getReader().viewOptions.leftMargin.value = value
    }

    override fun getRightMargin(): Int = getReader().viewOptions.rightMargin.value

    override fun setRightMargin(value: Int) {
        getReader().viewOptions.rightMargin.value = value
    }

    override fun getParagraphsNumber(): Int = getReader().model?.getTextModel()?.paragraphsNumber ?: 0

    override fun getParagraphElementsCount(paragraphIndex: Int): Int {
        val cursor = ZLTextWordCursor(getReader().getTextView().getStartCursor())
        cursor.moveToParagraph(paragraphIndex)
        cursor.moveToParagraphEnd()
        return cursor.elementIndex
    }

    override fun getParagraphText(paragraphIndex: Int): String {
        val sb = StringBuilder()
        val cursor = ZLTextWordCursor(getReader().getTextView().getStartCursor())
        cursor.moveToParagraph(paragraphIndex)
        cursor.moveToParagraphStart()
        while (!cursor.isEndOfParagraph) {
            val element = cursor.getElement()
            if (element is ZLTextWord) {
                sb.append(element.toString()).append(" ")
            }
            cursor.nextWord()
        }
        return sb.toString()
    }

    override fun getParagraphWords(paragraphIndex: Int): List<String> {
        val words = ArrayList<String>()
        val cursor = ZLTextWordCursor(getReader().getTextView().getStartCursor())
        cursor.moveToParagraph(paragraphIndex)
        cursor.moveToParagraphStart()
        while (!cursor.isEndOfParagraph) {
            val element = cursor.getElement()
            if (element is ZLTextWord) {
                words.add(element.toString())
            }
            cursor.nextWord()
        }
        return words
    }

    override fun getParagraphWordIndices(paragraphIndex: Int): List<Int> {
        val indices = ArrayList<Int>()
        val cursor = ZLTextWordCursor(getReader().getTextView().getStartCursor())
        cursor.moveToParagraph(paragraphIndex)
        cursor.moveToParagraphStart()
        while (!cursor.isEndOfParagraph) {
            val element = cursor.getElement()
            if (element is ZLTextWord) {
                indices.add(cursor.elementIndex)
            }
            cursor.nextWord()
        }
        return indices
    }

    override fun listActions(): List<String> = emptyList() // TODO: implement

    override fun listActionNames(actions: List<String>): List<String> = emptyList() // TODO: implement

    override fun getKeyAction(key: Int, longPress: Boolean): String = myBindings.getBinding(key, longPress) ?: ""

    override fun setKeyAction(key: Int, longPress: Boolean, action: String) {
        // TODO: implement
    }

    override fun listZoneMaps(): List<String> = TapZoneMap.zoneMapNames()

    override fun getZoneMap(): String = getReader().pageTurningOptions.tapZoneMap.value ?: ""

    override fun setZoneMap(name: String) {
        getReader().pageTurningOptions.tapZoneMap.value = name
    }

    override fun getZoneMapHeight(name: String): Int = TapZoneMap.zoneMap(name).getHeight()

    override fun getZoneMapWidth(name: String): Int = TapZoneMap.zoneMap(name).getWidth()

    override fun createZoneMap(name: String, width: Int, height: Int) {
        TapZoneMap.createZoneMap(name, width, height)
    }

    override fun isZoneMapCustom(name: String): Boolean = TapZoneMap.zoneMap(name).isCustom()

    override fun deleteZoneMap(name: String) {
        TapZoneMap.deleteZoneMap(name)
    }

    override fun getTapZoneAction(name: String, h: Int, v: Int, singleTap: Boolean): String =
        TapZoneMap.zoneMap(name).getActionByZone(
            h, v, if (singleTap) TapZoneMap.Tap.singleNotDoubleTap else TapZoneMap.Tap.doubleTap
        ) ?: ""

    override fun getTapActionByCoordinates(name: String, x: Int, y: Int, width: Int, height: Int, tap: String): String {
        val id = try {
            TapZoneMap.Tap.valueOf(tap)
        } catch (e: Exception) {
            TapZoneMap.Tap.singleTap
        }
        return TapZoneMap.zoneMap(name).getActionByCoordinates(x, y, width, height, id) ?: ""
    }

    override fun setTapZoneAction(name: String, h: Int, v: Int, singleTap: Boolean, action: String) {
        TapZoneMap.zoneMap(name).setActionForZone(h, v, singleTap, action)
    }

    private fun setMenuTitles(nodes: List<MenuNode>, menuResource: ZLResource) {
        for (n in nodes) {
            n.optionalTitle = menuResource.getResource(n.code).value
            if (n is MenuNode.Submenu) {
                setMenuTitles(n.children, menuResource)
            }
        }
    }

    override fun getMainMenuContent(): List<MenuNode> {
        val nodes = MenuData.topLevelNodes()
        val copies = nodes.map { it.clone() }
        setMenuTitles(copies, ZLResource.resource("menu"))
        return copies
    }

    override fun getResourceString(vararg keys: String): String {
        var resource = ZLResource.resource(keys[0])
        for (i in 1 until keys.size) {
            resource = resource.getResource(keys[i])
        }
        return resource.value ?: ""
    }

    override fun getBitmap(resourceId: Int): Bitmap =
        BitmapFactory.decodeResource(myContext.resources, resourceId) ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    companion object {
        fun sendEvent(context: ContextWrapper, eventType: String) {
            context.sendBroadcast(
                Intent(FBReaderIntents.Action.API_CALLBACK)
                    .putExtra(ApiClientImplementation.EVENT_TYPE, eventType)
            )
        }
    }
}
