---
schema: 2
id: TASK-140717
title: A refused player may press again, and a test says so
type: task
status: backlog
parent: STORY-1407
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [client, profile]
depends_on: [TASK-140712]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/NameAsk.test.tsx > "${TMPDIR:-/tmp}/ask9.txt" 2>&1; grep -qF 'Tests  9 passed (9)' "${TMPDIR:-/tmp}/ask9.txt"
  - cd web-client && grep -qF 'sends a second write after a refusal settles' src/profile/NameAsk.test.tsx
  - cd web-client && cp src/profile/NameAsk.tsx "${TMPDIR:-/tmp}/ask.fixed.tsx" && perl -0pi -e 's{submitInFlight\.current = false;}{}g' src/profile/NameAsk.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/NameAsk.test.tsx > "${TMPDIR:-/tmp}/r1.txt" 2>&1; cp "${TMPDIR:-/tmp}/ask.fixed.tsx" src/profile/NameAsk.tsx; cmp -s "${TMPDIR:-/tmp}/ask.fixed.tsx" src/profile/NameAsk.tsx && grep -qF 'Tests  1 failed | 8 passed (9)' "${TMPDIR:-/tmp}/r1.txt" && grep -qF 'sends a second write after a refusal settles' "${TMPDIR:-/tmp}/r1.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A player refused with a **retryable** outcome types a different name and presses again. That
second write must reach the server.

The behaviour already works — `TASK-140712` resets `submitInFlight.current` when a write settles.
What is missing is a test that would notice if it stopped working.

## Why this is its own ticket

Found while reviewing `TASK-140712`, and **measured rather than suspected**: deleting the guard's
reset — so the component permanently refuses every write after the first press — leaves **all eight
of that ticket's tests green**. A build in which one refusal locks a player out of naming
themselves for the life of the screen would therefore ship on a full green run.

`TASK-140712` is not at fault. Its `Tests` table names four cases and none of them is a retry, and
its *Out of scope* excludes *"a retry that resends by itself"* — which is a **different thing**: that
clause refuses an **automatic** resend, while this ticket is about the player pressing the button a
second time themselves. So the gap is real, is outside that ticket's promise, and belongs here.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/NameAsk.test.tsx` | modify |

Read [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md)
§§2–4 and `web-client/src/profile/NameAsk.tsx` — the component under test, which is **not edited**.
**Nothing outside the table above is changed.**

## Scope

One test: *sends a second write after a refusal settles*.

It presses submit, settles the write with a refusal `mayTryAgain` leaves open, types a different
name, presses again, and asserts the write count is **two**.

The two names must differ. A test that resubmits the same string cannot distinguish a component
that allowed the write from one that deduplicated it.

## Out of scope

- **Editing `NameAsk.tsx`.** The behaviour is correct; only the proof is missing. If the test fails
  against the shipped component, stop and report — that would mean the behaviour is wrong too, and
  it becomes a different ticket.
- **The refusal a player may not retry.** `TASK-140712` already covers the form withdrawing and the
  skip standing; this ticket is only about the retryable branch.
- **Any second write path** — the rename in `STORY-1409` is not this.

## Tests

| Test | What it refuses |
| --- | --- |
| `sends a second write after a refusal settles` | a guard that never reopens, so one refusal ends the player's ability to name themselves |

## Acceptance criteria

1. `NameAsk.test.tsx` reports `Tests  9 passed (9)`.
2. Deleting `submitInFlight.current = false;` from the component reddens **only** the new test,
   reporting `Tests  1 failed | 8 passed (9)`, and the component is restored byte-for-byte.
3. `NameAsk.tsx` is byte-unchanged in the diff.

## Definition of done

- [ ] Every command in `verify:` exits 0.
- [ ] The mutation in criterion 2 was run, observed red, and reverted.
- [ ] PR merged into `develop`.
