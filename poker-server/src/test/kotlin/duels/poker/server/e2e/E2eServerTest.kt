package duels.poker.server.e2e

import duels.poker.server.db.PostgresTestSupport
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

@Timeout(120)
internal class E2eServerTest {
    @Test
    fun theSameSeedGivesTheSameHandSeeds() {
        val seed = 7L
        val source1 = handSeedSource(seed)
        val source2 = handSeedSource(seed)

        val first1 = source1.newHandSeed()
        val first2 = source2.newHandSeed()

        val second1 = source1.newHandSeed()
        val second2 = source2.newHandSeed()

        val third1 = source1.newHandSeed()
        val third2 = source2.newHandSeed()

        // Both sources yield equal first three values
        assert(first1 == first2) { "First seeds should be equal" }
        assert(second1 == second2) { "Second seeds should be equal" }
        assert(third1 == third2) { "Third seeds should be equal" }

        // And those three values are not all equal to each other
        assert(setOf(first1, second1, third1).size > 1) {
            "Not all three seeds should be equal"
        }
    }

    @Test
    fun theInstalledServerWritesToTheDatabaseGiven() {
        PostgresTestSupport.requireDocker()
        val dataSource = freshMigratedDatabase()

        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }

            val session = client.webSocketSession("/ws")
            session.completeHandshake("probe")

            // Verify that a player was written to the database
            dataSource.connection.use { connection ->
                connection.createStatement().use { stmt ->
                    val resultSet = stmt.executeQuery("SELECT COUNT(*) FROM player")
                    resultSet.next()
                    val count = resultSet.getInt(1)
                    assert(count == 1) { "Expected 1 player in database, got $count" }
                }
            }
        }
    }
}
