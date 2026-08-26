package duels.poker.server.config

import duels.poker.server.auth.AttemptLimits
import duels.poker.server.room.RoomTimeouts
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.ConfigLoader

/**
 * The server's configuration, built from a Ktor [ApplicationConfig] with each value overridable
 * by an environment variable.
 *
 * This is the *only* place the server reads its environment. Fields from `ADR-0013`'s grace
 * period and `ADR-0011`'s database URL now land here as `val`s with their own key, env name
 * and default.
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
    val roomWaitingTimeoutMillis: Long,
    val roomFinishedTimeoutMillis: Long,
    // This field has a default so that three test files — DatabasePoolTest, DatabaseStartupTest,
    // PersistenceSurvivesRestartTest — can construct ServerConfig(...) field by field to point
    // Testcontainers at a database without opining on a duel's grace window. If this field
    // required explicit construction, this ticket would touch four files instead of two.
    val disconnectGraceMillis: Long = RoomTimeouts.DEFAULT_DISCONNECT_GRACE_MILLIS,
    // This field has a default for the same reason as disconnectGraceMillis, allowing existing
    // construction sites to compile without specifying the sweep period.
    val sweepPeriodMillis: Long = 1_000L,
    // These four fields have defaults so that existing construction sites can compile without
    // specifying auth budgets, which are only used by TASK-040521 and TASK-040523.
    val signUpMaxAttempts: Int = 5,
    val signUpWindowMillis: Long = 900_000L,
    val signInMaxAttempts: Int = 10,
    val signInWindowMillis: Long = 60_000L,
    // This field has a default so that existing construction sites can compile without
    // specifying the recovery link origin, which is only read when a mail sender is configured.
    val baseUrl: String = DEFAULT_BASE_URL,
) {
    /** Bundles the three room timeouts so callers do not reassemble them. */
    public fun roomTimeouts(): RoomTimeouts = RoomTimeouts(roomWaitingTimeoutMillis, roomFinishedTimeoutMillis, disconnectGraceMillis)

    /** Bundles the sign-up budget's two numbers so callers do not reassemble them. */
    public fun signUpLimits(): AttemptLimits = AttemptLimits(signUpMaxAttempts, signUpWindowMillis)

    /** Bundles the sign-in budget's two numbers so callers do not reassemble them. */
    public fun signInLimits(): AttemptLimits = AttemptLimits(signInMaxAttempts, signInWindowMillis)

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

        public const val DEFAULT_ROOM_WAITING_TIMEOUT_MILLIS: Long = RoomTimeouts.DEFAULT_WAITING_MILLIS
        public const val ROOM_WAITING_TIMEOUT_MILLIS_KEY: String = "room.waitingTimeoutMillis"
        public const val ROOM_WAITING_TIMEOUT_MILLIS_ENV: String = "ROOM_WAITING_TIMEOUT_MILLIS"

        public const val DEFAULT_ROOM_FINISHED_TIMEOUT_MILLIS: Long = RoomTimeouts.DEFAULT_FINISHED_MILLIS
        public const val ROOM_FINISHED_TIMEOUT_MILLIS_KEY: String = "room.finishedTimeoutMillis"
        public const val ROOM_FINISHED_TIMEOUT_MILLIS_ENV: String = "ROOM_FINISHED_TIMEOUT_MILLIS"

        public const val DEFAULT_DISCONNECT_GRACE_MILLIS: Long = RoomTimeouts.DEFAULT_DISCONNECT_GRACE_MILLIS
        public const val DISCONNECT_GRACE_MILLIS_KEY: String = "duel.disconnectGraceMillis"
        public const val DISCONNECT_GRACE_MILLIS_ENV: String = "DISCONNECT_GRACE_MILLIS"

        public const val DEFAULT_SWEEP_PERIOD_MILLIS: Long = 1_000L
        public const val SWEEP_PERIOD_MILLIS_KEY: String = "server.sweepPeriodMillis"
        public const val SWEEP_PERIOD_MILLIS_ENV: String = "SWEEP_PERIOD_MILLIS"

        public const val DEFAULT_SIGN_UP_MAX_ATTEMPTS: Int = 5
        public const val SIGN_UP_MAX_ATTEMPTS_KEY: String = "auth.signUpMaxAttempts"
        public const val AUTH_SIGN_UP_MAX_ATTEMPTS: String = "AUTH_SIGN_UP_MAX_ATTEMPTS"

        public const val DEFAULT_SIGN_UP_WINDOW_MILLIS: Long = 900_000L
        public const val SIGN_UP_WINDOW_MILLIS_KEY: String = "auth.signUpWindowMillis"
        public const val AUTH_SIGN_UP_WINDOW_MILLIS: String = "AUTH_SIGN_UP_WINDOW_MILLIS"

        public const val DEFAULT_SIGN_IN_MAX_ATTEMPTS: Int = 10
        public const val SIGN_IN_MAX_ATTEMPTS_KEY: String = "auth.signInMaxAttempts"
        public const val AUTH_SIGN_IN_MAX_ATTEMPTS: String = "AUTH_SIGN_IN_MAX_ATTEMPTS"

        public const val DEFAULT_SIGN_IN_WINDOW_MILLIS: Long = 60_000L
        public const val SIGN_IN_WINDOW_MILLIS_KEY: String = "auth.signInWindowMillis"
        public const val AUTH_SIGN_IN_WINDOW_MILLIS: String = "AUTH_SIGN_IN_WINDOW_MILLIS"

        public const val DEFAULT_BASE_URL: String = "http://localhost:5173"
        public const val BASE_URL_KEY: String = "server.baseUrl"
        public const val BASE_URL_ENV: String = "BASE_URL"

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

            val roomWaitingTimeoutMillisString = resolve(
                config,
                env,
                ROOM_WAITING_TIMEOUT_MILLIS_ENV,
                ROOM_WAITING_TIMEOUT_MILLIS_KEY,
                DEFAULT_ROOM_WAITING_TIMEOUT_MILLIS.toString(),
            )
            val roomWaitingTimeoutMillis = requireNotNull(roomWaitingTimeoutMillisString.toLongOrNull()) {
                "room waiting timeout must be an integer, got: $roomWaitingTimeoutMillisString"
            }

            val roomFinishedTimeoutMillisString = resolve(
                config,
                env,
                ROOM_FINISHED_TIMEOUT_MILLIS_ENV,
                ROOM_FINISHED_TIMEOUT_MILLIS_KEY,
                DEFAULT_ROOM_FINISHED_TIMEOUT_MILLIS.toString(),
            )
            val roomFinishedTimeoutMillis = requireNotNull(roomFinishedTimeoutMillisString.toLongOrNull()) {
                "room finished timeout must be an integer, got: $roomFinishedTimeoutMillisString"
            }

            val disconnectGraceMillisString = resolve(
                config,
                env,
                DISCONNECT_GRACE_MILLIS_ENV,
                DISCONNECT_GRACE_MILLIS_KEY,
                DEFAULT_DISCONNECT_GRACE_MILLIS.toString(),
            )
            val disconnectGraceMillis = requireNotNull(disconnectGraceMillisString.toLongOrNull()) {
                "disconnect grace window must be an integer, got: $disconnectGraceMillisString"
            }

            val sweepPeriodMillisString = resolve(
                config,
                env,
                SWEEP_PERIOD_MILLIS_ENV,
                SWEEP_PERIOD_MILLIS_KEY,
                DEFAULT_SWEEP_PERIOD_MILLIS.toString(),
            )
            val sweepPeriodMillis = requireNotNull(sweepPeriodMillisString.toLongOrNull()) {
                "sweep period must be an integer, got: $sweepPeriodMillisString"
            }

            val signUpMaxAttemptsString = resolve(
                config,
                env,
                AUTH_SIGN_UP_MAX_ATTEMPTS,
                SIGN_UP_MAX_ATTEMPTS_KEY,
                DEFAULT_SIGN_UP_MAX_ATTEMPTS.toString(),
            )
            val signUpMaxAttempts = requireNotNull(signUpMaxAttemptsString.toIntOrNull()) {
                "sign-up max attempts must be an integer, got: $signUpMaxAttemptsString"
            }

            val signUpWindowMillisString = resolve(
                config,
                env,
                AUTH_SIGN_UP_WINDOW_MILLIS,
                SIGN_UP_WINDOW_MILLIS_KEY,
                DEFAULT_SIGN_UP_WINDOW_MILLIS.toString(),
            )
            val signUpWindowMillis = requireNotNull(signUpWindowMillisString.toLongOrNull()) {
                "sign-up window must be an integer, got: $signUpWindowMillisString"
            }

            val signInMaxAttemptsString = resolve(
                config,
                env,
                AUTH_SIGN_IN_MAX_ATTEMPTS,
                SIGN_IN_MAX_ATTEMPTS_KEY,
                DEFAULT_SIGN_IN_MAX_ATTEMPTS.toString(),
            )
            val signInMaxAttempts = requireNotNull(signInMaxAttemptsString.toIntOrNull()) {
                "sign-in max attempts must be an integer, got: $signInMaxAttemptsString"
            }

            val signInWindowMillisString = resolve(
                config,
                env,
                AUTH_SIGN_IN_WINDOW_MILLIS,
                SIGN_IN_WINDOW_MILLIS_KEY,
                DEFAULT_SIGN_IN_WINDOW_MILLIS.toString(),
            )
            val signInWindowMillis = requireNotNull(signInWindowMillisString.toLongOrNull()) {
                "sign-in window must be an integer, got: $signInWindowMillisString"
            }

            val baseUrl = resolve(config, env, BASE_URL_ENV, BASE_URL_KEY, DEFAULT_BASE_URL)
            require(baseUrl.isNotEmpty()) {
                "$BASE_URL_KEY must not be empty"
            }
            require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
                "$BASE_URL_KEY must be an absolute http or https origin, got: $baseUrl"
            }
            require(!baseUrl.endsWith("/")) {
                "$BASE_URL_KEY must not have a trailing slash, got: $baseUrl"
            }

            return ServerConfig(
                port = port,
                maxFrameLength = maxFrameLength,
                maxFrameNestingDepth = maxFrameNestingDepth,
                databaseUrl = databaseUrl,
                databaseUser = databaseUser,
                databasePassword = databasePassword,
                databasePoolSize = databasePoolSize,
                roomWaitingTimeoutMillis = roomWaitingTimeoutMillis,
                roomFinishedTimeoutMillis = roomFinishedTimeoutMillis,
                disconnectGraceMillis = disconnectGraceMillis,
                sweepPeriodMillis = sweepPeriodMillis,
                signUpMaxAttempts = signUpMaxAttempts,
                signUpWindowMillis = signUpWindowMillis,
                signInMaxAttempts = signInMaxAttempts,
                signInWindowMillis = signInWindowMillis,
                baseUrl = baseUrl,
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
