package duels.poker.server.mail

import duels.poker.server.auth.EmailAddress
import duels.poker.server.auth.RecoveryMailer
import duels.poker.server.auth.ResetToken
import duels.poker.server.auth.VerificationToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.Logger

/**
 * A [RecoveryMailer] decorator that detaches each send onto [scope] and returns before the send
 * finishes.
 *
 * `ADR-0077` §2 makes detachment a property of the wiring rather than of a route handler: this is
 * the one place a recovery mail is `launch`ed, so no route file holds a [CoroutineScope], a
 * `launch` or a `Job`. Whoever builds this decorator decides whether it is applied at all —
 * [NoRecoveryMailer] composes underneath it exactly as a configured transport would.
 *
 * Both members of [RecoveryMailer] return `Unit`, so neither this class nor its caller ever learns
 * whether [delegate] ran, let alone whether it succeeded: there is no near side of the port to
 * answer that on. A failed delivery is therefore handled entirely here, per `ADR-0077` §4:
 * [CancellationException] always rethrows, and every other [Throwable] is caught and logged once,
 * by member name and failure class only. `ADR-0077` §5 is why nothing here retries, queues or
 * compensates a failure.
 *
 * @param delegate The mailer whose sends this launches instead of awaiting.
 * @param scope The scope each send is launched into. This class receives it and constructs none —
 *   building it and ending its lifetime is the wiring's concern.
 * @param log Where a failed delivery is logged.
 */
public class DetachedRecoveryMailer(
    private val delegate: RecoveryMailer,
    private val scope: CoroutineScope,
    private val log: Logger,
) : RecoveryMailer {
    override suspend fun sendVerification(address: EmailAddress, token: VerificationToken) {
        detach("sendVerification") { delegate.sendVerification(address, token) }
    }

    override suspend fun sendPasswordReset(address: EmailAddress, token: ResetToken, handle: String) {
        detach("sendPasswordReset") { delegate.sendPasswordReset(address, token, handle) }
    }

    /**
     * Launches [send] into [scope] and returns immediately, joining and awaiting nothing.
     *
     * The one `try`/`catch` shared by both members above: [CancellationException] always
     * rethrows, and every other failure is logged once against [member], per `ADR-0077` §4.
     */
    private fun detach(member: String, send: suspend () -> Unit) {
        scope.launch {
            try {
                send()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                log.error("$member failed: ${failure::class.simpleName}")
            }
        }
    }
}
