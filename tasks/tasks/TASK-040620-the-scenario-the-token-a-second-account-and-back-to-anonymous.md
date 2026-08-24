---
schema: 2
id: TASK-040620
title: The scenario, steps five to eleven — the token, a second account, and back
type: task
status: backlog
parent: STORY-0406
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, e2e, coins, invariant, auth]
depends_on: [TASK-040619]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.IdentityMovesNoCoinTest' -PrequireDocker=true
---

## Goal

The rest of `ADR-0030` §5's scenario runs in the same test: signing in, reconnecting under the
token, signing into a **second** account from the same device, winning a duel as that account,
signing out, and coming back as the original anonymous profile — with P1 and P2 asserted after every
one of those steps.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/IdentityMovesNoCoinTest.kt` | modify |

Read, and do not edit:
`poker-server/src/test/kotlin/duels/poker/server/e2e/SocketDuel.kt` (the token-carrying opener
`TASK-040619` added), `docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md` §5 and §6.

## Scope

- The guest of step 1's duel signs up under its **own** handle during the fixture, so a second
  account exists to sign into. That is the only change to the first four steps.
- Steps five to eleven are appended to `theFirstFourStepsMoveNoCoin`, which is renamed
  `theWholeScenarioMovesNoCoin` — **the one rename this ticket makes**, because the method's subject
  genuinely changes and leaving the old name would be a lie in the report. Every assertion already
  in it stays, in place, unweakened.
  5. **Sign in.** `POST /api/auth/sign-in` as the host's account → token `T1`.
  6. **Reconnect with the token.** A `Hello` carrying `T1` from the host's device: the `Welcome`
     names the host's player.
  7. **Sign into the second account from the same device.** `POST /api/auth/sign-in` as the guest's
     account → `T2`; a `Hello` carrying `T2` **from the host's device** names the *guest's* player.
     `ADR-0030` §6 is what makes this legal, and this step is the whole reason the scenario exists.
  8. **Play a duel as that account**, host socket presenting `T2`, opponent presenting a third,
     plain device id. The winner is read from the outcome.
  9. **Sign out.** `POST /api/auth/sign-out` with `T2`, `204`.
  10. **Reconnect anonymously.** A `Hello` with the host's device id and no token names the host's
      original player again.
  11. **Read the profile back.** `GET /api/me` for the host's device id: the same `playerId`, and the
      balance it had at the end of step 2 — step 8's coin went to the guest's account, not here.
- `assertCoinInvariantHolds("<step>")` after **each** of steps 5 to 11 — seven more calls, seven more
  distinct step strings, twelve in the method in total.
- The `player` multiset via `playerTableSnapshot()` is compared before and after **step 5**, **step
  7**, **step 9** and **step 10**, byte-identical each time and with **no permitted exception**:
  signing in, signing out and reconnecting write to `auth_session` and to nothing else. Step 8 is a
  duel, so it is deliberately not one of the four.

## Out of scope

- Revocation — `TASK-040621`, same file, same method.
- Any file under `poker-server/src/main`.
- `SocketDuel.kt` and `E2eServer.kt` — **named prohibitions.** `TASK-040619` gave them everything
  this ticket needs; if something is missing, the ticket stops and reports.

## Tests

`IdentityMovesNoCoinTest`

| Test | Proves |
| --- | --- |
| `theWholeScenarioMovesNoCoin` | Twelve `assertCoinInvariantHolds` calls, all passing, in order. The story's first acceptance criterion, whole |
| `theSecondAccountIsSeatedFromTheFirstsDevice` | Step 7's `Welcome` names the **guest's** player id, and step 6's named the **host's** — two handshakes on one device with two different expected answers, asserted in one test |
| `theSecondDuelPaidTheAccountAndNotTheDevice` | After step 8, the guest's account balance has moved by the duel's delta while the host's original anonymous profile reads exactly what it read at the end of step 2. **Two players, two different expected values**, and it is the assertion that catches a socket crediting the device rather than the session |
| `signingInAndOutLeavesThePlayerTableByteIdentical` | The four snapshot pairs — steps 5, 7, 9 and 10 — are each equal. The story's second acceptance criterion for sign-in and sign-out |
| `theDeviceIsItselfAgainAfterSigningOut` | Step 10's `Welcome` and step 11's `GET /api/me` both name the host's **original** player id, the one recorded at step 1 |

## Acceptance criteria

- [ ] All five test methods above pass
- [ ] `theWholeScenarioMovesNoCoin` contains exactly twelve calls to `assertCoinInvariantHolds`, with
      twelve different step strings
- [ ] `theFirstFourStepsMoveNoCoin` no longer exists, and every assertion it held is present in
      `theWholeScenarioMovesNoCoin`
- [ ] The other four methods `TASK-040618` added still exist, still pass, and none of their
      assertions is edited
- [ ] `theSecondDuelPaidTheAccountAndNotTheDevice` compares the host's balance against the value
      recorded at the end of step 2, not against a literal
- [ ] The diff against `develop` touches exactly one file under `poker-server/`, and it is the
      one in the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

Mutate the socket's identity resolution at its source: in `IdentityResolver.resolve`, fall back to
the device id when a token is presented but resolves to nothing — the silent downgrade `ADR-0027` §4
forbids — and additionally make step 7 present an expired token by deleting `T2`'s `auth_session` row
between the sign-in and the `Hello`.

`theSecondAccountIsSeatedFromTheFirstsDevice` reddens: step 7's `Welcome` names the *host's* player.
`theSecondDuelPaidTheAccountAndNotTheDevice` reddens with it, because step 8's coin now lands on the
host's anonymous profile. `theWholeScenarioMovesNoCoin` stays **green** — a coin that moved the
ordinary way, to the wrong player, still satisfies P1 and P2 — and that is worth stating: the coin
properties say *no coin was minted*, never *the right player got it*, and the two named tests are
what cover the difference. Revert both edits.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
