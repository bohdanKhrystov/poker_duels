package duels.poker.server.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import duels.poker.server.config.ServerConfig

/**
 * Builds the single HikariCP connection pool the server process holds.
 *
 * There is exactly one pool per process, built once from [ServerConfig] at startup; nothing else
 * opens a second one. The caller owns the returned [HikariDataSource] and is responsible for
 * closing it when the process shuts down. `ADR-0011` keeps SQL behind the repository boundary, so
 * nothing outside `duels.poker.server.db` should hold this type — callers elsewhere depend on a
 * repository interface, never on Hikari or a raw `DataSource`.
 */
public object Database {
    /**
     * Builds a [HikariDataSource] from [config]'s database URL, credentials and pool size.
     *
     * @param config The server configuration to read the database settings from
     * @return A pool ready to serve connections; the caller must [HikariDataSource.close] it
     */
    public fun connectionPool(config: ServerConfig): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.databaseUrl
            username = config.databaseUser
            password = config.databasePassword
            maximumPoolSize = config.databasePoolSize
            poolName = "poker-duels"
        }
        return HikariDataSource(hikariConfig)
    }
}
