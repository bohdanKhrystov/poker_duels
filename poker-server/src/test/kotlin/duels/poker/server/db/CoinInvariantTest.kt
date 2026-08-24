package duels.poker.server.db

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

/**
 * Proves [assertCoinInvariantHolds] and [playerTableSnapshot] against a real schema, and proves
 * neither of `ADR-0030` §5's two properties subsumes the other:
 * [thePerPlayerCheckCatchesACoinMovedBetweenBalances] breaks P1 while leaving P2's two sums at
 * zero, and [theGlobalSumCatchesWhatThePerPlayerCheckCannot] breaks P2 while leaving P1's row
 * count at zero. Neither fixture is reachable through the other property, which is the whole
 * claim this file exists to check — see the ticket's `## Proof` for the mutation that confirms it.
 */
class CoinInvariantTest {
    private lateinit var dataSource: PGSimpleDataSource

    @BeforeEach
    fun setUp() {
        PostgresTestSupport.requireDocker()
        val coordinates = PostgresTestSupport.containerCoordinates()
        dataSource = PGSimpleDataSource().apply {
            setUrl(coordinates.url)
            user = coordinates.user
            password = coordinates.password
        }
        Migrations.migrate(dataSource)
    }

    @Test
    fun aCorrectLedgerPasses() {
        dataSource.writeFinishedDuel()

        // Without this positive control, the four failure tests below would pass against a
        // helper that threw unconditionally.
        assertDoesNotThrow { dataSource.assertCoinInvariantHolds("control") }
    }

    @Test
    fun anEmptyDatabasePasses() {
        // No players and no duels: both of ADR-0030 §5's statements COALESCE to 0, so a scenario
        // can assert the invariant before its first step.
        assertDoesNotThrow { dataSource.assertCoinInvariantHolds("empty") }
    }

    @Test
    fun thePerPlayerCheckCatchesACoinMovedBetweenBalances() {
        val fixture = dataSource.writeFinishedDuel()

        dataSource.moveACoinBetweenBalances(fixture.winner, fixture.loser)

        // The mutation only ever writes player.coin_balance, so duel_result's own sum is
        // untouched and the two balances were set to opposite values — P2's claim that it still
        // holds is checked here, not merely asserted by this test's name.
        val (playerBalanceSum, duelResultDeltaSum) = dataSource.coinInvariantP2Sums()
        assertEquals(0, playerBalanceSum)
        assertEquals(0, duelResultDeltaSum)

        assertThrows(IllegalStateException::class.java) {
            dataSource.assertCoinInvariantHolds("moved a coin")
        }
    }

    @Test
    fun theGlobalSumCatchesWhatThePerPlayerCheckCannot() {
        val fixture = dataSource.writeFinishedDuel()

        dataSource.deleteLosersDeltaAndZeroTheirBalance(fixture.duelId, fixture.loser)

        // The loser is now internally consistent — no duel_result row, and a balance of zero to
        // match — so P1's claim that it still holds is checked here, not merely asserted by this
        // test's name.
        assertEquals(emptyList<UUID>(), dataSource.p1BrokenBalancePlayerIds())

        assertThrows(IllegalStateException::class.java) {
            dataSource.assertCoinInvariantHolds("orphaned delta")
        }
    }

    @Test
    fun theFailureMessageNamesTheStep() {
        val fixture = dataSource.writeFinishedDuel()

        // Broken in P1's way, not P2's: ADR-0030 §5's P1 alone must catch this, or this test
        // would stop throwing at all if P1 were ever the property removed from the helper.
        dataSource.moveACoinBetweenBalances(fixture.winner, fixture.loser)

        val exception = assertThrows(IllegalStateException::class.java) {
            dataSource.assertCoinInvariantHolds("after sign-up")
        }

        // A scenario calling assertCoinInvariantHolds after every step is unusable if the
        // failure it reports does not say which step it was.
        assertTrue(exception.message!!.contains("after sign-up"))
    }

    @Test
    fun theSnapshotSeesEveryColumnAndDetectsAChangedOne() {
        val fixture = dataSource.writeFinishedDuel()

        val beforeChange = dataSource.playerTableSnapshot()
        dataSource.incrementBalanceBy(fixture.winner, 1)
        val afterChange = dataSource.playerTableSnapshot()
        // An equality that always fails would pass this half alone, so the no-op half below is
        // required too.
        assertNotEquals(beforeChange, afterChange)

        val beforeNoOp = dataSource.playerTableSnapshot()
        dataSource.updateThatMatchesNoRow()
        val afterNoOp = dataSource.playerTableSnapshot()
        // An equality that always passes would pass this half alone — only the pair rules both
        // useless implementations out.
        assertEquals(beforeNoOp, afterNoOp)
    }
}

/** The two players and the duel [writeFinishedDuel] wrote. */
private data class FinishedDuelFixture(val duelId: UUID, val winner: UUID, val loser: UUID)

