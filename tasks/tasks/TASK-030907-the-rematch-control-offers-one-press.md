---
schema: 2
id: TASK-030907
title: The rematch control offers one press, and a second press is harmless
type: task
status: done
parent: STORY-0309
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, result, ui]
depends_on: [TASK-030906]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +553 passed \(553\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers one press, labelled Rematch'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'calls onOffer once for one press'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'stays live for a second press'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers nothing to a client that holds no seat'
  - cd web-client && npm run check
---

## Goal

The rematch control exists: one button, one call outward per press, and no lock of its own.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/RematchControl.tsx` | create |
| `web-client/src/result/RematchControl.test.tsx` | create |
| `web-client/src/result/DuelResult.tsx` | read — the `bg-accent-fill` treatment this button takes over |

## Scope

- One component:

  ```tsx
  export function RematchControl(props: {
    mySeat: number | null;
    onOffer: () => void;
  }): ReactElement | null
  ```

- Returns `null` when `mySeat` is `null`, with a comment saying why: that client never received
  `RoomJoined`, so its socket entered no room, and an offer from it would answer
  `Failure(UNKNOWN_ROOM)` (`ADR-0044` §§1, 6). A button that can only fail is not offered.
- Otherwise one `<button type="button">Rematch</button>` whose `onClick` calls `props.onOffer()`.
- **No `disabled`, no in-flight `useState`, no ref.** Say why in the KDoc: `ADR-0044` §3 makes the
  offer idempotent on the wire — a repeat is answered with the same `RematchOffered`, never an
  error — so a double press cannot produce an error state and needs no guard. This is deliberately
  unlike `ActionBar`'s `sent` lock, which exists because an `Act` is *not* idempotent.
- The design's bright action (`design/screens/duel-end.html`, `.btn.fill`), in the tokens the panel
  already uses: `rounded-medium bg-accent-fill px-5 py-4 leading-tight font-medium text-on-accent`.
  No colour literal — `styles/color-literals.test.ts` is the guard.

## Out of scope

- Anything about who has offered. `offers` is not a prop yet — `TASK-030908` adds it and uses
  `rematchStand`.
- Refusals. `TASK-030909` adds `refusal`.
- Sending. This component never imports `useSend` and never builds a `ClientMessage`; it calls the
  callback it was given. `TASK-030910` wires it.
- Any timer, countdown or expiry. The wire carries no deadline (`ADR-0044`, Alternatives).

## Tests

`web-client/src/result/RematchControl.test.tsx`, describe block `"the rematch control"`. Four.

| Test | Proves |
| --- | --- |
| `offers one press, labelled Rematch` | with `mySeat` **1**, `getAllByRole("button")` has length 1 and its accessible name is exactly `Rematch` |
| `calls onOffer once for one press` | one `fireEvent.click` ⇒ the `vi.fn()` has `toHaveBeenCalledTimes(1)` |
| `stays live for a second press` | two clicks ⇒ `toHaveBeenCalledTimes(2)`, and after the first click the button's `disabled` is `false` — the idempotence claim, made executable |
| `offers nothing to a client that holds no seat` | `mySeat={null}` ⇒ `container.firstChild` is `null` and `queryByRole("button")` is `null` |

Seat **1** in the first three: a component that rendered only for seat 0 would pass a suite that
never left it, which is how `STORY-0213` shipped eight tests a hard-coded `seat = 0` satisfied.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 553 passed (553)` | four added to 549 |
| the four `--reporter=verbose` greps | all four names exist |

**Name the edit that makes each assertion red:**

1. Add `disabled={sent}` with a `useState` set on click → `stays live for a second press` fails on
   both the call count and the `disabled` check. Revert.
2. Return the button unconditionally → `offers nothing to a client that holds no seat` fails.
   Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the rematch control > offers one press, labelled Rematch` passes
- [ ] `the rematch control > calls onOffer once for one press` passes
- [ ] `the rematch control > stays live for a second press` passes
- [ ] `the rematch control > offers nothing to a client that holds no seat` passes
- [ ] `RematchControl.tsx` contains no `disabled`, no `useState`, no `useRef` and no `useEffect`
- [ ] `RematchControl.tsx` imports nothing from `../store/` and builds no `ClientMessage`
- [ ] `npm run --silent test` reports `Tests  553 passed (553)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
