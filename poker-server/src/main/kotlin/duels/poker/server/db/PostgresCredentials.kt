package duels.poker.server.db

import duels.poker.server.auth.CreateCredentialResult
import duels.poker.server.auth.CredentialKind
import duels.poker.server.auth.Credentials
import duels.poker.server.auth.PresentedSecret
import duels.poker.server.auth.passwordIsWithinTheWorkBound
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

/**
 * Implements [Credentials] against the `credential` table.
 *
 * [create] is the single `INSERT` `ADR-0027` describes. `SQLSTATE 23505` — the unique index on
 * `(kind, identifier)` — is translated into [CreateCredentialResult.IdentifierTaken], matched on
 * the code and never on the message; any other `SQLException` is rethrown unchanged rather than
 * swallowed, so a real failure still surfaces.
 *
 * [verify] is the single `SELECT` the port's contract requires. A matching row answers a
 * [PlayerId]; a wrong secret and an unknown identifier both answer `null`, indistinguishably
 * (`ADR-0027` §6). A missing row and a row whose `secret_hash` is `NULL` both run [hasher]'s
 * dummy verification against [DUMMY_PHC] before answering `null`, so the no-such-account path
 * costs one real Argon2 verification too — without that, Argon2 itself becomes the timing oracle
 * that tells a stranger whether an identifier exists. `secret_hash` is read here, compared, and
 * dropped: nothing on this class returns it, logs it, or puts it in an exception message.
 *
 * [verifyCurrent] answers a different question for a different caller: not *who is this*, but
 * *is this a player the caller already knows the correct password*, keyed by `(player_id, kind)`
 * rather than `(kind, identifier)`. It runs no dummy hash — see its own KDoc for why.
 *
 * [hasher] defaults to [Argon2Hasher] via the public constructor. The primary constructor takes
 * it explicitly and is `internal` because [SecretHasher] is — a public constructor cannot expose
 * an internal type — which is also the seam `TASK-040313` uses to inject one that counts calls.
 */
