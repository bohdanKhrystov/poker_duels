---
id: STORY-0410
title: The display-name product rules — screened when set, and takeable away
type: story
status: backlog
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

| ID | Title | Status |
| --- | --- | --- |
| — | *Not split yet. `DEC-042` is answered; the split follows `ADR-0051`.* | — |

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
