package duels.poker.server.db

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

/**
 * `ADR-0030` §2: *"After `EPIC-04` there are exactly three statements that write `player`"*, of
 * which exactly one — the `INSERT INTO player` inside [PostgresPlayerDirectory] — creates a row.
 * This scans every file under `poker-server/src/main/kotlin` for that statement's literal text and
 * asserts the file set it finds is exactly `{PostgresPlayerDirectory.kt}`, so a write path added a
 * year from now, by someone who never read that ADR, fails the build the day it adds a second one.
 *
 * `TASK-040706`'s scenario and `IdentityMovesNoCoinTest.everyApiPathInTheRouteSourcesIsExercisedByTheScenario`
 * both fall short of this claim. A minted row has `coin_balance` `0` and no `duel_result` rows, so
 * the coin invariant holds for an orphan profile and the scenario's assertions never notice one.
 * The route enumeration sees a new **route**; an orphan-minting statement reachable from an
 * existing route is a new **statement** behind unchanged routing, and neither gate reads statements.
 *
 * Two honest limits:
 * 1. It reads source text, not the compiled statement set. A statement **assembled from constants**,
 *    or with a line break between `INSERT INTO` and the table name, escapes it. Every insert in the
 *    repository today writes both words together on one line, and this test is the reason to keep
 *    doing so.
 * 2. It is a **file-set assertion**. A second `INSERT INTO player` added inside
 *    `PostgresPlayerDirectory.kt` escapes it. That is deliberate: minting is that class's job, and
 *    the defect this guards against is a new write path somewhere else.
 *
 * Needs no database: [mainSourceFilesContaining] reads source text alone, so this class never calls
 * `PostgresTestSupport.requireDocker()` and opens no `DataSource`.
 */
internal class ProfileCreationIsOneStatementTest {
    @Test
    fun onlyThePlayerDirectoryCreatesAProfile() {
        assertEquals(
            setOf("PostgresPlayerDirectory.kt"),
            mainSourceFilesContaining("INSERT INTO player"),
            "expected only PostgresPlayerDirectory.kt to contain the literal text \"INSERT INTO player\"",
        )
    }

    /**
     * The vacuity guard [onlyThePlayerDirectoryCreatesAProfile] cannot supply alone: two different
     * statements, over the same helper, with two different non-empty expected file sets. A scan
     * that matched nothing would satisfy an empty-versus-empty comparison in the test above, and a
     * scan that ignored its argument and always answered `{PostgresPlayerDirectory.kt}` would pass
     * it too — this test fails either way, because `duel_result` names a different file.
     */
    @Test
    fun theScanTellsTwoStatementsApart() {
        assertEquals(
            setOf("PostgresDuelResultStore.kt"),
            mainSourceFilesContaining("INSERT INTO duel_result"),
            "expected only PostgresDuelResultStore.kt to contain the literal text \"INSERT INTO duel_result\"",
        )
    }
}

/**
 * Finds `poker-server/src/main/kotlin` by walking upward from [File]`("")`'s absolute path — the
 * same technique `apiPathLiteralsInRouteSources` in `IdentityMovesNoCoinTest` uses to find the
 * `http` package, so this does not depend on whether Gradle's test working directory is the module
 * root or the repository root — then walks every file beneath the directory it finds, keeps the
 * ones whose name ends `.kt`, and returns the file names, not the full paths, of those whose text
 * `contains` [statement].
 *
 * File names, not paths, and a set, not a count: a count would be a magic number stale on the next
 * refactor, and would be tripped by `PostgresPlayerDirectory.kt`'s own KDoc, which quotes
 * `INSERT INTO player` in prose, legitimately, to explain that very statement.
 */
private fun mainSourceFilesContaining(statement: String): Set<String> {
    val mainSourceDirectory = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "poker-server/src/main/kotlin") }
        .firstOrNull { it.isDirectory }
        ?: error("poker-server/src/main/kotlin not found above ${File("").absolutePath}")
    return mainSourceDirectory.walkTopDown()
        .filter { it.isFile && it.name.endsWith(".kt") }
        .filter { it.readText().contains(statement) }
        .map { it.name }
        .toSet()
}
