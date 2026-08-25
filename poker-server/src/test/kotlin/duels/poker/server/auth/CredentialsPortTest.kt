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
    fun theHoldsQueryAnswersANonNullBoolean() {
        val function = Credentials::class.memberFunctions.first { it.name == "holdsCredential" }
        val returnType = function.returnType

        // Assert the return type has classifier Boolean
        assertEquals(Boolean::class, returnType.classifier)

        // Assert the return type is not nullable
        assertTrue(!returnType.isMarkedNullable, "holdsCredential must return Boolean, not Boolean?")
    }

    @Test
    fun noFunctionOnThePortReturnsAString() {
        val allPublicFunctions = Credentials::class.memberFunctions
            .filter { it.name !in setOf("equals", "hashCode", "toString") }

        // Assert the sweep is non-empty by checking we found the expected functions
        val functionNames = allPublicFunctions.map { it.name }.toSet()
        assertEquals(
            setOf("verify", "verifyCurrent", "create", "holdsCredential"),
            functionNames,
            "Expected exactly verify, verifyCurrent, create, and holdsCredential functions",
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
    fun aTestDoubleAnswersWhetherItHoldsOne() {
        val testDoubleWithTrue = TestDoubleCredentials(PlayerId("7"), holds = true)
        val testDoubleWithFalse = TestDoubleCredentials(PlayerId("8"), holds = false)

        runBlocking {
            val resultTrue = testDoubleWithTrue.holdsCredential(
                PlayerId("7"),
                CredentialKind.PASSWORD,
            )
            assertEquals(true, resultTrue)

            val resultFalse = testDoubleWithFalse.holdsCredential(
                PlayerId("8"),
                CredentialKind.PASSWORD,
            )
            assertEquals(false, resultFalse)
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
    private class TestDoubleCredentials(
        val playerId: PlayerId,
        val holds: Boolean = false,
    ) : Credentials {
        override suspend fun verify(
            kind: CredentialKind,
            identifier: String,
            presented: PresentedSecret,
        ): PlayerId? = playerId

        override suspend fun verifyCurrent(
            playerId: PlayerId,
            kind: CredentialKind,
            presented: PresentedSecret,
        ): Boolean = true

        override suspend fun create(
            playerId: PlayerId,
            kind: CredentialKind,
            identifier: String,
            secret: PresentedSecret,
        ): CreateCredentialResult = CreateCredentialResult.Created

        override suspend fun holdsCredential(
            playerId: PlayerId,
            kind: CredentialKind,
        ): Boolean = holds
    }
}
