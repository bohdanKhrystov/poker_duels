---
id: STORY-0404
title: Sign-up — one endpoint, and it attaches an account to the profile already here
type: story
status: ready
parent: EPIC-04
module: poker-server
labels: [server, auth, http]
depends_on: [STORY-0403]
---

## Goal

`POST /api/auth/sign-up` takes a handle and a password and writes **one row**: a `credential`
pointing at the `player` row this request already resolves to. The coins, the history and the name
that profile holds are untouched, because they never move.

## Why

This is [`ADR-0012`](../../docs/adr/ADR-0012-device-bound-anonymous-profiles.md)'s stated debt — *a
lost device is a lost profile* — and the first half of paying it. It is also the story where the
single most expensive mistake in this epic is available: creating a second `player` row and copying
`duel_result` rows onto it, which satisfies a naive reading of "migrate my balance" and is wrong.

## Design notes

- **There is one endpoint and there is no `/api/auth/claim`.**
  [`ADR-0030`](../../docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) §1 collapses
  them: on a device that has a profile — which is every device that has ever connected — *creating
  an account* and *claiming this profile* are the same operation, and the only difference a second
  endpoint could express is the second `player` row the ADR exists to forbid.
- **The whole write is one `INSERT INTO credential`.** No `player` row is created. No `duel_result`
  row is written, moved, copied or deleted. `player.device_id`, `player.coin_balance`,
  `player.display_name` and `player.created_at` are untouched. The `INSERT` is the whole
  transaction, so a failed sign-up leaves nothing behind.
- **`player_id` comes from the resolved identity, never from the request body.** A body carrying a
  player id is a client asserting who it is, which
  [`ADR-0002`](../../docs/adr/ADR-0002-server-authoritative.md) forbids.
- **Sign-up creates no profile; it requires one.** No resolvable identity is `401` with an empty
  body and no rows written — `ADR-0012`'s rule that HTTP never mints a `player` row survives without
  an exception.
- **Identity here resolves by device id**, exactly as `GET /api/me` does. `ADR-0027`'s
  `IdentityResolver`, with its session precedence, arrives in `STORY-0405` and this endpoint moves
  onto it there; today a device id is the only credential a caller can hold.
- **A player who already holds a credential of the same kind gets `409` and nothing is written**
  (`ADR-0030` §1). Deliberately the conservative direction: loosening a refusal later is additive.
- **The handle rules are `STORY-0403`'s fold**, applied here: refused handles answer `400`, a taken
  handle answers `409`, and the two are distinguishable because `ADR-0031` §5 says so — a handle
  collision has to be reportable or the form is unusable.
- **No address field exists on this endpoint** (`ADR-0031` §5). Attaching a recovery email is its
  own endpoint in `STORY-0416`, and it costs the current password.
- The endpoint issues **no session**. A client signs in afterwards like anybody else; sign-in is
  `STORY-0405`.

### Added when the story was split, on 2026-08-17

Five things the notes above did not say, each settled by an ADR that merged after they were written
or by reading the code `STORY-0403` actually landed:

- **The password rule is `ADR-0048`'s and it is one rule: 8 to 128 code points of the NFC form.**
  Under 8 or over 128 answers **`422` with an empty body**, distinct from the handle's `400` and
  `409` because the form must know which field to mark. Nothing is trimmed, every code point is
  permitted, and **NFC is applied in the one place a secret becomes bytes** — `Argon2Hasher.tagFor`
  — so sign-up and sign-in cannot disagree. `PresentedSecret` gains no `init` and no `require`;
  `ADR-0048` §6 refuses that by name.
- **Success answers `201 Created` with an empty body.** No ADR fixed the code, and it is written
  down here rather than left to whoever types the handler: exactly one row is created, and `204`
  would say nothing happened — the misreading `ADR-0030` exists to prevent. There is no `Location`
  header because nothing reads a credential back (`ADR-0027` §1).
- **The `409` guard is a port read, not a unique index.** `Credentials` gains
  `holdsCredential(playerId, kind): Boolean`, asked before `create`, so a refused sign-up costs no
  Argon2 slot. `UNIQUE (player_id, kind)` would be cheaper and is deliberately not built:
  `ADR-0030` §7 adds no constraint and no index, and the constraint would freeze `DEC-027` — *may
  one player hold several credentials* — into the schema, where `ADR-0030` §1 calls the `409` *"a
  guard, not a rule about what an account is"*. The residual is recorded rather than hidden: two
  sign-ups racing for one player can both pass the read under `READ COMMITTED`. A double-clicked
  form sends the **same** handle and is caught by `UNIQUE (kind, identifier)`.
