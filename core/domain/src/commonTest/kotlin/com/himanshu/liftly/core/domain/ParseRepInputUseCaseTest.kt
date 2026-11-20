package com.himanshu.liftly.core.domain

import com.himanshu.liftly.core.domain.usecase.ParseRepInputUseCase
import com.himanshu.liftly.core.domain.usecase.ParseRepInputUseCase.Reason
import com.himanshu.liftly.core.domain.usecase.ParseRepInputUseCase.Result
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseRepInputUseCaseTest {
    private val parse = ParseRepInputUseCase()

    @Test fun spaceSeparated() = assertEquals(Result.Valid(listOf(8, 8, 7, 6)), parse("8 8 7 6"))

    @Test fun commaSeparated() = assertEquals(Result.Valid(listOf(8, 8, 7, 6)), parse("8,8,7,6"))

    @Test fun setsTimesReps() = assertEquals(Result.Valid(listOf(8, 8, 8, 8)), parse("8x4"))

    @Test fun otherMultiplySigns() {
        assertEquals(Result.Valid(listOf(5, 5, 5)), parse("5×3"))
        assertEquals(Result.Valid(listOf(5, 5, 5)), parse("5 * 3"))
    }

    @Test fun mixedTokens() = assertEquals(Result.Valid(listOf(10, 10, 8)), parse("10x2 8"))

    @Test fun extraSpacesAndCommas() = assertEquals(Result.Valid(listOf(8, 7)), parse("   8 ,  ,7  "))

    @Test fun emptyInput() {
        assertEquals(Result.Empty, parse(""))
        assertEquals(Result.Empty, parse("  , "))
    }

    @Test fun invalidInput() {
        assertEquals(Result.Invalid(Reason.NOT_A_NUMBER), parse("eight"))
        assertEquals(Result.Invalid(Reason.NOT_A_NUMBER), parse("8 a"))
        assertEquals(Result.Invalid(Reason.REPS_OUT_OF_RANGE), parse("0"))
        assertEquals(Result.Invalid(Reason.REPS_OUT_OF_RANGE), parse("101"))
        assertEquals(Result.Invalid(Reason.TOO_MANY_SETS), parse("5x21"))
        assertEquals(Result.Invalid(Reason.TOO_MANY_SETS), parse("5x0"))
    }

    @Test fun phoneKeypadSeparators() {
        assertEquals(Result.Valid(listOf(8, 8, 7)), parse("8+8+7"))
        assertEquals(Result.Valid(listOf(8, 8, 8)), parse("8*3"))
    }

    @Test fun formatRoundTrips() = assertEquals("8 8 7 6", ParseRepInputUseCase.format(listOf(8, 8, 7, 6)))
}
