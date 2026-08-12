package duels.poker.server.db

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MigrationsTest {
    @Test
    fun appliesEveryMigrationToAnEmptyDatabase() {
        val dataSource = PostgresTestSupport.freshDatabase()

        val executed = Migrations.migrate(dataSource)

        assertTrue(executed >= 1)
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement
                    .executeQuery(
                        "SELECT count(*) FROM flyway_schema_history WHERE version = '1'",
                    ).use { resultSet ->
                        resultSet.next()
                        assertEquals(1, resultSet.getInt(1))
                    }
            }
        }
    }

    @Test
    fun aSecondRunAppliesNothing() {
        val dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)

        val secondRun = Migrations.migrate(dataSource)

        assertEquals(0, secondRun)
    }
}
