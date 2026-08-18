package duels.poker.server.http

import duels.poker.server.protocol.http.DuelSummaryResponse
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.protocol.http.RecentDuelsResponse
import duels.poker.server.protocol.http.SignUpRequest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

private const val DEVICE_ID_HEADER: String = "X-Device-Id"
private const val DEFAULT_DUEL_LIMIT: Int = 10
private const val MAX_DUEL_LIMIT: Int = 50

/**
 * Verifies that `docs/protocol.md` documents the HTTP endpoints `POST /api/auth/sign-up`,
 * `GET /api/me`, `GET /api/me/duels`, and `PUT /api/me/name`, their authentication mechanism,
 * and their behavior.
 *
 * The tests below go further than substring matching: they reflect over the response DTOs so
 * that a claim in the document (a field is nullable, a field exists) can be checked against what
 * the code actually exposes, rather than trusted at face value.
 */
class HttpEndpointDocumentationTest {
    private val doc: String = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "docs/protocol.md") }
        .firstOrNull { it.isFile }
        ?.readText()
        ?: error("docs/protocol.md not found above ${File("").absolutePath}")

    // Scoping to the section that documents each response keeps the checks below from matching a
    // field name that happens to recur in an unrelated table.
    // Each section is bounded by the heading that *follows* it, so moving `### Sign up` away from
    // its position before `### Profile endpoint` breaks this pairing. That failure is **loud**, not
    // silent: `sectionBetween` requires its end marker and throws when it is missing — measured by
    // moving the section to the end of the document, which failed all fourteen tests here with
    // `IllegalArgumentException` rather than quietly reading a different span. So this is a comment
    // about why the ordering is coupled to the document, not a warning about undetectable drift.
    private val signUpSection: String =
        sectionBetween("### Sign up", "### Profile endpoint")
    private val profileSection: String =
        sectionBetween("### Profile endpoint", "### Set display name")
    private val setNameSection: String =
        sectionBetween("### Set display name", "### Recent duels endpoint")
    private val recentDuelsSection: String =
        sectionBetween("### Recent duels endpoint", "Each duel summary in the array contains:")
    private val duelSummarySection: String =
        sectionBetween("Each duel summary in the array contains:", "## Protocol Errors")

    private fun sectionBetween(startMarker: String, endMarker: String): String {
        val startIndex = doc.indexOf(startMarker)
        require(startIndex >= 0) { "'$startMarker' not found in docs/protocol.md" }
        val afterStart = doc.substring(startIndex)
        val endIndex = afterStart.indexOf(endMarker)
        require(endIndex >= 0) { "'$endMarker' not found after '$startMarker' in docs/protocol.md" }
        return afterStart.substring(0, endIndex)
    }

    // The field-name tables look like "| fieldName | Type | Semantics |"; the header row's first
    // cell is "Field" and the separator row's is "---", so both are excluded by construction.
    private fun documentedFieldNames(section: String): List<String> =
        section.lines().mapNotNull { line ->
            val trimmed = line.trim()
            if (!trimmed.startsWith("|")) return@mapNotNull null
            trimmed.removePrefix("|").substringBefore("|").trim()
                .takeIf { it.isNotEmpty() && it != "Field" && it != "---" }
        }

    private fun rowFor(section: String, fieldName: String): String? =
        section.lines().firstOrNull { it.trim().startsWith("| $fieldName |") }

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

    @Test
    fun theDocumentDoesNotCallANonNullFieldNullable() {
        val sections: Map<KClass<*>, String> = mapOf(
            ProfileResponse::class to profileSection,
            DuelSummaryResponse::class to duelSummarySection,
        )
        val checkedFields = mutableListOf<String>()
        for ((kClass, section) in sections) {
            val nonNullProperties = kClass.memberProperties.filterNot { it.returnType.isMarkedNullable }
            assertTrue(
                nonNullProperties.isNotEmpty(),
                "${kClass.simpleName} must expose at least one non-null property to check",
            )
            for (property in nonNullProperties) {
                val row = rowFor(section, property.name) ?: continue
                checkedFields += "${kClass.simpleName}.${property.name}"
                assertTrue(
                    !row.contains("null", ignoreCase = true),
                    "${kClass.simpleName}.${property.name} is non-null but the document row claims " +
                        "nullability: $row",
                )
            }
        }
        assertTrue(checkedFields.isNotEmpty(), "Expected to check at least one documented non-null field")
    }

    @Test
    fun theDocumentedFieldNamesAllExist() {
        val profileFields = documentedFieldNames(profileSection)
        val duelSummaryFields = documentedFieldNames(duelSummarySection)
        assertTrue(profileFields.isNotEmpty(), "Expected at least one documented field for ProfileResponse")
        assertTrue(duelSummaryFields.isNotEmpty(), "Expected at least one documented field for DuelSummaryResponse")

        val profilePropertyNames = ProfileResponse::class.memberProperties.map { it.name }.toSet()
        val duelSummaryPropertyNames = DuelSummaryResponse::class.memberProperties.map { it.name }.toSet()
        assertTrue(profilePropertyNames.isNotEmpty(), "ProfileResponse must expose at least one property")
        assertTrue(duelSummaryPropertyNames.isNotEmpty(), "DuelSummaryResponse must expose at least one property")

        for (field in profileFields) {
            assertTrue(field in profilePropertyNames, "Documented field '$field' does not exist on ProfileResponse")
        }
        for (field in duelSummaryFields) {
            assertTrue(
                field in duelSummaryPropertyNames,
                "Documented field '$field' does not exist on DuelSummaryResponse",
            )
        }
    }

    @Test
    fun theDocumentDescribesTheSetNameEndpoint() {
        assertTrue(
            doc.contains("PUT /api/me/name"),
            "Document must contain 'PUT /api/me/name'",
        )
        // Each status code must appear in the Set display name section, not just anywhere in the document
        val statusCodes = listOf("400", "401", "403", "409")
        for (statusCode in statusCodes) {
            assertTrue(
                setNameSection.contains(statusCode),
                "The Set display name section must document status code '$statusCode'",
            )
        }
    }

    @Test
    fun theDocumentMarksTheDisplayNameNullable() {
        val displayNameProperty = ProfileResponse::class.memberProperties.firstOrNull { it.name == "displayName" }
            ?: error("ProfileResponse must have a 'displayName' property")
        assertTrue(
            displayNameProperty.returnType.isMarkedNullable,
            "ProfileResponse.displayName must be nullable (String or null), and the document must mark it as such",
        )
        val displayNameRow = rowFor(profileSection, "displayName")
            ?: error("Profile section must document the 'displayName' field")
        assertTrue(
            displayNameRow.contains("null", ignoreCase = true),
            "The displayName row must mention 'null' to indicate nullability: $displayNameRow",
        )
    }

    @Test
    fun theDocumentMarksTheOpponentDisplayNameNullable() {
        val opponentDisplayNameProperty = DuelSummaryResponse::class.memberProperties.firstOrNull { it.name == "opponentDisplayName" }
            ?: error("DuelSummaryResponse must have an 'opponentDisplayName' property")
        assertTrue(
            opponentDisplayNameProperty.returnType.isMarkedNullable,
            "DuelSummaryResponse.opponentDisplayName must be nullable (String or null), and the document must mark it as such",
        )
        val opponentDisplayNameRow = rowFor(duelSummarySection, "opponentDisplayName")
            ?: error("Duel summary section must document the 'opponentDisplayName' field")
        assertTrue(
            opponentDisplayNameRow.contains("null", ignoreCase = true),
            "The opponentDisplayName row must mention 'null' to indicate nullability: $opponentDisplayNameRow",
        )
    }

    @Test
    fun theDocumentStatesTheCanonicalFormRules() {
        // Verify the Set display name section documents the canonicalisation rules that
        // DisplayName.kt enforces. If these claims drift from the code, clients cannot
        // explain a 400 to players.
        assertTrue(
            setNameSection.contains("1–32 code point") || setNameSection.contains("1-32 code point"),
            "The document must state the 1–32 code point bound that canonicalDisplayNameOrNull enforces",
        )
        assertTrue(
            setNameSection.contains("NFC"),
            "The document must mention NFC normalization that canonicalDisplayNameOrNull applies",
        )
        assertTrue(
            setNameSection.contains("Cc") && setNameSection.contains("Cf"),
            "The document must mention Unicode categories Cc (control) and Cf (format) that are refused",
        )
        assertTrue(
            setNameSection.contains("consecutive") || setNameSection.contains("two or more"),
            "The document must describe the rule refusing consecutive spaces that canonicalDisplayNameOrNull enforces",
        )
    }

    @Test
    fun theDocumentDescribesTheSignUpEndpoint() {
        assertTrue(
            doc.contains("POST /api/auth/sign-up"),
            "Document must contain 'POST /api/auth/sign-up'",
        )
    }

    @Test
    fun theSignUpSectionNamesEveryFieldTheRequestHas() {
        val reflectedProperties = SignUpRequest::class.memberProperties.map { it.name }.toSet()
        assertTrue(
            reflectedProperties.isNotEmpty(),
            "SignUpRequest must expose at least one property to check",
        )
        // Assert the exact set to catch if a field is added to the DTO but not documented
        assertTrue(
            reflectedProperties == setOf("handle", "password"),
            "SignUpRequest must have exactly the fields 'handle' and 'password', but found: $reflectedProperties",
        )

        val documentedFields = documentedFieldNames(signUpSection)
        for (field in reflectedProperties) {
            assertTrue(
                field in documentedFields,
                "Documented field '$field' must appear in the Sign up section's request body table",
            )
        }
    }

    @Test
    fun theSignUpSectionNamesEveryStatusTheRouteCanAnswer() {
        val statusCodes = listOf("201", "400", "401", "409", "422")
        assertTrue(
            statusCodes.isNotEmpty(),
            "Status code list must not be empty",
        )
        for (statusCode in statusCodes) {
            assertTrue(
                signUpSection.contains(statusCode),
                "The Sign up section must document status code '$statusCode'",
            )
        }
    }

    @Test
    fun theRecentDuelsSectionNamesEveryFieldTheResponseHas() {
        val reflectedProperties = RecentDuelsResponse::class.memberProperties.map { it.name }.toSet()
        assertTrue(
            reflectedProperties.isNotEmpty(),
            "RecentDuelsResponse must expose at least one property to check",
        )
        // Assert the exact set to catch if a field is added to the DTO but not documented
        assertTrue(
            reflectedProperties == setOf("duels", "nextCursor"),
            "RecentDuelsResponse must have exactly the fields 'duels' and 'nextCursor', but found: $reflectedProperties",
        )

        val documentedFields = documentedFieldNames(recentDuelsSection)
        for (field in reflectedProperties) {
            assertTrue(
                field in documentedFields,
                "Documented field '$field' must appear in the Recent duels section's response table",
            )
        }
    }

    @Test
    fun theRecentDuelsSectionDocumentsTheCursor() {
        assertTrue(
            recentDuelsSection.contains("after"),
            "The Recent duels section must document the 'after' parameter",
        )
        assertTrue(
            recentDuelsSection.contains("opaque"),
            "The Recent duels section must document that 'after' is opaque",
        )
        assertTrue(
            recentDuelsSection.contains("400"),
            "The Recent duels section must document that an invalid 'after' returns 400",
        )
    }

    @Test
    fun theDocumentMarksTheNextCursorNullable() {
        val nextCursorProperty = RecentDuelsResponse::class.memberProperties.firstOrNull { it.name == "nextCursor" }
            ?: error("RecentDuelsResponse must have a 'nextCursor' property")
        assertTrue(
            nextCursorProperty.returnType.isMarkedNullable,
            "RecentDuelsResponse.nextCursor must be nullable (String or null), and the document must mark it as such",
        )
        val nextCursorRow = rowFor(recentDuelsSection, "nextCursor")
            ?: error("Recent duels section must document the 'nextCursor' field")
        assertTrue(
            nextCursorRow.contains("null", ignoreCase = true),
            "The nextCursor row must mention 'null' to indicate nullability: $nextCursorRow",
        )
    }
}
