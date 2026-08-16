package duels.poker.server.protocol

import duels.poker.server.protocol.typescript.protocolDeclarations
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.security.MessageDigest

/**
 * Verifies that `docs/protocol-versions.md` — the claim ledger `ADR-0047` introduces — matches
 * both [PROTOCOL_VERSION] and the live wire.
 *
 * The ledger's own textual conflict is what stops two branches from claiming the same version
 * number at merge time; this test is what stops every wrong way of resolving that conflict from
 * passing `check` anyway (`ADR-0047` §3): keeping both rows fails [versionsAscendByExactlyOneFromRowToRow],
 * keeping the other branch's row fails [theLastRowsFingerprintEqualsTheComputedFingerprint],
 * renumbering and forgetting the constant fails [theLastRowsVersionEqualsProtocolVersion], and
 * moving the wire with no bump at all fails the fingerprint check too.
 */
class ProtocolVersionLedgerTest {

    private val doc: String = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "docs/protocol-versions.md") }
        .firstOrNull { it.isFile }
        ?.readText()
        ?: error("docs/protocol-versions.md not found above ${File("").absolutePath}")

    /** `ADR-0047` §1's row shape: a version number, then a 16-hex fingerprint in backticks. */
    private val rowPattern = Regex("^\\| (\\d+) \\| `([0-9a-f]{16})` \\|")

    /**
     * Every line of [doc] that occupies a data-row position in the ledger's single table — after
     * the header and the separator, whatever is actually there, parsed or not. Rows are never
     * filtered through [rowPattern] to build this list: a row that fails to parse must still reach
     * every assertion that needs one, rather than silently vanishing from the count.
     */
    private fun rowLines(): List<String> =
        doc.lines().filter { it.startsWith("|") }.drop(2)

    /**
     * The first 16 hexadecimal characters of the SHA-256 of the live wire, per `ADR-0047` §2: the
     * `text` of every [duels.poker.server.protocol.typescript.TypeScriptDeclaration] returned by
     * `protocolDeclarations()`, sorted by declaration name and joined with `\n`. The generated
     * file's header and its `ProtocolVersion` alias are excluded because `protocolDeclarations()`
     * never emits them — the fingerprint names the shape, not the number.
     */
    private fun computedFingerprint(): String {
        val text = protocolDeclarations()
            .sortedBy { it.name }
            .joinToString("\n") { it.text }
        val hash = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it.toInt() and 0xFF) }.take(16)
    }

    /** Parses every row line, failing loudly — never silently — on the first one that does not. */
    private fun parsedRows(): List<Pair<Int, String>> =
        rowLines().map { line ->
            val match = rowPattern.find(line)
                ?: error("docs/protocol-versions.md has a row that does not parse: \"$line\"")
            match.groupValues[1].toInt() to match.groupValues[2]
        }

    /** The `ADR-0047` §5 failure message: the row to append, and what to do if the number is taken. */
    private fun rowToAppendMessage(lastClaimedVersion: Int): String =
        """
        docs/protocol-versions.md does not match the wire.
          PROTOCOL_VERSION is $PROTOCOL_VERSION; the last ledger row claims $lastClaimedVersion.
          Append this row, then re-run:
              | $PROTOCOL_VERSION | `${computedFingerprint()}` | STORY-XXXX | YYYY-MM-DD |
          If another version already claims your number, rebase on develop and take the next free
          one (ADR-0045 §4).
        """.trimIndent()

    @Test
    fun everyTableRowParses() {
        val rows = rowLines()
        assertTrue(rows.isNotEmpty(), "docs/protocol-versions.md has no table rows")

        val unparsed = rows.filterNot { rowPattern.containsMatchIn(it) }
        assertTrue(
            unparsed.isEmpty(),
            "docs/protocol-versions.md has a row that does not parse: $unparsed",
        )
    }

    @Test
    fun versionsAscendByExactlyOneFromRowToRow() {
        val versions = parsedRows().map { it.first }
        for (index in 1 until versions.size) {
            assertEquals(
                versions[index - 1] + 1,
                versions[index],
                "docs/protocol-versions.md row claiming ${versions[index]} must follow the row " +
                    "claiming ${versions[index - 1]} by exactly one, never skip or repeat a version",
            )
        }
    }

    @Test
    fun theLastRowsVersionEqualsProtocolVersion() {
        val lastVersion = parsedRows().last().first
        assertEquals(PROTOCOL_VERSION, lastVersion, rowToAppendMessage(lastVersion))
    }

    @Test
    fun theLastRowsFingerprintEqualsTheComputedFingerprint() {
        val (lastVersion, lastFingerprint) = parsedRows().last()
        assertEquals(computedFingerprint(), lastFingerprint, rowToAppendMessage(lastVersion))
    }
}
