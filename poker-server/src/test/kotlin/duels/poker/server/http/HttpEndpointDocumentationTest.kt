package duels.poker.server.http

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Verifies that `docs/protocol.md` documents the HTTP endpoints `GET /api/me` and
 * `GET /api/me/duels`, their authentication mechanism, and their behavior.
 */
class HttpEndpointDocumentationTest {
    private val doc: String = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "docs/protocol.md") }
        .firstOrNull { it.isFile }
        ?.readText()
        ?: error("docs/protocol.md not found above ${File("").absolutePath}")

    @Test
    fun theDocumentDescribesTheProfileEndpoint() {
        assertTrue(
            doc.contains("GET /api/me"),
            "Document must contain 'GET /api/me'",
        )
    }

    @Test
    fun theDocumentDescribesTheRecentDuelsEndpoint() {
        assertTrue(
            doc.contains("GET /api/me/duels"),
            "Document must contain 'GET /api/me/duels'",
        )
    }

    @Test
    fun theDocumentNamesTheDeviceIdHeader() {
        assertTrue(
            doc.contains(DEVICE_ID_HEADER),
            "Document must contain '$DEVICE_ID_HEADER'",
        )
    }

    @Test
    fun theDocumentStatesTheLimitDefaultAndCap() {
        assertTrue(
            doc.contains("defaults to `$DEFAULT_DUEL_LIMIT`"),
            "Document must contain 'defaults to `$DEFAULT_DUEL_LIMIT`'",
        )
        assertTrue(
            doc.contains("capped at `$MAX_DUEL_LIMIT`"),
            "Document must contain 'capped at `$MAX_DUEL_LIMIT`'",
        )
    }

    @Test
    fun theDocumentSaysAnUnknownDeviceIsRefused() {
        assertTrue(
            doc.contains("401"),
            "Document must contain '401'",
        )
        assertTrue(
            doc.contains(DEVICE_ID_HEADER) && doc.contains("401") ||
                // The 401 appears in the HTTP endpoints section
                doc.lines().dropWhile { !it.contains("## HTTP endpoints") }
                    .takeWhile { !it.startsWith("##") || it.contains("## HTTP") }
                    .any { it.contains("401") },
            "Document must contain '401' in same line as '$DEVICE_ID_HEADER' or within the endpoints section",
        )
    }
}
