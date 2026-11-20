package com.himanshu.liftly.core.domain.usecase

/**
 * Parses the quick reps field:
 * - `8 8 7 6`, `8,8,7,6`, `8, 8 ,7` → one value per set
 * - `8x4` (also `8×4`, `8*4`) → 8 reps for 4 sets; can be mixed: `10x2 8` → 10, 10, 8
 * - `8+8+7` / `8#8` / `8.8` also separate sets (what a phone keypad can type)
 */
class ParseRepInputUseCase {
    sealed interface Result {
        data class Valid(val reps: List<Int>) : Result
        data object Empty : Result
        data class Invalid(val reason: Reason) : Result
    }

    enum class Reason { NOT_A_NUMBER, REPS_OUT_OF_RANGE, TOO_MANY_SETS }

    // Early exits per token keep the parser linear and readable.
    @Suppress("ReturnCount")
    operator fun invoke(input: String): Result {
        val tokens = input.trim().replace(MULTIPLY, "x").split(SEPARATORS).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return Result.Empty
        val reps = mutableListOf<Int>()
        for (token in tokens) {
            val match = TOKEN.matchEntire(token) ?: return Result.Invalid(Reason.NOT_A_NUMBER)
            val value = match.groupValues[1].toIntOrNull() ?: return Result.Invalid(Reason.NOT_A_NUMBER)
            val times = match.groupValues[2].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 1
            if (value !in REPS_RANGE) return Result.Invalid(Reason.REPS_OUT_OF_RANGE)
            if (times !in 1..MAX_SETS || reps.size + times > MAX_SETS) return Result.Invalid(Reason.TOO_MANY_SETS)
            repeat(times) { reps += value }
        }
        return Result.Valid(reps)
    }

    companion object {
        val REPS_RANGE = 1..100
        const val MAX_SETS = 20
        private val MULTIPLY = Regex("\\s*[xX×*]\\s*")

        // Space/comma, plus "+", "#", "." so the phone keypad (numbers, + * #) can type every format.
        private val SEPARATORS = Regex("[\\s,;.+#/-]+")
        private val TOKEN = Regex("(\\d{1,4})(?:x(\\d{1,3}))?")

        /** Canonical text for a reps list, e.g. [8, 8, 7, 6] → "8 8 7 6". */
        fun format(reps: List<Int?>): String = reps.joinToString(" ") { it?.toString() ?: "" }.trim()
    }
}
