# ADR-0015 — A draw writes two result rows of zero, not no rows

- **Status:** Accepted
- **Date:** 2026-08-13
- **Clarifies:** `ADR-0014` — which decided what a draw *pays*, not what it *records*
- **Constrains:** `STORY-0210` (the write path), `STORY-0211` (the read path)

## Context

`ADR-0014` settled the economy: winner +1, loser −1, **a draw pays nothing**. It said nothing
about whether a drawn duel produces rows in `duel_result`, because paying nothing and recording
nothing are easy to conflate and only one of them was decided.

The gap became visible when `STORY-0210` was split. The merged migration
`V1__initial_schema.sql` carries the comment:

```sql
-- ADR-0014: signed, like coin_balance above — winner +1, loser -1, draw absent (no row).
```

while the story requires *one result row per participant*. Those cannot both be true, and the
migration is merged — migrations are never edited, so the comment cannot simply be corrected.

The question is not cosmetic. `STORY-0211` renders a player's recent duels by joining
`duel_result` per player. If a draw writes no rows, a drawn duel is **invisible** in that list:
it happened, it is in the `duel` table, and the player's history silently omits it.

## Decision

A drawn duel writes **two `duel_result` rows, each with `coin_delta = 0`.**

- Every participant of every completed duel has exactly one `duel_result` row. No exceptions,
  so no consumer needs a special case.
- Neither balance moves, which is what `ADR-0014` actually requires.
- The comment in `V1__initial_schema.sql` is **wrong as written** and stands uncorrected because
  the migration is immutable. This ADR is the source of truth; the comment is a historical
  artefact. Read it as loose wording about the *balance*, which does not move.

## Consequences

**What it buys.** `duel_result` becomes a complete record of participation rather than a record
of coin movement, and "show me my recent duels" is one join with no special case. The project
owner's requirement for v0.1 is to see the results of games stored — a draw is a result, and a
history that quietly drops draws is not the honest record this project is otherwise careful to
keep.

It also keeps the invariant that makes the read path simple: the number of `duel_result` rows for
a duel is always two. A consumer can treat a different count as corruption rather than as a draw.

**What it costs.** Two rows that carry no economic information, and a permanent disagreement
between this ADR and a comment in `V1`. The disagreement is the real cost: someone reading the
schema first will believe the wrong thing. That is the price of immutable migrations, and it is
cheaper than a corrective migration whose only effect is to reword a comment.

**What it forecloses.** Nothing. Deriving a balance as `SUM(coin_delta)` still works, because the
added rows are zero.

## Alternatives considered

**No rows for a draw, as the `V1` comment says.** Slightly less data, and defensible if
`duel_result` were only a ledger of coin movements. Rejected: it makes a drawn duel invisible to
the per-player read path, and it forces every consumer to reconstruct "did this player play in
this duel?" from the `duel` table instead of the results table.

**One row for the duel rather than one per player.** Rejected: the composite key is
`(duel_id, player_id)`, and a per-duel row cannot express a per-player delta at all.

**A corrective migration `V2` to reword the `V1` comment.** Rejected as ceremony. It changes no
schema object, and a migration whose entire content is a comment is worse noise than the
disagreement it fixes.
