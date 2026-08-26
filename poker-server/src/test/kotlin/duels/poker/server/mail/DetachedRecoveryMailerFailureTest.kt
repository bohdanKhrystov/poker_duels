package duels.poker.server.mail

import duels.poker.server.auth.EmailAddress
import duels.poker.server.auth.RecoveryMailer
import duels.poker.server.auth.ResetToken
import duels.poker.server.auth.VerificationToken
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.helpers.LegacyAbstractLogger
import org.slf4j.helpers.MessageFormatter
import kotlin.coroutines.coroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val ADDRESS = "duelist-041631@example.test"
private const val VERIFICATION_TOKEN = "verify-041631-b7f2"
private const val FAILURE_MESSAGE = "transport-refused-041631-do-not-log-me"

/**
 * `ADR-0077` §4's two refusals — the line names a class and nothing else, and there is no line at
 * all for a success or a cancellation — get their own file, the same reason `TASK-041602` gave: a
 * refusal is its own test and its own argument, not a spare assertion at the bottom of a file about
 * what the thing does.
 *
 * Built on the same scope and join-then-cancel ordering as [DetachedRecoveryMailerTest], and for the
 * same reason: `kotlinx-coroutines-test` is not on this module's classpath, so there is no
 * `runTest`. [SupervisorJob] does not complete on its own once its children finish — every test
 * here joins the coroutine it launched (or [cancelAndJoin]s the scope's own job) before the scope
 * is cancelled, never after, so `runBlocking` cannot hang waiting on a subtree nothing ended.
 */
internal class DetachedRecoveryMailerFailureTest {
    @Test
    fun aFailureLeavesTheScopeAliveAndASecondSendStillArrives(): Unit = runBlocking {
        val log = RecordingLogger()
        val delegate = ThrowingRecoveryMailer(IllegalStateException(FAILURE_MESSAGE))
        val job = SupervisorJob(coroutineContext.job)
        val scope = CoroutineScope(coroutineContext + job)
        val mailer = DetachedRecoveryMailer(delegate, scope, log)

        mailer.sendVerification(EmailAddress(ADDRESS), VerificationToken(VERIFICATION_TOKEN))
        job.children.forEach { it.join() }

        assertFalse(job.isCancelled, "a failed send must not cancel the scope's job")
        assertFalse(job.isCompleted, "a failed send must not complete the scope's job")
        assertEquals(1, delegate.calls.size)

        mailer.sendVerification(EmailAddress(ADDRESS), VerificationToken(VERIFICATION_TOKEN))
        job.children.forEach { it.join() }

        assertEquals(2, delegate.calls.size, "a second send on the same scope must still reach the delegate")

        scope.cancel()
    }

    @Test
    fun theFailureIsLoggedOnceAndTheLineNamesTheMemberAndTheExceptionClass(): Unit = runBlocking {
        val log = RecordingLogger()
        val delegate = ThrowingRecoveryMailer(IllegalStateException(FAILURE_MESSAGE))
        val job = SupervisorJob(coroutineContext.job)
        val scope = CoroutineScope(coroutineContext + job)
        val mailer = DetachedRecoveryMailer(delegate, scope, log)

        mailer.sendVerification(EmailAddress(ADDRESS), VerificationToken(VERIFICATION_TOKEN))
        job.children.forEach { it.join() }
        scope.cancel()

        assertEquals(1, log.events.size)
        val message = log.events.single().message
        assertTrue(message.contains("sendVerification"), "the line must name the member that failed")
        assertTrue(message.contains("IllegalStateException"), "the line must name the exception class")
    }

    @Test
    fun theFailureLineCarriesNoAddressNoTokenNoMessageAndNoThrowable(): Unit = runBlocking {
        val log = RecordingLogger()
        val delegate = ThrowingRecoveryMailer(IllegalStateException(FAILURE_MESSAGE))
        val job = SupervisorJob(coroutineContext.job)
        val scope = CoroutineScope(coroutineContext + job)
        val mailer = DetachedRecoveryMailer(delegate, scope, log)

        mailer.sendVerification(EmailAddress(ADDRESS), VerificationToken(VERIFICATION_TOKEN))
        job.children.forEach { it.join() }
        scope.cancel()

        val event = log.events.single()
        assertFalse(event.message.contains(ADDRESS), "the address must not appear in the log line")
        assertFalse(event.message.contains(VERIFICATION_TOKEN), "the token must not appear in the log line")
        assertFalse(event.message.contains(FAILURE_MESSAGE), "the exception message must not appear in the log line")
        assertNull(event.throwable, "no throwable should ever be attached to the log call")
    }

