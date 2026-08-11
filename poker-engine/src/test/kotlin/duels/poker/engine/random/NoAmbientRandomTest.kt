package duels.poker.engine.random

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class NoAmbientRandomTest {

    @Test
    fun engineProductionSourcesNameNoGlobalRandomSource() {
        // Text scan catches forbidden fragments anywhere they appear: in code, in KDoc examples,
        // or in commented-out lines. This is intentional — ambient randomness rules slip through
        // when developers can hide it in a comment "for now" or document it as a hypothetical.
        val sourceDir = File("src/main/kotlin")
        val forbiddenFragments = listOf("kotlin.random", "java.util.Random", "Math.random", "Random(")

        val violations = mutableListOf<String>()

        sourceDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".kt") }
            .forEach { file ->
                file.readLines().forEachIndexed { index, line ->
                    forbiddenFragments.forEach { fragment ->
                        if (line.contains(fragment)) {
                            violations.add("${file.path}:${index + 1}: contains '$fragment'")
                        }
                    }
                }
            }

        assertFalse(
            violations.isNotEmpty(),
            buildString {
                appendLine("Engine production sources must not reference global random sources:")
                violations.forEach { appendLine("  $it") }
            },
        )
    }

    @Test
    fun theScannedSourceTreeIsFound() {
        val sourceDir = File("src/main/kotlin")

        assertTrue(sourceDir.exists(), "src/main/kotlin directory not found")
        assertTrue(
            sourceDir.isDirectory,
            "src/main/kotlin is not a directory",
        )

        val kotlinFiles = sourceDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".kt") }
            .toList()

        assertTrue(
            kotlinFiles.isNotEmpty(),
            "No .kt files found in src/main/kotlin; the scan would be vacuous",
        )
    }
}
