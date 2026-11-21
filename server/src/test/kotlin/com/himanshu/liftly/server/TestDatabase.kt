package com.himanshu.liftly.server

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import javax.sql.DataSource

/** One real PostgreSQL (embedded binaries, no Docker) shared by all tests in the JVM. */
object TestDatabase {
    private val postgres: EmbeddedPostgres by lazy { EmbeddedPostgres.start() }

    val dataSource: DataSource by lazy { postgres.postgresDatabase.also(Database::migrate) }

    /** Empties every table; call before each test. */
    fun reset() {
        dataSource.connection.use { it.createStatement().execute("TRUNCATE records") }
    }
}
