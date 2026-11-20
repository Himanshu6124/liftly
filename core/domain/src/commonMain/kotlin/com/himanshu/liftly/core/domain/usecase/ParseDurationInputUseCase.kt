package com.himanshu.liftly.core.domain.usecase

/**
 * Parses per-set durations: `60 45 45` (seconds), `1:00 0:45`, `60x3` / `1:00x3`.
 * Same separators as the reps field so the phone keypad works.
 */
class ParseDurationInputUseCase {
    sealed interface Result {
        data class Valid(val seconds: List<Int>) : Result
        data object Empty : Result
        data object Invalid : Result
    }

    @Suppress("ReturnCount") // early exits per token
    operator fun invoke(input: String): Result {
        val tokens = input.trim().replace(MULTIPLY, "x").split(SEPARATORS).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return Result.Empty
        val out = mutableListOf<Int>()
        for (token in tokens) {
            val m = TOKEN.matchEntire(token) ?: return Result.Invalid
            val seconds = parseClock(m.groupValues[1]) ?: return Result.Invalid
            val times = m.groupValues[2].toIntOrNull() ?: 1
            if (seconds !in 1..MAX_SECONDS || times !in 1..ParseRepInputUseCase.MAX_SETS) return Result.Invalid
            if (out.size + times > ParseRepInputUseCase.MAX_SETS) return Result.Invalid
            repeat(times) { out += seconds }
        }
        return Result.Valid(out)
    }

    companion object {
        const val MAX_SECONDS = 6 * 60 * 60
        private val MULTIPLY = Regex("\\s*[xX×*]\\s*")
        private val SEPARATORS = Regex("[\\s,;+#/]+")
        private val TOKEN = Regex("([0-9:.]+)(?:x(\\d{1,3}))?")

        /** "90" → 90, "1:30" → 90, "1.30" → 90 (phone keypads may lack ':'), "1:02:03" → 3723. */
        fun parseClock(text: String): Int? {
            val parts = text.split(':', '.')
            if (parts.any { it.isEmpty() || !it.all(Char::isDigit) } || parts.size > 3) return null
            val nums = parts.map { it.toInt() }
            if (nums.drop(1).any { it >= 60 }) return null
            return nums.fold(0) { acc, n -> acc * 60 + n }
        }

        fun format(seconds: Int): String {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            return if (h >
                0
            ) {
                "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
            } else {
                "$m:${s.toString().padStart(2, '0')}"
            }
        }

        fun formatAll(seconds: List<Int?>): String = seconds.joinToString(" ") { it?.let(::format) ?: "" }.trim()
    }
}