    @Test
    fun nothingIsLoggedWhenASendSucceeds(): Unit = runBlocking {
        val log = RecordingLogger()
        val job = SupervisorJob(coroutineContext.job)
        val scope = CoroutineScope(coroutineContext + job)
        val mailer = DetachedRecoveryMailer(SucceedingRecoveryMailer, scope, log)

        mailer.sendVerification(EmailAddress(ADDRESS), VerificationToken(VERIFICATION_TOKEN))
        job.children.forEach { it.join() }
        scope.cancel()

        assertEquals(0, log.events.size)
    }

    @Test
    fun aCancelledSendIsNotLoggedAsAFailure(): Unit = runBlocking {
        val log = RecordingLogger()
        val delegate = SuspendingRecoveryMailer()
        val job = SupervisorJob(coroutineContext.job)
        val scope = CoroutineScope(coroutineContext + job)
        val mailer = DetachedRecoveryMailer(delegate, scope, log)

        mailer.sendVerification(EmailAddress(ADDRESS), VerificationToken(VERIFICATION_TOKEN))
        delegate.started.await()
        job.cancelAndJoin()

        assertEquals(0, log.events.size)
    }

    @Test
    fun aFailedSendIsNotRetried(): Unit = runBlocking {
        val log = RecordingLogger()
        val delegate = ThrowingRecoveryMailer(IllegalStateException(FAILURE_MESSAGE))
        val job = SupervisorJob(coroutineContext.job)
        val scope = CoroutineScope(coroutineContext + job)
        val mailer = DetachedRecoveryMailer(delegate, scope, log)

        mailer.sendVerification(EmailAddress(ADDRESS), VerificationToken(VERIFICATION_TOKEN))
        job.children.forEach { it.join() }
        scope.cancel()

        assertEquals(1, delegate.calls.size)
    }
}

/**
 * One recorded call: the formatted [message] SLF4J would render, and the [throwable] argument
 * passed alongside it. `logback-classic`'s `ListAppender` is not on this module's test compile
 * classpath, so this pair — read out of [LegacyAbstractLogger]'s own normalized call — is what lets
 * a test assert the throwable is `null` separately from what the rendered string says.
 */
private data class LoggedEvent(val message: String, val throwable: Throwable?)

/**
 * The smallest [org.slf4j.Logger] that exposes both halves of a call: [LegacyAbstractLogger]
 * normalizes every `error`/`warn`/… overload into one call carrying the raw pattern, the
 * substitution arguments and the throwable (already split out of a trailing-`Throwable` argument),
 * so [handleNormalizedLoggingCall] renders the pattern itself via [MessageFormatter] and records the
 * two halves as a [LoggedEvent].
 */
private class RecordingLogger : LegacyAbstractLogger() {
    val events: MutableList<LoggedEvent> = mutableListOf()

    override fun getFullyQualifiedCallerName(): String? = null

    override fun handleNormalizedLoggingCall(
        level: Level,
        marker: Marker?,
        messagePattern: String,
        arguments: Array<Any>?,
        throwable: Throwable?,
    ) {
        val message = MessageFormatter.arrayFormat(messagePattern, arguments).message
        events += LoggedEvent(message, throwable)
    }

    override fun isTraceEnabled(): Boolean = true

    override fun isDebugEnabled(): Boolean = true

    override fun isInfoEnabled(): Boolean = true

    override fun isWarnEnabled(): Boolean = true

    override fun isErrorEnabled(): Boolean = true
}

/**
 * A [RecoveryMailer] whose members always throw [failure], recording each call it received so a
 * test can assert both how many times the delegate ran and — via a [RecordingLogger] — what got
 * logged about it.
 */
private class ThrowingRecoveryMailer(private val failure: Throwable) : RecoveryMailer {
    val calls: MutableList<String> = mutableListOf()

    override suspend fun sendVerification(address: EmailAddress, token: VerificationToken) {
        calls += "sendVerification"
        throw failure
    }

    override suspend fun sendPasswordReset(address: EmailAddress, token: ResetToken, handle: String) {
        calls += "sendPasswordReset"
        throw failure
    }
}

/** A [RecoveryMailer] whose members return normally, standing in for a delivery that succeeds. */
private object SucceedingRecoveryMailer : RecoveryMailer {
    override suspend fun sendVerification(address: EmailAddress, token: VerificationToken) {}

    override suspend fun sendPasswordReset(address: EmailAddress, token: ResetToken, handle: String) {}
}

/**
 * A [RecoveryMailer] whose members complete [started] and then suspend until cancelled, standing in
 * for a transport that is genuinely mid-flight when the caller's scope is torn down. A test awaits
 * [started] before cancelling, so the delegate is provably suspended rather than merely scheduled.
 */
private class SuspendingRecoveryMailer : RecoveryMailer {
    val started: CompletableDeferred<Unit> = CompletableDeferred()

    override suspend fun sendVerification(address: EmailAddress, token: VerificationToken) {
        started.complete(Unit)
        awaitCancellation()
    }

    override suspend fun sendPasswordReset(address: EmailAddress, token: ResetToken, handle: String) {
        started.complete(Unit)
        awaitCancellation()
    }
}
