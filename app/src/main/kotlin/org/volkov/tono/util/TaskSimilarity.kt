package org.volkov.tono.util

/**
 * Matching of task *text*, used to decide whether a freshly typed line is really the same
 * task the user has been carrying for a while — so its age keeps running instead of resetting.
 *
 * Moving a task inside one screen preserves its row (and therefore its age) already; this is
 * for the cases where the row does not survive: retyping a month task onto a day, deleting and
 * re-adding, or rewording a line into something almost identical.
 *
 * Kept pure so it can be unit-tested without the Android runtime.
 */

/** Score at or above which two task texts are treated as the same task. */
const val SIMILARITY_THRESHOLD = 0.75

/**
 * Below this length a character-level match is not trusted on its own — at three or four
 * characters a single edit ("milk" / "silk") already looks like a typo of the same word.
 */
private const val MIN_FUZZY_LENGTH = 8

/** Length at which two tokens may match fuzzily rather than exactly. */
private const val MIN_FUZZY_TOKEN_LENGTH = 4

/** Per-token match score required inside [tokenOverlap]. */
private const val TOKEN_THRESHOLD = 0.8

private val WHITESPACE = Regex("\\s+")

/**
 * Case-, punctuation- and spacing-insensitive form of a task line. This is the key task
 * history is stored under, so `Call mom!` and `call  mom` are one entry.
 */
fun normalizeTaskText(text: String): String =
    text.lowercase()
        .map { if (it.isLetterOrDigit()) it else ' ' }
        .joinToString("")
        .replace(WHITESPACE, " ")
        .trim()

fun taskTokens(text: String): List<String> =
    normalizeTaskText(text).split(' ').filter { it.isNotEmpty() }

/** Edit distance, two-row DP — task lines are short, so the O(n·m) form is plenty. */
fun levenshtein(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length

    var previous = IntArray(b.length + 1) { it }
    var current = IntArray(b.length + 1)

    for (i in 1..a.length) {
        current[0] = i
        for (j in 1..b.length) {
            val substitution = previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
            current[j] = minOf(current[j - 1] + 1, previous[j] + 1, substitution)
        }
        val swap = previous
        previous = current
        current = swap
    }
    return previous[b.length]
}

/** Edit distance expressed as a 0..1 similarity, 1 meaning identical. */
fun levenshteinRatio(a: String, b: String): Double {
    val longest = maxOf(a.length, b.length)
    if (longest == 0) return 1.0
    return 1.0 - levenshtein(a, b).toDouble() / longest
}

/**
 * How alike two task lines are, on 0..1. Two measures are taken and the stronger wins:
 *
 * - **character level** — catches typo fixes and small rewordings (`fix parser` / `fix parsr`);
 * - **token level** — order-insensitive Dice overlap, which catches added or dropped filler
 *   (`remember the milk` / `remember milk`) that a character diff would punish too hard.
 */
fun taskSimilarity(a: String, b: String): Double {
    val normA = normalizeTaskText(a)
    val normB = normalizeTaskText(b)
    if (normA.isEmpty() || normB.isEmpty()) return if (normA == normB) 1.0 else 0.0
    if (normA == normB) return 1.0

    val charScore =
        if (maxOf(normA.length, normB.length) >= MIN_FUZZY_LENGTH) levenshteinRatio(normA, normB)
        else 0.0

    return maxOf(charScore, tokenOverlap(taskTokens(normA), taskTokens(normB)))
}

fun isSameTask(a: String, b: String): Boolean = taskSimilarity(a, b) >= SIMILARITY_THRESHOLD

/** Dice coefficient over greedily paired tokens, where a pair may itself be a near-match. */
private fun tokenOverlap(a: List<String>, b: List<String>): Double {
    if (a.isEmpty() || b.isEmpty()) return 0.0
    val unpaired = b.toMutableList()
    var paired = 0
    a.forEach { token ->
        val i = unpaired.indexOfFirst { tokensMatch(token, it) }
        if (i >= 0) {
            unpaired.removeAt(i)
            paired++
        }
    }
    return 2.0 * paired / (a.size + b.size)
}

private fun tokensMatch(a: String, b: String): Boolean = when {
    a == b -> true
    minOf(a.length, b.length) < MIN_FUZZY_TOKEN_LENGTH -> false
    else -> levenshteinRatio(a, b) >= TOKEN_THRESHOLD
}
