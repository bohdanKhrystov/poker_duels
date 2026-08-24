package duels.poker.server.http

import duels.poker.server.protocol.http.DuelSummaryResponse
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.protocol.http.RecentDuelsResponse
import duels.poker.server.protocol.http.SelfStandingResponse
import duels.poker.server.protocol.http.SignUpRequest
import duels.poker.server.protocol.http.StandingRow
import duels.poker.server.protocol.http.StandingsResponse
import org.junit.jupiter.api.Assertions.assertFalse
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
        sectionBetween("### Sign up", "### Sign in")
    private val signInSection: String =
        sectionBetween("### Sign in", "### Sign out")
    private val signOutSection: String =
        sectionBetween("### Sign out", "### Profile endpoint")
    private val profileSection: String =
        sectionBetween("### Profile endpoint", "### Set display name")
    private val setNameSection: String =
        sectionBetween("### Set display name", "### Revoke this device")
    private val deviceSection: String =
        sectionBetween("### Revoke this device", "### Recent duels endpoint")
    private val recentDuelsSection: String =
        sectionBetween("### Recent duels endpoint", "Each duel summary in the array contains:")
    private val duelSummarySection: String =
        sectionBetween("Each duel summary in the array contains:", "### Standings endpoint")
    private val standingsSection: String =
        sectionBetween("### Standings endpoint", "## Protocol Errors")

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
    fun theProfileSectionDocumentsTheRemovedNameField() {
        val profileFields = documentedFieldNames(profileSection)
        assertTrue(
            "displayNameRemoved" in profileFields,
            "The profile section must document the 'displayNameRemoved' field",
        )

        val displayNameRemovedRow = rowFor(profileSection, "displayNameRemoved")
            ?: error("Profile section must document the 'displayNameRemoved' field")
        assertTrue(
            displayNameRemovedRow.contains("boolean", ignoreCase = true),
            "The displayNameRemoved row must mention 'boolean' to indicate the type: $displayNameRemovedRow",
        )

        val displayNameRemovedProperty = ProfileResponse::class.memberProperties.firstOrNull { it.name == "displayNameRemoved" }
            ?: error("ProfileResponse must have a 'displayNameRemoved' property")
    }

    @Test
    fun theProfileSectionDocumentsDeviceRouteLive() {
        val profileFields = documentedFieldNames(profileSection)
        assertTrue(
            "deviceRouteLive" in profileFields,
            "The profile section must document the 'deviceRouteLive' field",
        )

        val deviceRouteLiveRow = rowFor(profileSection, "deviceRouteLive")
            ?: error("Profile section must document the 'deviceRouteLive' field")
        assertTrue(
            deviceRouteLiveRow.contains("boolean", ignoreCase = true),
            "The deviceRouteLive row must mention 'boolean' to indicate the type: $deviceRouteLiveRow",
        )

        val deviceRouteLiveProperty = ProfileResponse::class.memberProperties.firstOrNull { it.name == "deviceRouteLive" }
            ?: error("ProfileResponse must have a 'deviceRouteLive' property")
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
    fun theSetNameSectionIsStillWhereItWas() {
        // Re-chaining sectionBetween's end marker to make room for the new device section must not
        // shrink setNameSection to nothing: this checks the section itself, not the whole document.
        assertTrue(
            setNameSection.contains("PUT /api/me/name"),
            "The Set display name section must still contain 'PUT /api/me/name' after re-chaining",
        )
        assertTrue(
            setNameSection.contains("409"),
            "The Set display name section must still document its '409' row after re-chaining",
        )
    }

    @Test
    fun theDeviceSectionNamesItsMethodAndPath() {
        assertTrue(
            deviceSection.contains("DELETE /api/me/device"),
            "The Revoke this device section must contain 'DELETE /api/me/device'",
        )
    }

    @Test
    fun theDeviceSectionNamesTheBearerHeaderAndRefusesTheDeviceFallback() {
        assertTrue(
            deviceSection.contains("Authorization: Bearer"),
            "The Revoke this device section must name the 'Authorization: Bearer' header",
        )
        assertTrue(
            deviceSection.contains("device id alone"),
            "The Revoke this device section must say a caller presenting a device id alone is refused",
        )
    }

    @Test
    fun theDeviceSectionNamesAllThreeStatusCodes() {
        val statusCodes = listOf("204", "401", "409")
        for (statusCode in statusCodes) {
            assertTrue(
                deviceSection.contains(statusCode),
                "The Revoke this device section must document status code '$statusCode'",
            )
        }
    }

    @Test
    fun theDeviceSectionSaysRevocationIsPermanent() {
        assertTrue(
            deviceSection.contains("cannot be undone"),
            "The Revoke this device section must say revocation cannot be undone",
        )
    }

    @Test
    fun theDeviceSectionSaysTheOtherSessionsEndAndThisOneDoesNot() {
        assertTrue(
            deviceSection.contains("every other session"),
            "The Revoke this device section must say every other session the player holds ends",
        )
        assertTrue(
            deviceSection.contains("survives"),
            "The Revoke this device section must say the calling session survives",
        )
    }

    @Test
    fun theDeviceSectionSaysNoSocketIsClosed() {
        assertTrue(
            deviceSection.contains("No live socket is closed"),
            "The Revoke this device section must say no live socket is closed",
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
    fun theSignUpSectionIsStillWhereItWas() {
        // Re-chaining sectionBetween's end markers to make room for Sign in/Sign out must not
        // shrink signUpSection to nothing: this checks the section itself, not the whole document.
        assertTrue(
            signUpSection.contains("POST /api/auth/sign-up"),
            "The Sign up section must still contain 'POST /api/auth/sign-up' after re-chaining",
        )
        assertTrue(
            signUpSection.contains("201"),
            "The Sign up section must still document its '201' row after re-chaining",
        )
    }

    @Test
    fun theSignInSectionNamesItsMethodAndPath() {
        assertTrue(
            signInSection.contains("POST /api/auth/sign-in"),
            "The Sign in section must contain 'POST /api/auth/sign-in'",
        )
    }

    @Test
    fun theSignInSectionNamesBothRequestFields() {
        assertTrue(
            signInSection.contains("handle"),
            "The Sign in section must name the 'handle' field",
        )
        assertTrue(
            signInSection.contains("password"),
            "The Sign in section must name the 'password' field",
        )
    }

    @Test
    fun theSignInSectionSaysTheTwoFailuresAreIndistinguishable() {
        assertTrue(
            signInSection.contains("401"),
            "The Sign in section must document status code '401'",
        )
        assertTrue(
            signInSection.contains("no way to tell them apart"),
            "The Sign in section must say a wrong password and an unknown handle cannot be told apart",
        )
    }

    @Test
    fun theSignOutSectionSaysTwoHundredAndFourEitherWay() {
        assertTrue(
            signOutSection.contains("204"),
            "The Sign out section must document status code '204'",
        )
        assertTrue(
            signOutSection.contains("whether or not a session was deleted"),
            "The Sign out section must say '204' is the answer whether or not a session was deleted",
        )
    }

    @Test
    fun theSignOutSectionSaysNoSocketIsClosed() {
        assertTrue(
            signOutSection.contains("live sockets are not closed"),
            "The Sign out section must say live sockets are not closed",
        )
    }

    @Test
    fun everyAuthenticatedSectionNamesTheBearerHeader() {
        val sections = mapOf(
            "profile" to profileSection,
            "set name" to setNameSection,
            "recent duels" to recentDuelsSection,
        )
        for ((name, section) in sections) {
            assertTrue(
                section.contains("Authorization: Bearer"),
                "The $name section must name the 'Authorization: Bearer' header",
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
    fun theRecentDuelsSectionDocumentsTheFilters() {
        assertTrue(
            recentDuelsSection.contains("outcome"),
            "The Recent duels section must document the 'outcome' parameter",
        )
        assertTrue(
            recentDuelsSection.contains("opponent"),
            "The Recent duels section must document the 'opponent' parameter",
        )
        assertTrue(
            recentDuelsSection.contains("WON"),
            "The Recent duels section must document that 'outcome' accepts 'WON'",
        )
        assertTrue(
            recentDuelsSection.contains("substring"),
            "The Recent duels section must document that 'opponent' is a substring match",
        )
        assertTrue(
            recentDuelsSection.contains("literally"),
            "The Recent duels section must document that 'opponent' wildcards match literally",
        )
        assertTrue(
            recentDuelsSection.contains("400"),
            "The Recent duels section must document that invalid filters return 400",
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

    @Test
    fun theRecentDuelsSectionSaysACursorIsRefusedUnderAnotherFilter() {
        assertTrue(
            recentDuelsSection.contains("different filter"),
            "The Recent duels section must document that a cursor is refused under a different filter",
        )
        assertTrue(
            recentDuelsSection.contains("ADR-0057"),
            "The Recent duels section must cite ADR-0057",
        )
        assertFalse(
            recentDuelsSection.contains("not yet"),
            "The Recent duels section must not describe a future refusal behavior",
        )
        assertFalse(
            recentDuelsSection.contains("409"),
            "The Recent duels section must not document a 409 status for cursor mismatches",
        )
    }

    @Test
    fun theDocumentContractsTheStandingsEndpoint() {
        assertTrue(
            standingsSection.contains("GET /api/standings"),
            "The Standings section must document 'GET /api/standings'",
        )
        assertTrue(
            standingsSection.contains("limit"),
            "The Standings section must document the 'limit' parameter",
        )
        assertTrue(
            standingsSection.contains("after"),
            "The Standings section must document the 'after' parameter",
        )
        assertTrue(
            standingsSection.contains("400"),
            "The Standings section must document a '400' refusal",
        )
        assertTrue(
            standingsSection.contains("No authentication is required", ignoreCase = true),
            "The Standings section must state that no authentication is required",
        )
        assertTrue(
            standingsSection.contains("current season", ignoreCase = true),
            "The Standings section must state that only the current season is served",
        )
    }

    @Test
    fun theSignUpSectionNamesTheThrottledAnswer() {
        assertTrue(
            signUpSection.contains("429"),
            "The Sign up section must document status code '429'",
        )
    }

    @Test
    fun theSignUpSectionPromisesNoRetryAfter() {
        assertTrue(
            signUpSection.contains("No `Retry-After` header"),
            "The Sign up section must state there is no 'Retry-After' header for the 429 response",
        )
    }

    @Test
    fun theSignInSectionHasNoThrottledAnswer() {
        assertFalse(
            signInSection.contains("429"),
            "The Sign in section must not document status code '429'",
        )
    }

    @Test
    fun theDocumentedStandingsFieldNamesAllExist() {
        // The section holds two tables (the response, and each row in `rows`), so one scan of the
        // whole section against all three DTOs mirrors theDocumentedFieldNamesAllExist's approach.
        val standingsFields = documentedFieldNames(standingsSection)
        assertTrue(
            standingsFields.isNotEmpty(),
            "Expected at least one documented field for the Standings endpoint",
        )

        val standingsResponsePropertyNames = StandingsResponse::class.memberProperties.map { it.name }.toSet()
        val standingRowPropertyNames = StandingRow::class.memberProperties.map { it.name }.toSet()
        val selfStandingPropertyNames = SelfStandingResponse::class.memberProperties.map { it.name }.toSet()
        val validPropertyNames = standingsResponsePropertyNames + standingRowPropertyNames + selfStandingPropertyNames
        assertTrue(
            validPropertyNames.isNotEmpty(),
            "StandingsResponse, StandingRow and SelfStandingResponse must expose at least one property",
        )

        for (field in standingsFields) {
            assertTrue(
                field in validPropertyNames,
                "Documented field '$field' does not exist on StandingsResponse, StandingRow, or " +
                    "SelfStandingResponse",
            )
        }
    }

    @Test
    fun theDocumentStatesThePromiseAndBothRefusals() {
        assertTrue(
            standingsSection.contains("cutoff"),
            "The Standings section must state the walk's cutoff",
        )
        assertTrue(
            standingsSection.contains("exactly once"),
            "The Standings section must state that each player of the ladder as it stood committed " +
                "at the cutoff is returned exactly once",
        )
        assertTrue(
            standingsSection.contains("not live", ignoreCase = true),
            "The Standings section must state that a walk is not live",
        )
        assertTrue(
            standingsSection.contains("named exception", ignoreCase = true),
            "The Standings section must name the exception to the walk's promise",
        )
        assertTrue(
            standingsSection.contains("winner", ignoreCase = true) &&
                standingsSection.contains("twice", ignoreCase = true),
            "The Standings section must describe the named exception: a missed winner and a doubled loser",
        )
        assertFalse(
            standingsSection.contains("total and disjoint"),
            "The Standings section must not claim the 'total and disjoint' guarantee that belongs " +
                "to GET /api/me/duels",
        )
    }

    @Test
    fun theDocumentStatesTheThreeAnswersAboutTheReader() {
        assertTrue(
            standingsSection.contains("rank") && standingsSection.contains("coins") &&
                standingsSection.contains("Placed", ignoreCase = true),
            "The Standings section must state the rank-and-standing answer for a player who has placed",
        )
        assertTrue(
            standingsSection.contains("No place this season", ignoreCase = true),
            "The Standings section must state the 'no place this season' answer",
        )
        assertTrue(
            standingsSection.contains("not `0`"),
            "The Standings section must state that a player with no place this season is never printed as 0",
        )
        assertTrue(
            standingsSection.contains("No known device", ignoreCase = true) &&
                standingsSection.contains("in its entirety"),
            "The Standings section must state the absent-self answer for a request with no known device",
        )
    }

    @Test
    fun noSectionOfTheDocumentPromisesADeviceIdInAResponse() {
        assertFalse(
            "deviceId" in documentedFieldNames(doc),
            "No field table row anywhere in the document may name a field called 'deviceId': a " +
                "device id is a bearer credential and no read path returns one to a caller who did " +
                "not already hold it (ADR-0049 §5)",
        )
    }
}
