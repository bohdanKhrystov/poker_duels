package duels.poker.server.config

import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.ConfigLoader

/**
 * The server's configuration, built from a Ktor [ApplicationConfig] with each value overridable
 * by an environment variable.
 *
 * This is the *only* place the server reads its environment. Future fields from `ADR-0013`'s
 * grace period and `ADR-0011`'s database URL will land here as further `val`s with their own
 * key, env name and default.
 *
 * The database credentials are development defaults for a container listening on localhost;
 * production secrets arrive by environment variable.
 */
public data class ServerConfig(
    val port: Int,
    val maxFrameLength: Int,
    val maxFrameNestingDepth: Int,
    val databaseUrl: String,
    val databaseUser: String,
    val databasePassword: String,
    val databasePoolSize: Int,
) {
    public companion object {
        public const val DEFAULT_PORT: Int = 8080
        public const val PORT_KEY: String = "server.port"
        public const val PORT_ENV: String = "PORT"

        /**
         * The longest client frame, in UTF-16 code units, the codec will attempt to parse.
         * Refusing by length is the cheapest possible check against an oversized frame, so it
         * runs before any other validation. The default sits comfortably above any frame a real
         * client sends, while still bounding the memory a single hostile frame can cost.
         */
        public const val DEFAULT_MAX_FRAME_LENGTH: Int = 1 shl 20 // 1 MiB
        public const val MAX_FRAME_LENGTH_KEY: String = "server.maxFrameLength"
        public const val MAX_FRAME_LENGTH_ENV: String = "MAX_FRAME_LENGTH"

        /**
         * The deepest object/array nesting the codec will accept in a client frame before
         * refusing it outright, well short of the depth at which the recursive-descent JSON
         * parser would exhaust the stack.
         */
        public const val DEFAULT_MAX_FRAME_NESTING_DEPTH: Int = 64
        public const val MAX_FRAME_NESTING_DEPTH_KEY: String = "server.maxFrameNestingDepth"
        public const val MAX_FRAME_NESTING_DEPTH_ENV: String = "MAX_FRAME_NESTING_DEPTH"

        public const val DEFAULT_DATABASE_URL: String = "jdbc:postgresql://localhost:5432/poker_duels"
        public const val DATABASE_URL_KEY: String = "database.url"
        public const val DATABASE_URL_ENV: String = "DATABASE_URL"

        public const val DEFAULT_DATABASE_USER: String = "poker"
        public const val DATABASE_USER_KEY: String = "database.user"
        public const val DATABASE_USER_ENV: String = "DATABASE_USER"

        public const val DEFAULT_DATABASE_PASSWORD: String = "poker"
        public const val DATABASE_PASSWORD_KEY: String = "database.password"
        public const val DATABASE_PASSWORD_ENV: String = "DATABASE_PASSWORD"

        public const val DEFAULT_DATABASE_POOL_SIZE: Int = 8
        public const val DATABASE_POOL_SIZE_KEY: String = "database.poolSize"
        public const val DATABASE_POOL_SIZE_ENV: String = "DATABASE_POOL_SIZE"

        /**
         * Build a [ServerConfig] from a Ktor [ApplicationConfig] with environment variable
         * overrides.
         *
         * Precedence for each field is the same: the environment lookup, then
         * `config.propertyOrNull(key)?.getString()`, then the default. A value that is present
         * but not an integer is a startup error and throws [IllegalArgumentException].
         *
         * @param config The Ktor application configuration, typically loaded from `application.conf`
         * @param env A function to look up environment variables; defaults to [System.getenv]
         * @return A configured [ServerConfig]
         * @throws IllegalArgumentException if a value is present but not a valid integer
         */
        public fun from(
            config: ApplicationConfig,
            env: (String) -> String? = { name -> System.getenv(name) },
        ): ServerConfig {
            val portString = resolve(config, env, PORT_ENV, PORT_KEY, DEFAULT_PORT.toString())
            val port = requireNotNull(portString.toIntOrNull()) {
                "server port must be an integer, got: $portString"
            }

            val maxFrameLengthString = resolve(
                config,
                env,
                MAX_FRAME_LENGTH_ENV,
                MAX_FRAME_LENGTH_KEY,
                DEFAULT_MAX_FRAME_LENGTH.toString(),
            )
            val maxFrameLength = requireNotNull(maxFrameLengthString.toIntOrNull()) {
                "server max frame length must be an integer, got: $maxFrameLengthString"
            }

            val maxFrameNestingDepthString = resolve(
                config,
                env,
                MAX_FRAME_NESTING_DEPTH_ENV,
                MAX_FRAME_NESTING_DEPTH_KEY,
                DEFAULT_MAX_FRAME_NESTING_DEPTH.toString(),
            )
            val maxFrameNestingDepth = requireNotNull(maxFrameNestingDepthString.toIntOrNull()) {
                "server max frame nesting depth must be an integer, got: $maxFrameNestingDepthString"
            }

            val databaseUrl = resolve(config, env, DATABASE_URL_ENV, DATABASE_URL_KEY, DEFAULT_DATABASE_URL)
            val databaseUser = resolve(config, env, DATABASE_USER_ENV, DATABASE_USER_KEY, DEFAULT_DATABASE_USER)
            val databasePassword = resolve(config, env, DATABASE_PASSWORD_ENV, DATABASE_PASSWORD_KEY, DEFAULT_DATABASE_PASSWORD)

            val databasePoolSizeString = resolve(
                config,
                env,
                DATABASE_POOL_SIZE_ENV,
                DATABASE_POOL_SIZE_KEY,
                DEFAULT_DATABASE_POOL_SIZE.toString(),
            )
            val databasePoolSize = requireNotNull(databasePoolSizeString.toIntOrNull()) {
                "database pool size must be an integer, got: $databasePoolSizeString"
            }

            return ServerConfig(
                port = port,
                maxFrameLength = maxFrameLength,
                maxFrameNestingDepth = maxFrameNestingDepth,
                databaseUrl = databaseUrl,
                databaseUser = databaseUser,
                databasePassword = databasePassword,
                databasePoolSize = databasePoolSize,
            )
        }

        private fun resolve(
            config: ApplicationConfig,
            env: (String) -> String?,
            envName: String,
            key: String,
            default: String,
        ): String = env(envName) ?: config.propertyOrNull(key)?.getString() ?: default

        /**
         * Load the server configuration from the shipped `application.conf` resource, with environment
         * variable overrides.
         *
         * This function is called once during startup from `main`, and the resulting [ServerConfig]
         * is passed to dependent services rather than being re-read. Each field's precedence is:
         * environment variable, then file, then default.
         *
         * @param env A function to look up environment variables; defaults to [System.getenv]
         * @return A configured [ServerConfig]
         * @throws IllegalArgumentException if a value is present but not a valid integer
         */
        public fun load(env: (String) -> String? = { name -> System.getenv(name) }): ServerConfig =
            from(ConfigLoader.load("application.conf"), env)
    }
}
