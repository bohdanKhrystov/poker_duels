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
 */
public data class ServerConfig(val port: Int) {
    public companion object {
        public const val DEFAULT_PORT: Int = 8080
        public const val PORT_KEY: String = "server.port"
        public const val PORT_ENV: String = "PORT"

        /**
         * Build a [ServerConfig] from a Ktor [ApplicationConfig] with environment variable
         * overrides.
         *
         * Precedence for each field: the environment lookup `env(PORT_ENV)`, then
         * `config.propertyOrNull(PORT_KEY)?.getString()`, then the default. A value that is
         * present but not an integer is a startup error and throws [IllegalArgumentException].
         *
         * @param config The Ktor application configuration, typically loaded from `application.conf`
         * @param env A function to look up environment variables; defaults to [System.getenv]
         * @return A configured [ServerConfig]
         * @throws IllegalArgumentException if a port value is present but not a valid integer
         */
        public fun from(
            config: ApplicationConfig,
            env: (String) -> String? = { name -> System.getenv(name) },
        ): ServerConfig {
            val portEnv = env(PORT_ENV)
            val portConfig = config.propertyOrNull(PORT_KEY)?.getString()

            val portString = portEnv ?: portConfig ?: DEFAULT_PORT.toString()
            val port = requireNotNull(portString.toIntOrNull()) {
                "server port must be an integer, got: $portString"
            }

            return ServerConfig(port = port)
        }

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
         * @throws IllegalArgumentException if a port value is present but not a valid integer
         */
        public fun load(env: (String) -> String? = { name -> System.getenv(name) }): ServerConfig =
            from(ConfigLoader.load("application.conf"), env)
    }
}
