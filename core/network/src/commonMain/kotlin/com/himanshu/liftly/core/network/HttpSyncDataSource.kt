package com.himanshu.liftly.core.network

import com.himanshu.liftly.core.data.sync.RemoteSyncDataSource
import com.himanshu.liftly.core.protocol.PullPage
import com.himanshu.liftly.core.protocol.PushAck
import com.himanshu.liftly.core.protocol.SyncRecord
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/** The sync engine's transport: `/sync/{table}/push|pull` with the session's access token. */
class HttpSyncDataSource(private val client: HttpClient, private val session: AuthSession) : RemoteSyncDataSource {
    override suspend fun push(table: String, records: List<SyncRecord>): List<PushAck> = session.authorized { token ->
        client.post("sync/$table/push") {
            bearerAuth(token)
            setBody(records)
        }.body()
    }

    override suspend fun pull(table: String, cursor: String?, limit: Int): PullPage = session.authorized { token ->
        client.get("sync/$table/pull") {
            bearerAuth(token)
            cursor?.let { parameter("cursor", it) }
            parameter("limit", limit)
        }.body()
    }
}
