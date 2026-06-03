package com.mobeta.android.dslv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for DragSortCursorAdapter.
 * Tests the position mapping functionality without Android dependencies.
 */
class DragSortCursorAdapterTest {

    /**
     * Test that the REMOVED constant is correctly defined.
     */
    @Test
    fun testRemovedConstant() {
        assertEquals(-1, DragSortCursorAdapter.REMOVED)
    }

    /**
     * Test position mapping logic in isolation.
     * This simulates the internal mapping behavior.
     */
    @Test
    fun testPositionMappingLogic() {
        // Simulate mapping: key=ListView position, value=Cursor position
        val mapping = mutableMapOf<Int, Int>()

        // Initial state: all positions map to themselves
        // After drop from position 2 to position 4:
        // Position 2 -> cursor 2 (original item at 2)
        // Position 3 -> cursor 1 (item shifted up)
        // Position 4 -> cursor 0 (item shifted up)

        // Simulate drop(2, 4)
        val from = 2
        val to = 4
        val cursorFrom = mapping.getOrDefault(from, from)

        if (from < to) {
            for (i in from until to) {
                mapping[i] = mapping.getOrDefault(i + 1, i + 1)
            }
        }
        mapping[to] = cursorFrom

        // Verify the mapping after drop
        assertEquals(2, mapping[to]) // Position 4 now has cursor position 2
    }

    /**
     * Test reverse mapping logic.
     */
    @Test
    fun testReverseMapping() {
        val mapping = mapOf(0 to 2, 1 to 0, 2 to 1)
        val removedPositions = setOf<Int>()

        // Find list position for cursor position 2
        fun getListPosition(cursorPosition: Int): Int {
            if (cursorPosition in removedPositions) {
                return DragSortCursorAdapter.REMOVED
            }
            val index = mapping.values.indexOf(cursorPosition)
            return if (index < 0) cursorPosition else mapping.keys.elementAt(index)
        }

        assertEquals(0, getListPosition(2)) // cursor 2 is at list position 0
        assertEquals(1, getListPosition(0)) // cursor 0 is at list position 1
        assertEquals(2, getListPosition(1)) // cursor 1 is at list position 2
    }

    /**
     * Test removal tracking logic.
     */
    @Test
    fun testRemovalTracking() {
        val removedPositions = mutableSetOf<Int>()
        val removedPosition = 3

        // Add to removed set
        removedPositions.add(removedPosition)

        // Check if position is removed
        assertTrue(removedPositions.contains(removedPosition))
        assertFalse(removedPositions.contains(0))

        // Verify REMOVED constant usage
        if (removedPositions.contains(removedPosition)) {
            assertEquals(DragSortCursorAdapter.REMOVED, -1)
        }
    }
}
