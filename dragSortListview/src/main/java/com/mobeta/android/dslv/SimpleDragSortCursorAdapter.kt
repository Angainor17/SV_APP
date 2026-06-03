/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mobeta.android.dslv

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView

// taken from sdk/sources/android-16/android/widget/SimpleCursorAdapter.java

/**
 * An easy adapter to map columns from a cursor to TextViews or ImageViews
 * defined in an XML file. You can specify which columns you want, which
 * views you want to display the columns, and the XML file that defines
 * the appearance of these views.
 *
 *
 * Binding occurs in two phases. First, if a
 * [android.widget.SimpleCursorAdapter.ViewBinder] is available,
 * [ViewBinder.setViewValue]
 * is invoked. If the returned value is true, binding has occured. If the
 * returned value is false and the view to bind is a TextView,
 * [setViewText] is invoked. If the returned value
 * is false and the view to bind is an ImageView,
 * [setViewImage] is invoked. If no appropriate
 * binding can be found, an [IllegalStateException] is thrown.
 *
 *
 * If this adapter is used with filtering, for instance in an
 * [android.widget.AutoCompleteTextView], you can use the
 * [android.widget.SimpleCursorAdapter.CursorToStringConverter] and the
 * [android.widget.FilterQueryProvider] interfaces
 * to get control over the filtering process. You can refer to
 * [convertToString] and
 * [runQueryOnBackgroundThread] for more information.
 */
open class SimpleDragSortCursorAdapter : ResourceDragSortCursorAdapter {

    /**
     * A list of columns containing the data to bind to the UI.
     * This field should be made private, so it is hidden from the SDK.
     * {@hide}
     */
    protected var mFrom: IntArray? = null

    /**
     * A list of View ids representing the views to which the data must be bound.
     * This field should be made private, so it is hidden from the SDK.
     * {@hide}
     */
    protected var mTo: IntArray = intArrayOf()

    private var mOriginalFrom: Array<String> = emptyArray()
    private var mStringConversionColumn = -1
    private var mCursorToStringConverter: CursorToStringConverter? = null
    private var mViewBinder: ViewBinder? = null

    /**
     * Constructor the enables auto-requery.
     *
     * @deprecated This option is discouraged, as it results in Cursor queries
     * being performed on the application's UI thread and thus can cause poor
     * responsiveness or even Application Not Responding errors.  As an alternative,
     * use [android.app.LoaderManager] with a [android.content.CursorLoader].
     */
    @Deprecated("")
    constructor(context: Context, layout: Int, c: Cursor?, from: Array<String>, to: IntArray) : super(context, layout, c) {
        mTo = to
        mOriginalFrom = from
        findColumns(c, from)
    }

    /**
     * Standard constructor.
     *
     * @param context The context where the ListView associated with this
     * SimpleListItemFactory is running
     * @param layout  resource identifier of a layout file that defines the views
     * for this list item. The layout file should include at least
     * those named views defined in "to"
     * @param c       The database cursor.  Can be null if the cursor is not available yet.
     * @param from    A list of column names representing the data to bind to the UI.  Can be null
     * if the cursor is not available yet.
     * @param to      The views that should display column in the "from" parameter.
     * These should all be TextViews. The first N views in this list
     * are given the values of the first N columns in the from
     * parameter.  Can be null if the cursor is not available yet.
     * @param flags   Flags used to determine the behavior of the adapter,
     * as per [CursorAdapter.CursorAdapter].
     */
    constructor(context: Context, layout: Int, c: Cursor?, from: Array<String>, to: IntArray, flags: Int) : super(context, layout, c, flags) {
        mTo = to
        mOriginalFrom = from
        findColumns(c, from)
    }

    /**
     * Binds all of the field names passed into the "to" parameter of the
     * constructor with their corresponding cursor columns as specified in the
     * "from" parameter.
     *
     *
     * Binding occurs in two phases. First, if a
     * [android.widget.SimpleCursorAdapter.ViewBinder] is available,
     * [ViewBinder.setViewValue]
     * is invoked. If the returned value is true, binding has occured. If the
     * returned value is false and the view to bind is a TextView,
     * [setViewText] is invoked. If the returned value is
     * false and the view to bind is an ImageView,
     * [setViewImage] is invoked. If no appropriate
     * binding can be found, an [IllegalStateException] is thrown.
     *
     * @throws IllegalStateException if binding cannot occur
     * @see android.widget.CursorAdapter.bindView
     * @see getViewBinder
     * @see setViewBinder
     * @see setViewImage
     * @see setViewText
     */
    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val binder = mViewBinder
        val count = mTo.size
        val from = mFrom ?: return
        val to = mTo

