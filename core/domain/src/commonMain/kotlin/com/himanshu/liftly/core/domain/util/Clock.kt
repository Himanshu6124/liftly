package com.himanshu.liftly.core.domain.util

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Injectable time source so use cases and repositories are deterministic in tests. */
fun interface TimeProvider {
    fun now(): Instant

    companion object {
        val System: TimeProvider = TimeProvider { Clock.System.now() }
    }
}

/** Injectable id generator; production uses random UUIDv4 strings. */
fun interface IdGenerator {
    fun newId(): String

    companion object {
        @OptIn(ExperimentalUuidApi::class)
        val Uuid: IdGenerator = IdGenerator { kotlin.uuid.Uuid.random().toString() }
    }
}
