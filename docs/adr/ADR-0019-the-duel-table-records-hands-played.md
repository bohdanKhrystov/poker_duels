# ADR-0019 — The duel table records how many hands were played

- **Status:** Accepted
- **Date:** 2026-08-13
- **Resolves:** `DEC-014`
- **Amends:** the schema established by `TASK-020904`

## Context

`STORY-0211`'s result line wants the number of hands a duel lasted. `DuelOutcome` carries it at
write time, but `V1__initial_schema.sql` has no column for it, so `handsPlayed` currently returns
`null` on every result — pinned by a test so no coder invents a number.

The decision had a deadline rather than a preference attached: **a column added after the first real
duel cannot be backfilled.** Every game played before it exists would show a blank forever, in the
list the project owner asked to see.

## Decision

**`duel` gains a `hands_played` column, now, while the table is empty**, via a `V2` migration. The
write path stores `DuelOutcome.handsPlayed`; the read path returns it, and `handsPlayed` stops being
null.

## Consequences

**What it buys.** The result line means what it says, and the decision is made while it is still
free. Doing it later is not merely more work — it is *impossible to do correctly*, because the
information exists only at the moment the duel finishes.

**What it costs.** A second migration and the small ceremony around it: `V2` is the first proof that
the migration chain works with more than one file, which is worth having before it matters.

The column is `NOT NULL`, since every completed duel played some number of hands and a nullable
column would push "did this duel record it?" onto every reader. There are no existing rows, so no
default is needed to backfill.

`handsPlayed` in the DTO stops being nullable. That is a **wire-visible change** to a response that
already ships in `develop`, and the test asserting it is null must go rather than be weakened —
`handsPlayedIsNullWhileTheColumnDoesNotExist` describes a condition that will no longer hold, and
leaving it passing would mean the column was not actually wired through.

**What it forecloses.** Nothing. Final stacks were considered alongside and deliberately left out:
`DuelOutcome` carries them too, but no one has asked to display them, and a column added
speculatively is a column someone must keep correct.

## Alternatives considered

**Drop `handsPlayed` from the read path entirely.** Honest, smaller, and removes a field that is
always null. Rejected: it forecloses showing it later without hitting exactly the same backfill
problem, and the owner's requirement is to see the results of games — how long a duel ran is part of
a result.

**Leave it null until someone needs it.** Zero work now. Rejected on the deadline: the option only
stays open until the first real duel is played, and "we will add it when we need it" is a decision
to lose the data for every duel before that point.

**Derive it from a count of hand rows.** There are none — `MatchLog` is not stored per hand, only
the duel and its two result rows. Rejected as it would require a schema this product does not have.
