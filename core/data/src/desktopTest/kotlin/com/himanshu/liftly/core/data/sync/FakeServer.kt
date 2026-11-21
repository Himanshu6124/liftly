package com.himanshu.liftly.core.data.sync

import com.himanshu.liftly.core.protocol.PullPage
import com.himanshu.liftly.core.protocol.PushAck
import com.himanshu.liftly.core.protocol.SyncRecord

/** In-memory backend with last-write-wins and a global version counter, mimicking a real server. */
class FakeServer : RemoteSyncDataSource {
    private var version = 0L
    val rows = mutableMapOf<Pair<String, String>, SyncRecord>()
    var pushCalls = 0

    override suspend fun push(table: String, records: List<SyncRecord>): List<PushAck> {
        pushCalls++
        return records.map { r ->
            val key = table to r.id
            val existing = rows[key]
            if (existing == null || r.updatedAt >= existing.updatedAt) {
                rows[key] = r.copy(serverVersion = ++version)
            }
            PushAck(r.id, r.updatedAt, rows.getValue(key).serverVersion ?: 0)
        }
    }

    override suspend fun pull(table: String, cursor: String?, limit: Int): PullPage {
        val since = cursor?.toLong() ?: 0L
        val page = rows.values
            .filter { it.table == table && (it.serverVersion ?: 0) > since }
            .sortedBy { it.serverVersion }
            .take(limit)
        val next = page.lastOrNull()?.serverVersion?.toString() ?: cursor
        return PullPage(page, next, hasMore = page.size == limit)
    }

    /** Simulates another device writing directly to the server. */
    fun remoteWrite(record: SyncRecord) {
        rows[record.table to record.id] = record.copy(serverVersion = ++version)
    }
}