/** Mints a fresh `player` row with a random id and a starting balance of `0`. */
private fun DataSource.insertPlayer(): UUID {
    val id = UUID.randomUUID()
    connection.use { connection ->
        connection.prepareStatement("INSERT INTO player (id, coin_balance) VALUES (?, 0)").use { statement ->
            statement.setObject(1, id)
            statement.executeUpdate()
        }
    }
    return id
}

/**
 * Writes one correct, finished duel from scratch: two fresh `player` rows, a `duel` row, two
 * `duel_result` rows of `+1` and `-1`, and the matching `coin_balance` update on both players —
 * mirroring exactly what [duels.poker.server.db.PostgresDuelResultStore.record] writes for a real
 * duel. A fixture writing only the winner's side would break P2 before anything under test runs,
 * which is why both players are always written together.
 *
 * @return the winner's id, the loser's id and the duel's id.
 */
private fun DataSource.writeFinishedDuel(): FinishedDuelFixture {
    val winner = insertPlayer()
    val loser = insertPlayer()
    val duelId = UUID.randomUUID()
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    connection.use { connection ->
        connection.prepareStatement(
            "INSERT INTO duel (id, format, started_at, finished_at, hands_played) VALUES (?, ?, ?, ?, ?)",
        ).use { statement ->
            statement.setObject(1, duelId)
            statement.setString(2, "heads-up-no-limit")
            statement.setObject(3, now)
            statement.setObject(4, now)
            statement.setInt(5, 1)
            statement.executeUpdate()
        }
        listOf(winner to 1, loser to -1).forEach { (player, delta) ->
            connection.prepareStatement(
                "INSERT INTO duel_result (duel_id, player_id, coin_delta) VALUES (?, ?, ?)",
            ).use { statement ->
                statement.setObject(1, duelId)
                statement.setObject(2, player)
                statement.setInt(3, delta)
                statement.executeUpdate()
            }
            connection.prepareStatement("UPDATE player SET coin_balance = coin_balance + ? WHERE id = ?")
                .use { statement ->
                    statement.setInt(1, delta)
                    statement.setObject(2, player)
                    statement.executeUpdate()
                }
        }
    }
    return FinishedDuelFixture(duelId, winner, loser)
}

/**
 * Sets [winner]'s balance to `2` and [loser]'s to `-2` directly, without touching `duel_result`:
 * both balances move away from their own deltas (P1 breaks for both players) while their sum
 * stays `0` (P2's two sums are untouched, since nothing was added to or removed from either
 * total) — the shape [CoinInvariantTest.thePerPlayerCheckCatchesACoinMovedBetweenBalances] and
 * [CoinInvariantTest.theFailureMessageNamesTheStep] both need.
 */
private fun DataSource.moveACoinBetweenBalances(winner: UUID, loser: UUID) {
    connection.use { connection ->
        connection.prepareStatement("UPDATE player SET coin_balance = 2 WHERE id = ?").use { statement ->
            statement.setObject(1, winner)
            statement.executeUpdate()
        }
        connection.prepareStatement("UPDATE player SET coin_balance = -2 WHERE id = ?").use { statement ->
            statement.setObject(1, loser)
            statement.executeUpdate()
        }
    }
}

/**
 * Deletes [loser]'s `duel_result` row for [duelId] and sets their `coin_balance` back to `0`,
 * both together: the loser becomes individually consistent (P1 holds) while the deleted delta
 * leaves both global sums off by the value it should have contributed (P2 breaks) — the shape
 * [CoinInvariantTest.theGlobalSumCatchesWhatThePerPlayerCheckCannot] needs.
 */
private fun DataSource.deleteLosersDeltaAndZeroTheirBalance(duelId: UUID, loser: UUID) {
    connection.use { connection ->
        connection.prepareStatement("DELETE FROM duel_result WHERE duel_id = ? AND player_id = ?").use { statement ->
            statement.setObject(1, duelId)
            statement.setObject(2, loser)
            statement.executeUpdate()
        }
        connection.prepareStatement("UPDATE player SET coin_balance = 0 WHERE id = ?").use { statement ->
            statement.setObject(1, loser)
            statement.executeUpdate()
        }
    }
}

/** Adds [amount] to [player]'s `coin_balance` directly — the change [playerTableSnapshot] must see. */
private fun DataSource.incrementBalanceBy(player: UUID, amount: Int) {
    connection.use { connection ->
        connection.prepareStatement("UPDATE player SET coin_balance = coin_balance + ? WHERE id = ?").use { statement ->
            statement.setInt(1, amount)
            statement.setObject(2, player)
            statement.executeUpdate()
        }
    }
}

/** An `UPDATE` that matches no row — a statement genuinely issued, but one that writes nothing. */
private fun DataSource.updateThatMatchesNoRow() {
    connection.use { connection ->
        connection.prepareStatement("UPDATE player SET coin_balance = coin_balance WHERE id = ?").use { statement ->
            statement.setObject(1, UUID.randomUUID())
            statement.executeUpdate()
        }
    }
}
