---
id: STORY-0410
title: The display-name product rules — screened when set, and takeable away
type: story
status: ready
parent: EPIC-04
module: poker-server
labels: [server, moderation, schema, identity]
depends_on: [STORY-0401]
---

## Goal

A name is screened against a blocklist when it is set; a name that should never have been allowed can
be taken away afterwards; and a name taken away is **retired**, never released back into the pool.

## Why

[`ADR-0029`](../../docs/adr/ADR-0029-a-display-name-is-unique-and-permanent.md) made a name unique
and permanent and said nothing about which names may be set — which turns an offensive name, a
squatted name and a homoglyph into the same unfixable problem.
[`ADR-0038`](../../docs/adr/ADR-0038-a-name-is-screened-when-set-and-can-be-taken-away.md) is the
human's answer: *blocklist + takedown*, and a taken name is *retired forever*.

## Unblocked

**`DEC-042` is answered** by
[`ADR-0051`](../../docs/adr/ADR-0051-a-name-is-registered-before-it-is-held.md), which the split
must follow. In short: **one table is the whole namespace.** `name_registry(name PK, reason,
retired_from, created_at)` holds names in use (`TAKEN`), blocked names and retired names behind one
unique index on `ADR-0029` §1's ICU fold, `player.display_name` gains a foreign key into it, and a
string never leaves the table — a takedown promotes `TAKEN → RETIRED` and deletes nothing. Setting a
name becomes two statements in one transaction, and **anything but one row from the second rolls the
transaction back**, or a refused claim permanently burns a string nobody holds (§2 — the defect this
story exists to not ship). `ADR-0029` §4's permanence trigger is replaced with **exactly one**
exception: `name → NULL`, and only when that name is already `RETIRED`. The operator path is
`retire_display_name(player_id, expected_name)` — a function in the migration, called from `psql`,
never an endpoint and never a Gradle task — documented in a new `docs/operations.md`.

**`DEC-046` (is the player *told*?) is answered too**, by
[`ADR-0052`](../../docs/adr/ADR-0052-a-takedown-is-told-to-the-player-it-happened-to.md): **yes**, so
silence is no longer the default this story ships under. It adds no column and nothing to the write
path — `SetNameResult`, the endpoint's four codes, `retire_display_name` and `docs/operations.md` are
exactly as `ADR-0051` left them — and **one** thing to the read path: `profileOf` answers whether a
name has been retired from the requesting player, which `ADR-0051` §1's `retired_from` already
records. The client half is `STORY-0411`'s.

**`DEC-047` (what shape does that fact take?) is answered too**, by
[`ADR-0053`](../../docs/adr/ADR-0053-the-profile-says-the-name-was-removed.md): `ProfileResponse`
gains **`displayNameRemoved: Boolean`** — non-null and **with no default value**, or it is present in
every test's JSON and absent from the wire — true **iff** the caller holds no display name **and** a
name has been retired from them, so the bit goes quiet once they set a new one. It is computed by
**one correlated `EXISTS`** inside the existing profile `SELECT`, correlated to `p.id` and **never a
`LEFT JOIN`** (a player may hold two retired names, and a join returns two rows for one profile), so
the read stays one round trip. `ADR-0051` §8's migration gains **one partial index**,
`name_registry_retired_from_idx ON name_registry (retired_from) WHERE retired_from IS NOT NULL`,
which costs the name-set path nothing. `PROTOCOL_VERSION` does not move; `docs/protocol.md` gains a
row that no gate enforces. `PostgresProfileWrites` passes the literal `false`. The split takes
`ADR-0053` §6 as its task list, including the criterion that the *retired* and *never-named* fixtures
must be **two players in one database** — two tests with one fixture each pass while the correlation
is missing altogether.

## Design notes

- **Uniqueness gains two more sources of truth** (`ADR-0038`): names in use, names retired by a
  takedown, and the blocklist. All three are consulted case-insensitively under the ICU collation
  `ADR-0029` §1 pinned — which is what puts all three in the database rather than in a resource file
  the JVM folds differently. `ADR-0051` §1 makes them **three values of one `reason` column in one
  table behind one index**, so the write path consults one structure and not three, and the race
  between a claim and a committing takedown has nowhere to happen.
