---
schema: 2
id: TASK-041617
title: Five endpoints and a field, written down
type: task
status: backlog
parent: STORY-0416
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 1
labels: [docs, http, api]
depends_on: [TASK-041616]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.HttpEndpointDocumentationTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.http.ProfileDtosTest'
---

## Goal

`docs/protocol.md` contracts `ADR-0031` §5's five endpoints and `ProfileResponse`'s new field, so
`STORY-0417`'s client has one place to read what every status means.

## Files

| File | Action |
| --- | --- |
| `docs/protocol.md` | modify |

Read, and do not edit:
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §5's table and the three paragraphs
under it;
`docs/adr/ADR-0080-the-password-is-judged-before-the-token-is-touched.md` §2 — the corrected
`reset-password` `422` sentence, which supersedes §5's parenthetical;
`docs/adr/ADR-0081-a-mailed-link-is-a-fragment-route-and-the-token-is-the-segment-behind-the-slug.md`
§1 — the two mailed links, character for character;
`poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt`.

## Scope

- One `### ` section per endpoint, in `ADR-0031` §5's order, in the shape *Sign in* already uses:
  method and path, authentication, request fields table, response table.
- `hasRecoveryEmail` added to the *Profile endpoint* field table, worded so it cannot be read as
  carrying an address: *"`true` when the caller has a **verified** recovery address. `false` covers
  three cases this field does not distinguish — never attached, attached but not yet verified, and
  detached. The address itself is returned by no endpoint (`ADR-0031` §6.3)."*
- **`reset-password`'s `422` row transcribes `ADR-0080` §2, not `ADR-0031` §5's parenthetical.**
  §5's table still says `422`; its gloss *"when the token was good and the new password fails
  policy"* describes an order the endpoint does not run in and was corrected by `ADR-0080`. The row
  reads as sign-up's already does — *the password is under 8 or over 128 code points* — plus **this
  is answered whether or not the token is good, and without the token being looked at**, with `400`
  for a bad token answered only once the password has passed. A client author who copies §5's
  sentence builds a form that says *your link is still good*, which `ADR-0080` §Consequences makes
  the one thing this status may never be reported as: it is a fact about the caller's own input and
  nothing else.
- Four sentences that the client half will otherwise get wrong, stated where a client author
  reads them:
  1. `forgot-password` answers `202` in **every** case — unknown, pending, verified, over budget,
     no sender — with an identical empty body. A client may not infer anything from it.
  2. `reset-password` accepts its token **only in a request body**, never as a query parameter. The
     mailed link is `<baseUrl>/#/reset/<token>` and the verification link is
     `<baseUrl>/#/verify/<token>` (`ADR-0081` §1): the token is the second segment **of the
     fragment**, so it reaches no server, no access log, no proxy record and no `Referer`. The
     client reads it once from `location.hash` and then replaces the fragment with `#/reset`
     through `history.replaceState`. **Neither link contains a `?`.**
  3. `reset-password` issues **no session** and returns **no token**. The player signs in
     afterwards, including on the device they just used.
  4. A `422` and a `400` from `reset-password` are answered in a fixed order the caller cannot
     choose — the password first, the token second — so a caller holding both a dead link and a
     short password needs two requests to learn it, and the first tells them nothing about the
     link (`ADR-0080` §Consequences).
- `recovery-email`'s `202` row says outright that it is also the answer when the address already
  belongs to another player, and that nothing is sent in that case.

## Out of scope

- Wording either mail. `ADR-0031`'s *What this does not settle* hands that to `STORY-0412` under
  `EPIC-06`'s design language.
- Documenting `baseUrl`'s configuration key. `ADR-0077` §6 settled it and `TASK-041632` ships it.
- The two budgets' numbers, which `ADR-0079` has now fixed. This ticket still writes *over budget
  answers `202`*, which `ADR-0031` §5 fixes, and names **no number**: the four values are
  configuration (`TASK-041628`), and a document that repeats them is a second place for them to be
  wrong with nothing comparing the two.
- Any code change. If a status in `ADR-0031` §5 turns out not to match what a route does, that is a
  ticket against the route, not a quiet edit here.

## Tests

No new test. `HttpEndpointDocumentationTest` is in `verify:` because it enforces **documented ⇒
exists**: a path written here that no route serves fails the build, which is the one automatic
check this ticket can fail. The reverse — an endpoint that exists and is undocumented — is not
enforced by anything, and that is stated here rather than left to be discovered, because it is
exactly why this ticket has to be written by hand.

`ProfileDtosTest` is in `verify:` as the pin on the field name: a document that spells it
`recoveryEmail` cannot be caught, but a *code* change that renames the field while this document
says otherwise fails there.

## Acceptance criteria

- [ ] `HttpEndpointDocumentationTest` passes
- [ ] `ProfileDtosTest` passes
- [ ] All five paths of `ADR-0031` §5 appear, spelled exactly as `AuthRoutes.kt` and
      `RecoveryRoutes.kt` register them
- [ ] Every status code in `ADR-0031` §5's table appears in this document against the endpoint it
      belongs to, and no status appears that the ADR does not name
- [ ] The `forgot-password` section states that all five cases answer `202` and lists them
- [ ] The `reset-password` section states *only in a request body, never as a query parameter*
- [ ] The `reset-password` `422` row says the status does **not** depend on the token, and nowhere
      in the document does a `422` mean, or read as, the link still being good (`ADR-0080` §2)
- [ ] The document spells the two mailed links `<baseUrl>/#/reset/<token>` and
      `<baseUrl>/#/verify/<token>`, and contains no `?` in either (`ADR-0081` §1, §2)
- [ ] The `hasRecoveryEmail` row names the three cases `false` covers
- [ ] No section names an email provider, an SMTP host, or a delivery guarantee
- [ ] Every command in `verify:` exits 0

## Proof

1. Add a sixth section for `POST /api/auth/resend-verification`, a path no route serves.
   **`HttpEndpointDocumentationTest` reddens alone**, naming the path. This is the only mutation in
   this ticket that a gate catches, and running it is how the ticket learns what its gate is worth.
   Revert.
2. Change the `hasRecoveryEmail` row to say the address is returned when `true`.
   **Nothing reddens.** Record it. The document's *prose* is ungated in both directions, which is
   why this ticket is `review: light` on the mechanics and why its acceptance criteria are phrased
   as things a reviewer can check by reading rather than as test names. Revert.
3. Delete the *Sign in* section entirely. **`HttpEndpointDocumentationTest` still passes** — it
   checks documented ⇒ exists, so removing documentation removes an obligation. Record this too:
   it is the sharper half of the same limit, and it means nothing in CI notices if this ticket is
   only half done. Revert.
4. Restore `ADR-0031` §5's original wording to the `reset-password` `422` row — *when the token was
   good and the new password fails policy* — and change the mailed link back to
   `<baseUrl>/reset#token=…`. **Nothing reddens**, in either case: the endpoint test reads paths,
   never prose, and no test in this repository has ever read a URL out of a document. Record both.
   These are the two rows in this ticket whose only gate is a reviewer, they are the two a reader
   is most likely to copy from a merged ADR that is now wrong on its own, and `ADR-0080` says a form
   built on the first *"cannot be quietly retracted"*. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
