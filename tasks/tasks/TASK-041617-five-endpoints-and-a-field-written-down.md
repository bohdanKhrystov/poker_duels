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
`poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt`.

## Scope

- One `### ` section per endpoint, in `ADR-0031` §5's order, in the shape *Sign in* already uses:
  method and path, authentication, request fields table, response table.
- `hasRecoveryEmail` added to the *Profile endpoint* field table, worded so it cannot be read as
  carrying an address: *"`true` when the caller has a **verified** recovery address. `false` covers
  three cases this field does not distinguish — never attached, attached but not yet verified, and
  detached. The address itself is returned by no endpoint (`ADR-0031` §6.3)."*
- Three sentences that the client half will otherwise get wrong, stated where a client author
  reads them:
  1. `forgot-password` answers `202` in **every** case — unknown, pending, verified, over budget,
     no sender — with an identical empty body. A client may not infer anything from it.
  2. `reset-password` accepts its token **only in a request body**, never as a query parameter. The
     link is `<baseUrl>/reset#token=…`; the client reads `location.hash` and clears it with
     `history.replaceState`.
  3. `reset-password` issues **no session** and returns **no token**. The player signs in
     afterwards, including on the device they just used.
- `recovery-email`'s `202` row says outright that it is also the answer when the address already
  belongs to another player, and that nothing is sent in that case.

## Out of scope

- Wording either mail. `ADR-0031`'s *What this does not settle* hands that to `STORY-0412` under
  `EPIC-06`'s design language.
- Documenting `baseUrl`'s configuration key. It is `DEC-072`'s and `TASK-041627`'s.
- The two budgets' numbers — `DEC-073`. This ticket writes *over budget answers `202`*, which
  `ADR-0031` §5 fixes, and names no number.
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

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
