package duels.poker.server.mail

import duels.poker.server.auth.EmailAddress
import duels.poker.server.auth.RecoveryMailer
import duels.poker.server.auth.ResetToken
import duels.poker.server.auth.VerificationToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test
import org.slf4j.helpers.NOPLogger
import kotlin.coroutines.coroutineContext
import kotlin.test.assertEquals

private const val ADDRESS = "duelist@example.test"
private const val VERIFICATION_TOKEN = "verify-9f86d081"
private const val RESET_TOKEN = "reset-4c3b2a19"
private const val HANDLE = "roundhouse"

/**
 * `ADR-0077` §7: this decorator is exercised on its own, with no HTTP and no
 * `kotlinx-coroutines-test` — this module does not carry it. Every test here is an ordering
 * assertion over one shared marker list rather than a wait: blocking the delegate on a
 * `CompletableDeferred` the test completes later would turn "forgot to detach" into a deadlock
 * instead of a red test, since nothing here has a timeout.
 */
internal class DetachedRecoveryMailerTest {
    @Test
    fun sendVerificationReturnsBeforeItsDeliveryRuns(): Unit = runBlocking {
        val markers = mutableListOf<String>()
        val delegate = RecordingRecoveryMailer(markers)
        val job = SupervisorJob(coroutineContext.job)
        val scope = CoroutineScope(coroutineContext + job)
        val mailer = DetachedRecoveryMailer(delegate, scope, NOPLogger.NOP_LOGGER)

        mailer.sendVerification(EmailAddress(ADDRESS), VerificationToken(VERIFICATION_TOKEN))
        markers += "returned"
        job.children.forEach { it.join() }
        scope.cancel()

        assertEquals(listOf("returned", "delivered"), markers)
    }

    @Test
    fun sendPasswordResetDetachesTheSameWay(): Unit = runBlocking {
        val markers = mutableListOf<String>()
        val delegate = RecordingRecoveryMailer(markers)
        val job = SupervisorJob(coroutineContext.job)
        val scope = CoroutineScope(coroutineContext + job)
        val mailer = DetachedRecoveryMailer(delegate, scope, NOPLogger.NOP_LOGGER)

        mailer.sendPasswordReset(EmailAddress(ADDRESS), ResetToken(RESET_TOKEN), HANDLE)
        markers += "returned"
        job.children.forEach { it.join() }
        scope.cancel()

        assertEquals(listOf("returned", "delivered"), markers)
    }

    @Test
    fun theDeliveryCarriesTheArgumentsItWasGiven(): Unit = runBlocking {
        val markers = mutableListOf<String>()
        val delegate = RecordingRecoveryMailer(markers)
        val job = SupervisorJob(coroutineContext.job)
        val scope = CoroutineScope(coroutineContext + job)
        val mailer = DetachedRecoveryMailer(delegate, scope, NOPLogger.NOP_LOGGER)

        mailer.sendPasswordReset(EmailAddress(ADDRESS), ResetToken(RESET_TOKEN), HANDLE)
        job.children.forEach { it.join() }
        scope.cancel()

        assertEquals(listOf(RecordedCall(ADDRESS, RESET_TOKEN, HANDLE, "sendPasswordReset")), delegate.calls)
    }
}

/**
 * One recorded call: [address], [token] and [handle] read off as plain strings — [handle] is
 * `null` for a [RecordingRecoveryMailer.sendVerification] call, which takes none — so a test can
 * compare against three independently chosen literals rather than the redacting `toString()` every
 * one of [EmailAddress], [VerificationToken] and [ResetToken] carries.
 */
private data class RecordedCall(val address: String, val token: String, val handle: String?, val member: String)

/**
 * The delegate every test in this file launches into: it [yield]s once — standing in for a
 * transport that actually suspends — then appends `"delivered"` to [markers] and records the call
 * it received. Never wrapped in a [DetachedRecoveryMailer] itself, so a test calling it directly
 * would see the un-detached order.
 */
private class RecordingRecoveryMailer(private val markers: MutableList<String>) : RecoveryMailer {
    val calls: MutableList<RecordedCall> = mutableListOf()

    override suspend fun sendVerification(address: EmailAddress, token: VerificationToken) {
        yield()
        calls += RecordedCall(address.value, token.value, handle = null, member = "sendVerification")
        markers += "delivered"
    }

    override suspend fun sendPasswordReset(address: EmailAddress, token: ResetToken, handle: String) {
        yield()
        calls += RecordedCall(address.value, token.value, handle, member = "sendPasswordReset")
        markers += "delivered"
    }
}
