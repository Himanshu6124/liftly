package com.himanshu.liftly.core.platform

/**
 * What the home-screen widgets show. Plain values (epoch millis) so Android, Swift and the widget
 * processes can store and read it without the app's domain types.
 */
data class WidgetSnapshot(
    val thisWeekCount: Int,
    val weeklyGoal: Int,
    val streakWeeks: Int,
    /** Monday 00:00 (local) of the week [thisWeekCount] belongs to; lets a widget roll over to a new week on its own. */
    val weekStartMillis: Long,
    /** Name of the routine "Start next" would start; null when there's no non-empty routine. */
    val nextRoutine: String?,
    val workoutInProgress: Boolean,
) {
    /**
     * The snapshot as it reads in a later week the app hasn't refreshed yet: nothing done this week,
     * streak kept only if the old week reached the goal.
     */
    fun rolledOver(): WidgetSnapshot = copy(thisWeekCount = 0, streakWeeks = if (thisWeekCount >= weeklyGoal) streakWeeks else 0)

    fun encode(): String = listOf(
        thisWeekCount,
        weeklyGoal,
        streakWeeks,
        weekStartMillis,
        nextRoutine.orEmpty().replace('\n', ' '),
        workoutInProgress,
    ).joinToString("\n")

    companion object {
        private const val FIELDS = 6

        /** Line-based so Kotlin and Swift read it with one split; null for anything malformed. */
        fun decode(text: String?): WidgetSnapshot? {
            val parts = text?.split("\n")?.takeIf { it.size == FIELDS } ?: return null
            val numbers = listOf(parts[0], parts[1], parts[2]).map { it.toIntOrNull() }
            val weekStart = parts[3].toLongOrNull()
            val inProgress = parts[5].toBooleanStrictOrNull()
            if (numbers.any { it == null } || weekStart == null || inProgress == null) return null
            return WidgetSnapshot(numbers[0]!!, numbers[1]!!, numbers[2]!!, weekStart, parts[4].ifBlank { null }, inProgress)
        }
    }
}

/** Pushes a new [WidgetSnapshot] to the platform's home-screen widgets and asks them to redraw. */
interface HomeWidgetBridge {
    fun update(snapshot: WidgetSnapshot)
}

object NoHomeWidgets : HomeWidgetBridge {
    override fun update(snapshot: WidgetSnapshot) = Unit
}
