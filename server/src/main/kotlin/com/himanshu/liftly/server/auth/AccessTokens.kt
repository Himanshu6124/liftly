package com.himanshu.liftly.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.util.Base64
import java.util.Date

/** Our own short-lived access tokens (HS256 JWT, `sub` = user id) and opaque refresh tokens. */
class AccessTokens(secret: String, private val clock: Clock = Clock.systemUTC()) {
    init {
        require(secret.length >= MIN_SECRET_LENGTH) { "JWT_SECRET must be at least $MIN_SECRET_LENGTH characters" }
    }

    private val algorithm = Algorithm.HMAC256(secret)
    private val verifier = JWT.require(algorithm).withIssuer(ISSUER).withAudience(AUDIENCE)
        .let { (it as JWTVerifier.BaseVerification).build(clock) }

    /** Returns the token and its expiry (epoch millis). */
    fun issue(userId: String): Pair<String, Long> {
        val now = clock.millis()
        val expiresAt = now + ACCESS_TTL_MILLIS
        val token = JWT.create()
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withSubject(userId)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(expiresAt))
            .sign(algorithm)
        return token to expiresAt
    }

    /** The user id, or null if the token is invalid or expired. */
    fun verify(token: String): String? = try {
        verifier.verify(token).subject
    } catch (_: JWTVerificationException) {
        null
    }

    companion object {
        const val ACCESS_TTL_MILLIS = 15 * 60 * 1000L
        const val REFRESH_TTL_MILLIS = 30 * 24 * 60 * 60 * 1000L
        private const val MIN_SECRET_LENGTH = 32
        private const val REFRESH_BYTES = 32
        private const val ISSUER = "liftly"
        private const val AUDIENCE = "liftly-app"
        private val random = SecureRandom()

        fun newRefreshToken(): String =
            Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(REFRESH_BYTES).also(random::nextBytes))

        fun hash(refreshToken: String): String =
            Base64.getUrlEncoder().withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256").digest(refreshToken.toByteArray()))
    }
}
