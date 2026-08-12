package duels.poker.server.db

import org.flywaydb.core.Flyway
import javax.sql.DataSource

/**
 * Applies the schema in `classpath:db/migration` to a [DataSource].
 *
 * Every migration under that path is `V<n>__name.sql`, applied in order and recorded by
 * checksum in `flyway_schema_history`. A merged migration is immutable: once a file has run
 * anywhere, editing it changes its checksum, and the next startup fails validation rather
 * than silently applying a rewritten history. A column, an index or a table added later is a
 * new `V<n>__` file, never an edit to one that already ran.
 */
public object Migrations {
    /**
     * Applies every pending migration to [dataSource] and returns how many ran.
     *
     * Running this against a database that is already up to date is a no-op: it applies
     * nothing and returns `0`.
     */
    public fun migrate(dataSource: DataSource): Int =
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
            .migrationsExecuted
}
