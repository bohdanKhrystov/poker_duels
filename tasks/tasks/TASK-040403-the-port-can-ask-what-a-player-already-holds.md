---
schema: 2
id: TASK-040403
title: The port can ask whether a player already holds a kind of credential
type: task
status: done
parent: STORY-0404
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [server, auth]
depends_on: [TASK-040402]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.auth.CredentialsPortTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`Credentials` gains one read — *does this player already hold a credential of this kind?* — which is
the question `ADR-0030` §1's `409` guard asks, and it answers a `Boolean` rather than anything that
could carry a secret.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/Credentials.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/auth/CredentialsPortTest.kt` | modify |

## Scope

- Add to the `Credentials` interface:

  ```kotlin
  public suspend fun holdsCredential(playerId: PlayerId, kind: CredentialKind): Boolean
  ```

- KDoc saying **why the guard is a port read and not a unique index**, because that is the thing a
  later reader will want to "fix": `ADR-0030` §7 adds no constraint and no index, and
  `UNIQUE (player_id, kind)` would freeze `DEC-027` — *may one player hold several credentials* —
  into the schema, where `ADR-0030` §1 explicitly calls the `409` *"a guard, not a rule about what
  an account is"*. Record the residual honestly in the same KDoc: two sign-ups racing for one
  player can both pass this read under `READ COMMITTED` and both insert. The realistic client
  behaviour — a double-clicked form — sends the **same** handle and is caught by
  `UNIQUE (kind, identifier)` as `IdentifierTaken`.
- The result is a `Boolean`, never a row, a credential or an identifier. Nothing on this port
  returns anything a secret could hide in.

## Out of scope

- The implementation — `TASK-040404`.

  **Corrected after implementation:** this was not achievable as written. Kotlin will not compile an
  implementing class that is missing a new abstract interface member, so `PostgresCredentials.kt` —
  a **third** file, not in the `Files` table above — had to change too. It carries
  `override suspend fun holdsCredential(...): Boolean = TODO("TASK-040404")`, a throwing stub, which
  is the least dangerous of the three available shapes: a real query would be `TASK-040404`'s whole
  job arriving early, and a hardcoded `false` would silently disable `ADR-0030` §1's `409` guard with
  nothing failing. Nothing calls it, so the stub is unreachable until `TASK-040404` replaces it —
  and that ticket already names this file as `modify`. `files_touched` stays `2` because the linter
  caps it at 3 and the frontmatter cannot express "2 plus one forced"; the real count is three.
- Any use of it — `TASK-040408`.
- Widening `CreateCredentialResult`. It keeps its two cases; the guard is a separate question asked
  before `create`, not a third answer from it, so a refused sign-up costs no Argon2 slot.

## Tests

`CredentialsPortTest`, whose existing assertions this ticket **moves** — named exactly, because the
scope cannot be implemented while they stand:

- `noFunctionOnThePortReturnsAString` asserts the port's function names are exactly
  `setOf("verify", "create")`. That set becomes `setOf("verify", "create", "holdsCredential")`.
  It is the only assertion in that test that changes: the `String`/`ByteArray` offender checks and
  the property check stay byte-identical and are not weakened.
- `TestDoubleCredentials` gains an `override` of `holdsCredential` returning a constructor-supplied
  `Boolean` defaulting to `false`, or it will not compile.

Nothing else in the file changes. `theCreateResultIsSealedAndHasExactlyTwoCases` stays exactly as it
is — this ticket adds no sealed case — and so do `verifyAnswersAPlayerIdOrNothing`,
`aTestDoubleAnswersTheIdentityItWasBuiltWith` and `anotherKindIsStillConstructible`.

| Test | Proves |
| --- | --- |
| `theHoldsQueryAnswersANonNullBoolean` | reflecting over `holdsCredential`: its return type's classifier is `Boolean::class` and `isMarkedNullable` is `false` — the same shape `verifyAnswersAPlayerIdOrNothing` asserts for `verify` |
| `aTestDoubleAnswersWhetherItHoldsOne` | the double built with `holds = true` answers `true` and the one built with the default answers `false`. **Two inputs, deliberately**: one fixture cannot tell a returned value from a hard-coded constant |

## Acceptance criteria

- [ ] `CredentialsPortTest.theHoldsQueryAnswersANonNullBoolean` passes
- [ ] `CredentialsPortTest.aTestDoubleAnswersWhetherItHoldsOne` passes, asserting **both** `true`
      and `false` from two differently-built doubles
- [ ] `noFunctionOnThePortReturnsAString` passes with the three-name set, and its offender checks
      are unchanged and not weakened
- [ ] `theCreateResultIsSealedAndHasExactlyTwoCases` passes unchanged
- [ ] `PublicApiHasNoHashTest` passes unchanged
- [ ] `holdsCredential` returns `Boolean`; no signature on `Credentials` returns a `String`, a
      `ByteArray` or a credential row
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
