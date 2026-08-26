package duels.poker.server.mail

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Tests for [RecoveryLinks], the builder for mailed recovery links.
 *
 * Every URL this system will ever mail is built in one function, from configuration, and nothing
 * under `poker-server/src/main` reads a `Host` header. `ADR-0081` makes both links fragment
 * routes, so the origin cannot come from a request — not from `Host`, not from
 * `X-Forwarded-Host`, not from anything a caller controls.
 */
internal class RecoveryLinksTest {
    @Test
    fun aResetLinkIsTheConfiguredOriginAndAFragment() {
        val links = RecoveryLinks("https://duels.test")
        assertEquals("https://duels.test/#/reset/abc123", links.reset("abc123"))
    }

    @Test
    fun aVerificationLinkIsTheConfiguredOriginAndAFragment() {
        val links = RecoveryLinks("https://duels.test")
        assertEquals("https://duels.test/#/verify/abc123", links.verification("abc123"))
    }

    @Test
    fun twoOriginsProduceTwoLinks() {
        val token = "token-xyz"
        val linksA = RecoveryLinks("https://a.test")
        val linksB = RecoveryLinks("https://b.test")

        val resetA = linksA.reset(token)
        val resetB = linksB.reset(token)

        assertEquals("https://a.test/#/reset/token-xyz", resetA)
        assertEquals("https://b.test/#/reset/token-xyz", resetB)
        assertTrue(resetA.contains("https://a.test"), "origin a not found in $resetA")
        assertTrue(resetB.contains("https://b.test"), "origin b not found in $resetB")
    }

    @Test
    fun neitherLinkCarriesTheTokenInAQueryString() {
        val links = RecoveryLinks("https://duels.test")
        val resetLink = links.reset("token-value")
        val verifyLink = links.verification("token-value")

        assertTrue(!resetLink.contains("?"), "reset link contains a query string: $resetLink")
        assertTrue(!verifyLink.contains("?"), "verify link contains a query string: $verifyLink")

        val resetHashIndex = resetLink.indexOf("#")
        val verifyHashIndex = verifyLink.indexOf("#")

        assertTrue(resetHashIndex >= 0, "reset link has no fragment: $resetLink")
        assertTrue(verifyHashIndex >= 0, "verify link has no fragment: $verifyLink")

        val tokenIndex = resetLink.indexOf("token-value")
        assertTrue(resetHashIndex < tokenIndex, "in reset link, token comes before #: $resetLink")

        val verifyTokenIndex = verifyLink.indexOf("token-value")
        assertTrue(verifyHashIndex < verifyTokenIndex, "in verify link, token comes before #: $verifyLink")
    }

    @Test
    fun theTokenIsPassedThroughUnchanged() {
        val links = RecoveryLinks("https://duels.test")
        val tokenWithSpecialChars = "abc-def_123"

        val resetLink = links.reset(tokenWithSpecialChars)
        val verifyLink = links.verification(tokenWithSpecialChars)

        assertTrue(resetLink.contains("abc-def_123"), "reset link did not preserve token: $resetLink")
        assertTrue(verifyLink.contains("abc-def_123"), "verify link did not preserve token: $verifyLink")
        assertTrue(!resetLink.contains("%"), "reset link URL-encoded the token: $resetLink")
        assertTrue(!verifyLink.contains("%"), "verify link URL-encoded the token: $verifyLink")
    }

    @Test
    fun noMainSourceFileReadsAHostHeader() {
        // Find the repository root by looking for a marker file.
        val repositoryRoot = generateSequence(File("").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "docs/protocol.md").isFile }
            ?: error("repository root not found above ${File("").absolutePath}")

        val mainKotlin = File(repositoryRoot, "poker-server/src/main/kotlin")
        assertTrue(mainKotlin.isDirectory, "main kotlin directory not found at ${mainKotlin.absolutePath}")

        // Find all Kotlin files in the main source tree.
        val kotlinFiles = mainKotlin.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".kt") }
            .toList()

        assertTrue(kotlinFiles.isNotEmpty(), "expected to find Kotlin files in ${mainKotlin.absolutePath}")

        val forbiddenTokens = listOf("X-Forwarded-Host", "\"Host\"")
        val fileContents = kotlinFiles.associate { it.absolutePath to it.readText() }

        for ((filePath, content) in fileContents) {
            for (token in forbiddenTokens) {
                assertTrue(
                    !content.contains(token),
                    "file $filePath contains forbidden header reference: $token",
                )
            }
        }
    }
}
