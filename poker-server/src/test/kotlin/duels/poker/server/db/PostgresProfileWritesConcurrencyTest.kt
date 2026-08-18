package duels.poker.server.db

import duels.poker.server.http.ProfileWrites
import duels.poker.server.http.SetNameResult
import duels.poker.server.session.DeviceId
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `ADR-0029` §5's race, checked against `postgres:16-alpine`.
 *
 * `TASK-040111` covered the single `UPDATE` and two *unheld* races (both writers start at a gate
 * and run to completion normally). An unheld race cannot rule out that the loser simply ran after
 * the winner's transaction had already committed — the [SetNameResult] looks identical either
 * way, so it is no proof that blocking is what happened. [secondWriterBlocksThenLosesWhenTheHolderCommits]
 * and [secondWriterSucceedsWhenTheHolderRollsBack] close that gap: one writer holds an open,
 * uncommitted transaction that has already inserted the `name_registry` row for the name, and this
 * test does not resolve it until `pg_stat_activity` shows a second writer genuinely blocked on that
 * row — only then does the holder commit or roll back, and the second writer's outcome is asserted
 * against each.
 */
class PostgresProfileWritesConcurrencyTest {
    private lateinit var dataSource: DataSource
    private lateinit var directory: PostgresPlayerDirectory
    private lateinit var writes: ProfileWrites

    @BeforeEach
    fun setupDatabase() {
        PostgresTestSupport.requireDocker()
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        directory = PostgresPlayerDirectory(dataSource)
        writes = PostgresProfileWrites(dataSource)
    }

    @Test
    @Timeout(60)
    fun twoPlayersSendingOneNameProduceOneWinner() = runBlocking(Dispatchers.Default) {
        repeat(10) { round ->
            val name = "racer$round"
            val playerA = directory.resolve(DeviceId("round-$round-a"))
            val playerB = directory.resolve(DeviceId("round-$round-b"))

            val results = race(name, playerA.id, playerB.id).toList()

            assertEquals(1, results.count { it is SetNameResult.NameSet }, "round $round: NameSet count")
            assertEquals(1, results.count { it is SetNameResult.NameTaken }, "round $round: NameTaken count")
        }
    }

    @Test
    @Timeout(60)
    fun theLoserIsStillUnnamed() = runBlocking(Dispatchers.Default) {
        val playerA = directory.resolve(DeviceId("unnamed-check-a"))
        val playerB = directory.resolve(DeviceId("unnamed-check-b"))

        val (resultA, resultB) = race("contested", playerA.id, playerB.id)
        val loserId = loserOf(playerA.id to resultA, playerB.id to resultB)

        assertNull(displayNameOf(loserId))
    }

    @Test
    @Timeout(60)
    fun theLoserCanTakeAnotherNameImmediately() = runBlocking(Dispatchers.Default) {
        val playerA = directory.resolve(DeviceId("retry-check-a"))
        val playerB = directory.resolve(DeviceId("retry-check-b"))

        val (resultA, resultB) = race("firstcome", playerA.id, playerB.id)
        val loserId = loserOf(playerA.id to resultA, playerB.id to resultB)

        // "secondchoice" is held by nobody else in this test: this second call is what makes
        // "burns nothing" an assertion rather than a sentence.
        val retry = writes.setDisplayName(loserId, "secondchoice")

        assertTrue(retry is SetNameResult.NameSet)
        assertEquals("secondchoice", (retry as SetNameResult.NameSet).profile.displayName)
    }

    @Test
    @Timeout(60)
    fun theWinnersNameIsTheOneStored() = runBlocking(Dispatchers.Default) {
        val playerA = directory.resolve(DeviceId("stored-check-a"))
        val playerB = directory.resolve(DeviceId("stored-check-b"))

        val (resultA, resultB) = race("keepsake", playerA.id, playerB.id)
        val winnerId = winnerOf(playerA.id to resultA, playerB.id to resultB)

        assertEquals("keepsake", displayNameOf(winnerId))
        assertEquals(1, countPlayersNamed("keepsake"))
    }

    @Test
    @Timeout(60)
    fun secondWriterBlocksThenLosesWhenTheHolderCommits() = runBlocking(Dispatchers.Default) {
        val name = "heldthencommitted"
        val holder = directory.resolve(DeviceId("holder-commit"))
        val waiter = directory.resolve(DeviceId("waiter-commit"))

        dataSource.connection.use { holding ->
            holding.autoCommit = false
            val holdingPid = backendPidOf(holding)
            assertEquals(1, updateDisplayName(holding, holder.id, name))

            // Launched only now, against a transaction that is open and stays open until this
            // test says otherwise — not autocommitted the instant its own UPDATE returns, which
            // is all an unheld race could ever guarantee.
            val waiterResult = async { writes.setDisplayName(waiter.id, name) }
            awaitLockWaitOn(excludingPid = holdingPid)

            holding.commit()
            assertEquals(SetNameResult.NameTaken, waiterResult.await())
        }

        assertEquals(name, displayNameOf(holder.id))
        assertNull(displayNameOf(waiter.id))
    }

