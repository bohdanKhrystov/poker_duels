package duels.poker.server.auth

import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.full.declaredMemberExtensionFunctions
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

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
 * Four more surfaces are asserted beyond that one, each through a named private helper below:
 * [KClass.declaredMemberProperties] ([theMailerDeclaresNoProperty]) — a
 * `val sendMarketing: suspend (EmailAddress) -> Unit` reads at a call site exactly like a third
 * function, and `declaredMemberFunctions` does not enumerate it at all;
 * [KClass.declaredMemberExtensionFunctions] ([theMailerDeclaresNoMemberExtension]) — a member
 * extension is a surface `declaredMemberFunctions` excludes by definition, not merely one nobody
 * had probed; [KClass.nestedClasses] ([theMailerHasNoNestedTypeAndNoCompanion]) — one read that
 * covers both a nested type and a companion object, since an unnamed `companion object` reports as
 * a nested class named `Companion`; and [KClass.supertypes] ([theMailerExtendsNothing]), compared
 * as the whole classifier list rather than by containment, since `Any` is present whether or not
 * `RecoveryMailer` extends a second interface that adds a third function — the largest of the
 * five, because *declared* excludes inherited members by definition.
 *
 * Three surfaces remain outside every read here, and no test in this file claims to see them. A
 * **top-level extension function** — `fun RecoveryMailer.sendNewsletter(address: EmailAddress)`,
 * declared in any file in any module — reads at a call site exactly like a member and is invisible
 * to every reflective read over `RecoveryMailer::class`, because it belongs to the file that
 * declares it, not to the interface. **What an implementation's body does** is invisible to every
 * shape test here: a `sendVerification` override that also posts to a marketing endpoint passes
 * all of them. **A second mail port declared elsewhere** — a wholly separate `interface
 * Newsletters` with its own transport — is a new file in a diff a reviewer reads, not a shape any
 * read over `RecoveryMailer::class` alone can see.
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

    /**
     * [KClass.declaredMemberProperties] of `RecoveryMailer::class` is empty. A
     * `val sendMarketing: suspend (EmailAddress) -> Unit` reads at a call site exactly like a
     * third function, and an implementer supplies the lambda — `declaredMemberFunctions` does not
     * enumerate it at all, so [theMailerDeclaresExactlyTwoMembers] cannot see it.
     */
    @Test
    fun theMailerDeclaresNoProperty() {
        assertEquals(emptySet<String>(), propertyNamesOf(RecoveryMailer::class))
    }

    /**
     * [KClass.declaredMemberExtensionFunctions] of `RecoveryMailer::class` is empty. A
     * `suspend fun EmailAddress.sendNewsletter(token: VerificationToken)` is a third declared
     * member that reads at a call site exactly like a real one, and `declaredMemberFunctions`
     * excludes member extensions by definition — [theMailerDeclaresExactlyTwoMembers] cannot see
     * it.
     */
    @Test
    fun theMailerDeclaresNoMemberExtension() {
        assertEquals(emptySet<String>(), memberExtensionNamesOf(RecoveryMailer::class))
    }

    /**
     * [KClass.nestedClasses] of `RecoveryMailer::class` is empty — one read that covers both a
     * nested type and a companion object, since Kotlin reflection reports an unnamed
     * `companion object` as a nested class named `Companion`. A companion member is callable as
     * `RecoveryMailer.sendNewsletter(...)`, which at a call site reads as the port itself sending
     * a third mail.
     */
    @Test
    fun theMailerHasNoNestedTypeAndNoCompanion() {
        assertEquals(emptySet<String>(), nestedClassNamesOf(RecoveryMailer::class))
    }

    /**
     * `RecoveryMailer::class.supertypes.map { it.classifier }` equals `listOf(Any::class)`. `Any`
     * is present in this list whether or not `RecoveryMailer` extends anything else, so the
     * comparison is the **whole list, in order**, rather than asking whether `Any::class` is a
     * member of it. `RecoveryMailer : Newsletters` fails this, and no `declared*` read can see it,
     * because *declared* excludes inherited members by definition.
     */
    @Test
    fun theMailerExtendsNothing() {
        assertEquals(listOf<KClassifier?>(Any::class), supertypeClassifiersOf(RecoveryMailer::class))
    }

    /**
     * The positive control for the four reads above: the same four helpers, run over
     * [ForbiddenShapesControl] instead of [RecoveryMailer], must find every shape they claim to
     * gate. Four assertions that a surface is empty pass identically whether the surface is
     * genuinely empty, the reflection call is broken, the helper ignores its `KClass` argument, or
     * the wrong type is read; running the same helpers against a type built to have all four
     * shapes at once is what rules that out.
     */
    @Test
    fun theFourReadsSeeTheShapesTheyClaimTo() {
        assertEquals(setOf("aProperty"), propertyNamesOf(ForbiddenShapesControl::class))
        assertEquals(setOf("aMemberExtension"), memberExtensionNamesOf(ForbiddenShapesControl::class))
        assertEquals(setOf("Nested", "Companion"), nestedClassNamesOf(ForbiddenShapesControl::class))
        assertNotEquals(
            listOf<KClassifier?>(Any::class),
            supertypeClassifiersOf(ForbiddenShapesControl::class),
        )
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

/**
 * Gives [ForbiddenShapesControl] a supertype other than `Any`, so
 * [theFourReadsSeeTheShapesTheyClaimTo]'s supertype assertion has something to find. A control
 * extending nothing would report `[kotlin.Any]` — identical to `RecoveryMailer` — on exactly the
 * surface it exists to probe.
 */
private interface ForbiddenShapesBase {
    fun inheritedMember()
}

/**
 * One type carrying all four forbidden shapes [RecoveryMailerShapeTest] gates against — a
 * property, a member extension, a nested type and a companion (through one read), and, via
 * [ForbiddenShapesBase], a supertype beyond `Any` — so
 * [theFourReadsSeeTheShapesTheyClaimTo] can prove the four helpers below read the `KClass`
 * argument they are given, rather than a fixed answer or nothing at all.
 *
 * `private` to this file, for the reason [ThreeMemberControl]'s KDoc gives: a private top-level
 * class is one JVM class, unique within the package, not a per-file facade the way a private
 * top-level function is. No other file under `duels.poker.server.auth` (main or test) declares a
 * top-level class, interface or object named `ForbiddenShapesControl` or `ForbiddenShapesBase`.
 */
private interface ForbiddenShapesControl : ForbiddenShapesBase {
    val aProperty: Int

    fun EmailAddress.aMemberExtension()

    interface Nested

    companion object
}

/** [KClass.declaredMemberFunctions], by name — the one reflection path both the real test and the control share. */
private fun memberNamesOf(klass: KClass<*>): Set<String> = klass.declaredMemberFunctions.map { it.name }.toSet()

/** [KClass.declaredMemberProperties], by name. */
private fun propertyNamesOf(klass: KClass<*>): Set<String> = klass.declaredMemberProperties.map { it.name }.toSet()

/**
 * [KClass.declaredMemberExtensionFunctions], by name — the surface
 * [KClass.declaredMemberFunctions] excludes by definition, not a shape nobody thought to probe.
 */
private fun memberExtensionNamesOf(klass: KClass<*>): Set<String> =
    klass.declaredMemberExtensionFunctions.map { it.name }.toSet()

/**
 * [KClass.nestedClasses], by simple name. One read that covers both a nested type and a companion
 * object: Kotlin reflection reports an unnamed `companion object` as a nested class named
 * `Companion`.
 */
private fun nestedClassNamesOf(klass: KClass<*>): Set<String> =
    klass.nestedClasses.mapNotNull { it.simpleName }.toSet()

/**
 * [KClass.supertypes], mapped to each supertype's classifier. `Any` is present in every
 * interface's supertype list whether or not it extends anything else, so a caller compares the
 * **whole list** rather than asking whether `Any::class` is a member of it.
 */
private fun supertypeClassifiersOf(klass: KClass<*>): List<KClassifier?> = klass.supertypes.map { it.classifier }
