package com.himanshu.liftly.core.domain.util

/**
 * Superset groups over an ordered list of exercises (one `Int?` per exercise, null = not in one).
 * A superset is a run of 2+ *adjacent* exercises with the same group; anything else is no group.
 * Groups are renumbered 1, 2, … in list order so labels (A, B, …) stay stable and short.
 */
object Supersets {
    fun normalize(groups: List<Int?>): List<Int?> {
        val out = MutableList<Int?>(groups.size) { null }
        var next = 1
        var start = 0
        while (start < groups.size) {
            val g = groups[start]
            var end = start
            while (end + 1 < groups.size && g != null && groups[end + 1] == g) end++
            if (g != null && end > start) {
                for (i in start..end) out[i] = next
                next++
            }
            start = end + 1
        }
        return out
    }

    /** Joins exercise [index] and the one after it (merging their groups if both had one). */
    fun linkWithNext(groups: List<Int?>, index: Int): List<Int?> {
        if (index !in 0 until groups.lastIndex) return normalize(groups)
        val current = normalize(groups)
        val target = current[index] ?: current[index + 1] ?: ((current.filterNotNull().maxOrNull() ?: 0) + 1)
        val absorbed = current[index + 1]?.takeIf { it != target }
        return normalize(
            current.mapIndexed { i, g ->
                when {
                    i == index || i == index + 1 -> target
                    absorbed != null && g == absorbed -> target
                    else -> g
                }
            },
        )
    }

    /** Takes exercise [index] out of its superset; the rest stays grouped only where still adjacent. */
    fun unlink(groups: List<Int?>, index: Int): List<Int?> {
        val current = normalize(groups)
        if (index !in current.indices || current[index] == null) return current
        // Give the part after [index] its own number so a middle removal splits into two supersets.
        val g = current[index]
        val split = (current.filterNotNull().maxOrNull() ?: 0) + 1
        return normalize(
            current.mapIndexed { i, v ->
                when {
                    i == index -> null
                    i > index && v == g -> split
                    else -> v
                }
            },
        )
    }

    /** 1 → "A", 2 → "B", … 27 → "AA". */
    fun label(group: Int): String {
        var n = group
        val sb = StringBuilder()
        while (n > 0) {
            val r = (n - 1) % LETTERS
            sb.insert(0, 'A' + r)
            n = (n - 1) / LETTERS
        }
        return sb.toString()
    }

    private const val LETTERS = 26
}
