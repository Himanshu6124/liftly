package com.himanshu.liftly.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.himanshu.liftly.server.auth.AccessTokens
import com.himanshu.liftly.server.auth.AccountStore
import com.himanshu.liftly.server.auth.AuthService
import com.himanshu.liftly.server.auth.IdTokenVerifier
import com.himanshu.liftly.server.auth.KeySource
import com.himanshu.liftly.server.auth.Provider
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Clock
import java.util.Date

/** Plays Apple and Google in tests: signs ID tokens with a local RSA key the verifiers trust. */
object TestAuth {
    const val KID = "test-key"
    const val GOOGLE_CLIENT = "test-client.apps.googleusercontent.com"
    const val SECRET = "test-secret-test-secret-test-secret"

    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val signing = Algorithm.RSA256(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey)

    private val keys = KeySource { kid -> (keyPair.public as RSAPublicKey).takeIf { kid == KID } }

    /** A key the providers don't publish (forged token). */
    private val otherKey = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    fun service(clock: Clock = Clock.systemUTC()) = AuthService(
        AccountStore(TestDatabase.dataSource),
        AccessTokens(SECRET, clock),
        mapOf(
            Provider.APPLE to IdTokenVerifier.apple(listOf(ServerConfig.IOS_BUNDLE_ID), keys),
            Provider.GOOGLE to IdTokenVerifier.google(listOf(GOOGLE_CLIENT), keys),
        ),
        clock,
    )

    @Suppress("LongParameterList")
    fun appleToken(
        sub: String = "apple-sub-1",
        rawNonce: String = "nonce-1",
        audience: String = ServerConfig.IOS_BUNDLE_ID,
        email: String? = "a@privaterelay.appleid.com",
        expiresInSeconds: Long = 600,
        forged: Boolean = false,
    ): String = JWT.create()
        .withKeyId(KID)
        .withIssuer("https://appleid.apple.com")
        .withAudience(audience)
        .withSubject(sub)
        .withClaim("nonce", IdTokenVerifier.sha256Hex(rawNonce))
        .withClaim("email", email)
        .withExpiresAt(Date(System.currentTimeMillis() + expiresInSeconds * 1000))
        .sign(if (forged) Algorithm.RSA256(otherKey.public as RSAPublicKey, otherKey.private as RSAPrivateKey) else signing)

    fun googleToken(sub: String = "google-sub-1", email: String = "g@example.com", emailVerified: Boolean = true): String = JWT.create()
        .withKeyId(KID)
        .withIssuer("https://accounts.google.com")
        .withAudience(GOOGLE_CLIENT)
        .withSubject(sub)
        .withClaim("email", email)
        .withClaim("email_verified", emailVerified)
        .withExpiresAt(Date(System.currentTimeMillis() + 600_000))
        .sign(signing)
}
