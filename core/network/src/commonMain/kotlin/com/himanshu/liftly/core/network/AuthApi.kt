package com.himanshu.liftly.core.network

import com.himanshu.liftly.core.protocol.Account
import com.himanshu.liftly.core.protocol.AuthTokens
import com.himanshu.liftly.core.protocol.RefreshRequest
import com.himanshu.liftly.core.protocol.SignInRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Our server's client: JSON, base URL, and non-2xx responses thrown as `ResponseException`. */
fun liftlyHttpClient(config: ApiConfig, base: HttpClient = HttpClient()): HttpClient = base.config {
    expectSuccess = true
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    defaultRequest {
        url(config.baseUrl.trimEnd('/') + "/")
        contentType(ContentType.Application.Json)
    }
}

/** Stateless calls to `/auth` and `/account` (see `core/protocol`). */
class AuthApi(private val client: HttpClient) {
    suspend fun signIn(provider: String, request: SignInRequest): AuthTokens = client.post("auth/$provider") { setBody(request) }.body()

    suspend fun refresh(refreshToken: String): AuthTokens = client.post("auth/refresh") { setBody(RefreshRequest(refreshToken)) }.body()

    suspend fun logout(refreshToken: String) {
        client.post("auth/logout") { setBody(RefreshRequest(refreshToken)) }
    }

    suspend fun account(accessToken: String): Account = client.get("account") { bearerAuth(accessToken) }.body()

    suspend fun deleteAccount(accessToken: String) {
        client.delete("account") { bearerAuth(accessToken) }
    }
}
