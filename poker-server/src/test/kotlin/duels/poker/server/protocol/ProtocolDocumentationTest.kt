package duels.poker.server.protocol

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Verifies that `docs/protocol.md` is kept in sync with the actual protocol definitions.
 *
 * If a message type is added to either [ClientMessage] or [ServerMessage] without adding a
 * corresponding row to the documentation table, this test will fail. The document is the contract,
 * and the contract must always match the implementation.
 */
class ProtocolDocumentationTest {
    private val doc: String = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "docs/protocol.md") }
        .firstOrNull { it.isFile }
        ?.readText()
        ?: error("docs/protocol.md not found above ${File("").absolutePath}")

    @Test
    fun everyClientMessageHasARowSayingClientToServer() {
        val clientMessages = subtypeNames(ClientMessage.serializer().descriptor)
        for (message in clientMessages) {
            val regex = Regex("^\\| `$message` \\|.*client → server.*$", RegexOption.MULTILINE)
            val matches = doc.split("\n").filter { regex.containsMatchIn(it) }
            assertEquals(
                1,
                matches.size,
                "Expected exactly one row for client message `$message` containing 'client → server', found ${matches.size}: $matches",
            )
        }
    }

    @Test
    fun everyServerMessageHasARowSayingServerToClient() {
        val serverMessages = subtypeNames(ServerMessage.serializer().descriptor)
        for (message in serverMessages) {
            val regex = Regex("^\\| `$message` \\|.*server → client.*$", RegexOption.MULTILINE)
            val matches = doc.split("\n").filter { regex.containsMatchIn(it) }
            assertEquals(
                1,
                matches.size,
                "Expected exactly one row for server message `$message` containing 'server → client', found ${matches.size}: $matches",
            )
        }
    }

    @Test
    fun theDocumentNamesNoMessageThatDoesNotExist() {
        val clientMessages = subtypeNames(ClientMessage.serializer().descriptor).toSet()
        val serverMessages = subtypeNames(ServerMessage.serializer().descriptor).toSet()
        val allMessages = clientMessages + serverMessages

        // Extract all backticked identifiers from table rows
        val messagePattern = Regex("^\\| `([A-Za-z]+)` \\|")
        val documentedMessages = doc.split("\n")
            .mapNotNull { messagePattern.find(it)?.groupValues?.get(1) }
            .toSet()

        val staleMessages = documentedMessages - allMessages
        assertTrue(
            staleMessages.isEmpty(),
            "Document names messages that do not exist: $staleMessages",
        )
    }

    @Test
    fun theDocumentStatesTheCurrentProtocolVersion() {
        val expected = "Protocol version: **$PROTOCOL_VERSION**"
        assertTrue(
            doc.contains(expected),
            "Document must contain '$expected'",
        )
    }

    @Test
    fun theDocumentListsEveryProtocolError() {
        for (error in ProtocolError.entries) {
            assertTrue(
                doc.contains(error.name),
                "Document must list ProtocolError.${error.name}",
            )
        }
    }
}
