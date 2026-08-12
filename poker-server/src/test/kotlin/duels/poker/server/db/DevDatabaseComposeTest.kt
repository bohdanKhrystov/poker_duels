package duels.poker.server.db

import duels.poker.server.config.ServerConfig
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * Plain text assertions on the docker-compose.yml file's contents — no YAML library, no Docker.
 *
 * These tests verify that the local development database configuration matches the test
 * database and the server's expected defaults.
 */
class DevDatabaseComposeTest {
    private val composeFile = File("../docker-compose.yml")

    @Test
    fun theComposeFileExists() {
        assertTrue(composeFile.exists(), "File does not exist at ${composeFile.absolutePath}")
        assertTrue(composeFile.length() > 0, "File is empty at ${composeFile.absolutePath}")
    }

    @Test
    fun theComposeDatabaseMatchesTheServerConfigDefaults() {
        val content = composeFile.readText()

        // Check database name
        assertTrue(
            content.contains("POSTGRES_DB: poker_duels"),
            "Compose file must contain 'POSTGRES_DB: poker_duels'",
        )

        // Check user matches ServerConfig default
        assertTrue(
            content.contains("POSTGRES_USER: ${ServerConfig.DEFAULT_DATABASE_USER}"),
            "Compose file must contain 'POSTGRES_USER: ${ServerConfig.DEFAULT_DATABASE_USER}'",
        )

        // Check password matches ServerConfig default
        assertTrue(
            content.contains("POSTGRES_PASSWORD: ${ServerConfig.DEFAULT_DATABASE_PASSWORD}"),
            "Compose file must contain 'POSTGRES_PASSWORD: ${ServerConfig.DEFAULT_DATABASE_PASSWORD}'",
        )

        // Check port mapping
        assertTrue(
            content.contains("5432:5432"),
            "Compose file must publish port 5432:5432",
        )

        // Verify the database URL matches
        val expectedUrl = ServerConfig.DEFAULT_DATABASE_URL
        assertTrue(
            expectedUrl == "jdbc:postgresql://localhost:5432/poker_duels",
            "ServerConfig.DEFAULT_DATABASE_URL must be jdbc:postgresql://localhost:5432/poker_duels, got $expectedUrl",
        )
    }

    @Test
    fun theComposeImageMatchesTheTestContainerImage() {
        val content = composeFile.readText()
        assertTrue(
            content.contains("image: ${PostgresTestSupport.IMAGE}"),
            "Compose file must contain 'image: ${PostgresTestSupport.IMAGE}'",
        )
    }
}
