package duels.poker.server.auth

import duels.poker.server.session.PlayerId
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `ADR-0082` §2: a login handle is read only from a proven email address, never from a
 * `PlayerId`. `Credentials.verifyCurrent`'s merged KDoc refused the reverse direction for months;
 * this test makes that refusal a build failure.
 *
 * No member of `Credentials` declares a return type whose classifier is `String` or `String?`.
 * The gate is green against the code as merged—the four members return `PlayerId?`, `Boolean`,
 * `CreateCredentialResult`, `Boolean`—and turns red exactly on `Credentials.handleOf(playerId:
 * PlayerId): String?`, the signature `ADR-0082` names.
 *
 * **Limits**: a handle read added to some other type passes this gate; reflection reports a @JvmInline return type as the wrapper. Both remain review matters; the sentence a reviewer
 * applies is `ADR-0082` §2's: *the only read in this system that produces a login handle takes a
 * proven `EmailAddress`.* Properties are also outside what `declaredMemberFunctions` can see.
 */
internal class CredentialsHasNoHandleReadTest {
    @Test
    fun credentialsDeclaresNoMemberReturningAString() {
        assertEquals(
            emptySet(),
            stringReturningMemberNamesOf(Credentials::class),
        )
    }

    /**
     * The positive control `ADR-0082` §2's mechanism needs: [stringReturningMemberNamesOf] run
     * over a class it was not written to already know the answer for. A helper that returned a
     * hard-coded empty set, or one that silently ignored its argument and always read
     * `Credentials::class`, would pass [credentialsDeclaresNoMemberReturningAString] and fail
     * here — [HandleReadingControl] declares two members returning `String`, and both of their
     * names appear on neither `Credentials`. Also, this asserts the set of four names, not a
     * count: a helper that answered an empty list would satisfy *no member returns a `String`*
     * vacuously, forever.
     */
    @Test
    fun theSweepSeesTheFourMembersItIsChecking() {
        assertEquals(
            setOf("verify", "verifyCurrent", "create", "holdsCredential"),
            Credentials::class.declaredMemberFunctions.map { it.name }.toSet(),
        )
    }

    /**
     * The same [stringReturningMemberNamesOf] helper applied to [HandleReadingControl], which
     * declares a member returning `String?`. This tests that the predicate catches nullable `String`
     * as well—that is, `returnType.classifier == String::class` rather than `returnType ==
     * typeOf<String>()`.
     */
    @Test
    fun theSweepFlagsABaitReturningANullableString() {
        val result = stringReturningMemberNamesOf(HandleReadingControl::class)
        assertTrue("handleOf" in result)
    }

    /**
     * The same [stringReturningMemberNamesOf] helper applied to [HandleReadingControl], which
     * also declares a member returning non-nullable `String`. This tests that the predicate
     * catches both nullability flavors, so neither `String` nor `String?` escapes.
     */
    @Test
    fun theSweepFlagsABaitReturningANonNullString() {
        val result = stringReturningMemberNamesOf(HandleReadingControl::class)
        assertTrue("identifierOf" in result)
    }
}

/**
 * A two-member control that exists only so [CredentialsHasNoHandleReadTest.theSweepFlagsABaitReturningANullableString]
 * and [CredentialsHasNoHandleReadTest.theSweepFlagsABaitReturningANonNullString] can prove
 * [stringReturningMemberNamesOf] actually reads the `KClass` it is given, rather than a fixed answer.
 *
 * `private` to this file. A private top-level *class* is not scoped to the file the way a private
 * top-level function is — Kotlin backs a private top-level function with a per-file facade, but a
 * private top-level class or interface is still one JVM class named after it, unique within the
 * package. No other file under `duels.poker.server.auth` (main or test) declares a top-level
 * class, interface or object named `HandleReadingControl`; a second one anywhere in this
 * compilation would be a redeclaration, not a shadow.
 */
private interface HandleReadingControl {
    fun handleOf(playerId: PlayerId): String?

    fun identifierOf(playerId: PlayerId): String
}

/**
 * [KClass.declaredMemberFunctions] whose `returnType.classifier == String::class`, by name — the
 * one reflection path both the real test and the controls share. `returnType.classifier` discards
 * nullability, so one comparison catches `String` and `String?` both; an equality against
 * `typeOf<String>()` would miss `handleOf(playerId): String?`, which is the exact signature this
 * gate exists for.
 */
private fun stringReturningMemberNamesOf(klass: KClass<*>): Set<String> =
    klass.declaredMemberFunctions
        .filter { it.returnType.classifier == String::class }
        .map { it.name }
        .toSet()
