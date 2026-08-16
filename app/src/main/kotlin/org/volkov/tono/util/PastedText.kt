package org.volkov.tono.util

/**
 * Result of splitting pasted multi-line text: every complete line (all but the last)
 * becomes its own entry; the trailing fragment stays live in the field being edited.
 */
data class PastedLines(val toCreate: List<String>, val remainder: String)

/**
 * Splits a pasted value on newlines. Complete lines are trimmed and blanks dropped,
 * so they can be persisted as separate tasks; the final segment (which may be empty,
 * e.g. when the paste ends in "\n") is returned verbatim as the remainder.
 */
fun splitPastedLines(value: String): PastedLines {
    val lines = value.split("\n")
    return PastedLines(
        toCreate = lines.dropLast(1).map { it.trim() }.filter { it.isNotBlank() },
        remainder = lines.last(),
    )
}
