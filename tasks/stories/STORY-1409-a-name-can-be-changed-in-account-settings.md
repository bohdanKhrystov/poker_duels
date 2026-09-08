---
id: STORY-1409
title: A name can be changed in account settings, and the name it leaves is spent
type: story
status: backlog
parent: EPIC-14
module: poker-server
labels: [server, db, client, design, docs, account]
depends_on: []
---

## Goal

A player who opens `Account` finds one form that both sets a first display name and changes an
existing one, as often as they like. The string they walk away from is spent forever: the registry
row moves `TAKEN → REPLACED` with `retired_from` recording who left it, the schema stops asserting
that a name is permanent and starts asserting the thing that is actually true — a name may leave a
player only after the registry has spent it — and `PUT /api/me/name` loses its `403` and gains a
`429` over a budget that meters the writes which **spend** a string, never the ones that fail.
`PERMANENCE_LINE` leaves the product with the promise it made.

## Why

This is `EPIC-14` item 1c, and its first half is the human's own words, recorded verbatim in
`ADR-0130` §1: *"player name can be changes at any time in accounts settings"*. It reverses their
earlier answer to `DEC-017`, and the product is currently telling players the opposite in a shipped
golden string — `ADR-0130` calls that *"a lie with a date on it"*.

Two merged ADRs settle it whole and this story implements them without reopening either:

- [`ADR-0130`](../../docs/adr/ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md)
  — the product half. *That* a name may change is the human's; **what becomes of the string is
  derived** from the vision as `ADR-0067` §1 read it — a leaderboard row is text and leads nowhere,
  and `ADR-0039` makes history a live join, so a reissued string would print a second player onto
  the first's finished duels. The name given up is therefore **retired, not released and not
  held**; history prints the name as it **is**; **one** form, on the **account screen**; and
  `PERMANENCE_LINE` and `refusalSentence`'s `permanent` sentence leave the product.
- [`ADR-0134`](../../docs/adr/ADR-0134-a-rename-spends-before-it-replaces.md) — the mechanism.
  The permanence trigger becomes `player_display_name_never_released` carrying `ADR-0051` §3's body
  **minus one disjunct**; the write is **four statements** in one transaction under
  `SELECT … FOR UPDATE`; the vacated registry row moves **`TAKEN → REPLACED`** with `retired_from`
  populated; `SetNameResult.AlreadyNamed` is **deleted**; `PUT /api/me/name` answers
  `200 | 400 | 401 | 409 | 429`; and the write is **budgeted** by `PlayerId`.

## Design notes

### `ADR-0134` §7's ordering is the spine, and it survives being checked

§7 says *"the migration can land before the write path, and the product does not change until the
write path does."* That was **measured, not taken on trust**. The migration is a pure relaxation:
it deletes one disjunct from a raise condition and widens two `CHECK`s and one monotonicity
predicate. Every shipping writer of `player.display_name` is unaffected —

- `PostgresProfileWrites.SET_NAME_SQL` still carries `AND display_name IS NULL`, so a named
  player's write matches zero rows and the `BEFORE UPDATE OF display_name` trigger never fires;
- `retire_display_name` promotes the registry row **before** it nulls the column, so it satisfies
  the surviving guard exactly as it satisfied the old one;
- the coin write on every finished duel is excluded by the `OF display_name` clause, unchanged.

Probed on `develop` at `1c3c7fd9` with `./gradlew check -PrequireDocker=true` — the command
`.github/workflows/build.yml` runs on a pull request — with the migration file added and nothing
else: **1794 tests completed, 3 failed**, and all three assert the *old* trigger's name, message or
refusal. No production behaviour moves. So the ordering holds, with one correction worth writing
down: the migration is behaviour-preserving for the **product**, not for the **test suite**, and
those three test files are inside `TASK-140901`'s own budget.

### One merged assertion inverts, and it is not a decision

