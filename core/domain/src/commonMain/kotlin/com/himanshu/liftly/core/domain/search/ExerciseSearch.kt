package com.himanshu.liftly.core.domain.search

import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.Exercise
import com.himanshu.liftly.core.domain.model.MuscleGroup

/**
 * Fuzzy, token-based exercise search over name + aliases.
 *
 * - Case-insensitive, punctuation-insensitive, word order doesn't matter.
 * - Gym abbreviations are expanded ("db" → dumbbell, "bb" → barbell, "kb" → kettlebell).
 * - Every query token must match some word (exactly, by prefix, by substring, or with one typo).
 * - Results are ranked by match quality, then name.
 */
object ExerciseSearch {

    private val ABBREVIATIONS = mapOf(
        "db" to "dumbbell", "dbs" to "dumbbell", "bb" to "barbell", "kb" to "kettlebell",
        "ez" to "ez", "bw" to "bodyweight", "ext" to "extension", "lat" to "lat",
        "tri" to "triceps", "bi" to "biceps", "delt" to "delt", "pushup" to "push up", "pullup" to "pull up",
        "situp" to "sit up", "chinup" to "chin up",
    )

    fun filter(
        exercises: List<Exercise>,
        query: String,
        muscleGroup: MuscleGroup? = null,
        equipment: Equipment? = null,
    ): List<Exercise> {
        val filtered = exercises.filter { e ->
            (muscleGroup == null || e.primaryMuscle in muscleGroup.muscles) &&
                (equipment == null || e.equipment == equipment)
        }
        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) return filtered.sortedBy { it.name.lowercase() }

        return filtered
            .mapNotNull { e -> score(e, queryTokens, query)?.let { e to it } }
            .sortedWith(compareByDescending<Pair<Exercise, Double>> { it.second }.thenBy { it.first.name.lowercase() })
            .map { it.first }
    }

    /** Best score over the name and all aliases, or null if this exercise doesn't match. */
    internal fun score(exercise: Exercise, queryTokens: List<String>, rawQuery: String): Double? {
        val normalizedQuery = tokenize(rawQuery).joinToString(" ")
        return (listOf(exercise.name) + exercise.aliases)
            .mapIndexedNotNull { index, text ->
                val targetTokens = tokenize(text)
                val base = matchScore(queryTokens, targetTokens) ?: return@mapIndexedNotNull null
                var s = base
                val normalizedTarget = targetTokens.joinToString(" ")
                if (normalizedTarget == normalizedQuery) s += 5.0
                if (normalizedTarget.startsWith(normalizedQuery)) s += 2.0
                if (index == 0) s += 0.5 // prefer the canonical name over an alias
                s - 0.1 * (targetTokens.size - queryTokens.size).coerceAtLeast(0) // shorter = more specific
            }
            .maxOrNull()
    }

    private fun matchScore(query: List<String>, target: List<String>): Double? {
        var total = 0.0
        for (q in query) {
            val best = target.maxOfOrNull { t -> tokenScore(q, t) } ?: 0.0
            if (best == 0.0) return null
            total += best
        }
        return total
    }

    private fun tokenScore(q: String, t: String): Double = when {
        q == t -> 3.0
        t.startsWith(q) -> 2.0
        q.length >= 3 && t.contains(q) -> 1.0
        q.length >= 4 && t.length >= 4 && withinOneEdit(q, t.take(q.length + 1)) -> 0.8
        q.length >= 4 && withinOneEdit(q, t) -> 0.8
        else -> 0.0
    }

    internal fun tokenize(text: String): List<String> = text.lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .split(' ')
        .filter { it.isNotEmpty() }
        .flatMap { (ABBREVIATIONS[it] ?: it).split(' ') }
        .map(::singular)

    /** "crunches" → "crunch", "presses" → "press", "raises" → "raise"; applied to both sides. */
    private fun singular(word: String): String = when {
        word.length > 4 && (word.endsWith("ches") || word.endsWith("shes") || word.endsWith("sses") || word.endsWith("xes")) ->
            word.dropLast(2)

        word.length > 3 && word.endsWith("s") && !word.endsWith("ss") -> word.dropLast(1)

        else -> word
    }

    /** True when the Levenshtein distance between [a] and [b] is at most 1. */
    internal fun withinOneEdit(a: String, b: String): Boolean {
        if (a == b) return true
        if (kotlin.math.abs(a.length - b.length) > 1) return false
        var i = 0
        var j = 0
        var edits = 0
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) {
                i++
                j++
                continue
            }
            if (++edits > 1) return false
            when {
                a.length > b.length -> i++

                a.length < b.length -> j++

                else -> {
                    i++
                    j++
                }
            }
        }
        return edits + (a.length - i) + (b.length - j) <= 1
    }
}
