package org.volkov.tono.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for multi-line paste splitting (the logic behind pasting a block
 * of text into a day and having each line become its own task).
 */
class PastedTextTest {

    @Test
    fun `complete lines become entries and the tail stays live`() {
        val (toCreate, remainder) = splitPastedLines("buy milk\nwalk dog\nhalf-typed")
        assertEquals(listOf("buy milk", "walk dog"), toCreate)
        assertEquals("half-typed", remainder)
    }

    @Test
    fun `trailing newline flushes every line and leaves an empty remainder`() {
        val (toCreate, remainder) = splitPastedLines("a\nb\n")
        assertEquals(listOf("a", "b"), toCreate)
        assertEquals("", remainder)
    }

    @Test
    fun `blank and whitespace-only lines are dropped from entries`() {
        val (toCreate, remainder) = splitPastedLines("a\n\n   \nb\nc")
        assertEquals(listOf("a", "b"), toCreate)
        assertEquals("c", remainder)
    }

    @Test
    fun `entries are trimmed but the remainder is preserved verbatim`() {
        val (toCreate, remainder) = splitPastedLines("  a  \n\tb\t\n  tail")
        assertEquals(listOf("a", "b"), toCreate)
        assertEquals("  tail", remainder) // remainder keeps leading spaces the user is still typing
    }

    @Test
    fun `a leading newline produces no entries`() {
        val (toCreate, remainder) = splitPastedLines("\nfirst")
        assertTrue(toCreate.isEmpty())
        assertEquals("first", remainder)
    }

    @Test
    fun `a value with no newline is returned untouched as the remainder`() {
        // Defensive: the ViewModel only calls this when a newline is present, but the
        // helper should still behave sensibly for a single fragment.
        val (toCreate, remainder) = splitPastedLines("just typing")
        assertTrue(toCreate.isEmpty())
        assertEquals("just typing", remainder)
    }
}
