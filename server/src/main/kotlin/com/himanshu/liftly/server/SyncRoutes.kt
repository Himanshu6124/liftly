package com.himanshu.liftly.server

import com.himanshu.liftly.core.protocol.SyncProtocol
import com.himanshu.liftly.core.protocol.SyncRecord
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/**
 * `POST /sync/{table}/push` (body: `List<SyncRecord>`, returns `List<PushAck>`) and
 * `GET /sync/{table}/pull?cursor=&limit=` (returns `PullPage`). See `core/protocol`.
 */
fun Route.syncRoutes(store: SyncStore, users: UserResolver) {
    route("/sync/{table}") {
        post("/push") {
            val userId = users.require(call)
            val table = call.table()
            val records = call.receive<List<SyncRecord>>()
            validate(records, table)
            call.respond(store.push(userId, table, records))
        }
        get("/pull") {
            val userId = users.require(call)
            val table = call.table()
            val cursor = call.request.queryParameters["cursor"]
                ?.let { it.toLongOrNull()?.takeIf { v -> v >= 0 } ?: badRequest("bad_cursor", "Cursor must be a version number") }
                ?: 0L
            val limit = call.request.queryParameters["limit"]
                ?.let { it.toIntOrNull()?.takeIf { v -> v in 1..SyncProtocol.MAX_BATCH } ?: badRequest("bad_limit", LIMIT_MSG) }
                ?: SyncProtocol.MAX_BATCH
            call.respond(store.pull(userId, table, cursor, limit))
        }
    }
}

private fun RoutingCall.table(): String {
    val table = parameters["table"].orEmpty()
    if (table !in SyncProtocol.TABLES) throw ApiException(HttpStatusCode.NotFound, "unknown_table", "Unknown table '$table'")
    return table
}

private fun validate(records: List<SyncRecord>, table: String) {
    if (records.size > SyncProtocol.MAX_BATCH) badRequest("batch_too_large", "At most ${SyncProtocol.MAX_BATCH} records per push")
    records.forEach { r ->
        if (r.table != table) badRequest("table_mismatch", "Record ${r.id} is for '${r.table}', not '$table'")
        if (r.id.isBlank() || r.id.length > MAX_ID_LENGTH) badRequest("bad_id", "Record ids must be 1-$MAX_ID_LENGTH characters")
    }
}

private fun badRequest(code: String, message: String): Nothing = throw ApiException(HttpStatusCode.BadRequest, code, message)

private const val MAX_ID_LENGTH = 128
private const val LIMIT_MSG = "Limit must be between 1 and ${SyncProtocol.MAX_BATCH}"
