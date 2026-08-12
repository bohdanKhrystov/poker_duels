---
schema: 2
id: TASK-010812
title: Headless simulation harness and invariant fuzzing
type: task
status: blocked
parent: STORY-0108
module: poker-ai
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [simulation, test, blocked]
depends_on: [TASK-010809, TASK-010811]
verify:
  - ./gradlew check
---

## Goal

A hundred thousand duels play with no UI and no crash, every invariant checked after every
action, and a failure hands back the `(seed, actions)` pair that reproduces it.

## Blocked on STORY-0107

**Do not implement this ticket.** A duel is a *match*, and the match layer does not exist yet:
[`STORY-0107`](../stories/STORY-0107-duel-format-and-match.md) introduces the duel format, the
blind schedule, match progression across hands and the value a finished duel produces. A harness
written before those types exist would invent an API and be rewritten. No `depends_on` entry
points at a `STORY-0107` task on purpose: that story is being split separately and its task ids
are not this planner's to invent.

What is *not* blocked, and is already ticketed: `Bot` and `RandomBot` (`TASK-010809`), and the
hand-level invariant checks this harness reuses rather than rewrites — `RandomHandPlayer.kt` with
`BettingInvariantTest` (`TASK-010521`) and `SettlementInvariantTest` (`TASK-010613`).

Once `STORY-0107` has merged, this ticket is **re-split** by the splitter into at least a runner,
an invariant set, a failure report and the tagged long run. The sketch below is a shape, not a
sized unit.

## Files (provisional)

| File | Action |
| --- | --- |
| `poker-ai/src/main/kotlin/duels/poker/ai/SimulationRunner.kt` | create |
| `poker-ai/src/main/kotlin/duels/poker/ai/SimulationReport.kt` | create |
| `poker-ai/src/test/kotlin/duels/poker/ai/SimulationTest.kt` | create |

## Scope (provisional)

- `SimulationRunner(bots, format, count, seed)` plays `count` duels and returns one aggregate
  value: duels completed, hands played, average hands per duel, and showdown category
  frequencies as a sanity check on the evaluator.
- Invariants after **every** action across the whole run: chip conservation, no duplicate card in
  play, action always on a seat that can act, no negative stack, and a bounded hand length.
- The first violation stops the run and reports the seed and the action sequence that reproduces
  it — a bug report is two lines of data, per `ADR-0001`.
- Determinism: the same seed gives the same aggregate report, every run, on any machine, drawing
  only from `Rng`.
- Single-threaded. Correct first; the numbers here do not need concurrency.
- A short run — 1 000 duels — belongs to the normal test suite; the 100 000-duel run is tagged
  for on-demand execution so CI stays fast.
- `DEC-002` — the hand evaluator's performance budget — is due before this run: 100 000 duels is
  the first thing in the project that makes evaluator speed observable. Whoever re-splits this
  ticket should check whether it has been answered.

## Out of scope

- Bots that play well — EPIC-09.
- A CLI front end — `poker-cli`.
- Persisting the simulated matches — EPIC-02.

## Acceptance criteria

- [ ] Not startable. `STORY-0107`'s match types have merged before this ticket is re-split and
      any code is written.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
