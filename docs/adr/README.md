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
| [0006](ADR-0006-mandatory-review-gate.md) | Every task ends in a reviewed, merged pull request | Accepted |

## Open decisions

Questions deliberately left open are marked `DEC-NNN` in the document they affect.

| ID | Question | Where | Due |
| --- | --- | --- | --- |
| DEC-001 | What exactly is one duel? | `../duel-rules.md` | before v0.2 |
