package duels.poker.server.mail

import duels.poker.server.auth.EmailAddress
import duels.poker.server.auth.RecoveryMailer
import duels.poker.server.auth.ResetToken
import duels.poker.server.auth.VerificationToken
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class NoRecoveryMailerTest {
    @Test
    fun bothMembersCompleteAndThrowNothing(): Unit = runBlocking {
        val mailer: RecoveryMailer = NoRecoveryMailer
        val address = EmailAddress("test@example.com")
        val verificationToken = VerificationToken("verification-token")
        val resetToken = ResetToken("reset-token")

        // Call both members
        mailer.sendVerification(address, verificationToken)
        mailer.sendPasswordReset(address, resetToken, "handle")

        // If we reach here, both calls completed without throwing
    }

    @Test
    fun itIsAnObjectAndNotAClass() {
        val objectInstance = NoRecoveryMailer::class.objectInstance
        assertNotNull(objectInstance, "objectInstance should not be null for an object declaration")
        assertSame(NoRecoveryMailer, objectInstance, "objectInstance should be the same reference as NoRecoveryMailer")

        // Verify it's assignable to RecoveryMailer
        val asMailer: RecoveryMailer = objectInstance
        assertNotNull(asMailer, "object should be assignable to RecoveryMailer type")
    }
}
