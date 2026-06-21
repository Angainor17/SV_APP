package com.mobeta.android.dslv

import android.content.Context
import android.database.Cursor
import android.util.SparseIntArray
import android.view.View
import android.view.ViewGroup
import androidx.cursoradapter.widget.CursorAdapter
import com.mobeta.android.dslv.DragSortCursorAdapter.Companion.REMOVED

/**
 * A subclass of [android.widget.CursorAdapter] that provides
 * reordering of the elements in the Cursor based on completed
 * drag-sort operations. The reordering is a simple mapping of
 * list positions into Cursor positions (the Cursor is unchanged).
 * To persist changes made by drag-sorts, one can retrieve the
 * mapping with the [getCursorPositions] method, which
 * returns the reordered list of Cursor positions.
 *
 *
 * An instance of this class is passed
 * to [DragSortListView.setAdapter] and, since
 * this class implements the [DragSortListView.DragSortListener]
 * interface, it is automatically set as the DragSortListener for
 * the DragSortListView instance.
 */
abstract class DragSortCursorAdapter : CursorAdapter, DragSortListView.DragSortListener {

    companion object {
        const val REMOVED = -1
    }

    /**
     * Key is ListView position, value is Cursor position
     */
    private var listMapping: SparseIntArray = SparseIntArray()

    private var removedCursorPositions: ArrayList<Int> = ArrayList()

    constructor(context: Context, c: Cursor?) : super(context, c)

    constructor(context: Context, c: Cursor?, autoRequery: Boolean) : super(context, c, autoRequery)

    constructor(context: Context, c: Cursor?, flags: Int) : super(context, c, flags)

    /**
     * Swaps Cursor and clears list-Cursor mapping.
     *
     * @see android.widget.CursorAdapter.swapCursor
     */
    override fun swapCursor(newCursor: Cursor?): Cursor? {
        val old = super.swapCursor(newCursor)
        resetMappings()
        return old
    }

    /**
     * Changes Cursor and clears list-Cursor mapping.
     *
     * @see android.widget.CursorAdapter.changeCursor
     */
    override fun changeCursor(cursor: Cursor?) {
        super.changeCursor(cursor)
        resetMappings()
    }

    /**
     * Resets list-cursor mapping.
     */
    fun reset() {
        resetMappings()
        notifyDataSetChanged()
    }

    private fun resetMappings() {
        listMapping.clear()
        removedCursorPositions.clear()
    }

    override fun getItem(position: Int): Any {
        return super.getItem(listMapping.get(position, position))
    }

    override fun getItemId(position: Int): Long {
        return super.getItemId(listMapping.get(position, position))
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getDropDownView(listMapping.get(position, position), convertView, parent)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getView(listMapping.get(position, position), convertView, parent)
    }

    /**
     * On drop, this updates the mapping between Cursor positions
     * and ListView positions. The Cursor is unchanged. Retrieve
     * the current mapping with [getCursorPositions].
     *
     * @see DragSortListView.DropListener.drop
     */
    override fun drop(from: Int, to: Int) {
        if (from != to) {
            val cursorFrom = listMapping.get(from, from)

            if (from > to) {
                for (i in from downTo to + 1) {
                    listMapping.put(i, listMapping.get(i - 1, i - 1))
                }
            } else {
                for (i in from until to) {
                    listMapping.put(i, listMapping.get(i + 1, i + 1))
                }
            }
            listMapping.put(to, cursorFrom)

            cleanMapping()
            notifyDataSetChanged()
        }
    }

    /**
     * On remove, this updates the mapping between Cursor positions
     * and ListView positions. The Cursor is unchanged. Retrieve
     * the current mapping with [getCursorPositions].
     *
     * @see DragSortListView.RemoveListener.remove
     */
    override fun remove(which: Int) {
        val cursorPos = listMapping.get(which, which)
        if (!removedCursorPositions.contains(cursorPos)) {
            removedCursorPositions.add(cursorPos)
        }

        val newCount = count
        for (i in which until newCount) {
            listMapping.put(i, listMapping.get(i + 1, i + 1))
        }

        listMapping.delete(newCount)

        cleanMapping()
        notifyDataSetChanged()
    }

    /**
     * Does nothing. Just completes DragSortListener interface.
     */
    override fun drag(from: Int, to: Int) {
        // do nothing
    }

    /**
     * Remove unnecessary mappings from sparse array.
     */
    private fun cleanMapping() {
        val toRemove = ArrayList<Int>()

        val size = listMapping.size()
        for (i in 0 until size) {
            if (listMapping.keyAt(i) == listMapping.valueAt(i)) {
                toRemove.add(listMapping.keyAt(i))
            }
        }

        for (key in toRemove) {
            listMapping.delete(key)
        }
    }

    override fun getCount(): Int {
        return super.getCount() - removedCursorPositions.size
    }

    /**
     * Get the Cursor position mapped to by the provided list position
     * (given all previously handled drag-sort
     * operations).
     *
     * @param position List position
     * @return The mapped-to Cursor position
     */
    fun getCursorPosition(position: Int): Int {
        return listMapping.get(position, position)
    }

    /**
     * Get the current order of Cursor positions presented by the
     * list.
     */
    fun getCursorPositions(): ArrayList<Int> {
        val result = ArrayList<Int>()

        for (i in 0 until count) {
            result.add(listMapping.get(i, i))
        }

        return result
    }

    /**
     * Get the list position mapped to by the provided Cursor position.
     * If the provided Cursor position has been removed by a drag-sort,
     * this returns [REMOVED].
     *
     * @param cursorPosition A Cursor position
     * @return The mapped-to list position or REMOVED
     */
    fun getListPosition(cursorPosition: Int): Int {
        if (removedCursorPositions.contains(cursorPosition)) {
            return REMOVED
        }

        val index = listMapping.indexOfValue(cursorPosition)
        return if (index < 0) {
            cursorPosition
        } else {
            listMapping.keyAt(index)
        }
    }
}
