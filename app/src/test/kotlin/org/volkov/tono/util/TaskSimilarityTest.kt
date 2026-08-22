package org.volkov.tono.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskSimilarityTest {

    // --- normalizeTaskText ---

    @Test
    fun `normalize lowercases and collapses whitespace`() {
        assertEquals("remember the milk", normalizeTaskText("  Remember   THE  milk \n"))
    }

    @Test
    fun `normalize drops punctuation`() {
        assertEquals("call mom", normalizeTaskText("Call mom!"))
        assertEquals("review pr 482", normalizeTaskText("review PR #482"))
    }

    @Test
    fun `normalize keeps digits and non-latin letters`() {
        assertEquals("купить молоко 2", normalizeTaskText("Купить молоко (2)"))
    }

    @Test
    fun `normalize of blank text is empty`() {
        assertEquals("", normalizeTaskText("   \t "))
    }

    // --- levenshtein ---

    @Test
    fun `levenshtein counts single edits`() {
        assertEquals(0, levenshtein("milk", "milk"))
        assertEquals(1, levenshtein("milk", "silk"))
        assertEquals(1, levenshtein("parser", "parsr"))
        assertEquals(4, levenshtein("", "milk"))
    }

    @Test
    fun `levenshteinRatio is 1 for identical and 0 for disjoint`() {
        assertEquals(1.0, levenshteinRatio("abc", "abc"), 1e-9)
        assertEquals(1.0, levenshteinRatio("", ""), 1e-9)
        assertEquals(0.0, levenshteinRatio("abc", "xyz"), 1e-9)
    }

    // --- taskSimilarity / isSameTask ---

    @Test
    fun `identical text after normalization is the same task`() {
        assertEquals(1.0, taskSimilarity("Call mom!", "call  MOM"), 1e-9)
        assertTrue(isSameTask("Call mom!", "call  MOM"))
    }

    @Test
    fun `dropped filler word is still the same task`() {
        assertTrue(isSameTask("remember the milk", "remember milk"))
    }

    @Test
    fun `reordered words are still the same task`() {
        assertTrue(isSameTask("book dentist appointment", "dentist appointment book"))
    }

    @Test
    fun `typo fix is still the same task`() {
        assertTrue(isSameTask("fix bug in parser", "fix bug in parsr"))
    }

    @Test
    fun `added detail keeps a short task recognisable`() {
        assertTrue(isSameTask("call mom", "call mom tomorrow"))
    }

    @Test
    fun `same verb different object is a different task`() {
        assertFalse(isSameTask("buy milk", "buy bread"))
        assertFalse(isSameTask("pay rent", "pay taxes"))
    }

    @Test
    fun `unrelated tasks are not the same`() {
        assertFalse(isSameTask("stand-up @ 10", "review PR #482"))
        assertFalse(isSameTask("gym", "call mom"))
    }

    @Test
    fun `short words are not fuzzy-matched on characters alone`() {
        // Four letters and one edit apart: too little text to call it the same task.
        assertFalse(isSameTask("milk", "silk"))
    }

    @Test
    fun `a much longer line is not swallowed by a short one`() {
        assertFalse(isSameTask("gym", "gym membership renewal paperwork"))
    }

    @Test
    fun `blank text matches nothing`() {
        assertEquals(0.0, taskSimilarity("", "call mom"), 1e-9)
        assertFalse(isSameTask("   ", "call mom"))
    }

    @Test
    fun `similarity is symmetric`() {
        val a = "remember the milk"
        val b = "remember milk"
        assertEquals(taskSimilarity(a, b), taskSimilarity(b, a), 1e-9)
    }
}
