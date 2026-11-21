package com.himanshu.liftly.server

import io.ktor.http.HttpStatusCode

/** Thrown by routes; turned into an `ApiError` JSON response with [status]. */
class ApiException(val status: HttpStatusCode, val code: String, override val message: String) : RuntimeException(message)
