---
id: STORY-0410
title: The display-name product rules — screened when set, and takeable away
type: story
status: blocked
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

## Blocked on

**`DEC-042` — the architect's.** `ADR-0038` fixes that an operator *may* force-rename a profile and
does not say by what path. A new authenticated admin HTTP surface, a Gradle or CLI task run against
the database, and a documented SQL procedure are three defensible answers with very different blast
radii — an admin endpoint on a public server with no role system is a security surface this epic has
not budgeted, and the ADR is explicit that the path *"does not need a role system to exist and will
not grow one speculatively"*. This must be answered once, in an ADR, before the story is split.

## Design notes

- **Uniqueness gains two more sources of truth** (`ADR-0038`): names in use, names retired by a
  takedown, and the blocklist. All three are consulted case-insensitively under the ICU collation
  `ADR-0029` §1 pinned — which is what puts all three in the database rather than in a resource file
  the JVM folds differently.
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
  "permanent" is now *permanent to the player*, so the operator path needs a route through that
  trigger that a player's `UPDATE` does not have. Which route is part of `DEC-042`.
- **Homoglyph impersonation is not solved here**, and `ADR-0038` says so rather than implying
  otherwise. A script restriction was on the table and was not chosen.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Blocked on `DEC-042`. Not split.* | — |

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