public class PostgresCredentials internal constructor(
    private val dataSource: DataSource,
    private val hasher: SecretHasher,
) : Credentials {
    public constructor(dataSource: DataSource) : this(dataSource, Argon2Hasher())

    override suspend fun verify(
        kind: CredentialKind,
        identifier: String,
        presented: PresentedSecret,
    ): PlayerId? =
        withContext(Dispatchers.IO) {
            val row = dataSource.connection.use { connection -> selectCredentialRow(connection, kind, identifier) }
            answerFor(row, presented)
        }

    private fun selectCredentialRow(connection: Connection, kind: CredentialKind, identifier: String): CredentialRow? =
        connection.prepareStatement(VERIFY_SQL).use { statement ->
            statement.setString(1, kind.value)
            statement.setString(2, identifier)
            statement.executeQuery().use { rows -> if (rows.next()) rows.toCredentialRow() else null }
        }

    private suspend fun answerFor(row: CredentialRow?, presented: PresentedSecret): PlayerId? {
        val secretHash = row?.secretHash
        if (row == null || secretHash == null) {
            // No row, or a row whose secret_hash is still NULL: nothing to compare against, but
            // the no-such-account path must cost what the found-and-checked path costs. This
            // discards the result on purpose — the dummy exists to spend time, not to answer.
            hasher.matches(presented, DUMMY_PHC)
            return null
        }
        return if (hasher.matches(presented, secretHash)) PlayerId(row.playerId) else null
    }

    /**
     * Reads `secret_hash` by `(player_id, kind)` and compares it to [presented] with [hasher].
     *
     * **No dummy hash, deliberately.** [verify]'s dummy verification against [DUMMY_PHC] exists
     * because its caller is a stranger who must not learn whether an identifier exists at all.
     * This caller has already presented a valid session naming [playerId], so there is nothing
     * left to enumerate: a player who holds no [kind] credential already knows that about
     * themselves. Answering `false` the instant no row is found is therefore correct, not a
     * shortcut — adding the dummy back "for consistency" would spend one Argon2 verification on
     * every wrong-password `403` for no defensive purpose.
     *
     * [passwordIsWithinTheWorkBound] is applied to [presented] before [hasher] ever sees it,
     * exactly as sign-in applies it: an unbounded secret reaching Argon2 here is the same denial
     * of service through a different door.
     */
    override suspend fun verifyCurrent(
        playerId: PlayerId,
        kind: CredentialKind,
        presented: PresentedSecret,
    ): Boolean =
        withContext(Dispatchers.IO) {
            if (!passwordIsWithinTheWorkBound(presented)) {
                return@withContext false
            }
            val secretHash =
                dataSource.connection.use { connection -> selectCurrentSecretHash(connection, playerId, kind) }
            secretHash != null && hasher.matches(presented, secretHash)
        }

    private fun selectCurrentSecretHash(connection: Connection, playerId: PlayerId, kind: CredentialKind): String? =
        connection.prepareStatement(VERIFY_CURRENT_SQL).use { statement ->
            statement.setObject(1, UUID.fromString(playerId.value))
            statement.setString(2, kind.value)
            statement.executeQuery().use { rows -> if (rows.next()) rows.getString("secret_hash") else null }
        }

    override suspend fun create(
        playerId: PlayerId,
        kind: CredentialKind,
        identifier: String,
        secret: PresentedSecret,
    ): CreateCredentialResult =
        withContext(Dispatchers.IO) {
            val secretHash = hasher.hash(secret)
            try {
                dataSource.connection.use { connection ->
                    insertCredential(connection, playerId, kind, identifier, secretHash)
                }
            } catch (failure: SQLException) {
                // 23505 is unique_violation on the (kind, identifier) unique index: this pair is
                // already held by some row. Any other SQLState is a real failure this port has no
                // answer for, so it is rethrown unchanged rather than swallowed.
                if (failure.sqlState == UNIQUE_VIOLATION_SQLSTATE) CreateCredentialResult.IdentifierTaken else throw failure
            }
        }

    override suspend fun holdsCredential(
        playerId: PlayerId,
        kind: CredentialKind,
    ): Boolean =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                connection.prepareStatement(HOLDS_CREDENTIAL_SQL).use { statement ->
                    statement.setObject(1, UUID.fromString(playerId.value))
                    statement.setString(2, kind.value)
                    statement.executeQuery().use { rows ->
                        rows.next()
                        rows.getBoolean(1)
                    }
                }
            }
        }

    private fun insertCredential(
        connection: Connection,
        playerId: PlayerId,
        kind: CredentialKind,
        identifier: String,
        secretHash: String,
    ): CreateCredentialResult {
        connection.prepareStatement(CREATE_SQL).use { statement ->
            statement.setObject(1, UUID.randomUUID())
            statement.setObject(2, UUID.fromString(playerId.value))
            statement.setString(3, kind.value)
            statement.setString(4, identifier)
            statement.setString(5, secretHash)
            statement.executeUpdate()
        }
        return CreateCredentialResult.Created
    }

    private data class CredentialRow(val playerId: String, val secretHash: String?)

    private fun ResultSet.toCredentialRow(): CredentialRow =
        CredentialRow(getString("player_id"), getString("secret_hash"))

    private companion object {
        private const val UNIQUE_VIOLATION_SQLSTATE = "23505"

        // The hash of nothing: 48 random bytes, kept by nobody. Well-formed so it parses and
        // costs one real Argon2 verification (ADR-0027 §6) rather than none — a malformed
        // constant would make `matches` return false immediately, and the no-such-account path
        // would answer in microseconds instead of the ~50 ms a real lookup costs, exactly the
        // timing oracle this constant exists to remove. `$` opens a template in a Kotlin string
        // literal, so every one below is escaped.
        private const val DUMMY_PHC =
            "\$argon2id\$v=19\$m=19456,t=2,p=1\$DOkn/CUOc626AxtmisFtpA\$cXtRt8rAT9o79X3bZokMdJtapvk9646A/0v+Mls0dE4"

        private const val VERIFY_SQL =
            "SELECT player_id, secret_hash FROM credential WHERE kind = ? AND identifier = ?"

        private const val VERIFY_CURRENT_SQL =
            "SELECT secret_hash FROM credential WHERE player_id = ? AND kind = ?"

        private const val CREATE_SQL =
            "INSERT INTO credential (id, player_id, kind, identifier, secret_hash) VALUES (?, ?, ?, ?, ?)"

        private const val HOLDS_CREDENTIAL_SQL =
            "SELECT EXISTS (SELECT 1 FROM credential WHERE player_id = ? AND kind = ?)"
    }
}
