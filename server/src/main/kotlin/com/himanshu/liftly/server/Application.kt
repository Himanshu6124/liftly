package com.himanshu.liftly.server

import com.himanshu.liftly.core.protocol.ApiError
import com.himanshu.liftly.server.auth.AccessTokens
import com.himanshu.liftly.server.auth.AccountStore
import com.himanshu.liftly.server.auth.AuthRateLimit
import com.himanshu.liftly.server.auth.AuthService
import com.himanshu.liftly.server.auth.IdTokenVerifier
import com.himanshu.liftly.server.auth.Provider
import com.himanshu.liftly.server.auth.authRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.time.Duration.Companion.minutes

fun main() {
    val config = ServerConfig.fromEnv()
    start(config, Database.connect(config.databaseUrl, config.databaseUser, config.databasePassword, config.databasePoolSize), wait = true)
}

fun start(config: ServerConfig, ds: DataSource, wait: Boolean) {
    if (config.devAuth) LoggerFactory.getLogger("Liftly").warn("LIFTLY_DEV_AUTH is on: any caller can act as any user")
    val auth = AuthService(
        AccountStore(ds),
        AccessTokens(config.jwtSecret),
        mapOf(
            Provider.APPLE to IdTokenVerifier.apple(config.appleAudiences),
            Provider.GOOGLE to IdTokenVerifier.google(config.googleAudiences),
        ),
    )
    embeddedServer(Netty, port = config.port, host = config.host) {
        liftlyModule(ds, auth, config.devAuth)
    }.start(wait = wait)
}

fun Application.liftlyModule(ds: DataSource, auth: AuthService, devAuth: Boolean, authRequestsPerMinute: Int = AUTH_REQUESTS_PER_MINUTE) {
    val users = FirstOf(
        listOfNotNull(BearerUserResolver(auth.accessTokens), DevUserResolver(AccountStore(ds)).takeIf { devAuth }),
    )
    install(ContentNegotiation) { json() }
    // Client IP from the reverse proxy (Caddy). The server port must never be reachable directly.
    install(XForwardedHeaders)
    install(RateLimit) {
        register(AuthRateLimit) {
            rateLimiter(limit = authRequestsPerMinute, refillPeriod = 1.minutes)
            requestKey { it.request.origin.remoteHost }
        }
    }
    // Logs method, path and status only: never bodies (workout data, tokens) or auth headers.
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
        status(HttpStatusCode.TooManyRequests) { call, status ->
            call.respond(status, ApiError("rate_limited", "Too many attempts, try again in a minute"))
        }
    }
    routing {
        get("/health") {
            val dbUp = ds.connection.use { it.isValid(HEALTH_TIMEOUT_SECONDS) }
            if (dbUp) call.respondText("ok") else call.respondText("database unavailable", status = HttpStatusCode.ServiceUnavailable)
        }
        authRoutes(auth, users)
        syncRoutes(SyncStore(ds), users)
    }
}

private const val HEALTH_TIMEOUT_SECONDS = 2
private const val AUTH_REQUESTS_PER_MINUTE = 20
