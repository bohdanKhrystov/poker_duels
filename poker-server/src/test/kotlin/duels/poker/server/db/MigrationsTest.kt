package duels.poker.server.db

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MigrationsTest {
    @Test
    fun bothMigrationsApplyToAnEmptyDatabase() {
        val dataSource = PostgresTestSupport.freshDatabase()

        Migrations.migrate(dataSource)

        // setupDatabase() already ran Migrations.migrate(dataSource) against a fresh
        // database; V2 is the first evidence that the migration chain works with more
        // than one file.
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY version",
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val versions = generateSequence { if (resultSet.next()) resultSet.getString(1) else null }.toList()
                    assertEquals(listOf("1", "2"), versions)
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
