package com.himanshu.liftly.server.auth

import com.himanshu.liftly.core.protocol.Account
import com.himanshu.liftly.core.protocol.AuthTokens
import java.time.Clock

/** Sign-in with a provider ID token, token refresh, logout and account deletion. */
class AuthService(
    private val accounts: AccountStore,
    val accessTokens: AccessTokens,
    private val verifiers: Map<Provider, IdTokenVerifier>,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun isEnabled(provider: Provider) = verifiers[provider]?.enabled == true

    /** @throws InvalidIdToken */
    suspend fun signIn(provider: Provider, idToken: String, rawNonce: String?): AuthTokens {
        val verifier = verifiers[provider] ?: throw InvalidIdToken("provider not configured")
        val identity = verifier.verify(idToken, rawNonce)
        val account = accounts.findOrCreate(provider, identity.subject, identity.email, clock.millis())
        val refresh = AccessTokens.newRefreshToken()
        accounts.saveRefreshToken(account.userId, AccessTokens.hash(refresh), clock.millis())
        return tokens(account, refresh)
    }

    /** New tokens, or null when the refresh token is no longer valid (sign in again). */
    suspend fun refresh(refreshToken: String): AuthTokens? {
        val next = AccessTokens.newRefreshToken()
        val result = accounts.rotate(AccessTokens.hash(refreshToken), AccessTokens.hash(next), clock.millis())
        if (result !is RefreshResult.Ok) return null
        val account = accounts.account(result.userId) ?: return null
        return tokens(account, next)
    }

    suspend fun logout(refreshToken: String) = accounts.logout(AccessTokens.hash(refreshToken), clock.millis())

    suspend fun account(userId: String): Account? = accounts.account(userId)

    suspend fun deleteAccount(userId: String) = accounts.delete(userId)

    private fun tokens(account: Account, refresh: String): AuthTokens {
        val (access, expiresAt) = accessTokens.issue(account.userId)
        return AuthTokens(access, expiresAt, refresh, account)
    }
}
