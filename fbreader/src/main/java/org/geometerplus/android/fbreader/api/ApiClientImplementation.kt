/*
 * This code is in the public domain.
 */

package org.geometerplus.android.fbreader.api

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.IBinder
import android.os.Parcelable
import java.io.Serializable

class ApiClientImplementation(
    private val myContext: Context,
    private val myListener: ConnectionListener?
) : ServiceConnection, Api {

    private val myApiListeners = mutableListOf<ApiListener>()
    @Volatile
    private var myInterface: ApiInterface? = null

    private val myEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (myInterface == null || myApiListeners.isEmpty()) {
                return
            }
            val code = intent.getIntExtra(EVENT_TYPE, -1)
            if (code != -1) {
                synchronized(myApiListeners) {
                    for (l in myApiListeners) {
                        l.onEvent(code)
                    }
                }
            }
        }
    }

    init {
        connect()
    }

    fun addListener(listener: ApiListener) {
        myApiListeners.add(listener)
    }

    fun removeListener(listener: ApiListener) {
        myApiListeners.remove(listener)
    }

    @Synchronized
    fun connect() {
        if (myInterface == null) {
            myContext.bindService(
                FBReaderIntents.defaultInternalIntent(FBReaderIntents.Action.API),
                this,
                Context.BIND_AUTO_CREATE
            )
            myContext.registerReceiver(myEventReceiver, IntentFilter(FBReaderIntents.Action.API_CALLBACK))
        }
    }

    @Synchronized
    fun disconnect() {
        if (myInterface != null) {
            myContext.unregisterReceiver(myEventReceiver)
            try {
                myContext.unbindService(this)
            } catch (e: IllegalArgumentException) {
            }
            myInterface = null
        }
    }

    @Synchronized
    override fun onServiceConnected(className: ComponentName, service: IBinder) {
        myInterface = ApiInterface.Stub.asInterface(service)
        myListener?.onConnected()
    }

    @Synchronized
    override fun onServiceDisconnected(name: ComponentName) {
        myInterface = null
    }

    @Synchronized
    fun isConnected(): Boolean = myInterface != null

    @Synchronized
    @Throws(ApiException::class)
    private fun checkConnection() {
        if (myInterface == null) {
            throw ApiException("Not connected to FBReader")
        }
    }

    @Synchronized
    @Throws(ApiException::class)
    private fun request(method: Int, params: Array<ApiObject>): ApiObject {
        checkConnection()
        return try {
            val obj = myInterface!!.request(method, params)
            if (obj is ApiObject.Error) {
                throw ApiException(obj.message)
            }
            obj
        } catch (e: android.os.RemoteException) {
            throw ApiException(e)
        }
    }

    @Synchronized
    @Throws(ApiException::class)
    private fun requestList(method: Int, params: Array<ApiObject>): List<ApiObject> {
        checkConnection()
        return try {
            val list = myInterface!!.requestList(method, params)
            for (obj in list) {
                if (obj is ApiObject.Error) {
                    throw ApiException(obj.message)
                }
            }
            list
        } catch (e: android.os.RemoteException) {
            throw ApiException(e)
        }
    }

    @Throws(ApiException::class)
    private fun requestString(method: Int, params: Array<ApiObject>): String {
        val obj = request(method, params)
        if (obj !is ApiObject.String) {
            throw ApiException("Cannot cast return type of method $method to String")
        }
        return obj.value
    }

    @Throws(ApiException::class)
    private fun requestDate(method: Int, params: Array<ApiObject>): java.util.Date {
        val obj = request(method, params)
        if (obj !is ApiObject.Date) {
            throw ApiException("Cannot cast return type of method $method to Date")
        }
        return obj.value
    }

    @Throws(ApiException::class)
    private fun requestInt(method: Int, params: Array<ApiObject>): Int {
        val obj = request(method, params)
        if (obj !is ApiObject.Integer) {
            throw ApiException("Cannot cast return type of method $method to int")
        }
        return obj.value
    }

    @Throws(ApiException::class)
    private fun requestFloat(method: Int, params: Array<ApiObject>): Float {
        val obj = request(method, params)
        if (obj !is ApiObject.Float) {
            throw ApiException("Cannot cast return type of method $method to float")
        }
        return obj.value
    }

    @Throws(ApiException::class)
    private fun requestBoolean(method: Int, params: Array<ApiObject>): Boolean {
        val obj = request(method, params)
        if (obj !is ApiObject.Boolean) {
            throw ApiException("Cannot cast return type of method $method to boolean")
        }
        return obj.value
    }

    @Throws(ApiException::class)
    private fun requestTextPosition(method: Int, params: Array<ApiObject>): TextPosition {
        val obj = request(method, params)
        if (obj !is TextPosition) {
            throw ApiException("Cannot cast return type of method $method to TextPosition")
        }
        return obj
    }

    @Throws(ApiException::class)
    private fun <T : Parcelable> requestParcelable(method: Int, params: Array<ApiObject>): T {
        val obj = request(method, params)
        if (obj !is ApiObject.Parcelable) {
            throw ApiException("Cannot cast return type of method $method to Parcelable")
        }
        @Suppress("UNCHECKED_CAST")
        return obj.value as T
    }

    @Throws(ApiException::class)
    private fun requestStringList(method: Int, params: Array<ApiObject>): List<String> {
        val list = requestList(method, params)
        val stringList = ArrayList<String>(list.size)
        for (obj in list) {
            if (obj !is ApiObject.String) {
                throw ApiException("Cannot cast an element returned from method $method to String")
            }
            stringList.add(obj.value)
        }
        return stringList
    }

    @Throws(ApiException::class)
    private fun <T : Serializable> requestSerializableList(method: Int, params: Array<ApiObject>): List<T> {
        val list = requestList(method, params)
        val serializableList = ArrayList<T>(list.size)
        for (obj in list) {
            if (obj !is ApiObject.Serializable) {
                throw ApiException("Cannot cast an element returned from method $method to Serializable")
            }
            @Suppress("UNCHECKED_CAST")
            serializableList.add(obj.value as T)
        }
        return serializableList
    }

    @Throws(ApiException::class)
    private fun requestIntegerList(method: Int, params: Array<ApiObject>): List<Int> {
        val list = requestList(method, params)
        val intList = ArrayList<Int>(list.size)
        for (obj in list) {
            if (obj !is ApiObject.Integer) {
                throw ApiException("Cannot cast an element returned from method $method to Integer")
            }
            intList.add(obj.value)
        }
        return intList
    }

    // Api interface implementation
    override fun getFBReaderVersion(): String = requestString(ApiMethods.GET_FBREADER_VERSION, EMPTY_PARAMETERS)

    override fun getOptionGroups(): List<String> = requestStringList(ApiMethods.LIST_OPTION_GROUPS, EMPTY_PARAMETERS)

    override fun getOptionNames(group: String): List<String> = requestStringList(ApiMethods.LIST_OPTION_NAMES, envelope(group))

    override fun getOptionValue(group: String, name: String): String = requestString(
        ApiMethods.GET_OPTION_VALUE,
        arrayOf(ApiObject.envelope(group), ApiObject.envelope(name))
    )

    override fun setOptionValue(group: String, name: String, value: String) {
        request(
            ApiMethods.SET_OPTION_VALUE,
            arrayOf(ApiObject.envelope(group), ApiObject.envelope(name), ApiObject.envelope(value))
        )
    }

    override fun getBookLanguage(): String = requestString(ApiMethods.GET_BOOK_LANGUAGE, EMPTY_PARAMETERS)
    override fun getBookTitle(): String = requestString(ApiMethods.GET_BOOK_TITLE, EMPTY_PARAMETERS)
    override fun getBookTags(): List<String> = requestStringList(ApiMethods.LIST_BOOK_TAGS, EMPTY_PARAMETERS)
    override fun getBookFilePath(): String = requestString(ApiMethods.GET_BOOK_FILE_PATH, EMPTY_PARAMETERS)
    override fun getBookHash(): String = requestString(ApiMethods.GET_BOOK_HASH, EMPTY_PARAMETERS)
    override fun getBookAuthors(): List<String> = requestStringList(ApiMethods.LIST_BOOK_AUTHORS, EMPTY_PARAMETERS)
    override fun getBookProgress(): Float = requestFloat(ApiMethods.GET_BOOK_PROGRESS, EMPTY_PARAMETERS)
    override fun getBookUniqueId(): String = requestString(ApiMethods.GET_BOOK_UNIQUE_ID, EMPTY_PARAMETERS)
    override fun getBookLastTurningTime(): java.util.Date = requestDate(ApiMethods.GET_BOOK_LAST_TURNING_TIME, EMPTY_PARAMETERS)

    override fun getBookLanguage(id: Long): String = requestString(ApiMethods.GET_BOOK_LANGUAGE, envelope(id))
    override fun getBookTitle(id: Long): String = requestString(ApiMethods.GET_BOOK_TITLE, envelope(id))
    override fun getBookTags(id: Long): List<String> = requestStringList(ApiMethods.LIST_BOOK_TAGS, envelope(id))
    override fun getBookFilePath(id: Long): String = requestString(ApiMethods.GET_BOOK_FILE_PATH, envelope(id))
    override fun getBookHash(id: Long): String = requestString(ApiMethods.GET_BOOK_HASH, envelope(id))
    override fun getBookUniqueId(id: Long): String = requestString(ApiMethods.GET_BOOK_UNIQUE_ID, envelope(id))
    override fun getBookLastTurningTime(id: Long): java.util.Date = requestDate(ApiMethods.GET_BOOK_LAST_TURNING_TIME, envelope(id))

    override fun getPageStart(): TextPosition = requestTextPosition(ApiMethods.GET_PAGE_START, EMPTY_PARAMETERS)
    override fun setPageStart(position: TextPosition) { request(ApiMethods.SET_PAGE_START, arrayOf(position)) }
    override fun getPageEnd(): TextPosition = requestTextPosition(ApiMethods.GET_PAGE_END, EMPTY_PARAMETERS)
    override fun isPageEndOfSection(): Boolean = requestBoolean(ApiMethods.IS_PAGE_END_OF_SECTION, EMPTY_PARAMETERS)
    override fun isPageEndOfText(): Boolean = requestBoolean(ApiMethods.IS_PAGE_END_OF_TEXT, EMPTY_PARAMETERS)

    override fun getParagraphsNumber(): Int = requestInt(ApiMethods.GET_PARAGRAPHS_NUMBER, EMPTY_PARAMETERS)
    override fun getParagraphText(paragraphIndex: Int): String = requestString(ApiMethods.GET_PARAGRAPH_TEXT, envelope(paragraphIndex))
    override fun getParagraphElementsCount(paragraphIndex: Int): Int = requestInt(ApiMethods.GET_PARAGRAPH_ELEMENTS_COUNT, envelope(paragraphIndex))
    override fun getParagraphWords(paragraphIndex: Int): List<String> = requestStringList(ApiMethods.GET_PARAGRAPH_WORDS, envelope(paragraphIndex))
    override fun getParagraphWordIndices(paragraphIndex: Int): List<Int> = requestIntegerList(ApiMethods.GET_PARAGRAPH_WORD_INDICES, envelope(paragraphIndex))

    override fun highlightArea(start: TextPosition, end: TextPosition) { request(ApiMethods.HIGHLIGHT_AREA, arrayOf(start, end)) }
    override fun clearHighlighting() { request(ApiMethods.CLEAR_HIGHLIGHTING, EMPTY_PARAMETERS) }

    override fun getBottomMargin(): Int = requestInt(ApiMethods.GET_BOTTOM_MARGIN, EMPTY_PARAMETERS)
    override fun setBottomMargin(value: Int) { request(ApiMethods.SET_BOTTOM_MARGIN, arrayOf(ApiObject.envelope(value))) }
    override fun getTopMargin(): Int = requestInt(ApiMethods.GET_TOP_MARGIN, EMPTY_PARAMETERS)
    override fun setTopMargin(value: Int) { request(ApiMethods.SET_TOP_MARGIN, arrayOf(ApiObject.envelope(value))) }
    override fun getLeftMargin(): Int = requestInt(ApiMethods.GET_LEFT_MARGIN, EMPTY_PARAMETERS)
    override fun setLeftMargin(value: Int) { request(ApiMethods.SET_LEFT_MARGIN, arrayOf(ApiObject.envelope(value))) }
    override fun getRightMargin(): Int = requestInt(ApiMethods.GET_RIGHT_MARGIN, EMPTY_PARAMETERS)
    override fun setRightMargin(value: Int) { request(ApiMethods.SET_RIGHT_MARGIN, arrayOf(ApiObject.envelope(value))) }

    override fun getKeyAction(key: Int, longPress: Boolean): String = requestString(
        ApiMethods.GET_KEY_ACTION,
        arrayOf(ApiObject.envelope(key), ApiObject.envelope(longPress))
    )

    override fun setKeyAction(key: Int, longPress: Boolean, action: String) {
        request(
            ApiMethods.SET_KEY_ACTION,
            arrayOf(ApiObject.envelope(key), ApiObject.envelope(longPress), ApiObject.envelope(action))
        )
    }

    override fun listActions(): List<String> = requestStringList(ApiMethods.LIST_ACTIONS, EMPTY_PARAMETERS)
    override fun listActionNames(actions: List<String>): List<String> = requestStringList(ApiMethods.LIST_ACTION_NAMES, envelope(actions))

    override fun listZoneMaps(): List<String> = requestStringList(ApiMethods.LIST_ZONEMAPS, EMPTY_PARAMETERS)
    override fun getZoneMap(): String = requestString(ApiMethods.GET_ZONEMAP, EMPTY_PARAMETERS)
    override fun setZoneMap(name: String) { request(ApiMethods.SET_ZONEMAP, envelope(name)) }
    override fun getZoneMapHeight(name: String): Int = requestInt(ApiMethods.GET_ZONEMAP_HEIGHT, envelope(name))
    override fun getZoneMapWidth(name: String): Int = requestInt(ApiMethods.GET_ZONEMAP_WIDTH, envelope(name))

    override fun createZoneMap(name: String, width: Int, height: Int) {
        request(
            ApiMethods.CREATE_ZONEMAP,
            arrayOf(ApiObject.envelope(name), ApiObject.envelope(width), ApiObject.envelope(height))
        )
    }

    override fun isZoneMapCustom(name: String): Boolean = requestBoolean(ApiMethods.IS_ZONEMAP_CUSTOM, envelope(name))
    override fun deleteZoneMap(name: String) { request(ApiMethods.DELETE_ZONEMAP, envelope(name)) }

    override fun getTapZoneAction(name: String, h: Int, v: Int, singleTap: Boolean): String = requestString(
        ApiMethods.GET_TAPZONE_ACTION,
        arrayOf(ApiObject.envelope(name), ApiObject.envelope(h), ApiObject.envelope(v), ApiObject.envelope(singleTap))
    )

    override fun getTapActionByCoordinates(name: String, x: Int, y: Int, width: Int, height: Int, tap: String): String = requestString(
        ApiMethods.GET_TAP_ACTION_BY_COORDINATES,
        arrayOf(ApiObject.envelope(name), ApiObject.envelope(x), ApiObject.envelope(y), ApiObject.envelope(width), ApiObject.envelope(height), ApiObject.envelope(tap))
    )

    override fun setTapZoneAction(name: String, h: Int, v: Int, singleTap: Boolean, action: String) {
        request(
            ApiMethods.SET_TAPZONE_ACTION,
            arrayOf(ApiObject.envelope(name), ApiObject.envelope(h), ApiObject.envelope(v), ApiObject.envelope(singleTap), ApiObject.envelope(action))
        )
    }

    override fun getMainMenuContent(): List<MenuNode> = requestSerializableList(ApiMethods.GET_MAIN_MENU_CONTENT, EMPTY_PARAMETERS)
    override fun getResourceString(vararg keys: String): String = requestString(ApiMethods.GET_RESOURCE_STRING, keys.map { ApiObject.envelope(it) }.toTypedArray())
    override fun getBitmap(resourceId: Int): Bitmap = requestParcelable(ApiMethods.GET_BITMAP, envelope(resourceId))

    interface ConnectionListener {
        fun onConnected()
    }

    companion object {
        const val EVENT_TYPE = "event.type"
        private val EMPTY_PARAMETERS = emptyArray<ApiObject>()

        private fun envelope(value: String): Array<ApiObject> = arrayOf(ApiObject.envelope(value))
        private fun envelope(value: Int): Array<ApiObject> = arrayOf(ApiObject.envelope(value))
        private fun envelope(value: Long): Array<ApiObject> = arrayOf(ApiObject.envelope(value))
        private fun envelope(value: Boolean): Array<ApiObject> = arrayOf(ApiObject.envelope(value))
        private fun envelope(value: List<String>): Array<ApiObject> = value.map { ApiObject.envelope(it) }.toTypedArray()
        private fun envelope(value: Array<String>): Array<ApiObject> = value.map { ApiObject.envelope(it) }.toTypedArray()
    }
}
