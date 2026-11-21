package com.himanshu.liftly.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

object Database {
    /** Connection pool, with the schema migrated to the latest version (`resources/db/migration`). */
    fun connect(url: String, user: String, password: String): HikariDataSource {
        val ds = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = url
                username = user
                this.password = password
                maximumPoolSize = POOL_SIZE
            },
        )
        migrate(ds)
        return ds
    }

    fun migrate(ds: DataSource) {
        Flyway.configure().dataSource(ds).load().migrate()
    }

    private const val POOL_SIZE = 10
}
