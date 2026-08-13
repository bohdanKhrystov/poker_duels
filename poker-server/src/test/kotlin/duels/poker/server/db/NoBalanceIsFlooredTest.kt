package duels.poker.server.db

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * Plain text assertions on the source files that handle coin deltas and balance updates.
 *
 * These tests verify that balances may go negative and nothing in the write path floors them.
 * Per ADR-0014, a balance is wins − losses and may be negative; a player who loses their first
 * three duels sits at −3. Flooring it at zero would make a struggling player indistinguishable
 * from one who never played, which is the meaning the decision exists to create.
 */
class NoBalanceIsFlooredTest {
    private val coinDeltasFile = File("src/main/kotlin/duels/poker/server/duel/CoinDeltas.kt")
    private val storeFile = File("src/main/kotlin/duels/poker/server/db/PostgresDuelResultStore.kt")

    @Test
    fun theCoinRuleClampsNothing() {
        assertTrue(
            coinDeltasFile.exists(),
            "File does not exist at ${coinDeltasFile.absolutePath}",
        )
        assertTrue(
            coinDeltasFile.length() > 0,
            "File is empty at ${coinDeltasFile.absolutePath}",
        )
        val content = coinDeltasFile.readText()

        val forbiddenTokens = listOf("coerceAtLeast", "coerceIn", "maxOf", "Math.max", "absoluteValue", "UInt")
        forbiddenTokens.forEach { token ->
            assertTrue(
                !content.contains(token),
                "CoinDeltas.kt must not contain '$token' (ADR-0014: a balance is wins − losses and may be negative)",
            )
        }
    }

    @Test
    fun theStoreClampsNothing() {
        assertTrue(
            storeFile.exists(),
            "File does not exist at ${storeFile.absolutePath}",
        )
        assertTrue(
            storeFile.length() > 0,
            "File is empty at ${storeFile.absolutePath}",
        )
        val content = storeFile.readText()

        val forbiddenTokens = listOf("coerceAtLeast", "coerceIn", "maxOf", "Math.max", "absoluteValue", "UInt")
        forbiddenTokens.forEach { token ->
            assertTrue(
                !content.contains(token),
                "PostgresDuelResultStore.kt must not contain '$token' (ADR-0014: a balance is wins − losses and may be negative)",
            )
        }
    }

    @Test
    fun theStoreMovesBalancesWithAnSqlIncrement() {
        assertTrue(
            storeFile.exists(),
            "File does not exist at ${storeFile.absolutePath}",
        )
        assertTrue(
            storeFile.length() > 0,
            "File is empty at ${storeFile.absolutePath}",
        )
        val content = storeFile.readText()

        assertTrue(
            content.contains("coin_balance = coin_balance +"),
            "PostgresDuelResultStore.kt must contain 'coin_balance = coin_balance +' to pin the atomic SQL increment",
        )
    }
}
