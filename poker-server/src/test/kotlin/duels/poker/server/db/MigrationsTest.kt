package duels.poker.server.db

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MigrationsTest {
    @Test
    fun everyMigrationAppliesToAnEmptyDatabase() {
        val dataSource = PostgresTestSupport.freshDatabase()

        Migrations.migrate(dataSource)

        // setupDatabase() already ran Migrations.migrate(dataSource) against a fresh
        // database; V2 and V3 are the first evidence that the migration chain works with more
        // than one file.
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY version",
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val versions = generateSequence { if (resultSet.next()) resultSet.getString(1) else null }.toList()
                    assertEquals(listOf("1", "2", "3"), versions)
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

    @Test
    fun theThirdMigrationAddsANullableDisplayNameColumn() {
        val dataSource = PostgresTestSupport.freshDatabase()

        Migrations.migrate(dataSource)

        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT column_name, is_nullable, data_type
                FROM information_schema.columns
                WHERE table_name = 'player' AND column_name = 'display_name'
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    assertEquals(true, resultSet.next(), "display_name column should exist")
                    assertEquals("display_name", resultSet.getString("column_name"))
                    assertEquals("YES", resultSet.getString("is_nullable"))
                    assertEquals("text", resultSet.getString("data_type"))
                }
            }
        }
    }

    @Test
    fun theThirdMigrationAddsTheIndexAndTheTrigger() {
        val dataSource = PostgresTestSupport.freshDatabase()

        Migrations.migrate(dataSource)

        dataSource.connection.use { connection ->
            // Check for the unique index
            connection.prepareStatement(
                """
                SELECT indexname FROM pg_indexes
                WHERE tablename = 'player' AND indexname = 'player_display_name_unique'
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    assertEquals(true, resultSet.next(), "player_display_name_unique index should exist")
                    assertEquals("player_display_name_unique", resultSet.getString("indexname"))
                }
            }

            // Check for the trigger
            connection.prepareStatement(
                """
                SELECT trigger_name FROM information_schema.triggers
                WHERE trigger_name = 'player_display_name_permanent'
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    assertEquals(true, resultSet.next(), "player_display_name_permanent trigger should exist")
                    assertEquals("player_display_name_permanent", resultSet.getString("trigger_name"))
                }
            }
        }
    }
}