- **The screen fails closed.** A blocklist that cannot be read refuses the name rather than accepting
  it. That is stated in `ADR-0038` and is the single most likely thing to be got backwards.
- **A refused name answers like a taken one** — the same kind of error the uniqueness check produces
  — so the endpoint has no second failure vocabulary and tells a prober nothing extra.
- **A force-rename returns the profile to *unset*, not to a name it did not choose.** `display_name`
  is nullable and the read path already handles the unset case; a profile must never end up holding a
  name it cannot change and did not pick.
- **The retired set only grows**, accepted deliberately: it is small, and it is the price of closing
  the re-registration loop.
- **The permanence trigger must not forbid the takedown.** `ADR-0029` §4 raises on `name → NULL`;
  "permanent" is now *permanent to the player*. `ADR-0051` §3 is the route: the function is replaced
  with `CREATE OR REPLACE` in the new migration (never an edit to `V3`), and its single exception is
  scoped to the **transition** — `name → NULL`, and only when that name is already `RETIRED` — never
  to a role, a `current_user` or a GUC. `name → a different name` still raises for everybody,
  including the operator.
- **Homoglyph impersonation is not solved here**, and `ADR-0038` says so rather than implying
  otherwise. A script restriction was on the table and was not chosen.

## Tasks

Split on 2026-08-18 into **twenty-two**, following `ADR-0051`, `ADR-0052` and `ADR-0053`. The chain
is linear: every ticket touches at least one file the one before it touched, and the run is
sequential.

**Two judgements the split had to make, recorded here rather than left in a ticket:**

- **`ADR-0051` §8's migration is split across two `V<n>` files.** `V5` is everything in §8 except
  `ALTER TABLE player ADD CONSTRAINT player_display_name_registered`, which is `V6`. §8 also says
  *"the code lands in the same PR as the migration"* and names the test files that write a display
  name directly — and those two sentences cannot both be obeyed under a three-file ticket cap,
  because the foreign key is what breaks every direct fixture at once. The end schema is identical
  and §8's ordering rule (create and backfill before the key) is preserved by the version order.
  `TASK-041002` carries the full reasoning; merging the two files back is one migration's work if
  the architect prefers it.
- **`ADR-0051` §8's list of seven test files is eight.** `DuelHistoryFilterDatabaseTest` also writes
  `display_name` with raw SQL and is converted by `TASK-041004`.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-041001](../tasks/TASK-041001-the-migration-test-derives-its-version-list.md) | The migration test derives its version list from the migrations it applies | ready |
