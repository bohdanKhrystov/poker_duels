package duels.poker.server.auth

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.kotlinFunction
import kotlin.reflect.jvm.kotlinProperty
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * `ADR-0027` §1: "no function anywhere returns a hash." This reflects over every public class the
 * compiler actually produced for `duels.poker.server.auth` and `duels.poker.server.db` and asserts
 * that claim structurally, so a future `fun storedHash(): String` fails the build rather than
 * relying on someone remembering the rule.
 */
class PublicApiHasNoHashTest {
    @Test
    fun noPublicMemberOfTheAuthOrDbPackagesReturnsAHash() {
        val offenders = sweptClasses().flatMap(::offendingMembersOf)

        if (offenders.isNotEmpty()) {
            fail(
                "Public members that read as a hash:\n" +
                    offenders.joinToString("\n") { "  ${it.owner}.${it.member.name}: ${it.member.returnType}" },
            )
        }
    }

    @Test
    fun theSweepSawTheApiItIsChecking() {
        val classes = sweptClasses()
        val classNames = classes.mapNotNull { it.simpleName }

        assertContains(classNames, "Credentials")
        assertContains(classNames, "CredentialKind")
        assertContains(classNames, "CreateCredentialResult")
        assertContains(classNames, "PresentedSecret")
        assertContains(classNames, "SessionToken")
        assertContains(classNames, "LoginHandleKt")
        assertContains(classNames, "PostgresCredentials")
        assertContains(classNames, "PostgresProfileReads")

        val memberNames = classes.flatMap(::publicMembersOf).map { it.name }
        assertContains(memberNames, "verify")
        assertContains(memberNames, "create")
        // `declaredMemberFunctions` alone silently returns none of a file facade's top-level
        // functions; finding this one by name is the proof that the sweep actually reaches them.
        assertContains(memberNames, "loginHandleOrNull")
    }

    @Test
    fun theSweepFlagsABaitThatReturnsAByteArray() {
        val offenders = offendingMembersOf(BaitReturningByteArray::class)

        assertTrue(offenders.isNotEmpty(), "expected the ByteArray-returning bait to be flagged, found: $offenders")
        assertContains(offenders.map { it.member.name }, "tagBytes")
    }

    @Test
    fun theSweepFlagsABaitNamedForAHash() {
        val offenders = offendingMembersOf(BaitNamedForAHash::class)

        assertTrue(offenders.isNotEmpty(), "expected the hash-named bait to be flagged, found: $offenders")
        assertContains(offenders.map { it.member.name }, "secretHash")
    }

    @Test
    fun aClassThatCannotBeLoadedFailsTheSweepNamingIt() {
        val bogusName = "duels.poker.server.auth.ThisClassWasNeverCompiled"

        val error = assertFailsWith<AssertionError> {
            loadKotlinClass(bogusName, Thread.currentThread().contextClassLoader)
        }

        assertContains(error.message.orEmpty(), bogusName)
    }
}

/** A public member whose return type is [ByteArray] — the return-type half of the rule must catch this. */
class BaitReturningByteArray {
    fun tagBytes(): ByteArray = ByteArray(0)
}

/** A public member whose name reads as a hash but whose return type is innocent — the name half must catch this. */
class BaitNamedForAHash {
    fun secretHash(): String = ""
}

private val SWEPT_PACKAGE_PATHS = listOf("duels/poker/server/auth", "duels/poker/server/db")

private val HASH_LIKE_WORDS = listOf("hash", "digest", "phc", "tag")

private data class Offender(val owner: String, val member: KCallable<*>)

private fun sweptClasses(): List<KClass<*>> = SWEPT_PACKAGE_PATHS.flatMap(::classesUnder)

/**
 * Every class the compiler emitted for [packagePath], loaded from `classes/kotlin/main` only —
 * the same resource name resolves in `classes/kotlin/test` too, which would silently fold this
 * file's own bait and probe classes into the sweep it is meant to be checking.
 */
