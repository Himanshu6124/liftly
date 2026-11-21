package com.himanshu.liftly.server

import com.himanshu.liftly.core.protocol.PullPage
import com.himanshu.liftly.core.protocol.PushAck
import com.himanshu.liftly.core.protocol.SyncRecord
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import javax.sql.DataSource

/**
 * PostgreSQL side of sync, same semantics as the app's `FakeServer` test double:
 * last-write-wins on `updatedAt` (ties go to the incoming write), every accepted write gets a new
 * version, and a rejected (older) write is still acked with the current version so the client
 * stops re-pushing it and picks up the winner on its next pull.
 */
class SyncStore(private val ds: DataSource) {

    suspend fun push(userId: String, table: String, records: List<SyncRecord>): List<PushAck> = try {
        transaction { conn ->
            // Serialize one user's pushes so their versions commit in order: otherwise a pull could
            // advance its cursor past a lower version that commits later, and miss that row forever.
            Sql.lockUser(conn, userId)
            records.map { r -> PushAck(r.id, r.updatedAt, upsert(conn, userId, table, r) ?: currentVersion(conn, userId, table, r.id)) }
        }
    } catch (e: SQLException) {
        // The account was deleted while this access token was still valid.
        if (e.sqlState ==
            Sql.FOREIGN_KEY_VIOLATION
        ) {
            throw ApiException(HttpStatusCode.Unauthorized, "unauthorized", "Account no longer exists")
        }
        throw e
    }

    suspend fun pull(userId: String, table: String, afterVersion: Long, limit: Int): PullPage = transaction { conn ->
        val records = conn.prepareStatement(PULL).use { st ->
            st.setString(1, userId)
            st.setString(2, table)
            st.setLong(3, afterVersion)
            st.setInt(4, limit)
            st.executeQuery().use { rs -> generateSequence { if (rs.next()) rs.toRecord(table) else null }.toList() }
        }
        val next = records.lastOrNull()?.serverVersion ?: afterVersion
        PullPage(records, nextCursor = next.toString(), hasMore = records.size == limit)
    }

    /** New version, or null when the stored row is newer and the write was rejected. */
    private fun upsert(conn: Connection, userId: String, table: String, r: SyncRecord): Long? =
        conn.prepareStatement(UPSERT).use { st ->
            st.setString(1, userId)
            st.setString(2, table)
            st.setString(3, r.id)
            st.setString(4, json.encodeToString(JsonObject.serializer(), r.payload))
            st.setLong(5, r.updatedAt)
            val deletedAt = r.deletedAt
            if (deletedAt == null) st.setNull(6, Types.BIGINT) else st.setLong(6, deletedAt)
            st.executeQuery().use { rs -> if (rs.next()) rs.getLong(1) else null }
        }

    private fun currentVersion(conn: Connection, userId: String, table: String, id: String): Long =
        conn.prepareStatement("SELECT version FROM records WHERE user_id = ? AND tbl = ? AND id = ?").use { st ->
            st.setString(1, userId)
            st.setString(2, table)
            st.setString(3, id)
            st.executeQuery().use { rs ->
                rs.next()
                rs.getLong(1)
            }
        }

    private fun ResultSet.toRecord(table: String) = SyncRecord(
        table = table,
        id = getString("id"),
        updatedAt = getLong("updated_at"),
        deletedAt = getLong("deleted_at").takeUnless { wasNull() },
        serverVersion = getLong("version"),
        payload = json.decodeFromString(JsonObject.serializer(), getString("payload")),
    )

    private suspend fun <T> transaction(block: (Connection) -> T): T = Sql.transaction(ds, block)

    private companion object {
        val json = Json

        const val UPSERT = """
            INSERT INTO records (user_id, tbl, id, payload, updated_at, deleted_at, version)
            VALUES (?, ?, ?, ?::jsonb, ?, ?, nextval('record_version_seq'))
            ON CONFLICT (user_id, tbl, id) DO UPDATE
                SET payload = EXCLUDED.payload,
                    updated_at = EXCLUDED.updated_at,
                    deleted_at = EXCLUDED.deleted_at,
                    version = EXCLUDED.version
                WHERE records.updated_at <= EXCLUDED.updated_at
            RETURNING version
        """

        const val PULL = """
            SELECT id, payload::text AS payload, updated_at, deleted_at, version
            FROM records
            WHERE user_id = ? AND tbl = ? AND version > ?
            ORDER BY version
            LIMIT ?
        """
    }
}
