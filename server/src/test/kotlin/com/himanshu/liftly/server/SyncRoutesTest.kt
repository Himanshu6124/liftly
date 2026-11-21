package com.himanshu.liftly.server

import com.himanshu.liftly.core.protocol.ApiError
import com.himanshu.liftly.core.protocol.PullPage
import com.himanshu.liftly.core.protocol.PushAck
import com.himanshu.liftly.core.protocol.SyncProtocol
import com.himanshu.liftly.core.protocol.SyncRecord
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncRoutesTest {

    @BeforeTest
    fun reset() = TestDatabase.reset()

    private fun routine(id: String, name: String, updatedAt: Long, deletedAt: Long? = null) = SyncRecord(
        table = SyncProtocol.ROUTINES,
        id = id,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        serverVersion = null,
        payload = buildJsonObject { put("name", JsonPrimitive(name)) },
    )

    private fun serverTest(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        application { liftlyModule(TestDatabase.dataSource, DevUserResolver(enabled = true)) }
        block(createClient { install(ContentNegotiation) { json() } })
    }

    private suspend fun HttpClient.push(records: List<SyncRecord>, user: String = "alice", table: String = SyncProtocol.ROUTINES) =
        post("/sync/$table/push") {
            header(DevUserResolver.HEADER, user)
            contentType(ContentType.Application.Json)
            setBody(records)
        }

    private suspend fun HttpClient.pull(cursor: String? = null, limit: Int? = null, user: String = "alice"): HttpResponse =
        get("/sync/${SyncProtocol.ROUTINES}/pull") {
            header(DevUserResolver.HEADER, user)
            cursor?.let { url.parameters.append("cursor", it) }
            limit?.let { url.parameters.append("limit", it.toString()) }
        }

    private fun SyncRecord.name() = (payload["name"] as JsonPrimitive).content

    @Test
    fun pushedRecordsArePulledBackWithVersions() = serverTest { client ->
        val acks = client.push(listOf(routine("r1", "Push", 10), routine("r2", "Pull", 11))).body<List<PushAck>>()
        assertEquals(listOf("r1", "r2"), acks.map { it.id })
        assertTrue(acks[0].serverVersion < acks[1].serverVersion)

        val page = client.pull().body<PullPage>()
        assertEquals(listOf("Push", "Pull"), page.records.map { it.name() })
        assertEquals(acks.map { it.serverVersion }, page.records.map { it.serverVersion })
        assertEquals(acks[1].serverVersion.toString(), page.nextCursor)
        assertEquals(false, page.hasMore)
        // Nothing new after the cursor.
        assertEquals(0, client.pull(cursor = page.nextCursor).body<PullPage>().records.size)
    }

    @Test
    fun newerWriteWinsAndOlderWriteIsAckedButIgnored() = serverTest { client ->
        client.push(listOf(routine("r1", "Newer", 20)))
        val stale = client.push(listOf(routine("r1", "Older", 10))).body<List<PushAck>>().single()
        val page = client.pull().body<PullPage>()
        assertEquals("Newer", page.records.single().name())
        // Acked with the winner's version, so the client stops re-pushing and pulls the winner.
        assertEquals(page.records.single().serverVersion, stale.serverVersion)

        client.push(listOf(routine("r1", "Newest", 30)))
        assertEquals("Newest", client.pull().body<PullPage>().records.single().name())
    }

    @Test
    fun tombstonesArePulled() = serverTest { client ->
        client.push(listOf(routine("r1", "Push", 10)))
        val cursor = client.pull().body<PullPage>().nextCursor
        client.push(listOf(routine("r1", "Push", 20, deletedAt = 20)))
        val deleted = client.pull(cursor = cursor).body<PullPage>().records.single()
        assertEquals(20L, deleted.deletedAt)
    }

    @Test
    fun pagesFollowTheCursor() = serverTest { client ->
        client.push((1..5).map { routine("r$it", "R$it", it.toLong()) })
        val first = client.pull(limit = 2).body<PullPage>()
        assertEquals(listOf("r1", "r2"), first.records.map { it.id })
        assertTrue(first.hasMore)
        val rest = client.pull(cursor = first.nextCursor, limit = 10).body<PullPage>()
        assertEquals(listOf("r3", "r4", "r5"), rest.records.map { it.id })
        assertEquals(false, rest.hasMore)
    }

    @Test
    fun usersOnlySeeTheirOwnRows() = serverTest { client ->
        client.push(listOf(routine("r1", "Alice's", 10)), user = "alice")
        client.push(listOf(routine("r1", "Bob's", 5)), user = "bob")
        assertEquals("Alice's", client.pull(user = "alice").body<PullPage>().records.single().name())
        assertEquals("Bob's", client.pull(user = "bob").body<PullPage>().records.single().name())
    }

    @Test
    fun rejectsBadRequests() = serverTest { client ->
        suspend fun HttpResponse.code() = status to body<ApiError>().code

        assertEquals(HttpStatusCode.NotFound to "unknown_table", client.push(emptyList(), table = "users").code())
        assertEquals(
            HttpStatusCode.BadRequest to "table_mismatch",
            client.push(listOf(routine("r1", "x", 1).copy(table = SyncProtocol.WORKOUTS))).code(),
        )
        assertEquals(HttpStatusCode.BadRequest to "bad_id", client.push(listOf(routine("", "x", 1))).code())
        assertEquals(
            HttpStatusCode.BadRequest to "batch_too_large",
            client.push((0..SyncProtocol.MAX_BATCH).map { routine("r$it", "x", 1) }).code(),
        )
        assertEquals(HttpStatusCode.BadRequest to "bad_cursor", client.pull(cursor = "abc").code())
        assertEquals(HttpStatusCode.BadRequest to "bad_limit", client.pull(limit = 0).code())
        val malformed = client.post("/sync/routines/push") {
            header(DevUserResolver.HEADER, "alice")
            contentType(ContentType.Application.Json)
            setBody("{not json")
        }
        assertEquals(HttpStatusCode.BadRequest to "bad_request", malformed.code())
    }

    @Test
    fun requiresAUser() = testApplication {
        application { liftlyModule(TestDatabase.dataSource, DevUserResolver(enabled = false)) }
        val response = client.get("/sync/routines/pull") { header(DevUserResolver.HEADER, "alice") }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue("unauthorized" in response.bodyAsText())
    }

    @Test
    fun healthChecksTheDatabase() = serverTest { client ->
        assertEquals("ok", client.get("/health").bodyAsText())
    }
}
