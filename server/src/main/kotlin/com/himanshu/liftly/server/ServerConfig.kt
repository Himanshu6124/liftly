package com.himanshu.liftly.server

/** Everything the server reads from the environment. Secrets never live in the repo. */
data class ServerConfig(
    val port: Int,
    val databaseUrl: String,
    val databaseUser: String,
    val databasePassword: String,
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
            databaseUrl = env.required("DATABASE_URL"),
            databaseUser = env.required("DATABASE_USER"),
            databasePassword = env.required("DATABASE_PASSWORD"),
            jwtSecret = env.required("JWT_SECRET"),
            appleAudiences = env.list("APPLE_AUDIENCES") ?: listOf(IOS_BUNDLE_ID),
            googleAudiences = env.list("GOOGLE_AUDIENCES").orEmpty(),
            devAuth = env["LIFTLY_DEV_AUTH"] == "true",
        )

        private fun Map<String, String>.required(key: String) = get(key) ?: error("Missing environment variable $key")

        private fun Map<String, String>.list(key: String) = get(key)?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }

        const val DEFAULT_PORT = 8080
        const val IOS_BUNDLE_ID = "com.himanshu.liftly"
    }
}
