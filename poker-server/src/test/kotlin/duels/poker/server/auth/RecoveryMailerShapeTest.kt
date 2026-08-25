package duels.poker.server.auth

import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.test.assertEquals

/**
 * `ADR-0031` §6.2: *"a test asserts the interface declares exactly these two members,
 * structurally over the public API."* This is that test, over `RecoveryMailer::class` directly,
 * so a third member — `sendNewsletter`, `sendDigest`, or a generic `send` — fails the build
 * rather than relying on someone remembering the rule.
 *
 * "Member" here means [KClass.declaredMemberFunctions]: the *Kotlin* reflection view
 * (`kotlin.reflect.full`) of the functions `RecoveryMailer` declares directly, not
 * `java.lang.Class.declaredMethods`. Three JVM artefacts are deliberately not part of the count:
 * - **The `suspend` `Continuation` parameter and the `Any?` return type.** The compiler rewrites
 *   every `suspend fun` into a JVM method with an extra trailing parameter and an erased return
 *   type; Kotlin reflection reports the original Kotlin-level signature — the real parameter list
 *   and the real `Unit` return type — because it reads the compiler's `@Metadata`, not the erased
 *   bytecode. Reading over Java reflection instead would make [neitherMailFunctionReturnsAnything]
 *   fail for a reason that has nothing to do with a mailer handing a result back to its caller.
 * - **`DefaultImpls`.** A Kotlin interface only grows one when a member has a body. Neither
 *   function here does, so none exists to exclude.
 * - **Inherited members**, such as `equals`, `hashCode` and `toString` from `Any`. `declared`
 *   member functions are exactly the ones `RecoveryMailer` itself introduces; `RecoveryMailer`
 *   extends nothing, so nothing is inherited into the count in the first place.
 *
 * [KClass.declaredMemberFunctions] also does not enumerate declared *properties* — a
 * `val newsletter: (EmailAddress) -> Unit` would not appear in it at all. `ADR-0031` §6.2 and this
 * ticket both specify `declaredMemberFunctions` by name, so that is the surface this test guards;
 * a property-shaped addition to `RecoveryMailer` is outside what this specific assertion can see.
 */
internal class RecoveryMailerShapeTest {
    @Test
    fun theMailerDeclaresExactlyTwoMembers() {
        assertEquals(
            setOf("sendVerification", "sendPasswordReset"),
            memberNamesOf(RecoveryMailer::class),
        )
    }

    /**
     * The positive control `ADR-0031` §6.2's mechanism needs: [memberNamesOf] run over a class it
     * was not written to already know the answer for. A helper that returned a hard-coded set of
     * names, or one that silently ignored its argument and always read `RecoveryMailer::class`,
     * would pass [theMailerDeclaresExactlyTwoMembers] and fail here — [ThreeMemberControl]
     * declares three members, and none of their names appears on `RecoveryMailer`.
     */
    @Test
    fun theReflectionSeesTheMembersItClaimsTo() {
        assertEquals(
            setOf("firstMember", "secondMember", "thirdMember"),
            memberNamesOf(ThreeMemberControl::class),
        )
    }

    @Test
    fun neitherMailFunctionReturnsAnything() {
        val members = RecoveryMailer::class.declaredMemberFunctions
        val sendVerification = members.single { it.name == "sendVerification" }
        val sendPasswordReset = members.single { it.name == "sendPasswordReset" }

        assertEquals(Unit::class, sendVerification.returnType.classifier)
        assertEquals(Unit::class, sendPasswordReset.returnType.classifier)
    }
}

/**
 * A three-member interface that exists only so [theReflectionSeesTheMembersItClaimsTo] can prove
 * [memberNamesOf] actually reads the `KClass` it is given, rather than a fixed answer.
 *
 * `private` to this file. A private top-level *class* is not scoped to the file the way a private
 * top-level function is — Kotlin backs a private top-level function with a per-file facade, but a
 * private top-level class or interface is still one JVM class named after it, unique within the
 * package. No other file under `duels.poker.server.auth` (main or test) declares a top-level
 * class, interface or object named `ThreeMemberControl`; a second one anywhere in this
 * compilation would be a redeclaration, not a shadow.
 */
private interface ThreeMemberControl {
    fun firstMember()

    fun secondMember()

    fun thirdMember()
}

/** [KClass.declaredMemberFunctions], by name — the one reflection path both the real test and the control share. */
private fun memberNamesOf(klass: KClass<*>): Set<String> = klass.declaredMemberFunctions.map { it.name }.toSet()
