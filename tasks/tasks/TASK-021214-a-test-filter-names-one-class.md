---
schema: 2
id: TASK-021214
title: A test filter names one class, so a green run cannot have run nothing
type: task
status: backlog
parent: STORY-0212
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [process, tests]
depends_on: [TASK-021206]
verify:
  - python3 .github/scripts/lint_tickets.py
  - grep -c "\-\-tests '\*SocketDuelTest'" tasks/tasks/TASK-021211-a-dropped-socket-rejoins.md | grep -qx 0
  - grep -q 'names exactly one class' tasks/README.md
---

## Goal

No ticket's `verify:` block can report success while the suite it names never ran.

## What was found

Found in `TASK-021206`. A stale Gradle daemon in a fresh worktree held a VFS snapshot predating the
`e2e` test sources, so `compileTestKotlin` produced no classes for that package **while reporting
success**. The verify command then ran:

```
./gradlew :poker-server:test --tests '*SocketDuelTest'
```

`*SocketDuelTest` matches **two** classes — the intended `duels.poker.server.e2e.SocketDuelTest` and
the unrelated, pre-existing `duels.poker.server.DuelSocketDuelTest`. With the intended one missing,
the filter still matched the other, which passed. **Exit 0, and the ticket's own suite never ran.**

Gradle fails a filter that matches nothing. It does not fail one that matches something else, and a
`*Suffix` wildcard silently matches every class whose name ends that way.

A scan of all 224 `--tests '*X'` patterns across the backlog finds four that match more than one
existing class:

| Pattern | Matches |
| --- | --- |
| `*SocketDuelTest` | `SocketDuelTest`, `DuelSocketDuelTest` |
| `*RoomTest` | `RoomTest`, `DuelSocketRoomTest` |
| `*HandshakeTest` | `HandshakeTest`, `DuelSocketHandshakeTest`, `ServerMessageHandshakeTest` |
| `*DuelResultSinkTest` | `DuelResultSinkTest`, `PostgresDuelResultSinkTest` |

Three of those four sit in tickets already `done`, whose work CI verified with a full
`./gradlew check` on a clean runner — nothing landed is at risk. Only `TASK-021211` is still
unstarted and still carries an ambiguous filter.

## Files

| File | Action |
| --- | --- |
| `tasks/tasks/TASK-021211-a-dropped-socket-rejoins.md` | modify |
| `tasks/README.md` | modify |

## Scope

- In `TASK-021211`'s `verify:` block, replace `--tests '*SocketDuelTest'` with the fully qualified
  `--tests 'duels.poker.server.e2e.SocketDuelTest'`. Change nothing else in that ticket.
- In `tasks/README.md`, where the `verify:` block is described, add the rule: **a `--tests` filter
  names exactly one class.** Prefer the fully qualified name; a `*Suffix` wildcard is only safe when
  no other class ends the same way, and nothing stops one being added later. State the failure it
  prevents — a filter that matches a sibling reports success while the intended suite never ran.

## Out of scope

- The three `done` tickets' verify blocks. Their work was verified by CI's full `check`; editing a
  landed ticket's commands changes the record without changing anything real.
- The stale-daemon behaviour itself. `./gradlew --stop` is the workaround; making the build immune is
  a much larger question and not this ticket's.

## Tests

No new test. The evidence is the two `grep` commands in `verify:` and a green backlog lint.

## Acceptance criteria

- [ ] `TASK-021211` names one fully qualified class in its `verify:` block
- [ ] `tasks/README.md` states the one-class rule and the failure it prevents
- [ ] `python3 .github/scripts/lint_tickets.py` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
