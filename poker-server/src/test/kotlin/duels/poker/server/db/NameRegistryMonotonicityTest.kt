package duels.poker.server.db

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Exercises `name_registry_monotone` (`V5`) directly with raw SQL against the table, never through
 * `retire_display_name` (`TASK-041012`) or `player` (no test here writes to it). `ADR-0051` §3: a
 * `TAKEN` or `RETIRED` row can never be deleted, no `name` may ever be rewritten, the only permitted
 * update is `TAKEN -> RETIRED`, and a `BLOCKED` row stays deletable.
 */
class NameRegistryMonotonicityTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
    }

    @Test
    fun aTakenNameCannotBeDeleted() {
        insertName("Ann", "TAKEN")

        val exception = assertFailsWith<SQLException> { deleteName("Ann") }

        assertEquals("23001", exception.sqlState)
        val row = readRow("Ann")
        assertEquals("Ann", row?.name)
        assertEquals("TAKEN", row?.reason)
    }

    @Test
    fun aRetiredNameCannotBeDeleted() {
        insertName("Ann", "TAKEN")
        promoteToRetired("Ann")

        val exception = assertFailsWith<SQLException> { deleteName("Ann") }

        assertEquals("23001", exception.sqlState)
        val row = readRow("Ann")
        assertEquals("Ann", row?.name)
        assertEquals("RETIRED", row?.reason)
    }

    @Test
    fun aBlockedNameCanBeDeleted() {
        insertName("Slur", "BLOCKED")

        deleteName("Slur")

        assertNull(readRow("Slur"))
    }

    /**
     * `name_registry_is_monotone`'s UPDATE branch checks only `name` and `reason`; it never
     * references `created_at` or `retired_from`. This test asserts what *this* statement, which
     * never mentions those columns, happens to leave alone, not a guarantee the trigger enforces.
     * An UPDATE that also set `created_at` would succeed unchecked.
     */
    @Test
    fun takenBecomesRetiredAndTheNameAndCreatedAtAreUntouched() {
        insertName("Ann", "TAKEN")
        val before = readRow("Ann")

        promoteToRetired("Ann")

        val after = readRow("Ann")
        assertEquals("RETIRED", after?.reason)
        assertEquals(before?.name, after?.name)
        assertEquals(before?.createdAt, after?.createdAt)
        assertEquals(before?.retiredFrom, after?.retiredFrom)
    }

    @Test
    fun retiredCannotGoBackToTaken() {
        insertName("Ann", "TAKEN")
        promoteToRetired("Ann")

        val exception = assertFailsWith<SQLException> { updateReason("Ann", "TAKEN") }

        assertEquals("23001", exception.sqlState)
        val row = readRow("Ann")
        assertEquals("Ann", row?.name)
        assertEquals("RETIRED", row?.reason)
    }

    @Test
    fun aRegisteredNameCannotBeRewritten() {
        insertName("Ann", "TAKEN")

        val exception = assertFailsWith<SQLException> { renameName("Ann", "Anne") }

        assertEquals("23001", exception.sqlState)
        val row = readRow("Ann")
        assertEquals("Ann", row?.name)
        assertEquals("TAKEN", row?.reason)
        assertNull(readRow("Anne"))
    }

    @Test
    fun aRenameSmuggledIntoARetirementIsRefused() {
        insertName("Ann", "TAKEN")

        val exception = assertFailsWith<SQLException> { renameAndRetire("Ann", "Anne") }

        assertEquals("23001", exception.sqlState)
        val row = readRow("Ann")
        assertEquals("Ann", row?.name)
        assertEquals("TAKEN", row?.reason)
    }

    /**
     * Leaves `retired_from` NULL: this file writes nothing to `player`, and any other value would
     * need a row there to satisfy the foreign key. The trigger does not examine the column at all,
     * so NULL still exercises the permitted TAKEN -> RETIRED transition; a real `retired_from` is
     * `retire_display_name`'s concern (`TASK-041012`).
     */
    private fun promoteToRetired(name: String) = updateReason(name, "RETIRED")

    private fun insertName(name: String, reason: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO name_registry (name, reason) VALUES (?, ?)",
            ).use { statement ->
                statement.setString(1, name)
                statement.setString(2, reason)
                statement.executeUpdate()
            }
        }
    }

    private fun deleteName(name: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "DELETE FROM name_registry WHERE name = ?",
            ).use { statement ->
                statement.setString(1, name)
                statement.executeUpdate()
            }
        }
    }

    private fun updateReason(name: String, reason: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE name_registry SET reason = ? WHERE name = ?",
            ).use { statement ->
                statement.setString(1, reason)
                statement.setString(2, name)
                statement.executeUpdate()
            }
        }
    }

    private fun renameName(oldName: String, newName: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE name_registry SET name = ? WHERE name = ?",
            ).use { statement ->
                statement.setString(1, newName)
                statement.setString(2, oldName)
                statement.executeUpdate()
            }
        }
    }

    /**
     * The statement that mutation (b), which drops `NEW.name <> OLD.name` from the UPDATE guard,
     * would permit: a rename riding along with the one legal reason transition. `retired_from` is
     * left out of the SET list for the same reason every other helper leaves it out: this file
     * writes nothing to `player`.
     */
    private fun renameAndRetire(oldName: String, newName: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE name_registry SET name = ?, reason = 'RETIRED' WHERE name = ?",
            ).use { statement ->
                statement.setString(1, newName)
                statement.setString(2, oldName)
                statement.executeUpdate()
            }
        }
    }

    private fun readRow(name: String): RegistryRow? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT name, reason, retired_from, created_at FROM name_registry WHERE name = ?",
            ).use { statement ->
                statement.setString(1, name)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        return RegistryRow(
                            name = resultSet.getString("name"),
                            reason = resultSet.getString("reason"),
                            retiredFrom = resultSet.getObject("retired_from", UUID::class.java),
                            createdAt = resultSet.getTimestamp("created_at").toInstant(),
                        )
                    }
                }
            }
        }
        return null
    }

    private data class RegistryRow(
        val name: String,
        val reason: String,
        val retiredFrom: UUID?,
        val createdAt: Instant,
    )
}
