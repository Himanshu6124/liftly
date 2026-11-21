package com.himanshu.liftly.core.data.sync

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import co.touchlab.kermit.Logger
import com.himanshu.liftly.core.data.db.AppDatabase
import com.himanshu.liftly.core.data.db.entity.SyncCursorEntity
import com.himanshu.liftly.core.data.db.entity.SyncStatus
import com.himanshu.liftly.core.data.db.entity.SyncableEntity
import com.himanshu.liftly.core.protocol.SyncProtocol
import com.himanshu.liftly.core.protocol.SyncRecord
import kotlin.coroutines.cancellation.CancellationException

/**
 * Push-then-pull reconciliation between the local Room database (source of truth for the UI)
 * and a [RemoteSyncDataSource].
 *
 * Conflict policy: last-write-wins on `updatedAt`. A local row with unpushed changes that is
 * newer than the incoming server copy is kept and pushed on the next round.
 */
class SyncEngine(
    private val db: AppDatabase,
    private val remote: RemoteSyncDataSource,
    private val tables: SyncTables,
    private val sql: SyncSql,
    private val pageSize: Int = SyncProtocol.MAX_BATCH,
) {
    private val log = Logger.withTag("SyncEngine")

    suspend fun syncOnce(): Result<Unit> = try {
        tables.all.forEach { push(it) }
        tables.all.forEach { pull(it) }
        // Children before parents so cascades never remove a tombstone that still needs pushing.
        tables.all.asReversed().forEach { sql.purgeSyncedTombstones(it.name) }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        log.w(e) { "Sync failed" }
        Result.failure(e)
    }

    private suspend fun <E : SyncableEntity> push(table: TableSync<E>) {
        val pending = table.dao.pendingSync()
        if (pending.isEmpty()) return
        pending.chunked(pageSize).forEach { chunk ->
            val acks = remote.push(table.name, chunk.map(table::toRecord))
            sql.markSynced(table.name, acks)
        }
    }

    private suspend fun <E : SyncableEntity> pull(table: TableSync<E>) {
        val cursorDao = db.syncCursorDao()
        var cursor = cursorDao.get(table.name)
        do {
            val page = remote.pull(table.name, cursor, pageSize)
            db.useWriterConnection { tx ->
                tx.immediateTransaction {
                    applyRemote(table, page.records)
                    page.nextCursor?.let { cursorDao.put(SyncCursorEntity(table.name, it)) }
                }
            }
            cursor = page.nextCursor ?: cursor
        } while (page.hasMore)
    }

    private suspend fun <E : SyncableEntity> applyRemote(table: TableSync<E>, records: List<SyncRecord>) {
        if (records.isEmpty()) return
        val local = table.dao.getByIdsIncludingDeleted(records.map { it.id }).associateBy { it.id }
        val winners = records.filter { remote ->
            val mine = local[remote.id] ?: return@filter true
            val localWins = mine.sync.syncStatus == SyncStatus.PENDING && mine.sync.updatedAt > remote.updatedAt
            !localWins
        }
        table.dao.upsertAll(winners.map { table.merge(local[it.id], table.fromRecord(it)) })
    }
}
