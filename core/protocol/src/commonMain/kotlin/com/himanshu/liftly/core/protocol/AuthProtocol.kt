package com.himanshu.liftly.core.protocol

import kotlinx.serialization.Serializable

/** Body of `POST /auth/apple` and `POST /auth/google`: the provider's ID token, verified by the server. */
@Serializable
data class SignInRequest(
    val idToken: String,
    /** Raw nonce the app put (hashed) into the Apple request; null for Google. */
    val nonce: String? = null,
)

/** Body of `POST /auth/refresh` and `POST /auth/logout`. */
@Serializable
data class RefreshRequest(val refreshToken: String)

/** Returned by sign-in and refresh. The refresh token is single-use: a refresh returns a new one. */
@Serializable
data class AuthTokens(
    val accessToken: String,
    /** Epoch millis when [accessToken] expires. */
    val accessExpiresAt: Long,
    val refreshToken: String,
    val account: Account,
)

@Serializable
data class Account(val userId: String, val email: String?)

/** Body of every non-2xx response. */
@Serializable
data class ApiError(val code: String, val message: String)
