package com.himanshu.liftly.core.data.sync

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import androidx.sqlite.SQLiteStatement
import com.himanshu.liftly.core.data.db.AppDatabase
import com.himanshu.liftly.core.data.db.entity.SyncStatus
import com.himanshu.liftly.core.protocol.PushAck

/**
 * Table-agnostic bookkeeping statements. Table names come only from [SyncTables]
 * (never user input), so interpolating them is safe.
 */
class SyncSql(private val db: AppDatabase) {
    companion object {
        /** Row in `sync_cursors` holding the account the local sync state belongs to (not a table). */
        const val ACCOUNT_KEY = "_account"
    }

    /** Marks a pushed row as synced, unless it was edited again while the push was in flight. */
    suspend fun markSynced(table: String, acks: List<PushAck>) {
        if (acks.isEmpty()) return
        db.useWriterConnection { tx ->
            tx.immediateTransaction {
                acks.forEach { ack ->
                    usePrepared(
                        "UPDATE $table SET syncStatus = ?, serverVersion = ? WHERE id = ? AND updatedAt = ?",
                    ) { st ->
                        st.bindText(1, SyncStatus.SYNCED.name)
                        st.bindLong(2, ack.serverVersion)
                        st.bindText(3, ack.id)
                        st.bindLong(4, ack.updatedAt)
                        st.step()
                    }
                }
            }
        }
    }

    /** Removes tombstones the server already knows about. */
    suspend fun purgeSyncedTombstones(table: String) {
        db.useWriterConnection { tx ->
            tx.immediateTransaction {
                usePrepared("DELETE FROM $table WHERE deletedAt IS NOT NULL AND syncStatus = ?") { st ->
                    st.bindText(1, SyncStatus.SYNCED.name)
                    st.step()
                }
            }
        }
    }

    /**
     * Ties the local sync state to [userId] (no-op when it already is). When another account, or
     * none, owned it: local data is kept and treated as never uploaded, so it goes to this account
     * as new. Tombstones are dropped (no server to tell), every row the user changed is marked
     * PENDING without a server version, and all pull cursors are cleared.
     * [tables] in dependency order (parents first).
     */
    suspend fun bindAccount(userId: String, tables: List<String>) {
        if (db.syncCursorDao().get(ACCOUNT_KEY) == userId) return
        db.useWriterConnection { tx ->
            tx.immediateTransaction {
                // Children first, so a cascade never meets a row that's still needed.
                tables.asReversed().forEach { table ->
                    usePrepared("DELETE FROM $table WHERE deletedAt IS NOT NULL") { it.step() }
                }
                tables.forEach { table ->
                    usePrepared("UPDATE $table SET serverVersion = NULL, syncStatus = CASE WHEN updatedAt > 0 THEN ? ELSE syncStatus END") {
                        it.bindText(1, SyncStatus.PENDING.name)
                        it.step()
                    }
                }
                usePrepared("DELETE FROM sync_cursors") { it.step() }
                usePrepared("INSERT INTO sync_cursors (tableName, cursor) VALUES (?, ?)") {
                    it.bindText(1, ACCOUNT_KEY)
                    it.bindText(2, userId)
                    it.step()
                }
            }
        }
    }

    /**
     * Deletes rows. Rows never seen by a server are removed outright; others become tombstones
     * so the deletion can be pushed. Call inside the repository's transaction for consistency.
     */
    suspend fun delete(table: String, ids: List<String>, now: Long) {
        if (ids.isEmpty()) return
        db.useWriterConnection { tx ->
            tx.immediateTransaction {
                val placeholders = ids.joinToString(",") { "?" }
                usePrepared("DELETE FROM $table WHERE serverVersion IS NULL AND id IN ($placeholders)") { st ->
                    st.bindIds(ids, startIndex = 1)
                    st.step()
                }
                usePrepared(
                    "UPDATE $table SET deletedAt = ?, updatedAt = ?, syncStatus = ? WHERE id IN ($placeholders)",
                ) { st ->
                    st.bindLong(1, now)
                    st.bindLong(2, now)
                    st.bindText(3, SyncStatus.PENDING.name)
                    st.bindIds(ids, startIndex = 4)
                    st.step()
                }
            }
        }
    }

    private fun SQLiteStatement.bindIds(ids: List<String>, startIndex: Int) {
        ids.forEachIndexed { i, id -> bindText(startIndex + i, id) }
    }
}
