package com.himanshu.liftly.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import javax.sql.DataSource

/** Small JDBC helpers shared by the stores. */
object Sql {
    /** Runs [block] in one transaction on the IO dispatcher; rolls back on any exception. */
    suspend fun <T> transaction(ds: DataSource, block: (Connection) -> T): T = withContext(Dispatchers.IO) {
        ds.connection.use { conn ->
            conn.autoCommit = false
            try {
                block(conn).also { conn.commit() }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    /** Serializes writes for one user until the transaction ends (pushes, account deletion). */
    fun lockUser(conn: Connection, userId: String) {
        conn.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))").use {
            it.setString(1, userId)
            it.executeQuery().close()
        }
    }

    /** PostgreSQL `foreign_key_violation`. */
    const val FOREIGN_KEY_VIOLATION = "23503"
}
