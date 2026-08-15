# ADR-0039 — v0.1 offers no account deletion, and the schema keeps every answer open

- **Status:** Accepted
- **Date:** 2026-08-15
- **Resolves:** `DEC-029` — **the human's call**, made as *"no deletion in v0.1"*.
- **Constrains:** `STORY-0403`'s schema, which must not foreclose the answer this ADR declines to
  give.

## Context

`DEC-029` was registered before any credential existed, for a stated reason: so that the
credential schema would not answer the deletion question silently, in the way `ADR-0021` refused
to answer `DEC-017` by adding a `UNIQUE` constraint nobody had asked for.

The hard part was never the deleting player. It is the opponent. A `duel_result` row is *two*
players' history — the row that says someone lost to Ann is also the row that says you beat her,
and it is the row your coin balance was computed from. Deleting one player's data therefore
edits another player's record, and there is no arrangement in which that is free.

## Decision

**v0.1 offers no account deletion.** There is no delete path, no request flow, and no
tombstone. This is recorded as a position rather than left implied by the absence of a button,
which is the entire reason `DEC-029` was registered in the first place.

**The schema must keep both real answers reachable.** Concretely, `STORY-0403` and everything
downstream may not make either of these harder than it is today:

- **Anonymisation** — scrubbing the `player` row and its credentials while `duel_result` rows
  survive intact. This requires that no `duel_result` row *copy* a display name; it must join to
  `player` for it, as `ADR-0021` already has it do. Denormalising a name into the result rows for
  read performance would foreclose this answer, and is therefore forbidden without a new ADR.
- **Cascading delete** — removing the rows outright. This requires that coin balances remain
  derivable from, or reconcilable with, the surviving rows rather than being a free-floating
  counter that silently disagrees once rows disappear.

**No deletion mechanism is designed here**, deliberately. Choosing between anonymisation and
cascade in advance of needing either would be guessing at a question whose answer may be
determined by an obligation this project does not yet have.

## Consequences

- `EPIC-04` builds no deletion, and its definition of done does not include one.
- **The prohibition on denormalising a display name into `duel_result` is a real constraint with
  a real cost.** The history read path joins for the name and will keep joining. That is the
  price of keeping anonymisation available, and it is stated here so a future performance ticket
  encounters a decision rather than an easy-looking optimisation.
- If a legal obligation to delete arrives, it arrives against a schema that can serve it, and the
  work is a story rather than a migration of history nobody can reconstruct.
- **A player who wants out today has no path**, and the product should not pretend otherwise. If
  that becomes untenable before the mechanism is built, the answer is to build the mechanism, not
  to quietly widen this ADR.
- `ADR-0038`'s retired-name set interacts with whatever deletion eventually does: a deleted
  player's name would need a rule — released, retained, or retired — and this ADR does not settle
  it, since there is nothing yet to settle it for.

## Alternatives considered

**Anonymise on deletion, keeping `duel_result` intact.** The likely eventual answer, and the one
this ADR is careful to keep available. Not chosen now because building it means designing the
tombstone, the name rule and the read-path fallback for a feature nobody has asked for, in an
epic that has enough scope.

**Hard delete with cascade.** The cleanest privacy story and the worst record-keeping one: it
silently rewrites histories belonging to people who did not delete anything, and it can leave a
coin balance that no longer matches the duels behind it. Rejected as a default; not foreclosed,
because a legal obligation may not care about the objection.
