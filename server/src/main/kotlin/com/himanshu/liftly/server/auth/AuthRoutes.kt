package com.himanshu.liftly.server.auth

import com.himanshu.liftly.core.protocol.RefreshRequest
import com.himanshu.liftly.core.protocol.SignInRequest
import com.himanshu.liftly.server.ApiException
import com.himanshu.liftly.server.UserResolver
import com.himanshu.liftly.server.require
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.slf4j.LoggerFactory

val AuthRateLimit = RateLimitName("auth")

/**
 * `POST /auth/apple`, `POST /auth/google` (body `SignInRequest`) and `POST /auth/refresh` return
 * `AuthTokens`; `POST /auth/logout` revokes a refresh token; `GET /account` and `DELETE /account`
 * need an access token.
 */
fun Route.authRoutes(auth: AuthService, users: UserResolver) {
    rateLimit(AuthRateLimit) {
        route("/auth") {
            post("/apple") { call.signIn(auth, Provider.APPLE) }
            post("/google") { call.signIn(auth, Provider.GOOGLE) }
            post("/refresh") {
                val tokens = auth.refresh(call.receive<RefreshRequest>().refreshToken) ?: throw sessionExpired()
                call.respond(tokens)
            }
            post("/logout") {
                auth.logout(call.receive<RefreshRequest>().refreshToken)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
    route("/account") {
        get {
            val account = auth.account(users.require(call)) ?: throw sessionExpired()
            call.respond(account)
        }
        delete {
            auth.deleteAccount(users.require(call))
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private suspend fun RoutingCall.signIn(auth: AuthService, provider: Provider) {
    if (!auth.isEnabled(provider)) {
        throw ApiException(HttpStatusCode.NotFound, "provider_disabled", "${provider.name.lowercase()} sign-in is not configured")
    }
    val request = receive<SignInRequest>()
    val tokens = try {
        auth.signIn(provider, request.idToken, request.nonce)
    } catch (e: InvalidIdToken) {
        // The reason helps debugging but stays in the log, not the response.
        log.info("Rejected {} sign-in: {}", provider, e.message)
        throw ApiException(HttpStatusCode.Unauthorized, "invalid_id_token", "Sign-in could not be verified")
    }
    respond(tokens)
}

private fun sessionExpired() = ApiException(HttpStatusCode.Unauthorized, "session_expired", "Sign in again")

private val log = LoggerFactory.getLogger("AuthRoutes")
