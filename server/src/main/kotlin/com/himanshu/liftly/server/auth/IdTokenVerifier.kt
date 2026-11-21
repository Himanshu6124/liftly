package com.himanshu.liftly.server.auth

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import java.net.URI
import java.security.MessageDigest
import java.security.interfaces.RSAPublicKey
import java.time.Clock
import java.util.concurrent.TimeUnit

/** A provider's RSA signing keys by key id (`kid`). */
fun interface KeySource {
    /** The key, or null when the provider doesn't publish [kid]. */
    fun key(kid: String): RSAPublicKey?
}

/** Provider keys from its JWKS URL, cached for a day and fetched at most 10 times a minute. */
class JwksKeySource(url: String) : KeySource {
    private val provider = JwkProviderBuilder(URI(url).toURL())
        .cached(CACHE_SIZE, 1, TimeUnit.DAYS)
        .rateLimited(FETCHES_PER_MINUTE, 1, TimeUnit.MINUTES)
        .build()

    override fun key(kid: String): RSAPublicKey? = runCatching { provider.get(kid).publicKey as RSAPublicKey }.getOrNull()

    private companion object {
        const val CACHE_SIZE = 10L
        const val FETCHES_PER_MINUTE = 10L
    }
}

data class VerifiedIdentity(val subject: String, val email: String?)

/**
 * Checks an OpenID Connect ID token from Apple or Google: RS256 signature with the provider's
 * published key, issuer, audience (our app/client ids), expiry, and for Apple the nonce.
 */
class IdTokenVerifier(
    private val issuers: List<String>,
    private val audiences: List<String>,
    private val keys: KeySource,
    private val requireNonce: Boolean,
    private val clock: Clock = Clock.systemUTC(),
) {
    val enabled: Boolean get() = audiences.isNotEmpty()

    /** @throws InvalidIdToken */
    fun verify(idToken: String, rawNonce: String?): VerifiedIdentity {
        val jwt = signedByProvider(idToken)
        problemWith(jwt, rawNonce)?.let { throw InvalidIdToken(it) }
        // Google can return unverified addresses; Apple's are verified (or private relay) by default.
        val emailVerified = jwt.getClaim("email_verified").let { it.asBoolean() ?: it.asString()?.toBoolean() } ?: true
        val email = jwt.getClaim("email").asString()?.takeIf { emailVerified }
        return VerifiedIdentity(jwt.subject.orEmpty(), email)
    }

    /** Signature (provider key by `kid`) and time claims (exp/nbf/iat, with leeway). */
    private fun signedByProvider(idToken: String): DecodedJWT = try {
        val key = JWT.decode(idToken).keyId?.let(keys::key)?.takeIf { enabled } ?: throw InvalidIdToken("unknown key or provider off")
        JWT.require(Algorithm.RSA256(key, null))
            .acceptLeeway(LEEWAY_SECONDS)
            .let { (it as JWTVerifier.BaseVerification).build(clock) }
            .verify(idToken)
    } catch (e: JWTVerificationException) {
        throw InvalidIdToken(e.message ?: "invalid token", e)
    }

    private fun problemWith(jwt: DecodedJWT, rawNonce: String?): String? = when {
        jwt.issuer !in issuers -> "wrong issuer"

        jwt.audience.orEmpty().none { it in audiences } -> "wrong audience"

        jwt.expiresAt == null -> "no expiry"

        jwt.subject.isNullOrBlank() -> "no subject"

        // The app sends Apple sha256(rawNonce) and us rawNonce, so a captured token can't be replayed.
        requireNonce && (rawNonce == null || jwt.getClaim("nonce").asString() != sha256Hex(rawNonce)) -> "nonce mismatch"

        else -> null
    }

    companion object {
        private const val LEEWAY_SECONDS = 60L

        fun apple(audiences: List<String>, keys: KeySource = JwksKeySource("https://appleid.apple.com/auth/keys")) =
            IdTokenVerifier(listOf("https://appleid.apple.com"), audiences, keys, requireNonce = true)

        fun google(audiences: List<String>, keys: KeySource = JwksKeySource("https://www.googleapis.com/oauth2/v3/certs")) =
            IdTokenVerifier(listOf("https://accounts.google.com", "accounts.google.com"), audiences, keys, requireNonce = false)

        fun sha256Hex(value: String): String =
            MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

class InvalidIdToken(message: String, cause: Throwable? = null) : Exception(message, cause)
