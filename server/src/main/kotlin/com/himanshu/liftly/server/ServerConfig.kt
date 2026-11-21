package com.himanshu.liftly.server

/** Everything the server reads from the environment. Secrets never live in the repo. */
data class ServerConfig(
    val port: Int,
    /** Interface to listen on. `127.0.0.1` behind a reverse proxy on the same machine. */
    val host: String = "0.0.0.0",
    val databaseUrl: String,
    val databaseUser: String,
    val databasePassword: String,
    /** Database connections; keep it small on small machines (each costs Postgres memory). */
    val databasePoolSize: Int = DEFAULT_POOL_SIZE,
    /** Signs our access tokens. At least 32 random characters; rotating it signs everyone out of their current access token. */
    val jwtSecret: String,
    /** Accepted `aud` of Apple ID tokens: the iOS bundle id. Empty disables Apple sign-in. */
    val appleAudiences: List<String>,
    /** Accepted `aud` of Google ID tokens: our OAuth client ids. Empty disables Google sign-in. */
    val googleAudiences: List<String>,
    /**
     * Accepts `X-Liftly-Dev-User` as the caller's id with no token. Local development and tests
     * only. Never set it in production.
     */
    val devAuth: Boolean,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()) = ServerConfig(
            port = env["PORT"]?.toInt() ?: DEFAULT_PORT,
            host = env["HOST"] ?: "0.0.0.0",
            databaseUrl = env.required("DATABASE_URL"),
            databaseUser = env.required("DATABASE_USER"),
            databasePassword = env.required("DATABASE_PASSWORD"),
            databasePoolSize = env["DATABASE_POOL_SIZE"]?.toInt() ?: DEFAULT_POOL_SIZE,
            jwtSecret = env.required("JWT_SECRET"),
            appleAudiences = env.list("APPLE_AUDIENCES") ?: listOf(IOS_BUNDLE_ID),
            googleAudiences = env.list("GOOGLE_AUDIENCES").orEmpty(),
            devAuth = env["LIFTLY_DEV_AUTH"] == "true",
        )

        private fun Map<String, String>.required(key: String) = get(key) ?: error("Missing environment variable $key")

        private fun Map<String, String>.list(key: String) = get(key)?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }

        const val DEFAULT_PORT = 8080
        const val DEFAULT_POOL_SIZE = 10
        const val IOS_BUNDLE_ID = "com.himanshu.liftly"
    }
}
