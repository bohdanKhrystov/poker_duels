# ADR-0007 — Token-lean agent workflow

- **Status:** Accepted
- **Date:** 2026-08-10
- **Amends:** [ADR-0006](ADR-0006-mandatory-review-gate.md) — review is now tiered by risk, and
  merging is automated. The requirement that every task ends in a reviewed, merged PR stands.

## Context

The project runs on a personal subscription. Budget, not capability, is the binding constraint,
and the first ticket produced hard evidence of how badly the original design fitted it:

| Operation | Cost | For |
| --- | --- | --- |
| `/code-review high` | **132 379 tokens**, 18 agents | one documentation PR |
| one background fork | **185 227 tokens**, 76 tool uses | re-deriving context from cold |

The review exhausted the session limit and returned nothing usable: 14 of its 18 agents died
mid-run, `verified: 0, refuted: 0`. We paid for a fan-out review of a markdown change and got an
inconclusive result.

Three lessons, none of them subtle in hindsight:

1. **Reading dominates.** Generation is cheap. Context — re-sent every turn, re-derived by every
   cold subagent — is what costs.
2. **Uniform review effort is mispriced.** A `data class` and a min-raise calculation do not
   warrant the same scrutiny, and pretending otherwise means either overpaying everywhere or
   underpaying where it matters.
3. **Vague acceptance criteria are unaffordable on a cheap model.** "Handles edge cases
   correctly" fails on every retry, and each retry costs a full cold start.

## Decision

### One capable planning pass, then cheap execution

```
PLANNER  (Opus, high effort)   once per story
   ↓  freezes thinking into tickets
CODER    (Haiku by default)    once per ticket, fresh context
   ↓
REVIEWER (Haiku by default)    once per ticket, fresh context
   ↓
PR → auto-merge
```

Expensive reasoning happens **once** and is frozen into artifacts that cheap agents consume. No
mid-level planner: it would pay a second cold start to re-derive what the first already knows.

The driver (`/build-epic`) is a **scheduler, not a participant**. It reads no source, writes no
code, reviews no diff, and keeps one line per finished ticket. Its context stays flat across an
epic instead of growing with it.

### Done is an exit code

Every schema-2 ticket carries executable acceptance criteria:

```yaml
verify:
  - ./gradlew :poker-engine:test --tests '*CardTest'
```

**Done means every command exits 0.** Nothing is left to a small model's judgement. Criteria map
one-to-one onto named tests, and the ticket names those tests so the coder writes the thing the
gate runs.

A criterion that cannot be a passing test is not a criterion. It is a ticket that needs
sharpening or splitting.

### Tickets shrink

| | Before | Now |
| --- | --- | --- |
| Sizes | S ≤ 100, M ≤ 300 | **XS ≤ 40, S ≤ 120** — `M` removed |
| Files touched | ≤ 10 | **≤ 3** |
| Files readable | unbounded | **≤ 5, named in the ticket** |

The named file list *is* the context budget. An agent that greps the repository to "understand
the codebase" has already overspent.

### Model tier per ticket, promoted on failure

`tier: haiku` is the default. `verify` fails twice → the driver rewrites the ticket to
`tier: sonnet`, retries, and **commits that change with the work**. Three failures → `blocked`.

Guessing low is the cheaper mistake: a wrong guess costs one fast failed attempt, and the record
of which tickets Haiku could not handle is real data for the case study rather than speculation.

### Review is priced by risk

| `review:` | Mechanism | For |
| --- | --- | --- |
| `light` | reviewer subagent (Haiku) | types, parsing, config, wiring — most tickets |
| `standard` | reviewer subagent (Sonnet) | ordinary logic |
| `deep` | reviewer subagent + `/code-review low` | hand evaluation, betting rules, pot and showdown, card secrecy, chip conservation |

`/code-review high` is never invoked from a loop. It remains available as a deliberate,
human-initiated act.

### Strictly sequential

One ticket at a time. Parallel agents on a shared codebase produce merge conflicts and
half-finished branches, and the coordination costs more than the concurrency saves at this size.

### Merging is automated

This is the part that changes ADR-0006, and it is the part most worth arguing about.

ADR-0006 put a human on the merge button as the checkpoint compensating for a permissive
permission model. That checkpoint is removed: **a PR merges automatically when every objective
gate is green.**

The gates that replace the human:

- every `verify` command exits 0,
- the reviewer subagent returns `pass`,
- CI is green,
- exactly one squashed commit per ticket, so any merge is one `git revert` away.

The trade, stated plainly: **nothing human-checked reaches `develop` any more.** A subtly wrong
poker rule now merges silently rather than being caught by a person reading the diff. That risk
is accepted deliberately, and it is mitigated by where the tests point — chip conservation,
determinism, evaluator agreement against a brute-force oracle, and the assertion that folded
hole cards appear in no event. Those are exactly the failures a human skim would miss anyway.

The goal this serves is the one that was asked for: *state a goal, answer only key decisions*.

### Decisions are batched, never guessed

An agent that meets a question no ADR answers registers a `DEC-NNN`, marks the ticket `blocked`,
and **continues to the next startable ticket**. Questions accumulate and are presented together
at the end of a run.

No agent may decide an open question to keep the loop moving. A wrong decision propagates into
everything built on top of it, and that is the one failure this budget cannot absorb.

## Consequences

**Gained**

- Per-ticket cost roughly an order of magnitude lower: a small model, a small context, and no
  fan-out review.
- Driver context stays flat across an epic rather than growing with it.
- "Done" is objective, which is what makes a cheap model trustworthy.
- One command per epic instead of one interaction per ticket.

**Cost**

- Haiku will fail tickets Sonnet would pass, and each costs a retry. Cheap to detect — an exit
  code in seconds — but not free.
- Planning is front-loaded and expensive, and a bad `verify` command is now the most expensive
  possible defect: it makes a correct implementation look like a failure and burns three
  dispatches plus a promotion before anyone notices. `/plan-story` therefore dry-runs the
  `verify` commands before accepting a plan.
- No human reads the code before it lands on `develop`.
- Two ticket schemas coexist while EPIC-01 migrates story by story.

## Alternatives considered

- **Sonnet everywhere** — simpler and materially more expensive across ~50 tickets, and it never
  teaches us where Haiku's limit actually is.
- **Parallel ticket execution** — rejected under the explicit constraint that it produces
  unfinished work in flight, and it conflicts on a shared codebase.
- **Human merges everything** — the status quo from ADR-0006. Rejected because ~50 merge
  interactions per epic is the thing the redesign exists to remove.
- **Auto-merge only low-risk tickets, hold correctness-critical ones for a human** — proposed,
  and genuinely the safer design. Not chosen: full autonomy was the explicit requirement. Worth
  revisiting if a wrong rule ever reaches `develop`, and the metrics in `tasks/BOARD.md` are
  what would show it.
