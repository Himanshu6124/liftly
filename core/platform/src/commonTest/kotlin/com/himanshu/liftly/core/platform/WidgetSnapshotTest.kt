package com.himanshu.liftly.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WidgetSnapshotTest {
    private val snapshot = WidgetSnapshot(2, 3, 4, 1_700_000_000_000, "Push\nday", workoutInProgress = false)

    @Test
    fun roundTripsThroughText() {
        assertEquals(snapshot.copy(nextRoutine = "Push day"), WidgetSnapshot.decode(snapshot.encode()))
        val noRoutine = snapshot.copy(nextRoutine = null, workoutInProgress = true)
        assertEquals(noRoutine, WidgetSnapshot.decode(noRoutine.encode()))
    }

    @Test
    fun rejectsMalformedText() {
        assertNull(WidgetSnapshot.decode(null))
        assertNull(WidgetSnapshot.decode("1\n2"))
        assertNull(WidgetSnapshot.decode("x\n3\n4\n5\nPush\nfalse"))
    }

    @Test
    fun newWeekKeepsTheStreakOnlyIfTheGoalWasMet() {
        assertEquals(WidgetSnapshot(0, 3, 0, snapshot.weekStartMillis, "Push\nday", false), snapshot.rolledOver())
        val met = snapshot.copy(thisWeekCount = 3)
        assertEquals(4, met.rolledOver().streakWeeks)
        assertEquals(0, met.rolledOver().thisWeekCount)
    }
}
