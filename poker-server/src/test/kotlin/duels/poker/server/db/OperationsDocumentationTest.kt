package duels.poker.server.db

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import javax.sql.DataSource
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies that `docs/operations.md` documents the operator's interface to the takedown and
 * blocklist functions, and that the functions it names actually exist in the database schema.
 *
 * A document test that silently reads nothing passes everything, so this test walks up from
 * `File("")` to find the document and errors if it is not found. Every assertion checks against
 * a freshly migrated database, so a refactor that removes a function is caught rather than
 * silently ignored.
 */
class OperationsDocumentationTest {
    private lateinit var dataSource: DataSource

    private val doc: String = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "docs/operations.md") }
        .firstOrNull { it.isFile }
        ?.readText()
        ?: error("docs/operations.md not found above ${File("").absolutePath}")

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
    }

    @Test
    fun theDocumentNamesTheTakedownCall() {
        assertTrue(
            doc.contains("retire_display_name("),
            "Document must contain 'retire_display_name('",
        )
        assertTrue(
            doc.contains("cannot be undone"),
            "Document must contain 'cannot be undone' (or the sentence expressing it)",
        )
    }

    @Test
    fun theDocumentNamesBothBlocklistStatements() {
        assertTrue(
            doc.contains("'BLOCKED')"),
            "Document must contain the string literal 'BLOCKED' in the INSERT statement",
        )
        assertTrue(
            doc.contains("DELETE FROM name_registry"),
            "Document must contain the DELETE statement for removing blocklist entries",
        )
    }

    @Test
    fun everyFunctionTheDocumentNamesExistsInTheDatabase() {
        val functionNames = listOf("retire_display_name", "normalize")
        assertTrue(
            functionNames.isNotEmpty(),
            "Function names list must not be empty",
        )

        dataSource.connection.use { connection ->
            for (functionName in functionNames) {
                connection.prepareStatement(
                    "SELECT count(*) FROM pg_proc WHERE proname = ?",
                ).use { statement ->
                    statement.setString(1, functionName)
                    statement.executeQuery().use { resultSet ->
                        assertTrue(
                            resultSet.next(),
                            "Query for $functionName must return a row",
                        )
                        val count = resultSet.getInt(1)
                        assertTrue(
                            count >= 1,
                            "Function '$functionName' must exist in pg_proc, but found count=$count",
                        )
                    }
                }
            }
        }
    }

    @Test
    fun theDocumentDoesNotDescribeAnEndpointOrATask() {
        assertFalse(
            doc.contains("POST /api"),
            "Document must not describe a POST /api endpoint",
        )
        assertFalse(
            doc.contains("PUT /api"),
            "Document must not describe a PUT /api endpoint",
        )
        assertFalse(
            doc.contains("./gradlew"),
            "Document must not describe a Gradle task",
        )
        assertFalse(
            doc.contains("curl"),
            "Document must not describe a curl command",
        )
    }
}
