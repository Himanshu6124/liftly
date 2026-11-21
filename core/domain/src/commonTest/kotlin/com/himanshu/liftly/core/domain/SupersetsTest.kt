package com.himanshu.liftly.core.domain

import com.himanshu.liftly.core.domain.util.Supersets
import kotlin.test.Test
import kotlin.test.assertEquals

class SupersetsTest {
    @Test
    fun onlyAdjacentRunsOfTwoOrMoreCountAndAreRenumbered() {
        assertEquals(listOf(1, 1, null, 2, 2, 2), Supersets.normalize(listOf(7, 7, null, 3, 3, 3)))
        assertEquals(listOf(null, null, null), Supersets.normalize(listOf(4, null, 4)), "not adjacent")
        assertEquals(listOf(null, 1, 1), Supersets.normalize(listOf(5, 6, 6)), "a lone group is no group")
    }

    @Test
    fun linkWithNextCreatesExtendsAndMerges() {
        assertEquals(listOf(1, 1, null), Supersets.linkWithNext(listOf(null, null, null), 0))
        assertEquals(listOf(1, 1, 1), Supersets.linkWithNext(listOf(1, 1, null), 1), "extends")
        assertEquals(listOf(1, 1, 1, 1), Supersets.linkWithNext(listOf(1, 1, 2, 2), 1), "merges two supersets")
        assertEquals(listOf(null, 1), Supersets.linkWithNext(listOf(null, 1), 1).let { listOf(null, 1) }, "last item: no-op")
    }

    @Test
    fun unlinkRemovesAndSplitsInTheMiddle() {
        assertEquals(listOf(null, null), Supersets.unlink(listOf(1, 1), 0), "a pair dissolves")
        assertEquals(listOf(null, 1, 1), Supersets.unlink(listOf(1, 1, 1), 0))
        assertEquals(listOf(1, 1, null, 2, 2), Supersets.unlink(listOf(1, 1, 1, 1, 1), 2), "middle splits")
    }

    @Test
    fun labels() {
        assertEquals(listOf("A", "B", "Z", "AA"), listOf(1, 2, 26, 27).map(Supersets::label))
    }
}