    @Test
    @Timeout(60)
    fun secondWriterSucceedsWhenTheHolderRollsBack() = runBlocking(Dispatchers.Default) {
        val name = "heldthenabandoned"
        val holder = directory.resolve(DeviceId("holder-rollback"))
        val waiter = directory.resolve(DeviceId("waiter-rollback"))

        dataSource.connection.use { holding ->
            holding.autoCommit = false
            val holdingPid = backendPidOf(holding)
            assertEquals(1, updateDisplayName(holding, holder.id, name))

            val waiterResult = async { writes.setDisplayName(waiter.id, name) }
            awaitLockWaitOn(excludingPid = holdingPid)

            holding.rollback()
            val result = waiterResult.await()
            assertTrue(result is SetNameResult.NameSet)
            assertEquals(name, (result as SetNameResult.NameSet).profile.displayName)
        }

        // A name held only by a transaction that never committed was never taken.
        assertNull(displayNameOf(holder.id))
        assertEquals(name, displayNameOf(waiter.id))
    }

    /**
     * Launches both writes on [Dispatchers.Default] behind a shared gate, so they are genuinely
     * concurrent rather than interleaved by the order this function calls them in.
     */
    private suspend fun race(
        name: String,
        playerA: PlayerId,
        playerB: PlayerId,
    ): Pair<SetNameResult, SetNameResult> = coroutineScope {
        val gate = CompletableDeferred<Unit>()
        val jobA =
            async(Dispatchers.Default) {
                gate.await()
                writes.setDisplayName(playerA, name)
            }
        val jobB =
            async(Dispatchers.Default) {
                gate.await()
                writes.setDisplayName(playerB, name)
            }
        gate.complete(Unit)
        jobA.await() to jobB.await()
    }

    private fun loserOf(a: Pair<PlayerId, SetNameResult>, b: Pair<PlayerId, SetNameResult>): PlayerId {
        return listOf(a, b).single { (_, result) -> result is SetNameResult.NameTaken }.first
    }

    private fun winnerOf(a: Pair<PlayerId, SetNameResult>, b: Pair<PlayerId, SetNameResult>): PlayerId {
        return listOf(a, b).single { (_, result) -> result is SetNameResult.NameSet }.first
    }

    /**
     * Polls `pg_stat_activity` until some backend other than [excludingPid] is waiting on a lock
     * while running the `name_registry` `INSERT` — proof that it is genuinely blocked on the held
     * transaction's uncommitted registry row, not merely slower to start. No fixed sleep: the loop
     * re-checks every 5ms and [withTimeout] fails the test outright if blocking is never observed.
     */
    private suspend fun awaitLockWaitOn(excludingPid: Int) {
        withTimeout(30_000) {
            while (!isBlockedOnNameWrite(excludingPid)) {
                delay(5)
            }
        }
    }

    private fun isBlockedOnNameWrite(excludingPid: Int): Boolean {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT count(*) FROM pg_stat_activity " +
                    "WHERE wait_event_type = 'Lock' AND pid <> ? AND query ILIKE 'INSERT INTO name_registry%'",
            ).use { statement ->
                statement.setInt(1, excludingPid)
                statement.executeQuery().use { rows ->
                    rows.next()
                    rows.getInt(1) > 0
                }
            }
        }
    }

    private fun backendPidOf(connection: Connection): Int {
        return connection.prepareStatement("SELECT pg_backend_pid()").use { statement ->
            statement.executeQuery().use { rows ->
                rows.next()
                rows.getInt(1)
            }
        }
    }

    /**
     * The same effect as `PostgresProfileWrites`'s statements, run under this test's own control:
     * the registry insert on [connection] first, so the row it leaves behind is part of the
     * caller's open transaction — the row the second writer then blocks on — before the `UPDATE`
     * whose row count this still returns.
     */
    private fun updateDisplayName(connection: Connection, playerId: PlayerId, name: String): Int {
        connection.prepareStatement(
            "INSERT INTO name_registry (name, reason) VALUES (?, 'TAKEN')",
        ).use { statement ->
            statement.setString(1, name)
            statement.executeUpdate()
        }
        return connection.prepareStatement(
            "UPDATE player SET display_name = ? WHERE id = ? AND display_name IS NULL",
        ).use { statement ->
            statement.setString(1, name)
            statement.setObject(2, UUID.fromString(playerId.value))
            statement.executeUpdate()
        }
    }

    private fun displayNameOf(playerId: PlayerId): String? {
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT display_name FROM player WHERE id = ?").use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.executeQuery().use { rows ->
                    check(rows.next()) { "no player row for $playerId" }
                    rows.getString("display_name")
                }
            }
        }
    }

    private fun countPlayersNamed(name: String): Int {
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT count(*) FROM player WHERE display_name = ?").use { statement ->
                statement.setString(1, name)
                statement.executeQuery().use { rows ->
                    rows.next()
                    rows.getInt(1)
                }
            }
        }
    }
}
