package com.himanshu.liftly

import com.himanshu.liftly.navigation.AppAction
import com.himanshu.liftly.navigation.DeepLinks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class DeepLinksTest {
    @Test
    fun parsesKnownLinksOnly() {
        assertEquals(AppAction.START_NEXT_WORKOUT, DeepLinks.parse("liftly://start-next-workout"))
        assertEquals(AppAction.OPEN_WORKOUT, DeepLinks.parse(" LIFTLY://workout/ "))
        assertNull(DeepLinks.parse("liftly://nope"))
        assertNull(DeepLinks.parse("https://example.com"))
    }

    @Test
    fun pendingIsConsumedOnce() {
        assertFalse(DeepLinks.handle("liftly://nope"))
        DeepLinks.handle("liftly://workout")
        assertEquals(AppAction.OPEN_WORKOUT, DeepLinks.pending.value)
        DeepLinks.consume(AppAction.START_NEXT_WORKOUT) // a different action doesn't clear it
        assertEquals(AppAction.OPEN_WORKOUT, DeepLinks.pending.value)
        DeepLinks.consume(AppAction.OPEN_WORKOUT)
        assertNull(DeepLinks.pending.value)
    }
}
