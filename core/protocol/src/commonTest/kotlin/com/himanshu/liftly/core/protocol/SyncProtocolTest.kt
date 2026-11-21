package com.himanshu.liftly.core.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Pins the JSON field names: the server and older installed apps depend on them. */
class SyncProtocolTest {

    private val json = Json

    private val record = SyncRecord(
        table = SyncProtocol.WORKOUT_SETS,
        id = "a1",
        updatedAt = 10,
        deletedAt = null,
        serverVersion = 3,
        payload = buildJsonObject { put("reps", JsonPrimitive(8)) },
    )

    @Test
    fun syncRecordWireFormat() {
        val encoded = json.encodeToString(SyncRecord.serializer(), record)
        assertEquals(
            """{"table":"workout_sets","id":"a1","updatedAt":10,"deletedAt":null,"serverVersion":3,"payload":{"reps":8}}""",
            encoded,
        )
        assertEquals(record, json.decodeFromString(SyncRecord.serializer(), encoded))
    }

    @Test
    fun pullPageAndAckWireFormat() {
        val page = PullPage(listOf(record), nextCursor = "3", hasMore = false)
        val decoded = json.decodeFromString(PullPage.serializer(), json.encodeToString(PullPage.serializer(), page))
        assertEquals(page, decoded)
        assertEquals(
            """{"id":"a1","updatedAt":10,"serverVersion":3}""",
            json.encodeToString(PushAck.serializer(), PushAck("a1", 10, 3)),
        )
    }

    @Test
    fun authWireFormat() {
        val tokens = AuthTokens("acc", 99, "ref", Account("u1", null))
        assertEquals(
            """{"accessToken":"acc","accessExpiresAt":99,"refreshToken":"ref","account":{"userId":"u1","email":null}}""",
            json.encodeToString(AuthTokens.serializer(), tokens),
        )
        // Google sign-in sends no nonce; the default keeps the field optional on the wire.
        assertEquals(SignInRequest("t"), json.decodeFromString(SignInRequest.serializer(), """{"idToken":"t"}"""))
    }

    @Test
    fun tablesAreUniqueAndParentsFirst() {
        val t = SyncProtocol.TABLES
        assertEquals(t.size, t.toSet().size)
        assertTrue(t.indexOf(SyncProtocol.WORKOUTS) < t.indexOf(SyncProtocol.WORKOUT_EXERCISES))
        assertTrue(t.indexOf(SyncProtocol.WORKOUT_EXERCISES) < t.indexOf(SyncProtocol.WORKOUT_SETS))
        assertTrue(t.indexOf(SyncProtocol.ROUTINES) < t.indexOf(SyncProtocol.ROUTINE_EXERCISES))
    }
}
