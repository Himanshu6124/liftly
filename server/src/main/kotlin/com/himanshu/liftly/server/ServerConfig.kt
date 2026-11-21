package com.himanshu.liftly.server

/** Everything the server reads from the environment. Secrets never live in the repo. */
data class ServerConfig(
    val port: Int,
    val databaseUrl: String,
    val databaseUser: String,
    val databasePassword: String,
    /**
     * Accepts `X-Liftly-Dev-User` as the caller's id with no token. Local development and tests
     * only, until real sign-in arrives (Milestone 21). Never set it in production.
     */
    val devAuth: Boolean,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()) = ServerConfig(
            port = env["PORT"]?.toInt() ?: DEFAULT_PORT,
            databaseUrl = env.required("DATABASE_URL"),
            databaseUser = env.required("DATABASE_USER"),
            databasePassword = env.required("DATABASE_PASSWORD"),
            devAuth = env["LIFTLY_DEV_AUTH"] == "true",
        )

        private fun Map<String, String>.required(key: String) = get(key) ?: error("Missing environment variable $key")

        const val DEFAULT_PORT = 8080
    }
}
