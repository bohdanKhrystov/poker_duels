package duels.poker.server.db

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MigrationsTest {
    @Test
    fun everyMigrationAppliesToAnEmptyDatabase() {
        val dataSource = PostgresTestSupport.freshDatabase()

        Migrations.migrate(dataSource)

        // Get what Flyway sees on the classpath, configured identically to Migrations.migrate
        val onTheClasspath = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .info()
            .all()
            .map { it.version.version }

        // Get what was actually applied and recorded in the database
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY version",
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val applied = generateSequence { if (resultSet.next()) resultSet.getString(1) else null }.toList()

                    // Assertion 1: floor that at least V1-V4 are visible on the classpath (non-empty assertion)
                    assertTrue(onTheClasspath.containsAll(listOf("1", "2", "3", "4")))
                    // Assertion 2: every migration on the classpath is in the history
                    assertEquals(onTheClasspath, applied)
                    // Assertion 3: history is ordered
                    assertEquals(applied.sorted(), applied)
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

    @Test
    fun theFourthMigrationAddsTheCredentialTable() {
        val dataSource = PostgresTestSupport.freshDatabase()

        Migrations.migrate(dataSource)

        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT column_name, data_type, is_nullable
                FROM information_schema.columns
                WHERE table_name = 'credential'
                ORDER BY ordinal_position
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val columns = mutableListOf<Triple<String, String, String>>()
                    while (resultSet.next()) {
                        columns.add(
                            Triple(
                                resultSet.getString("column_name"),
                                resultSet.getString("data_type"),
                                resultSet.getString("is_nullable"),
                            ),
                        )
                    }
                    val expected = listOf(
                        Triple("id", "uuid", "NO"),
                        Triple("player_id", "uuid", "NO"),
                        Triple("kind", "text", "NO"),
                        Triple("identifier", "text", "NO"),
                        Triple("secret_hash", "text", "YES"),
                        Triple("created_at", "timestamp with time zone", "NO"),
                    )
                    assertEquals(expected, columns, "credential columns should match exactly")
                }
            }

            // Check for the unique constraint
            connection.prepareStatement(
                """
                SELECT constraint_name FROM information_schema.table_constraints
                WHERE table_name = 'credential' AND constraint_name = 'credential_kind_identifier_unique'
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    assertEquals(true, resultSet.next(), "credential_kind_identifier_unique constraint should exist")
                    assertEquals("credential_kind_identifier_unique", resultSet.getString("constraint_name"))
                }
            }
        }
    }

    @Test
    fun theFourthMigrationAddsTheAuthSessionTableAndItsIndex() {
        val dataSource = PostgresTestSupport.freshDatabase()

        Migrations.migrate(dataSource)

        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT column_name, data_type, is_nullable
                FROM information_schema.columns
                WHERE table_name = 'auth_session'
                ORDER BY ordinal_position
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val columns = mutableListOf<Triple<String, String, String>>()
                    while (resultSet.next()) {
                        columns.add(
                            Triple(
                                resultSet.getString("column_name"),
                                resultSet.getString("data_type"),
                                resultSet.getString("is_nullable"),
                            ),
                        )
                    }
                    val expected = listOf(
                        Triple("token_hash", "bytea", "NO"),
                        Triple("player_id", "uuid", "NO"),
                        Triple("issued_at", "timestamp with time zone", "NO"),
                        Triple("expires_at", "timestamp with time zone", "NO"),
                    )
                    assertEquals(expected, columns, "auth_session columns should match exactly")
                }
            }

            // Check for the index
            connection.prepareStatement(
                """
                SELECT indexname FROM pg_indexes
                WHERE tablename = 'auth_session' AND indexname = 'auth_session_player_id_idx'
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    assertEquals(true, resultSet.next(), "auth_session_player_id_idx index should exist")
                    assertEquals("auth_session_player_id_idx", resultSet.getString("indexname"))
                }
            }
        }
    }
}
