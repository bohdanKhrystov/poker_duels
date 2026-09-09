---
schema: 2
id: TASK-141506
title: A gone room retires the accept and says so, and no other refusal touches it
type: task
status: ready
parent: STORY-1415
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 2
labels: [client, rematch, notice]
depends_on: [TASK-141505]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/result/RematchNotice.test.tsx 2>&1 | grep -qE '^ *Tests +12 passed \(12\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/result/RematchControl.test.tsx 2>&1 | grep -qE '^ *Tests +12 passed \(12\)$'
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"UNKNOWN_ROOM") { n++ } END { exit (n != 1) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"{ROOM_GONE}") { n++ } END { exit (n != 1) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"REMATCH_UNAVAILABLE") { n++ } END { exit (n != 0) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"useEffect") || index($0,"useLayoutEffect") || index($0,"setTimeout") || index($0,"setInterval") { n++ } END { exit (n != 0) }' web-client/src/result/RematchNotice.tsx
  - git diff --quiet develop -- web-client/src/lobby/Lobby.tsx
  - git diff --quiet develop -- web-client/src/App.tsx
  - git diff --quiet develop -- web-client/src/result/RematchControl.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

When the room this tab holds is gone, the panel states `That duel room is gone.` in place of the
accept and stands there. `ADR-0044` §6's *"the frame that ends a rematch"*, and **no other refusal
touches it**.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/RematchNotice.tsx` | modify |
| `web-client/src/result/RematchNotice.test.tsx` | modify |

Read [`ADR-0044`](../../docs/adr/ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) §6,
[`ADR-0123`](../../docs/adr/ADR-0123-a-standing-rematch-offer-follows-the-rival.md) §5,
[`ADR-0138`](../../docs/adr/ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md)
§4, `web-client/src/result/RematchControl.tsx:38-45` (the merged shape of this branch) and
`design/screens/rematch-panel.html` frame 3. **Nothing outside the table above is changed.**

## Scope

- **One branch, above the span and above the standing offer**, reading `state.refusal`:

  ```tsx
  if (state.refusal === "UNKNOWN_ROOM") { … {ROOM_GONE} … }
  ```

  It replaces the accept and the dealing sentence; the panel itself stays. This is the same order
  `RematchControl` uses, and the same string, taken from `rematch-text.ts`.
- **`UNKNOWN_ROOM` and nothing else.** A gate holds `UNKNOWN_ROOM` at exactly **1** and
  `REMATCH_UNAVAILABLE` at **0**: `ADR-0044` §6 makes that one transient — the reducer already
  drops it without touching state (`duel-state.ts:371-375`) — so a client that tested `refusal !==
  null` would retire a panel over a refusal that changed nothing.
- **The gate is still `shown !== "first" && theirs`.** `Failure` sets `refusal` and clears no offer
  (`duel-state.ts:371-375`), which is exactly what lets a refused press leave the panel standing.
  Nothing about the gate moves.
- **Still no effect.** The gate that counts `useEffect`, `useLayoutEffect`, `setTimeout` and
  `setInterval` at **0** is repeated because this ticket reopens the file.

## Out of scope

- **`Not now`** — `TASK-141507`. The panel is still undismissable and mounted nowhere, so
  `ADR-0123` §5's *"stands there until it is dismissed"* becomes true one ticket later.
- **`RematchControl.tsx`**, whose identical branch is not touched, and **`duel-state.ts`**, whose
  handling of both errors already ships.

## Tests

`RematchNotice.test.tsx` grows from **9** to **12**.

| Test | Proves |
| --- | --- |
| `states that the room is gone in place of the accept` | offer standing at `#/account`, then `Failure` with `UNKNOWN_ROOM`: `ROOM_GONE` is on screen, and `queryByRole("button", { name: REMATCH_LABEL })` is null |
| `a transient refusal leaves the accept standing` | the same fixture, then `Failure` with `REMATCH_UNAVAILABLE`: the button is **still there** and `ROOM_GONE` is absent. This is the positive control for the test above — without it, a component that retired on any refusal passes |
| `the panel is still there after the room is gone` | in the same render as the first test, the `[role="status"]` root is still in the document. `ADR-0123` §5: the panel does not retire itself, and a *gone room* is not a fourth way off the screen |

Each frame goes in its own `act()`, for the reason `TASK-141505` measured: React batches applies
inside one `act()`, and a render this component needs can be batched away.

## What would still pass if this were built wrong

`states that the room is gone…` on its own is satisfied by a component that shows `ROOM_GONE` for
**any** `refusal`, which is the exact defect `ADR-0044` §6 warns about — so `a transient refusal
leaves the accept standing` is written with a different `ProtocolError` and the same everything
else, and it is the one that fails against that component.

`the panel is still there after the room is gone` is what stops the branch being written as
`return null`, which would satisfy both other tests' negative halves.

## Acceptance criteria

- [ ] `npx vitest run src/result/RematchNotice.test.tsx` reports **12 passed (12)**, the three new
      ones being the three named above and the previous nine unedited
- [ ] `src/result/RematchControl.test.tsx` reports **12 passed (12)** — measured on `develop` at
      `f20d07ed`, and that file is not opened
- [ ] `RematchNotice.tsx` has, on non-comment lines, `UNKNOWN_ROOM` **1**, `{ROOM_GONE}` **1**,
      `REMATCH_UNAVAILABLE` **0**, and still **0** of `useEffect`, `useLayoutEffect`, `setTimeout`,
      `setInterval`
- [ ] `git diff --quiet develop` is clean for `Lobby.tsx`, `App.tsx` and `RematchControl.tsx`
- [ ] **Shown red, then reverted:** widening the branch to `state.refusal !== null` makes
      `a transient refusal leaves the accept standing` fail and leaves the other eleven green. The
      mutation was run, observed red by that name, and reverted
- [ ] `npm run check` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
