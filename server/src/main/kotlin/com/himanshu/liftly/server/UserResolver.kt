package com.himanshu.liftly.server

import com.himanshu.liftly.server.auth.AccessTokens
import com.himanshu.liftly.server.auth.AccountStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall

/** Who is calling, from the request. */
fun interface UserResolver {
    suspend fun resolve(call: ApplicationCall): String?
}

suspend fun UserResolver.require(call: ApplicationCall): String =
    resolve(call) ?: throw ApiException(HttpStatusCode.Unauthorized, "unauthorized", "Sign in required")

/** `Authorization: Bearer <access token>` issued by `/auth/...`. */
class BearerUserResolver(private val tokens: AccessTokens) : UserResolver {
    override suspend fun resolve(call: ApplicationCall): String? {
        val header = call.request.headers["Authorization"] ?: return null
        if (!header.startsWith(PREFIX, ignoreCase = true)) return null
        return tokens.verify(header.substring(PREFIX.length).trim())
    }

    private companion object {
        const val PREFIX = "Bearer "
    }
}

/**
 * Dev/test only: trusts the `X-Liftly-Dev-User` header (creating that user if needed). Only
 * installed when `LIFTLY_DEV_AUTH=true`; never in production.
 */
class DevUserResolver(private val accounts: AccountStore) : UserResolver {
    override suspend fun resolve(call: ApplicationCall): String? {
        val id = call.request.headers[HEADER]?.takeIf { it.isNotBlank() } ?: return null
        accounts.ensureDevUser(id, System.currentTimeMillis())
        return id
    }

    companion object {
        const val HEADER = "X-Liftly-Dev-User"
    }
}

/** First resolver that recognizes the caller wins. */
class FirstOf(private val resolvers: List<UserResolver>) : UserResolver {
    override suspend fun resolve(call: ApplicationCall): String? = resolvers.firstNotNullOfOrNull { it.resolve(call) }
}
