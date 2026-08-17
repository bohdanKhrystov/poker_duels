---
schema: 2
id: TASK-040311
title: The Credentials port answers a PlayerId or nothing
type: task
status: done
parent: STORY-0403
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, auth, ports]
depends_on: [TASK-040310]
verify:
  - ./gradlew :poker-server:test --tests '*CredentialsPortTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

There is a port for proving a credential, its answer is a `PlayerId?` and never a hash, and its
write answers with a sealed type rather than a boolean or an exception.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/Credentials.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/auth/CredentialsPortTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileWrites.kt` | read — the port-and-sealed-result shape and KDoc style this copies |
| `docs/adr/ADR-0027-the-session-outranks-the-device-id.md` | read — §1, the port's signature and the structural rule behind it |

## Scope

- `public value class CredentialKind(public val value: String)`, `@JvmInline`, with
  `public val PASSWORD: CredentialKind` on its companion. **Not an enum**: `ADR-0041` says
  `"password"` is the only kind v0.1 writes *and* that the door stays open and unfinished, so any
  other kind stays constructible.
- `public interface Credentials` with exactly two functions:
  - `public suspend fun verify(kind: CredentialKind, identifier: String, presented: PresentedSecret): PlayerId?`
  - `public suspend fun create(playerId: PlayerId, kind: CredentialKind, identifier: String, secret: PresentedSecret): CreateCredentialResult`
- `public sealed interface CreateCredentialResult` in the same file, with `Created` and
  `IdentifierTaken` as objects — the same layout `SetNameResult` uses.
- KDoc that says, on the interface:
  - **nothing on this port returns a hash, and nothing ever will** — that is why `verify` answers an
    identity rather than a stored value for a caller to compare;
  - `identifier` has **already been folded** by `loginHandleOrNull`, exactly as `ProfileWrites`
    takes a `canonicalName`. A port that re-folds is a second place the rule lives;
  - `verify` answering `null` covers *both* an unknown identifier and a wrong secret, deliberately,
    and a caller cannot tell which — `ADR-0027` §6.
- No implementation in this ticket.

## Out of scope

- `PostgresCredentials` — `TASK-040312`.
- The third answer `STORY-0404` will need — *this player already holds a credential of this kind*,
  `ADR-0030` §1's `409`. It belongs with the endpoint that returns it; adding it here would be a
  case nothing constructs and nothing tests.
- Wiring anything into `ServerComponents`. Nothing calls this port until `STORY-0404`, and a
  component wired to no route is dead weight the next story would have to rewrite.
- The sweep over the whole package's public API — `TASK-040314`, once the implementation exists.

## Tests

`CredentialsPortTest`, reflecting over the port rather than reading it.

| Test | Proves |
| --- | --- |
| `verifyAnswersAPlayerIdOrNothing` | the declared return type of `verify` has classifier `PlayerId` and `isMarkedNullable` is true — the story's title, asserted |
| `noFunctionOnThePortReturnsAString` | over every public member of `Credentials`, none returns `String` or `ByteArray`; the assertion reports the collected offenders |
| `theCreateResultIsSealedAndHasExactlyTwoCases` | `CreateCredentialResult::class.sealedSubclasses` is exactly `{Created, IdentifierTaken}` — a third answer added later has to be added here too |
| `aTestDoubleAnswersTheIdentityItWasBuiltWith` | a hand-written double returning `PlayerId("7")` hands it back through the interface, so the signature is usable as written |
| `anotherKindIsStillConstructible` | `CredentialKind("passkey").value` is `"passkey"`, and `CredentialKind.PASSWORD.value` is `"password"` — `ADR-0041`'s open door, and the one value v0.1 walks through |

## Acceptance criteria

- [ ] All five tests above pass
- [ ] `verifyAnswersAPlayerIdOrNothing` asserts **both** the classifier and the nullability; the
      classifier alone passes for a non-null `PlayerId`, which is a different contract
- [ ] `noFunctionOnThePortReturnsAString` enumerates the port's members and asserts over the
      collected list, not with a single `assertTrue`
- [ ] `theCreateResultIsSealedAndHasExactlyTwoCases` asserts the exact set of names, not the count
      alone
- [ ] `Credentials.kt` declares no function returning `String`, `ByteArray` or anything named for a
      hash
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
