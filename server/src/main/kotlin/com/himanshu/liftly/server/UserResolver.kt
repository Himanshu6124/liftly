package com.himanshu.liftly.server

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall

/** Who is calling. Milestone 21 replaces [DevUserResolver] with JWT verification. */
fun interface UserResolver {
    fun resolve(call: ApplicationCall): String?
}

fun UserResolver.require(call: ApplicationCall): String =
    resolve(call) ?: throw ApiException(HttpStatusCode.Unauthorized, "unauthorized", "Sign in required")

/** Dev/test only: trusts the `X-Liftly-Dev-User` header. Disabled unless `LIFTLY_DEV_AUTH=true`. */
class DevUserResolver(private val enabled: Boolean) : UserResolver {
    override fun resolve(call: ApplicationCall): String? =
        if (enabled) call.request.headers[HEADER]?.takeIf { it.isNotBlank() } else null

    companion object {
        const val HEADER = "X-Liftly-Dev-User"
    }
}
