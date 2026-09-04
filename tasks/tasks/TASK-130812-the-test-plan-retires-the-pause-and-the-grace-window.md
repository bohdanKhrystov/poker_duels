---
schema: 2
id: TASK-130812
title: The test plan retires the pause case and the grace window it measured against
type: task
status: done
parent: STORY-1308
module: docs
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [docs, test-plan, clock]
depends_on: [TASK-130811]
verify:
  - sh -c 'test -f docs/test-plan.md && ! grep -q "DUEL_PAUSED" docs/test-plan.md'
  - sh -c '! grep -q "DEFAULT_DISCONNECT_GRACE_MILLIS" docs/test-plan.md'
  - sh -c '! grep -q "The duel is paused." docs/test-plan.md'
  - sh -c 'test "$(grep -c "^| .CORE-2[123]." docs/test-plan.md)" -eq 3'
  - sh -c 'test "$(grep -c "^| .CORE-1[89]." docs/test-plan.md)" -eq 2'
  - sh -c '! grep -qi "forfeit" docs/test-plan.md'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`docs/test-plan.md` stops asserting a refusal the product no longer makes and a constant the server
no longer has — the one artifact in this story's blast radius that **no gate would ever redden**
(`ADR-0113` §9).

## Why this is its own ticket

No test reads `docs/test-plan.md`, so nothing goes red when it goes stale. `ADR-0113` §9 names it
by hand for exactly that reason, and `ADR-0069` §5's precedent puts a file no gate holds in its own
ticket rather than smuggled into an `atomic:` one whose every other row names a failing exit code.

## Files

| File | Action |
| --- | --- |
| `docs/test-plan.md` | modify |

## Scope

Three edits, and nothing else in the document moves.

- **`CORE-23` is rewritten, not deleted.** Today it reads: *while a screen carries `The duel is
  paused.`, that seat clicks its own action → the action is **refused** — `ADR-0028` §6's
  `DUEL_PAUSED`*. Its subject is gone: the duel never pauses (`ADR-0108` §4) and the enum entry left
  the wire in `TASK-130805`. It becomes the **inverse** case a driver can still run — the rival's
  socket is down, the seat on turn acts, and **the action is applied** — with a *fails if* naming
  the old behaviour: *the action is refused, or the screen says the duel is paused*.
- **`CORE-22` loses its dead citation.** It measures *"longer than
  `RoomTimeouts.DEFAULT_DISCONNECT_GRACE_MILLIS`"*, a constant `TASK-130810` deletes. The case
  itself survives and is now sharper: an idle connected seat must still be able to act, and the
  duration it idles for is stated against `RoomTimeouts.DEFAULT_TURN_MILLIS` **plus**
  `DEFAULT_TIMEBANK_MILLIS` — because a connected seat that idles past both is now genuinely played
  by the server, and the case must not assert the opposite of the product.
- **The *Presence* section's prose paragraph** above the table says the cases *"test … that a duel
  remains playable after an idle grace window, and that the server enforces the paused state a
  client displays."* The second clause goes with the pause; the first is restated against the turn
  clock.

## Out of scope

- `CORE-18` and `CORE-19` — the away marking. Both survive **untouched**: `ADR-0113` §9 says so, and
  `ADR-0046`'s *Away* and *Timed out* keep their seats.
- The `first` screen's row in the coverage table at the end of the document, which cites `CORE-18`
  and `CORE-19` and does not change.
- Adding a case for the countdown the player sees — that is `STORY-1309`'s screen and belongs with
  the card it transcribes.
- Any code. This ticket changes one markdown file.

## Tests

None: this ticket ships a document. Its gates are the six greps in `verify:`, each of which asserts
the file exists before asserting what it does not contain, so none can pass vacuously.

## Acceptance criteria

- [ ] `docs/test-plan.md` exists and contains no `DUEL_PAUSED`
- [ ] It contains no `DEFAULT_DISCONNECT_GRACE_MILLIS`
- [ ] It contains no occurrence of the sentence *The duel is paused.*
- [ ] `CORE-21`, `CORE-22` and `CORE-23` all still exist as rows — exactly **3** — so the pause case
      was rewritten and not quietly dropped
- [ ] `CORE-18` and `CORE-19` both still exist as rows — exactly **2** — untouched
- [ ] The word *forfeit* appears nowhere in the document (`ADR-0046` §5), in any case
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
