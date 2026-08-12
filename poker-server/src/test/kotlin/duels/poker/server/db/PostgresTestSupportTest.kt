package duels.poker.server.db

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PostgresTestSupportTest {
    @Test
    fun runsWhenDockerIsAvailable() {
        assertEquals(
            PostgresTestSupport.Policy.RUN,
            PostgresTestSupport.policy(dockerAvailable = true, dockerRequired = false),
        )
        assertEquals(
            PostgresTestSupport.Policy.RUN,
            PostgresTestSupport.policy(dockerAvailable = true, dockerRequired = true),
        )
    }

    @Test
    fun skipsWhenDockerIsAbsentAndNotRequired() {
        assertEquals(
            PostgresTestSupport.Policy.SKIP,
            PostgresTestSupport.policy(dockerAvailable = false, dockerRequired = false),
        )
    }

    @Test
    fun failsWhenDockerIsAbsentAndRequired() {
        assertEquals(
            PostgresTestSupport.Policy.FAIL,
            PostgresTestSupport.policy(dockerAvailable = false, dockerRequired = true),
        )
    }

    @Test
    fun theFreshDatabaseAnswersSelectOne() {
        val dataSource = PostgresTestSupport.freshDatabase()
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT 1").use { resultSet ->
                    resultSet.next()
                    assertEquals(1, resultSet.getInt(1))
                }
            }
        }
    }

    @Test
    fun theFreshDatabaseHasNoTables() {
        val dataSource = PostgresTestSupport.freshDatabase()
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement
                    .executeQuery(
                        "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'",
                    ).use { resultSet ->
                        resultSet.next()
                        assertEquals(0, resultSet.getInt(1))
                    }
            }
        }
    }
}
