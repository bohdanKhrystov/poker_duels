package duels.poker.server.http

import duels.poker.server.protocol.http.SignUpRequest
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SignUpFieldsTest {
    @Test
    fun aGoodHandleAndPasswordAreAccepted() {
        val result = signUpFieldsOf(SignUpRequest("bob", "hunter2222"))

        assertEquals(SignUpFields.Accepted("bob"), result)
    }

    @Test
    fun theAcceptedHandleIsTheFoldedOne() {
        // Two distinct fold outcomes, neither already lower-case: a passthrough that returns
        // request.handle as typed, and a hardcoded constant, both fail here.
        val first = signUpFieldsOf(SignUpRequest("Bob_1", "hunter2222"))
        val second = signUpFieldsOf(SignUpRequest("SHOUT", "hunter2222"))

        assertEquals(SignUpFields.Accepted("bob_1"), first)
        assertEquals(SignUpFields.Accepted("shout"), second)
    }

    @Test
    fun aHandleTooShortIsFourHundred() {
        val result = signUpFieldsOf(SignUpRequest("ab", "hunter2222"))

        assertEquals(SignUpFields.Refused(HttpStatusCode.BadRequest), result)
    }

    @Test
    fun aHandleStartingWithAPunctuationCharacterIsFourHundred() {
        // A second refused handle, of a different shape than the length failure above, so the
        // 400 is not pinned by one way of failing to fold.
        val result = signUpFieldsOf(SignUpRequest("_alice", "hunter2222"))

        assertEquals(SignUpFields.Refused(HttpStatusCode.BadRequest), result)
    }

    @Test
    fun aPasswordOfSevenCodePointsIsFourTwoTwo() {
        val result = signUpFieldsOf(SignUpRequest("bob", "1234567"))

        assertEquals(SignUpFields.Refused(HttpStatusCode.UnprocessableEntity), result)
    }

    @Test
    fun aPasswordOfOneHundredAndTwentyNineCodePointsIsFourTwoTwo() {
        val result = signUpFieldsOf(SignUpRequest("bob", "a".repeat(129)))

        // The identical status as the short-password refusal: one rule, one code for both bounds.
        assertEquals(SignUpFields.Refused(HttpStatusCode.UnprocessableEntity), result)
    }

    @Test
    fun aPasswordOfEightCodePointsIsAccepted() {
        val result = signUpFieldsOf(SignUpRequest("bob", "12345678"))

        assertEquals(SignUpFields.Accepted("bob"), result)
    }

    @Test
    fun aPasswordOfOneHundredAndTwentyEightCodePointsIsAccepted() {
        // The upper boundary accepts too, so the rule is "at most 128", not "fewer than 128".
        val result = signUpFieldsOf(SignUpRequest("bob", "a".repeat(128)))

        assertEquals(SignUpFields.Accepted("bob"), result)
    }

    @Test
    fun theHandleIsJudgedBeforeThePassword() {
        // Both fields are bad here: "ab" fails the handle rule and "short" fails the password
        // rule. An implementation that checks the password first would answer 422; the handle
        // must be judged first, so this answers 400.
        val result = signUpFieldsOf(SignUpRequest("ab", "short"))

        assertEquals(SignUpFields.Refused(HttpStatusCode.BadRequest), result)
    }
}