        for (i in 0 until count) {
            val v = view.findViewById<View>(to[i])
            if (v != null) {
                var bound = false
                if (binder != null) {
                    bound = binder.setViewValue(v, cursor, from[i])
                }

                if (!bound) {
                    var text = cursor.getString(from[i])
                    if (text == null) {
                        text = ""
                    }

                    when (v) {
                        is TextView -> setViewText(v, text)
                        is ImageView -> setViewImage(v, text)
                        else -> throw IllegalStateException("${v.javaClass.name} is not a " +
                                " view that can be bounds by this SimpleCursorAdapter")
                    }
                }
            }
        }
    }

    /**
     * Returns the [ViewBinder] used to bind data to views.
     *
     * @return a ViewBinder or null if the binder does not exist
     * @see bindView
     * @see setViewBinder
     */
    open fun getViewBinder(): ViewBinder? {
        return mViewBinder
    }

    /**
     * Sets the binder used to bind data to views.
     *
     * @param viewBinder the binder used to bind data to views, can be null to
     * remove the existing binder
     * @see bindView
     * @see getViewBinder
     */
    open fun setViewBinder(viewBinder: ViewBinder?) {
        mViewBinder = viewBinder
    }

    /**
     * Called by bindView() to set the image for an ImageView but only if
     * there is no existing ViewBinder or if the existing ViewBinder cannot
     * handle binding to an ImageView.
     *
     *
     * By default, the value will be treated as an image resource. If the
     * value cannot be used as an image resource, the value is used as an
     * image Uri.
     *
     *
     * Intended to be overridden by Adapters that need to filter strings
     * retrieved from the database.
     *
     * @param v     ImageView to receive an image
     * @param value the value retrieved from the cursor
     */
    open fun setViewImage(v: ImageView, value: String) {
        try {
            v.setImageResource(value.toInt())
        } catch (e: NumberFormatException) {
            v.setImageURI(Uri.parse(value))
        }
    }

    /**
     * Called by bindView() to set the text for a TextView but only if
     * there is no existing ViewBinder or if the existing ViewBinder cannot
     * handle binding to a TextView.
     *
     *
     * Intended to be overridden by Adapters that need to filter strings
     * retrieved from the database.
     *
     * @param v    TextView to receive text
     * @param text the text to be set for the TextView
     */
    open fun setViewText(v: TextView, text: String) {
        v.text = text
    }

    /**
     * Return the index of the column used to get a String representation
     * of the Cursor.
     *
     * @return a valid index in the current Cursor or -1
     * @see android.widget.CursorAdapter.convertToString
     * @see setStringConversionColumn
     * @see setCursorToStringConverter
     * @see getCursorToStringConverter
     */
    open fun getStringConversionColumn(): Int {
        return mStringConversionColumn
    }

    /**
     * Defines the index of the column in the Cursor used to get a String
     * representation of that Cursor. The column is used to convert the
     * Cursor to a String only when the current CursorToStringConverter
     * is null.
     *
     * @param stringConversionColumn a valid index in the current Cursor or -1 to use the default
     * conversion mechanism
     * @see android.widget.CursorAdapter.convertToString
     * @see getStringConversionColumn
     * @see setCursorToStringConverter
     * @see getCursorToStringConverter
     */
    open fun setStringConversionColumn(stringConversionColumn: Int) {
        mStringConversionColumn = stringConversionColumn
    }

    /**
     * Returns the converter used to convert the filtering Cursor
     * into a String.
     *
     * @return null if the converter does not exist or an instance of
     * [android.widget.SimpleCursorAdapter.CursorToStringConverter]
     * @see setCursorToStringConverter
     * @see getStringConversionColumn
     * @see setStringConversionColumn
     * @see android.widget.CursorAdapter.convertToString
     */
    open fun getCursorToStringConverter(): CursorToStringConverter? {
        return mCursorToStringConverter
    }

    /**
     * Sets the converter  used to convert the filtering Cursor
     * into a String.
     *
     * @param cursorToStringConverter the Cursor to String converter, or
     * null to remove the converter
     * @see setCursorToStringConverter
     * @see getStringConversionColumn
     * @see setStringConversionColumn
     * @see android.widget.CursorAdapter.convertToString
     */
    open fun setCursorToStringConverter(cursorToStringConverter: CursorToStringConverter?) {
        mCursorToStringConverter = cursorToStringConverter
    }

    /**
     * Returns a CharSequence representation of the specified Cursor as defined
     * by the current CursorToStringConverter. If no CursorToStringConverter
     * has been set, the String conversion column is used instead. If the
     * conversion column is -1, the returned String is empty if the cursor
     * is null or Cursor.toString().
     *
     * @param cursor the Cursor to convert to a CharSequence
     * @return a non-null CharSequence representing the cursor
     */
    override fun convertToString(cursor: Cursor?): CharSequence {
        if (mCursorToStringConverter != null) {
            return mCursorToStringConverter!!.convertToString(cursor)
        } else if (mStringConversionColumn > -1 && cursor != null) {
            return cursor.getString(mStringConversionColumn)
        }

        return super.convertToString(cursor)
    }

    /**
     * Create a map from an array of strings to an array of column-id integers in cursor c.
     * If c is null, the array will be discarded.
     *
     * @param c    the cursor to find the columns from
     * @param from the Strings naming the columns of interest
     */
    private fun findColumns(c: Cursor?, from: Array<String>) {
        if (c != null) {
            val count = from.size
            if (mFrom == null || mFrom!!.size != count) {
                mFrom = IntArray(count)
            }
            for (i in 0 until count) {
                mFrom!![i] = c.getColumnIndexOrThrow(from[i])
            }
        } else {
            mFrom = null
        }
    }

    override fun swapCursor(c: Cursor?): Cursor? {
        // super.swapCursor() will notify observers before we have
        // a valid mapping, make sure we have a mapping before this
        // happens
        findColumns(c, mOriginalFrom)
        return super.swapCursor(c)
    }

    /**
     * Change the cursor and change the column-to-view mappings at the same time.
     *
     * @param c    The database cursor.  Can be null if the cursor is not available yet.
     * @param from A list of column names representing the data to bind to the UI.  Can be null
     * if the cursor is not available yet.
     * @param to   The views that should display column in the "from" parameter.
     * These should all be TextViews. The first N views in this list
     * are given the values of the first N columns in the from
     * parameter.  Can be null if the cursor is not available yet.
     */
    open fun changeCursorAndColumns(c: Cursor?, from: Array<String>, to: IntArray) {
        mOriginalFrom = from
        mTo = to
        // super.changeCursor() will notify observers before we have
        // a valid mapping, make sure we have a mapping before this
        // happens
        findColumns(c, mOriginalFrom)
        super.changeCursor(c)
    }

    /**
     * This class can be used by external clients of SimpleCursorAdapter
     * to bind values fom the Cursor to views.
     *
     *
     * You should use this class to bind values from the Cursor to views
     * that are not directly supported by SimpleCursorAdapter or to
     * change the way binding occurs for views supported by
     * SimpleCursorAdapter.
     *
     * @see SimpleCursorAdapter.bindView
     * @see SimpleCursorAdapter.setViewImage
     * @see SimpleCursorAdapter.setViewText
     */
    interface ViewBinder {
        /**
         * Binds the Cursor column defined by the specified index to the specified view.
         *
         *
         * When binding is handled by this ViewBinder, this method must return true.
         * If this method returns false, SimpleCursorAdapter will attempts to handle
         * the binding on its own.
         *
         * @param view        the view to bind the data to
         * @param cursor      the cursor to get the data from
         * @param columnIndex the column at which the data can be found in the cursor
         * @return true if the data was bound to the view, false otherwise
         */
        fun setViewValue(view: View, cursor: Cursor, columnIndex: Int): Boolean
    }

    /**
     * This class can be used by external clients of SimpleCursorAdapter
     * to define how the Cursor should be converted to a String.
     *
     * @see android.widget.CursorAdapter.convertToString
     */
    interface CursorToStringConverter {
        /**
         * Returns a CharSequence representing the specified Cursor.
         *
         * @param cursor the cursor for which a CharSequence representation
         * is requested
         * @return a non-null CharSequence representing the cursor
         */
        fun convertToString(cursor: Cursor?): CharSequence
    }
}
