package duels.poker.server.db

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DockerApiVersionTest {
    @Test
    fun theTestJvmIsToldWhichDockerApiVersionToSpeak() {
        val apiVersion = System.getProperty("api.version")
        assertNotNull(apiVersion, "api.version system property must be set by the build to reach the forked test JVM")
    }

    @Test
    fun theConfiguredApiVersionIsAtLeastTheModernMinimum() {
        val apiVersionString = System.getProperty("api.version")
        assertNotNull(apiVersionString)

        val parts = apiVersionString.split(".")
        assertTrue(
            parts.size >= 2,
            "api.version must be in major.minor format (got: $apiVersionString)",
        )

        val major = parts[0].toIntOrNull()
        val minor = parts[1].toIntOrNull()

        assertTrue(
            major != null && minor != null,
            "api.version major and minor components must be integers (got: $apiVersionString)",
        )

        // Minimum supported API version is 1.40
        val minimumMajor = 1
        val minimumMinor = 40

        val versionIsAcceptable = (major!! > minimumMajor) || (major == minimumMajor && minor!! >= minimumMinor)
        assertTrue(
            versionIsAcceptable,
            "api.version must be at least $minimumMajor.$minimumMinor (got: $apiVersionString). " +
                "Docker Engine 29+ rejects versions below 1.40.",
        )
    }
}
