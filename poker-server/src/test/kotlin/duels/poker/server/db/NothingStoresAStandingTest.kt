package duels.poker.server.db

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ADR-0061 §3 and ADR-0066 §1 demand that a standing is derived, never stored: no table, column,
 * or materialised view in the migrated schema holds one. This test guards against someone later
 * adding a `season_standing` table or a `standings` column that silently introduces disagreement
 * with the derived standings computed per request.
 */
class NothingStoresAStandingTest {
    @Test
    fun theSchemaHoldsNoStandingAndNoSeason() {
        PostgresTestSupport.requireDocker()
        val dataSource = PostgresTestSupport.freshDatabase()

        Migrations.migrate(dataSource)

        dataSource.connection.use { connection ->
            // Check tables: no table name (in the public schema) contains "standing" or "season"
            connection.prepareStatement(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                AND (LOWER(table_name) LIKE '%standing%' OR LOWER(table_name) LIKE '%season%')
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val tables = generateSequence { if (resultSet.next()) resultSet.getString(1) else null }.toList()
                    assertEquals(
                        emptyList(),
                        tables,
                        "The schema must not store a standing or season; found tables: $tables",
                    )
                }
            }

            // Check columns: no column name contains "standing" or "season"
            connection.prepareStatement(
                """
                SELECT table_name, column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                AND (LOWER(column_name) LIKE '%standing%' OR LOWER(column_name) LIKE '%season%')
                ORDER BY table_name, column_name
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val columns = mutableListOf<String>()
                    while (resultSet.next()) {
                        columns.add("${resultSet.getString(1)}.${resultSet.getString(2)}")
                    }
                    assertTrue(
                        columns.isEmpty(),
                        "The schema must not store a standing or season; found columns: $columns",
                    )
                }
            }
        }
    }

    @Test
    fun theSchemaHoldsNoMaterialisedView() {
        PostgresTestSupport.requireDocker()
        val dataSource = PostgresTestSupport.freshDatabase()

        Migrations.migrate(dataSource)

        dataSource.connection.use { connection ->
            // Check for materialised views: pg_matviews must be empty
            connection.prepareStatement(
                """
                SELECT schemaname, matviewname
                FROM pg_matviews
                ORDER BY schemaname, matviewname
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val views = mutableListOf<String>()
                    while (resultSet.next()) {
                        views.add("${resultSet.getString(1)}.${resultSet.getString(2)}")
                    }
                    assertTrue(
                        views.isEmpty(),
                        "The schema must not have any materialised views; found: $views",
                    )
                }
            }
        }
    }
}
