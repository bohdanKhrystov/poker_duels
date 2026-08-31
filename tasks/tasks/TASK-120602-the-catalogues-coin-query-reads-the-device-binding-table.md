---
schema: 2
id: TASK-120602
title: The catalogue's coin query reads the table the device id actually lives in
type: task
status: done
parent: STORY-1206
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [process, qa, harness]
depends_on: []
verify:
  - test 0 -eq "$(grep -cF 'p.device_id' docs/test-plan.md)"
  - test 0 -eq "$(grep -cF 'player.device_id' docs/test-plan.md)"
  - grep -qF 'device_binding' docs/test-plan.md
  - grep -qF 'revoked_at IS NULL' docs/test-plan.md
  - grep -qF '`05-03`' docs/test-plan.md && grep -qF '`CORE-13`' docs/test-plan.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The one SQL template `docs/test-plan.md` prescribes runs against the schema this product actually
has, instead of failing with `column p.device_id does not exist` for every tester who copies it.

## This is a harness defect, and what that means here

Filed under `ADR-0089` §4 and `EPIC-12` §Termination rule 6. It is **excluded from `B(2)`**, and
**no production code may be changed by this ticket** — the fix is one document. A `## Files` table
naming anything outside `docs/` is grounds to reject the diff on sight.

## The defect

Round 2 of `/qa-cycle regression`, 2026-08-29, commit `c7b35f4b`, reported as a process note rather
than as a finding, which is the right call: no product behaviour is involved.

`docs/test-plan.md`'s `05-03` preamble carries the round's only prescribed query:

    "SELECT p.device_id, SUM(dr.coin_delta)
       FROM player p
       JOIN duel_result dr ON dr.player_id = p.id
       JOIN duel d ON d.id = dr.duel_id
      WHERE p.device_id IN ('<A device>', '<B device>')
      ...
      GROUP BY p.device_id"

and the sentence beneath it: *"`player.device_id` is unique (`V1__initial_schema.sql`)"*.

**`player.device_id` has not existed since `V7`.** `V7__device_binding.sql` moves the edge into its
own table on `ADR-0049` §1 and ends with
`ALTER TABLE player DROP COLUMN device_id`. Confirmed against the running database this round:
`\d player` lists `id`, `coin_balance`, `created_at`, `display_name` and nothing else, while
`device_binding` holds `device_id`, `player_id`, `bound_at`, `revoked_at`.

The tester joined through `device_binding` to get their readings, so the round's numbers are sound;
what is broken is the instruction the **next** round copies.

**One correction to the report.** It says the template is *"`05-03`'s and `CORE-13`'s"*. `CORE-13`'s
row prescribes no SQL at all — it says *"`psql` the `duel_result` and `player` rows"* — so there is
exactly **one** stale query, in the `05-03` preamble, which that preamble itself calls
*"`CORE-13`'s shape"*. That is why a `CORE-13` tester lands on it, and why fixing the one block
fixes both readings. `CORE-13`'s row is not rewritten: `ADR-0090` §4 says `SMOKE` and `CORE` are not
retrofitted.

## Files

| File | Action |
| --- | --- |
| `docs/test-plan.md` | modify |

## Scope

- **Rewrite the query to join `device_binding`**, keeping what it is for: a season-bounded
  `SUM(dr.coin_delta)` per browser, reached from the two device ids `A device` and `B device` print,
  with the driver still never learning a `player_id`.
- **Restrict to live bindings.** `revoked_at IS NULL` is not decoration: `device_binding`'s
  uniqueness is the partial index `device_binding_live_device`, so a query without that predicate can
  return two rows for one device once `ADR-0049`'s revocation has been exercised.
- **Replace the uniqueness sentence** with what is true now — one live binding per device, by
  `device_binding_live_device` in `V7__device_binding.sql` — rather than deleting it, since it is
  what licenses reading a browser as one row.
- **Add one sentence** to the paragraph naming the migration a reader should check when a query in
  this document stops running, so the next stale query is found by reading rather than by a failed
  round.

## Out of scope

- **Any file outside `docs/test-plan.md`.** Named again because it is the rule this ticket exists
  under, not a preference.
- **`CORE-13`'s row**, and every other `SMOKE` or `CORE` row (`ADR-0090` §4).
- **Giving `CORE-13` a query of its own.** It has never had one; that it has none is a separate,
  smaller gap and is not this ticket's.
- **Running the query.** No `verify:` command here brings a stack up: `ADR-0089` §2b forbids a
  `verify:` block waiting on a QA case, so these gates check the text and the next round checks the
  query. Say so rather than implying more.

## Tests

None — the deliverable is document text, so the gates are structural checks over that text, the
shape `TASK-120201` and `TASK-120503` established.

| Gate | Proves | Today |
| --- | --- | --- |
| `test 0 -eq $(grep -cF 'p.device_id' …)` | the dropped column is not selected, filtered or grouped on | **exits 1** — three occurrences |
| `test 0 -eq $(grep -cF 'player.device_id' …)` | the prose no longer claims the column exists | **exits 1** — one occurrence |
| `grep -qF 'device_binding'` | the query reaches the table the edge actually lives in | **exits 1** — the word is absent |
| `grep -qF 'revoked_at IS NULL'` | it counts live bindings only | **exits 1** — absent |
| both ids still present | no row was deleted to satisfy the four above | exits 0 — it must keep doing so |

All five were run against `docs/test-plan.md` at commit `c7b35f4b`; the first four exit `1` and the
fifth exits `0`.

## Acceptance criteria

- [ ] `docs/test-plan.md` names no column of `player` that `V7__device_binding.sql` dropped.
- [ ] The query joins `device_binding` and restricts to `revoked_at IS NULL`.
- [ ] The uniqueness sentence names `device_binding_live_device` in `V7`, not `V1`.
- [ ] `05-03` and `CORE-13` are both still in the document, each with its cells intact.
- [ ] The diff touches exactly one file, and it is `docs/test-plan.md`.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
