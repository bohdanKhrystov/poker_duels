package duels.poker.server.http

import duels.poker.server.auth.PresentedSecret
import duels.poker.server.auth.loginHandleOrNull
import duels.poker.server.auth.passwordIsLongEnough
import duels.poker.server.auth.passwordIsWithinTheWorkBound
import duels.poker.server.protocol.http.SignUpRequest
import io.ktor.http.HttpStatusCode

/**
 * The verdict on a decoded [SignUpRequest]'s handle and password: either both passed and the
 * caller has the folded handle to store, or one of them failed and the caller has the one status
 * that reports it.
 */
internal sealed interface SignUpFields {
    /** Both fields passed their rules. [handle] is the folded form, never [SignUpRequest.handle]. */
    data class Accepted(val handle: String) : SignUpFields

    /** A field failed its rule. [status] is the response the caller must send, with no body. */
    data class Refused(val status: HttpStatusCode) : SignUpFields
}

/**
 * Judges [request]'s handle and password against the sign-up rules, in the fixed order the rules
 * must run in, and returns either the folded handle or the one status that refuses the request.
 *
 * The order is part of the contract, not an implementation detail:
 * 1. [loginHandleOrNull] on `request.handle` — `null` refuses with `400` (`ADR-0031` §1's fold,
 *    `ADR-0048` §6's `400` row). The handle is judged first, so a request with a bad handle
 *    answers `400` no matter what its password looks like.
 * 2. [passwordIsLongEnough] and [passwordIsWithinTheWorkBound] on `request.password` — either
 *    being `false` refuses with `422`. **One status for both bounds**: the body is empty and there
 *    is exactly one rule to state ("the password's length is wrong"), so nothing on the wire needs
 *    to distinguish too-short from too-long. `422`, not `400`, because `ADR-0031` §5 already spends
 *    `422` on exactly this refusal for `reset-password`, and one meaning per code beats two paths
 *    for one refusal — neither code is an oracle for which field misbehaved, both merely state a
 *    property of the caller's own input.
 * 3. otherwise, `Accepted` with the folded handle from step 1 — never `request.handle` — because
 *    a credential lookup takes an identifier the caller has already folded.
 *
 * Pure: no I/O, no suspension, no database and no clock. Delegates entirely to
 * [loginHandleOrNull], [passwordIsLongEnough] and [passwordIsWithinTheWorkBound] rather than
 * re-stating any of their rules, which is what lets sign-up and `STORY-0416`'s reset-password
 * apply the identical judgement.
 *
 * @param request the decoded sign-up request, before any lookup
 * @return [SignUpFields.Accepted] with the folded handle, or [SignUpFields.Refused] with the one
 *   status that names the failing rule
 */
internal fun signUpFieldsOf(request: SignUpRequest): SignUpFields {
    val handle = loginHandleOrNull(request.handle)
        ?: return SignUpFields.Refused(HttpStatusCode.BadRequest)

    val password = PresentedSecret(request.password)
    if (!passwordIsLongEnough(password) || !passwordIsWithinTheWorkBound(password)) {
        return SignUpFields.Refused(HttpStatusCode.UnprocessableEntity)
    }

    return SignUpFields.Accepted(handle)
}