| [TASK-041002](../tasks/TASK-041002-the-name-registry-its-guards-and-the-takedown.md) | The fifth migration creates the name registry, its guards and the takedown function | backlog |
| [TASK-041003](../tasks/TASK-041003-a-name-is-registered-before-it-is-held.md) | Setting a name registers it first, and a refused claim rolls the whole transaction back | backlog |
| [TASK-041004](../tasks/TASK-041004-three-fixtures-register-the-name-they-hand-a-player.md) | Three fixtures register the name they hand a player | backlog |
| [TASK-041005](../tasks/TASK-041005-the-uniqueness-fixtures-register-every-name.md) | The uniqueness fixtures register every name, and the fold refuses before the index does | backlog |
| [TASK-041006](../tasks/TASK-041006-the-permanence-fixtures-register-only-what-must-land.md) | The permanence fixtures register only the names that must land | backlog |
| [TASK-041007](../tasks/TASK-041007-the-schema-test-keeps-its-refusals-raw.md) | The display-name schema test registers what must land and keeps its refusals raw | backlog |
| [TASK-041008](../tasks/TASK-041008-the-fold-that-refuses-a-case-variant-is-the-registrys.md) | The fold that refuses a case variant is the registry's, and the schema test says so | backlog |
| [TASK-041009](../tasks/TASK-041009-the-held-race-moves-to-the-registry-row.md) | The held race moves to the registry row, and the probe that waits for it follows | backlog |
| [TASK-041010](../tasks/TASK-041010-a-display-name-may-only-be-a-registered-name.md) | The sixth migration makes a display name a registered name or nothing | backlog |
| [TASK-041011](../tasks/TASK-041011-a-registered-name-is-never-released.md) | A registered name is never released, and the only change it may take is TAKEN to RETIRED | backlog |
| [TASK-041012](../tasks/TASK-041012-the-takedown-is-one-function-call.md) | `retire_display_name` takes the name away and leaves the profile unset | backlog |
| [TASK-041013](../tasks/TASK-041013-the-permanence-trigger-has-exactly-one-exception.md) | The permanence trigger has exactly one exception, and it is a transition | backlog |
| [TASK-041014](../tasks/TASK-041014-a-takedown-moves-no-coin.md) | A takedown moves no coin | backlog |
| [TASK-041015](../tasks/TASK-041015-a-retired-name-is-spent-for-everybody.md) | A retired name is spent for everybody, including the player it was taken from | backlog |
| [TASK-041016](../tasks/TASK-041016-a-blocked-name-is-refused-and-the-screen-fails-closed.md) | A blocked name is refused when it is set, and the screen fails closed | backlog |
| [TASK-041017](../tasks/TASK-041017-the-port-test-builds-its-profile-through-the-builder.md) | The port test builds its profile through the shared builder | backlog |
| [TASK-041018](../tasks/TASK-041018-the-profile-says-the-name-was-removed.md) | The profile says the name was removed, from one correlated `EXISTS` | backlog |
| [TASK-041019](../tasks/TASK-041019-two-players-in-one-database.md) | Two players in one database, and only one of them reads true | backlog |
| [TASK-041020](../tasks/TASK-041020-a-takedown-is-invisible-to-everybody-else.md) | A takedown is invisible to everybody else, and its two strings live where they should | backlog |
| [TASK-041021](../tasks/TASK-041021-the-document-contracts-the-removed-name.md) | The protocol document contracts the removed-name field | backlog |
| [TASK-041022](../tasks/TASK-041022-the-operations-document-is-the-only-call-site.md) | `docs/operations.md` is the takedown's only call site | backlog |

### Which ticket answers which acceptance criterion

| Criterion | Ticket |
| --- | --- |
| A blocked name is refused at set time, differing only in case | `TASK-041016` |
| A blocklist that cannot be read refuses the name, planted | `TASK-041016` |
| An operator can take a name away, leaving no name rather than a new one | `TASK-041012` |
| A retired name cannot be claimed again, by anybody including its former holder | `TASK-041015` |
| A retired name is refused in a different case | `TASK-041015` |
| Uniqueness still refuses a held name, and the three sources refuse independently | `TASK-041016` |
| P1 and P2 hold across a force-rename | `TASK-041014` |
| The player whose name was removed is told (`ADR-0052`, `ADR-0053`) | `TASK-041018`, `TASK-041019`, `TASK-041020`, `TASK-041021` |

## Acceptance criteria

- [ ] A blocked name is refused at set time and nothing is written, asserted for a name that differs
      from a blocklist entry only in case.
- [ ] A blocklist that cannot be read refuses the name — the failure is planted, not assumed.
- [ ] An operator can take a name away, and the profile is left with no name rather than a new one.
- [ ] A retired name cannot be claimed again, by anybody, including the player it was taken from —
      asserted for both.
- [ ] A retired name is refused in a different case from the one it was registered in.
- [ ] Uniqueness still refuses a name held by another player, and the three sources of truth are
      each shown to refuse independently.
- [ ] P1 and P2 (`ADR-0030` §5) hold across a force-rename: a takedown moves no coin.

## Out of scope

- A role system, user accounts for operators, or any moderation queue.
- A script or alphabet restriction — named in `ADR-0038` as not chosen.
- The client's rendering of a refusal — `STORY-0411`.