private fun classesUnder(packagePath: String): List<KClass<*>> {
    val classLoader = Thread.currentThread().contextClassLoader
    val classDirs = classLoader.getResources(packagePath).toList()
        .filter { it.protocol == "file" }
        .map { File(it.toURI()) }
        .filter { it.path.contains("classes/kotlin/main") }

    val packageName = packagePath.replace('/', '.')
    return classDirs.flatMap { dir ->
        val classFiles = dir.listFiles { file -> file.isFile && file.extension == "class" }.orEmpty()
        classFiles.map { file ->
            val simpleFileName = file.name.removeSuffix(".class")
            loadKotlinClass("$packageName.$simpleFileName", classLoader)
        }
    }
}

/** `Class.forName`, converted to a [KClass] — never swallowed, so a class the sweep cannot even load fails by name. */
private fun loadKotlinClass(fullyQualifiedName: String, classLoader: ClassLoader): KClass<*> =
    try {
        Class.forName(fullyQualifiedName, false, classLoader).kotlin
    } catch (t: Throwable) {
        fail("The sweep could not load $fullyQualifiedName: ${t::class.simpleName}: ${t.message}")
    }

/**
 * The public functions and properties declared directly on [klass] — empty, deliberately, for a
 * non-public class, since `Argon2Hasher`, `Argon2Phc` and `SecretHasher` being `internal` is how
 * they stay out of this sweep entirely rather than needing their members individually excluded.
 */
private fun publicMembersOf(klass: KClass<*>): List<KCallable<*>> {
    if (klass.visibility != KVisibility.PUBLIC) return emptyList()

    val declared = try {
        val functions: Collection<KCallable<*>> = klass.declaredFunctions
        val properties: Collection<KCallable<*>> = klass.declaredMemberProperties
        functions + properties
    } catch (t: Throwable) {
        fail("The sweep could not reflect over ${klass.qualifiedName}: ${t::class.simpleName}: ${t.message}")
    }

    // `KClass.declaredFunctions`/`declaredMemberProperties` see nothing at all on a file facade
    // such as `LoginHandleKt` — kotlin-reflect does not model top-level declarations as members of
    // it. Bridging each JVM method/field back to Kotlin catches exactly that case, and only that
    // case: it runs solely when the class-based view found nothing to check.
    val members = if (declared.isEmpty()) {
        try {
            val fromMethods: List<KCallable<*>> = klass.java.declaredMethods.mapNotNull { it.kotlinFunction }
            val fromFields: List<KCallable<*>> = klass.java.declaredFields.mapNotNull { it.kotlinProperty }
            fromMethods + fromFields
        } catch (t: Throwable) {
            fail("The sweep could not reflect over ${klass.qualifiedName}: ${t::class.simpleName}: ${t.message}")
        }
    } else {
        declared
    }

    return members.filter { it.visibility == KVisibility.PUBLIC }
}

private fun offendingMembersOf(klass: KClass<*>): List<Offender> =
    publicMembersOf(klass)
        .filter(::offends)
        .map { Offender(klass.simpleName ?: klass.toString(), it) }

private fun offends(member: KCallable<*>): Boolean {
    // Every data class and every value class in these packages declares `hashCode(): Int` —
    // `Any`'s identity hash, not a credential hash. Excluding exactly that override keeps the
    // name-based half of the rule aimed at what it means to catch.
    if (isObjectIdentityHashCode(member)) return false

    val returnsByteArray = member.returnType.classifier == ByteArray::class
    val nameLooksHashLike = HASH_LIKE_WORDS.any { member.name.contains(it, ignoreCase = true) }
    return returnsByteArray || nameLooksHashLike
}

private fun isObjectIdentityHashCode(member: KCallable<*>): Boolean =
    member is KFunction<*> &&
        member.name == "hashCode" &&
        member.valueParameters.isEmpty() &&
        member.returnType.classifier == Int::class
