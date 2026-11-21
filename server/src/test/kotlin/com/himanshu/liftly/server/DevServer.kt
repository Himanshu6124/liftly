package com.himanshu.liftly.server

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import java.nio.file.Path

/**
 * `./gradlew :server:runDev`: the real server on :8080 with dev auth, backed by an embedded
 * PostgreSQL whose data survives restarts in `server/build/dev-db`. Local development only.
 */
fun main() {
    val postgres = EmbeddedPostgres.builder()
        .setDataDirectory(Path.of("build/dev-db"))
        .setCleanDataDirectory(false)
        .setPort(DEV_DB_PORT)
        .start()
    val ds = postgres.postgresDatabase
    Database.migrate(ds)
    start(ServerConfig(ServerConfig.DEFAULT_PORT, "", "", "", devAuth = true), ds, wait = true)
}

private const val DEV_DB_PORT = 54329
