package com.himanshu.liftly.server

import com.himanshu.liftly.core.protocol.ApiError
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory
import javax.sql.DataSource

fun main() {
    val config = ServerConfig.fromEnv()
    start(config, Database.connect(config.databaseUrl, config.databaseUser, config.databasePassword), wait = true)
}

fun start(config: ServerConfig, ds: DataSource, wait: Boolean) {
    if (config.devAuth) LoggerFactory.getLogger("Liftly").warn("LIFTLY_DEV_AUTH is on: any caller can act as any user")
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        liftlyModule(ds, DevUserResolver(config.devAuth))
    }.start(wait = wait)
}

fun Application.liftlyModule(ds: DataSource, users: UserResolver) {
    val store = SyncStore(ds)
    install(ContentNegotiation) { json() }
    // Logs method, path and status only: never bodies (workout data) or auth headers.
    install(CallLogging)
    install(StatusPages) {
        exception<ApiException> { call, e -> call.respond(e.status, ApiError(e.code, e.message)) }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ApiError("bad_request", "Malformed request body"))
        }
        exception<Throwable> { call, e ->
            call.application.environment.log.error("Unhandled error", e)
            call.respond(HttpStatusCode.InternalServerError, ApiError("internal", "Something went wrong"))
        }
    }
    routing {
        get("/health") {
            val dbUp = ds.connection.use { it.isValid(HEALTH_TIMEOUT_SECONDS) }
            if (dbUp) call.respondText("ok") else call.respondText("database unavailable", status = HttpStatusCode.ServiceUnavailable)
        }
        syncRoutes(store, users)
    }
}

private const val HEALTH_TIMEOUT_SECONDS = 2