`DisplayNamePermanenceTest.aRetiredNameStillCannotBecomeADifferentName` asserts that a player whose
held string is already `RETIRED` still cannot have their column moved to a different name. Under
`ADR-0134` §1's body that write **succeeds** — the surviving guard asks only whether the string
being left is spent. That is not a hole this story invents blindly: `ADR-0134` §1's third bullet
states the resulting rule in as many words (*"`name → a different name` succeeds only if the old
string is already `RETIRED` or `REPLACED`"*), §8 names the family it belongs to, and the state is
unreachable from the server, because `retire_display_name` nulls the column in the same transaction
that retires the row. It is still a **widening**, so `TASK-140901` rewrites that test into the
positive assertion of the new rule and rewrites the comment that argues for the old one. Keeping the
old name and the old expectation was never available.

### The budget needs no new machinery, only a different call site

`AttemptBudget` already carries both halves `ADR-0134` §6 asks for: `admit(key)` records and answers,
and `refund(key)` returns one reservation. `ADR-0074` §2's reserve-then-refund is **shipped** —
`AuthRoutes` reserves before the Argon2 verification and refunds on a correct password. Metering
*spending* rather than *trying* is the same two calls with the refund on the other branch: `admit`
before `writes.setDisplayName`, `refund` on every result that is **not** `NameSet`. So the story
adds a fifth `AttemptLimits` pair, a fifth `AttemptBudget` instance and one call site, and changes
`AttemptBudget` not at all. Two shipped properties are relied on and neither is edited: `admit`
records even when it refuses, so a throttled player extends their window rather than resetting it
(`ADR-0055` §1), and `refund` removes the most recently recorded timestamp under the same lock,
which is exactly right when the key is one player.

### Three tickets are `atomic:`, and each was probed to `exit 0`

| Ticket | Count | The gate that forbids a smaller commit |
| --- | --- | --- |
| `TASK-140901` | 4 | `:poker-server:test` — the trigger's name, its message and one inverted refusal are asserted in three files that the one migration file changes at once |
| `TASK-140902` | 4 | `:poker-server:test` — `retiredFromIsReadInExactlyOneFile` is an exact set equality that the mandated `retired_from` write breaks, and five behavioural assertions across two more files describe the `403` the change removes |
| `TASK-140903` | 4 | `:poker-server:compileTestKotlin` for three of them, and `ProfileWritesPortTest`'s **reflection** over `sealedSubclasses` for the fourth, which no compiler can see |
| `TASK-140906` | 7 | `:poker-server:compileTestKotlin` — a fourth required parameter on `profileRoutes` fails *No value passed for parameter 'budget'* at 69 call sites in five test files |
| `TASK-140910` | 5 | `npm run check`'s `tsc --noEmit` — `refusalSentence` and `mayTryAgain` switch exhaustively over `SetNameOutcome["kind"]` and fail *Function lacks ending return statement* the moment the union moves, and five literals typed as that union fail `TS2322`/`TS2345` |

Every other ticket touches at most three files and every intermediate state is green.

### The client chain deletes `PERMANENCE_LINE` in three plain tickets rather than one atomic one

Removing the export in the same diff that stops rendering it is a four-file commit the TypeScript
compiler forces. Splitting it is free: `TASK-140911` adds the two new lines beside it,
`TASK-140912` makes `NameSurface` render those and stop importing the old one, and `TASK-140913`
deletes the export and its golden test with no importer left. Measured: the union of the last two is
exactly the four-file state that `npm run check` and `npm run build` were driven to `exit 0` on.

**The blast radius of `PERMANENCE_LINE` was measured, not assumed.** Under `web-client/src` it is
named in exactly four files — `name-text.ts`, `name-text.test.ts`, `NameSurface.tsx` and
`NameSurface.test.tsx` — and nowhere under `design/`, `docs/` or `poker-server/`. Removing it
breaks **no** unrelated test: the order dependence this epic already found in `Lobby.test.tsx`
(`STORY-1408` §Design notes) does not fire here, and `npm run check` reached `exit 0` on the
deletion without touching that file.

### The form moves in four steps because the whole-client arc names its profile at the front door

`web-client/src/e2e/claimed-here-recovered-there.test.tsx` reaches `your display name` at fourteen
call sites, all on the front door. Deleting the front-door render in one diff with the wiring costs
three files at once and drags that arc; and deleting it alone leaves `setName` unused in `Lobby.tsx`
(`TS6133`, measured). So: `TASK-140914` gives `AccountScreen` an optional `setName` and renders the
surface for it; `TASK-140915` passes it from `Lobby` **while keeping** the front-door render;
`TASK-140916` moves the arc's fourteen call sites behind the product's own `Account` door; and
`TASK-140917` takes the front-door render away. The pattern `TASK-140916` uses — press `Account`,
name inside the `account` region, press `Back` — was verified on this tree against the first of the
four failing arc cases.

`TASK-140918` is last on purpose. It is the ticket that makes the surface offer its form to a player
who already holds a name, and landing it before `TASK-140917` would put a rename form on the front
door, which is the one placement `ADR-0130` §6 refuses.

### The words are the card's, so the card is the first client ticket

`ADR-0130` §5 puts the two obligations — *it can be changed later*, *the name you give up is gone
for good* — on the design card under `ADR-0091` §2, and `ADR-0134` §5 puts the `throttled` sentence
there too. `TASK-140909` draws them on `design/screens/account.html`; `TASK-140910` and
`TASK-140911` transcribe them character for character. No ticket below the card chooses a word, and
a card sentence that reads wrongly is reported rather than edited in a copy ticket.

### Serialisation

**`STORY-1409` must not run beside `STORY-1407`, `STORY-1408` or `STORY-1410`.**

- `STORY-1407` — `TASK-140715` edits `web-client/src/lobby/Lobby.tsx`, which `TASK-140915` and
  `TASK-140917` also edit. Its `TASK-140712` was written to import `refusalSentence` and
  `mayTryAgain` **without branching on an outcome kind**, precisely so `TASK-140910` deleting
  `permanent` changes that file not at all; that promise holds only if the two stories do not
  interleave their edits to `name-text.ts`.
- `STORY-1408` — `TASK-140812` edits `web-client/src/account/AccountScreen.tsx` and its test, which
  `TASK-140914` also edits; `TASK-140805` edits
  `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt`, which
  `TASK-140902` rewrites; and `TASK-140801` edits `Lobby.tsx`.
- `STORY-1410` — `ADR-0135`'s device bit edits `docs/protocol.md`'s *Profile endpoint* section, and
  `TASK-140907` edits both that section (`displayNameRemoved`'s fifth row, `ADR-0134` §4) and
  *Set display name*.

`STORY-1408`'s own note that `STORY-1409` *"is not in tension"* was written before this split and is
now half right: `PostgresProfileReads` is indeed untouched here, but `PostgresProfileWrites`,
`AccountScreen.tsx`, `Lobby.tsx` and the *Profile endpoint* section of `docs/protocol.md` all are.
Whichever story runs second rebases.

## Tasks

Eighteen tickets, one linear chain — the server first because `ADR-0130` §Consequences puts the
brake's deadline *before* this story ships, then the card, then the client. Exactly one ticket is
startable at a time.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-140901](../tasks/TASK-140901-the-trigger-stops-saying-permanent.md) | The trigger stops saying permanent, and the registry gains a fourth reason | backlog |
| [TASK-140902](../tasks/TASK-140902-a-rename-spends-before-it-replaces.md) | A rename spends before it replaces | backlog |
| [TASK-140903](../tasks/TASK-140903-already-named-is-deleted-and-403-leaves-the-route.md) | `AlreadyNamed` is deleted, and `403` leaves the route | backlog |
| [TASK-140904](../tasks/TASK-140904-the-name-writes-budget-has-two-numbers.md) | The name write's budget has two numbers | backlog |
| [TASK-140905](../tasks/TASK-140905-a-fifth-budget-stands-beside-the-four.md) | A fifth budget stands beside the four | backlog |
| [TASK-140906](../tasks/TASK-140906-the-write-is-admitted-before-it-spends.md) | The write is admitted before it spends, and refunded when it does not | backlog |
| [TASK-140907](../tasks/TASK-140907-the-document-answers-429-and-no-longer-answers-403.md) | The document answers `429` and no longer answers `403` | backlog |
| [TASK-140908](../tasks/TASK-140908-the-removal-bit-reads-false-for-a-renamer.md) | The removal bit reads false for a renamer, for two independent reasons | backlog |
| [TASK-140909](../tasks/TASK-140909-the-card-draws-the-name-form-on-the-account-screen.md) | The card draws the name form on the account screen | backlog |
| [TASK-140910](../tasks/TASK-140910-403-leaves-the-client-and-429-arrives.md) | `403` leaves the client and `429` arrives | backlog |
| [TASK-140911](../tasks/TASK-140911-the-two-obligations-arrive-in-words.md) | The two obligations arrive, in the card's words | backlog |
| [TASK-140912](../tasks/TASK-140912-the-surface-says-them-and-stops-saying-permanent.md) | The surface says them, and stops saying a name is permanent | backlog |
| [TASK-140913](../tasks/TASK-140913-permanence-line-leaves-the-product.md) | `PERMANENCE_LINE` leaves the product | backlog |
| [TASK-140914](../tasks/TASK-140914-the-account-screen-carries-the-name-form.md) | The account screen carries the name form | backlog |
| [TASK-140915](../tasks/TASK-140915-the-front-door-hands-the-form-to-the-account-screen.md) | The front door hands the form to the account screen | backlog |
| [TASK-140916](../tasks/TASK-140916-the-arc-names-its-profile-where-the-form-now-stands.md) | The arc names its profile where the form now stands | backlog |
| [TASK-140917](../tasks/TASK-140917-the-form-leaves-the-front-door.md) | The form leaves the front door | backlog |
| [TASK-140918](../tasks/TASK-140918-a-player-who-holds-a-name-is-offered-the-form-that-changes-it.md) | A player who holds a name is offered the form that changes it | backlog |

## Acceptance criteria

- [ ] `player_display_name_is_permanent` and `player_display_name_permanent` appear in no file under
      `poker-server/src/main`, and `player_display_name_never_released` fires on
      `UPDATE OF display_name` after `Migrations.migrate`.
- [ ] A player who holds `Ann` and writes `Bea` ends up holding `Bea`, with `Ann`'s registry row
      `REPLACED` and its `retired_from` naming that player — read back out of a real database.
- [ ] A rename that collides leaves **both** strings where they were: the old row still `TAKEN`, the
      player still holding the old name, and no second row for the string that was refused.
- [ ] `SetNameResult` has exactly two subclasses and `HttpStatusCode.Forbidden` appears nowhere in
      `ProfileRoutes.kt`.
- [ ] `PUT /api/me/name` answers `429` on the sixth write in a minute from one player and `200` on
      the sixth `409` in a minute from the same player — the budget meters spending, not trying.
- [ ] `docs/protocol.md`'s *Set display name* section documents `429` and documents no `403`.
- [ ] `PERMANENCE_LINE` and the string `That choice is permanent` appear in no file under
      `web-client/src`.
- [ ] The name form renders on the account screen and on no other screen, for a player who holds a
      name and for one who does not, with `ADR-0130` §5's two sentences above the field on both.
- [ ] `./gradlew check -PrequireDocker=true` and, in `web-client`, `npm run check` and
      `npm run build` all exit 0 on every ticket.

## Out of scope

- **Anything that lets a player take back a name they replaced.** `ADR-0130` §7 and `ADR-0134` §8
  refuse an un-retire, a release, a reclaim and an un-replace. `REPLACED` exists so a later decision
  *could* ask the question; nobody has, and this story builds none of it.
- **A quota, a counter, a cooling-off or a confirmation press.** `ADR-0130` §1 is *"at any time"*,
  and §5's form says nothing in advance about the budget — a rate limit is only ever seen after the
  fact (`ADR-0134` §6).
- **A suggestion on the change form.** `ADR-0130` §5: a player changing a name already knows what
  they want. The suggestion belongs to `NameAsk` and is `STORY-1407`'s (`ADR-0137`).
- **`NameAsk`'s own words.** `ADR-0119` §3's obligation 2 is replaced by `ADR-0130` §5 *on every
  surface that writes a name*, and `STORY-1407`'s `TASK-140710` owns that surface's copy. This story
  changes `name-text.ts` and `NameSurface.tsx` only.
- **A guard that the name a player *takes* is `TAKEN`.** `ADR-0134` §8 names the hole, says it is
  unreachable from the server, and refuses to close it here because closing it would newly constrain
  every fixture that writes a name. Not ticketed.
- **The `hasRecoveryEmail` literal in `PostgresProfileWrites.toProfile()`.** `TASK-140902` rewrites
  the statements around it and must not fix it: `ADR-0132` §Residuals owns it and `STORY-1408`
  already names it as an owed ticket that is deliberately not written.
- **Any change to `PostgresProfileReads.PROFILE_OF_SQL`.** `ADR-0134` §4 is explicit that the read
  changes *not at all*; `TASK-140908` edits one comment in that file and no SQL.
- **`retire_display_name`.** `ADR-0134` §1: not edited, and still the only route to nameless.
- **`PROTOCOL_VERSION`, `protocol.gen.ts` and anything on the socket.** `ADR-0134` §7, measured:
  no `ServerMessage` carries a display name and nothing here adds one.
