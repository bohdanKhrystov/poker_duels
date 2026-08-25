package duels.poker.server.auth

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class EmailAddressTest {
    @Test
    fun theValueIsTheAddressAsTyped() {
        val address = EmailAddress("Bob@Example.com")
        assertEquals("Bob@Example.com", address.value)
    }

    @Test
    fun printingOneRevealsNothing() {
        val address = EmailAddress("bob@example.com")
        val stringRepresentation = address.toString()
        val interpolated = "$address"

        // Both forms must equal the redaction
        assertEquals(EmailAddress.REDACTION, stringRepresentation)
        assertEquals(EmailAddress.REDACTION, interpolated)

        // Neither must contain parts of the address
        assertFalse(stringRepresentation.contains("bob"))
        assertFalse(stringRepresentation.contains("@"))
        assertFalse(stringRepresentation.contains("example.com"))
        assertFalse(interpolated.contains("bob"))
        assertFalse(interpolated.contains("@"))
        assertFalse(interpolated.contains("example.com"))
    }

    @Test
    fun twoDifferentAddressesPrintTheSameThing() {
        val address1 = EmailAddress("a@b.test")
        val address2 = EmailAddress("zzz@qqq.test")
        assertEquals(address1.toString(), address2.toString())
    }
}
