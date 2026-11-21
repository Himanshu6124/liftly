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
    val config = ServerConfig(
        port = ServerConfig.DEFAULT_PORT,
        databaseUrl = "",
        databaseUser = "",
        databasePassword = "",
        // Fixed so the app stays signed in across dev restarts. Production reads JWT_SECRET.
        jwtSecret = "dev-only-secret-never-use-in-production",
        appleAudiences = listOf(ServerConfig.IOS_BUNDLE_ID),
        googleAudiences = System.getenv("GOOGLE_AUDIENCES")?.split(',').orEmpty(),
        devAuth = true,
    )
    start(config, ds, wait = true)
}

private const val DEV_DB_PORT = 54329