- **`player.device_id` still exists today**, and this story does not touch it. `ADR-0049` moves the
  device→profile edge into its own `device_binding` table, and that migration lands in
  `STORY-0406` — so the "byte-identical `player` table" criterion is asserted over columns read
  from `ResultSetMetaData` rather than a hard-coded list, and keeps its meaning after the column
  goes.
- **Sign-up is not rate limited, and `DEC-048` asks whether it should be.** `ADR-0027` §6 budgets
  failed *sign-ins* by remote address; nothing says whether sign-up carries a budget or what over
  budget answers, and `ADR-0048` §6's response table has six rows and no seventh. The question is
  the architect's, it blocks no ticket here, and it is due before `STORY-0405` merges — that story
  must build §6's budget anyway, and one mechanism serving both is the cheap outcome.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-040401](../tasks/TASK-040401-one-rule-for-a-password-and-it-is-length.md) | One rule for a password, and it is length | ready |
| [TASK-040402](../tasks/TASK-040402-the-hasher-hashes-the-nfc-form.md) | The hasher hashes the NFC form, in the one place a secret becomes bytes | backlog |
| [TASK-040403](../tasks/TASK-040403-the-port-can-ask-what-a-player-already-holds.md) | The port can ask whether a player already holds a kind of credential | backlog |
| [TASK-040404](../tasks/TASK-040404-one-select-for-that-player-and-that-kind.md) | One SELECT, and it answers for that player and that kind only | backlog |
| [TASK-040405](../tasks/TASK-040405-the-sign-up-body-is-two-fields-and-it-prints-neither.md) | The sign-up body is two fields, and it prints neither | backlog |
| [TASK-040406](../tasks/TASK-040406-the-doubles-a-sign-up-route-test-records-against.md) | The doubles every sign-up route test records against | backlog |
| [TASK-040407](../tasks/TASK-040407-the-handle-is-judged-first-then-the-password.md) | The handle is judged first, then the password | backlog |
| [TASK-040408](../tasks/TASK-040408-sign-up-identity-first-then-the-body.md) | `POST /api/auth/sign-up` — identity first, then the body | backlog |
| [TASK-040409](../tasks/TASK-040409-one-create-with-the-player-the-server-resolved.md) | One create, with the player the server resolved | backlog |
| [TASK-040410](../tasks/TASK-040410-no-outcome-carries-a-body-and-nothing-prints-a-secret.md) | No outcome carries a body, and nothing on the path can print a secret | backlog |
| [TASK-040411](../tasks/TASK-040411-the-server-it-ships-with-can-sign-up.md) | The server it ships with can sign up | backlog |
| [TASK-040412](../tasks/TASK-040412-one-credential-row-and-the-player-table-untouched.md) | One credential row, and the player table untouched across it | backlog |
| [TASK-040413](../tasks/TASK-040413-the-coin-a-duel-paid-survives-the-sign-up.md) | The coin a duel paid is still there after the sign-up | backlog |
| [TASK-040414](../tasks/TASK-040414-the-document-names-the-sign-up-endpoint.md) | The document names the sign-up endpoint, and a test agrees with the code | backlog |

The chain is linear on purpose: the run is sequential, and `AuthRoutes.kt`, `AuthRouteTest.kt` and
`SignUpDatabaseTest.kt` are each touched by more than one ticket, so two startable tickets would be
two tickets editing one file.

## Acceptance criteria

- [ ] A device with a profile signs up and gains exactly one `credential` row pointing at that
      profile's `player.id`.
- [ ] The `player` table is byte-identical before and after — the whole table, asserted as a
      multiset of rows, not one column of one row.
- [ ] `SUM(player.coin_balance)` and `SUM(duel_result.coin_delta)` are unchanged by a sign-up, and
      per-player `coin_balance` still equals the sum of that player's deltas.
- [ ] A player who won a duel before signing up still reads back that coin afterwards.
- [ ] An unauthenticated sign-up answers `401`, and the `player` row count is unchanged.
- [ ] A second sign-up for the same player answers `409` and writes nothing.
- [ ] A taken handle answers `409`; a handle that breaks the character rule answers `400`.
- [ ] A password under 8 or over 128 code points of its NFC form answers `422` with an empty body.
- [ ] No response body and no log line contains the password or its hash, asserted structurally.

## Out of scope

- Sign-in, the session token, and the socket handshake — `STORY-0405`.
- **A rate-limit budget on this endpoint** — `DEC-048`, open, the architect's. Nothing here is
  blocked on it; the six answers `ADR-0048` §6 tabulates are complete without one.
- The reusable P1/P2 fixture and the whole-flow scenario test `ADR-0030` §5 describes —
  `STORY-0406`, per `EPIC-04`'s story table. This story asserts both properties for sign-up only.
- Revoking the device binding — `STORY-0406`.
- The recovery email — `STORY-0416`.
- Any screen — `STORY-0412`.
