# Architecture decision records

One file per significant decision. Small, dated, immutable.

An ADR is never edited to change its decision. If a decision is reversed, write a new ADR that
supersedes the old one and mark the old one `Superseded by ADR-NNNN`. The wrong turns are as
valuable as the right ones — especially for Product B.

## When to write one

Write an ADR when a choice:

- constrains something outside its own module,
- was contested, or had a plausible alternative,
- will make a future reader ask *"why is it like this?"*.

Do not write one for choices the code already makes obvious.

## Naming

`ADR-NNNN-short-kebab-title.md`, sequential, never reused.

## Template

```markdown
# ADR-NNNN — Title

- **Status:** Proposed | Accepted | Superseded by ADR-MMMM
- **Date:** YYYY-MM-DD

## Context
The forces at play. What makes this a real decision.

## Decision
What we are doing. Present tense, unambiguous.

## Consequences
What this buys, what it costs, and what it forecloses.

## Alternatives considered
Each with the reason it was not chosen.
```

## Index

| ADR | Title | Status |
| --- | --- | --- |
| [0001](ADR-0001-event-sourced-engine-contract.md) | Event-sourced engine contract | Accepted |
| [0002](ADR-0002-server-authoritative.md) | The server is authoritative | Accepted |
| [0003](ADR-0003-technology-stack.md) | Technology stack | Accepted |
| [0004](ADR-0004-branching-and-ticket-workflow.md) | Branching and ticket workflow | Accepted |
| [0005](ADR-0005-analysis-behind-an-interface.md) | Hand analysis sits behind an interface | Accepted |
| [0006](ADR-0006-mandatory-review-gate.md) | Every task ends in a reviewed, merged pull request | Amended by 0007 |
| [0007](ADR-0007-token-lean-agent-workflow.md) | Token-lean agent workflow | Accepted |
| [0008](ADR-0008-loser-mucks-at-showdown.md) | The loser mucks at showdown | Accepted |
| [0009](ADR-0009-match-events-are-their-own-hierarchy.md) | Match events are their own hierarchy | Accepted |
| [0010](ADR-0010-engine-takes-a-serialization-dependency.md) | The engine may depend on kotlinx.serialization | Accepted |
| [0011](ADR-0011-postgres-in-v01.md) | PostgreSQL lands in v0.1 | Accepted — amends 0003 |
| [0012](ADR-0012-device-bound-anonymous-profiles.md) | Anonymous profiles, bound to a device | Accepted |
| [0013](ADR-0013-disconnect-grace-period.md) | A dropped connection gets a grace period, then folds | Accepted |
| [0014](ADR-0014-duel-coin-economy.md) | The winner takes a coin, the loser gives one, a draw pays nothing | Accepted |

## Open decisions

Questions deliberately left open are marked `DEC-NNN` in the document they affect.

| ID | Question | Where | Due |
| --- | --- | --- | --- |
| DEC-001 | What exactly is one duel? | `../duel-rules.md` | before v0.2 |
| DEC-002 | What performance budget does the hand evaluator carry, how is it measured, and does `HandRank` become a packed integer? | `../../tasks/stories/STORY-0103-hand-evaluator.md` | before STORY-0108 |
| DEC-007 | How are the TypeScript protocol types generated from the Kotlin definitions, and what stops the checked-in output drifting? | `../../tasks/stories/STORY-0203-generated-typescript-protocol.md` | before EPIC-03 |
| DEC-008 | Is the full `MatchLog` persisted in v0.1, and where — a column, a table per hand, or object storage? | `../../tasks/stories/STORY-0209-postgres-schema-and-migrations.md` | before EPIC-08 |
| DEC-009 | Can a duel be watched, and if so what may a spectator see and when? | `../../tasks/stories/STORY-0204-player-view-projection.md` | before v0.2 |

## Answered decisions

| ID | Question | Answered by |
| --- | --- | --- |
| DEC-005 | Where does a match-level event live? | [ADR-0009](ADR-0009-match-events-are-their-own-hierarchy.md) — its own `MatchEvent` hierarchy |
| DEC-006 | Where does event-log serialisation live, and in what format? | [ADR-0010](ADR-0010-engine-takes-a-serialization-dependency.md) — kotlinx.serialization, inside the engine, behind a narrowed guard |

