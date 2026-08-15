---
schema: 2
id: TASK-030714
title: The refusal sentence stops when the server next speaks
type: task
status: ready
parent: STORY-0307
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, duel, store]
depends_on: [TASK-030713]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a refusal stops being shown when the next turn opens'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a refusal stops being shown when the next snapshot arrives'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a refusal stops being shown when the duel finishes'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a refusal closes no decision point'
  - cd web-client && npm run check
---

## Goal

`refusal` stops outliving the attempt it describes. Today nothing but a `RoomJoined` ever clears it,
so `Failure{DUEL_PAUSED}` leaves *"The duel is paused. That action was not applied."* under the bar
for the rest of the duel — through the resume, through the next forty hands.

[`ADR-0043`](../../docs/adr/ADR-0043-a-rejection-closes-no-decision-point.md) names this in *What it
forecloses*: **"`refusal` is untouched … that its sentence also never clears is the same lifetime bug
in a different field, and this ADR deliberately does not fix it. It is worth one ticket."** This is
that ticket, and it applies the ADR's own rule — decision point 3 — to the second field.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify — `refusal: null` on three existing cases |
| `web-client/src/store/duel-state.test.ts` | modify — four tests added; **no existing assertion moves** |

## Scope

- `YourTurn`, `Snapshot` and `DuelFinished` each additionally set `refusal: null`. That is the same
  three frames `ADR-0043` gave `rejection`, for the same reason: a refusal is shown until the server
  next speaks about the game.
- `Events` does **not** clear it — it is narration, and every `Events` frame the server sends is
  accompanied by a `Snapshot`. `RoomJoined` already clears it and keeps doing so.
- Nothing else moves. In particular the `Failure` case still returns
  `{ ...state, refusal: message.error }`: `pendingTurn` untouched, `rejectionCount` untouched. The
  bar therefore does **not** remount on a `Failure`, so the in-flight lock stays down and the player
  is not invited to re-send — which is exactly what `DUEL_PAUSED` demands.

**The sentence is what expires here, not the lock.** Clearing `refusal` moves nothing the bar's
remount key reads, so this ticket cannot re-enable a control. A `YourTurn` re-enables the bar because
it opens a new decision point, which was already true before this ticket.

## Out of scope

- Anything outside `web-client/src/store/`. No component changes; `ActionBar` already renders
  whatever `refusal` holds and renders nothing when it is `null`.
- Clearing `refusal` on `Events`, or on any client-side timer or dismiss button. Nothing on the wire
  carries a duration, and a dismiss control is a product choice nobody has made.
- Retrying the refused action. `DUEL_PAUSED` says *do not re-send*, and this client never re-sends.
- Distinguishing the lobby's refusals (`UNKNOWN_ROOM`, `ROOM_FULL`) from the duel's. The three frames
  added here only ever arrive inside a duel, so the lobby's sentences are untouched — and
  `a join that lands clears the refusal before it` still passes, unedited.

## Tests

`web-client/src/store/duel-state.test.ts`, in the existing `describe("the duel state")`. Four are
added, none is rewritten: no merged test asserts that a refusal survives any frame.

| Test | Proves |
| --- | --- |
| `a refusal stops being shown when the next turn opens` | `Failure{DUEL_PAUSED}` then `YourTurn` leaves `refusal` `null` |
| `a refusal stops being shown when the next snapshot arrives` | `Failure{DUEL_PAUSED}` then `Snapshot` leaves `refusal` `null` |
| `a refusal stops being shown when the duel finishes` | `Failure{DUEL_PAUSED}` then `DuelFinished` leaves `refusal` `null`, and the outcome is set |
| `a refusal closes no decision point` | `YourTurn{handNumber: 1, actionSequence: 1}` then `Failure{DUEL_PAUSED}`: `refusal` is set, and `pendingTurn` and `rejectionCount` are **unchanged** — the frame the player must not re-send does not unlock the bar |

## Proof

| Command | Proves |
| --- | --- |
| the four `--reporter=verbose` greps | each new test exists by name and ran |
| `npm run check` | typechecks, lints, formats, and every existing test still passes — including the four `Failure` tests already in this file |

**Name the edit that makes each assertion red**, and quote both in the PR:

1. Drop `refusal: null` from the `Snapshot` case → `a refusal stops being shown when the next
   snapshot arrives` fails. Revert.
2. Add `pendingTurn: null` to the `Failure` case → `a refusal closes no decision point` fails.
   Revert.

## Acceptance criteria

- [ ] `a refusal stops being shown when the next turn opens` passes
- [ ] `a refusal stops being shown when the next snapshot arrives` passes
- [ ] `a refusal stops being shown when the duel finishes` passes
- [ ] `a refusal closes no decision point` passes
- [ ] The `Events` case still does not touch `refusal`
- [ ] The `Failure` case still sets only `refusal`
- [ ] No existing test in `duel-state.test.ts` is renamed, deleted or weakened
- [ ] No file outside `web-client/src/store/` is touched
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
