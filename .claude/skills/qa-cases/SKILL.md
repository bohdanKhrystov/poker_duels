---
name: qa-cases
description: Authors catalogue cases for named epics from merged sources — Definitions of done, ADRs, docs/duel-rules.md, docs/vision.md and the client's own literals — and lands them through build-epic. Runs no browser and never starts a cycle. Use when the human asks to write QA cases for an epic, or catch the catalogue up before a round.
---

# Authoring the catalogue

`/qa-cases EPIC-04 EPIC-05` plans and lands the missing suite for the named epics, through
ordinary reviewed tickets, and then stops. It is the first of the two commands
[`ADR-0090`](../../../docs/adr/ADR-0090-a-skill-may-write-the-catalogue-or-run-it-never-both.md)
§3 fixes; the second is the human's own, typed separately, on a later turn.

Before writing a case, read the named epic's Definition of done and
[`docs/test-plan.md`](../../../docs/test-plan.md) §*How a case is written* and §*Per-epic suites*.

## What it may do

- Read the epics, the ADRs they name, `docs/duel-rules.md`, `docs/vision.md` and the client's
  own literals for any player-facing text a case quotes.
- Plan a story and its tickets that write the suite.
- Run them through `build-epic`, so every case lands as an ordinary reviewed PR.
- Update `docs/test-plan.md`, including striking the epic's row from §*Not yet written*.

## What it may not do

- **Bring the stack up.** No `scripts/qa/stack.sh`, no database, no server.
- **Start a browser.** No Chrome profile, no `scripts/qa/drive.mjs` call.
- **Dispatch `qa` or `qa-manager`.** Both belong to the cycle, never to authoring.
- **Invoke `/qa-cycle`, by any route** — not directly, not conditionally, not through a wrapper.

## Writing a case (`ADR-0090` §4)

Every row gets a fifth column, `source`, next to `id`, `do`, `expect`, `fails if`: the module
holding the literal for player-facing text, otherwise an ADR section or a `docs/duel-rules.md`
heading. **A case whose expectation has no merged source is not written** — register a `DEC-NNN`
in `docs/adr/README.md` for the **product owner**, and leave the row out until it answers.

## The marker (`ADR-0090` §5)

A suite filled in before an epic is tested is provisional. Carry this line above it, copied
byte-for-byte — the round that first tests the suite is what deletes it:

> **Provisional** — authored YYYY-MM-DD from merged sources, not yet run (`ADR-0090` §5).

## Report

Close every run — including one that stalls on a `DEC` — by naming what was written, what was
deferred, and the command the human types next, with the scope filled in:

```
/qa-cycle epic EPIC-04
```

For more than one epic, name `regression` instead — the only scope that reaches them all in one
command — and say plainly that it also covers every other suite in the catalogue, including ones
this pass did not write. It prints that line; it does not run it.
