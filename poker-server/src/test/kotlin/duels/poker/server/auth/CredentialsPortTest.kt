package duels.poker.server.auth

import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.test.assertTrue
import kotlin.test.fail

class CredentialsPortTest {
    @Test
    fun verifyAnswersAPlayerIdOrNothing() {
        val function = Credentials::class.memberFunctions.first { it.name == "verify" }
        val returnType = function.returnType

        // Assert the return type has classifier PlayerId
        assertEquals(PlayerId::class, returnType.classifier)

        // Assert the return type is nullable
        assertTrue(returnType.isMarkedNullable, "verify must return PlayerId?")
    }

    @Test
    fun noFunctionOnThePortReturnsAString() {
        val allPublicFunctions = Credentials::class.memberFunctions
            .filter { it.name !in setOf("equals", "hashCode", "toString") }

        // Assert the sweep is non-empty by checking we found the expected functions
        val functionNames = allPublicFunctions.map { it.name }.toSet()
        assertEquals(
            setOf("verify", "create"),
            functionNames,
            "Expected exactly verify and create functions",
        )

        // Collect offenders that return String or ByteArray from functions
        val functionOffenders = allPublicFunctions.filter { function ->
            val returnType = function.returnType
            val classifier = returnType.classifier
            classifier == String::class || classifier == ByteArray::class
        }

        if (functionOffenders.isNotEmpty()) {
            val offendingNames = functionOffenders.map { it.name }
            fail("No function should return String or ByteArray, but found: $offendingNames")
        }

        // Also check properties
        val propertyOffenders = Credentials::class.memberProperties.filter { property ->
            val returnType = property.returnType
            val classifier = returnType.classifier
            classifier == String::class || classifier == ByteArray::class
        }

        if (propertyOffenders.isNotEmpty()) {
            val offendingNames = propertyOffenders.map { it.name }
            fail("No property should return String or ByteArray, but found: $offendingNames")
        }
    }

    @Test
    fun theCreateResultIsSealedAndHasExactlyTwoCases() {
        val sealedSubclasses = CreateCredentialResult::class.sealedSubclasses

        // Assert the exact set of names, not just the count
        val subclassNames = sealedSubclasses.map { it.simpleName }.toSet()
        assertEquals(setOf("Created", "IdentifierTaken"), subclassNames)

        // Also verify the count is exactly 2
        assertEquals(2, sealedSubclasses.size)
    }

    @Test
    fun aTestDoubleAnswersTheIdentityItWasBuiltWith() {
        val testDouble = TestDoubleCredentials(PlayerId("7"))

        runBlocking {
            val result = testDouble.verify(
                CredentialKind.PASSWORD,
                "test",
                PresentedSecret("secret"),
            )
            assertEquals(PlayerId("7"), result)
        }
    }

    @Test
    fun anotherKindIsStillConstructible() {
        val passkeyKind = CredentialKind("passkey")
        assertEquals("passkey", passkeyKind.value)

        val passwordKind = CredentialKind.PASSWORD
        assertEquals("password", passwordKind.value)
    }

    /**
     * A test double that returns a fixed PlayerId for any verify call.
     */
    private class TestDoubleCredentials(val playerId: PlayerId) : Credentials {
        override suspend fun verify(
            kind: CredentialKind,
            identifier: String,
            presented: PresentedSecret,
        ): PlayerId? = playerId

        override suspend fun create(
            playerId: PlayerId,
            kind: CredentialKind,
            identifier: String,
            secret: PresentedSecret,
        ): CreateCredentialResult = CreateCredentialResult.Created
    }
}
