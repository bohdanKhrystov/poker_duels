---
schema: 2
id: TASK-030909
title: A room that is gone retires the control and says so
type: task
status: done
parent: STORY-0309
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, result, ui]
depends_on: [TASK-030908]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +559 passed \(559\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'retires the control when the room is gone'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves the control alone for any other refusal'
  - cd web-client && npm run check
---

## Goal

`UNKNOWN_ROOM` is the one frame that ends a rematch: the control goes, the reason is stated, and
no other refusal touches it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/RematchControl.tsx` | modify |
| `web-client/src/result/RematchControl.test.tsx` | modify |
| `docs/adr/ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md` | read — §6, the two refusals and which of them ends it |

## Scope

- Props gain `refusal: ProtocolError | null`.
- When it is `"UNKNOWN_ROOM"`, the component renders `That duel room is gone.` — class
  `text-center text-small text-text-muted` — and **nothing else**: no button, no chip, no rival
  line, whatever `offers` says. Checked first, ahead of `rematchStand`.
- Every other value, and `null`, leave the component exactly as `TASK-030908` left it.
- One comment, from `ADR-0044` §6: this is the frame that ends a rematch — the room has been
  reaped, and the client says so. The way back is `DuelResult`'s own `Back to the lobby` link,
  which is directly below and which this component does not render.

## Out of scope

- `REMATCH_UNAVAILABLE`. `TASK-030904` makes the reducer drop it, so it never reaches a prop — and
  a component branch for a value that cannot arrive is a branch no test can reach. `TASK-030914`
  proves it end to end, from the frame.
- Navigating anywhere. Nothing here calls `window.location`, and no `useEffect` appears.
- Forgetting the remembered room code. That is `boot.ts`'s and the way back's, decided by
  [`ADR-0072`](../../docs/adr/ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md)
  and ticketed separately — nothing in this ticket touches storage.

## Tests

`web-client/src/result/RematchControl.test.tsx`, describe block `"the rematch control"`. Two added.

Both are rendered with the **same** `offers={[0]}` and `mySeat={1}` — a state that would otherwise
show the rival line and a live button — so the pair isolates the refusal as the only difference,
and two different refusal values keep the branch from collapsing into *any refusal retires it*.

| Test | Proves |
| --- | --- |
| `retires the control when the room is gone` | `refusal="UNKNOWN_ROOM"` ⇒ `That duel room is gone.` is on screen, `queryByRole("button", { name: "Rematch" })` is `null`, and `Your rival offers a rematch` is absent |
| `leaves the control alone for any other refusal` | `refusal="DUEL_PAUSED"` ⇒ the `Rematch` button and `Your rival offers a rematch` are both on screen, and `That duel room is gone.` is absent |

## Proof

| Command | Proves |
| --- | --- |
| `Tests 559 passed (559)` | two added to 557 |
| the two `--reporter=verbose` greps | both names exist |
| `npm run check` | `refusal` is typed as the generated `ProtocolError` union, so a misspelled value fails the build |

**Name the edit that makes each assertion red:**

1. Broaden the check to `props.refusal !== null` → `leaves the control alone for any other refusal`
   fails. Revert.
2. Put the check **after** `rematchStand`, so the rival line renders as well → `retires the control
   when the room is gone` fails on the absence assertion. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the rematch control > retires the control when the room is gone` passes
- [ ] `the rematch control > leaves the control alone for any other refusal` passes
- [ ] `RematchControl.tsx` names `"UNKNOWN_ROOM"` exactly once and no other `ProtocolError` value
- [ ] The eight tests from `TASK-030907` and `TASK-030908` are unchanged
- [ ] `npm run --silent test` reports `Tests  559 passed (559)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
