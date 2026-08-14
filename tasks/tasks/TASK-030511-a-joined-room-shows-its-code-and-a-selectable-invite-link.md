---
schema: 2
id: TASK-030511
title: A joined room shows its code and a selectable invite link
type: task
status: ready
parent: STORY-0305
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, lobby, rooms]
depends_on: [TASK-030510]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +120 passed \(120\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows the room code the server named'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows an invite link carrying that code'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves the invite link selectable and focused for a copy by hand'
  - cd web-client && npm run check
---

## Goal

The story's first acceptance criterion: once `RoomJoined` lands, the lobby shows the eight-character
code and a link containing it — as read-only, pre-focused, selectable text that needs no clipboard.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |
| `web-client/src/lobby/room-link.ts` | read — `roomLink` |
| `web-client/src/store/duel-provider.tsx` | read — `useDuelState` |

## Scope

- `Lobby` gains `const state = useDuelState();` as its **first** line — before `useSend` and
  `useState`, and before any early return, because hooks may not sit behind a branch.
- After the hooks, one early return:

  ```tsx
  if (state.roomCode !== null) {
    return <WaitingForRival code={state.roomCode} />;
  }
  ```

- A second component in the same file, below `Lobby`:

  ```tsx
  /**
   * The invite is selectable text before it is anything else: the one interaction
   * this product depends on cannot need a working clipboard.
   */
  function WaitingForRival(props: { code: string }): ReactElement {
    const link = roomLink(window.location.origin, props.code);
    return (
      <section>
        <h2>Waiting for your rival</h2>
        <p>{props.code}</p>
        <label htmlFor="invite-link">Invite link</label>
        <input
          autoFocus
          id="invite-link"
          readOnly
          value={link}
          onFocus={(event) => event.currentTarget.select()}
        />
      </section>
    );
  }
  ```

- `roomLink` joins the existing `./room-link` import: `import { normalizeRoomCode, roomLink } from
  "./room-link";`.
- `autoFocus` and `onFocus → select()` are the whole of "obtainable by hand": the field is focused
  on arrival and selects itself when focused, so the link is one keystroke from copied with no
  clipboard API involved. `readOnly` — not `disabled` — because a disabled field is unselectable.
- `window.location.origin` is read in render, which is a read and not a side effect. The tab's
  origin cannot change under it.

## Out of scope

- The copy button — `TASK-030512`. Nothing in this ticket touches `navigator`.
- The refusal message — `TASK-030513`; and what happens once the duel starts — `TASK-030514`.
- Showing the rival's name, a spinner or a countdown. The waiting state ends on a `Snapshot`, and
  nothing else is known about the rival until then.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, describe block `"the lobby"`. Three `it` blocks appended
after `TASK-030510`'s three, which are not edited.

One fixture is added above the helpers:

```tsx
const ROOM_JOINED = { type: "RoomJoined", code: "ABCDEFGH", seat: 0 } as const;
```

Each new test builds a store, applies `ROOM_JOINED` **before** rendering, and calls
`renderLobby(store)`. The typed query `screen.getByLabelText<HTMLInputElement>("Invite link")` is
what gives access to `.value` and `.readOnly`.

| Test | Proves |
| --- | --- |
| `shows the room code the server named` | `screen.getByText("ABCDEFGH")` is findable and `screen.queryByRole("button", { name: "Create a duel room" })` is `null` — the entry screen is gone |
| `shows an invite link carrying that code` | the invite field's `.value` is exactly `"http://localhost:3000/?room=ABCDEFGH"` — jsdom's origin under this Vitest config |
| `leaves the invite link selectable and focused for a copy by hand` | the field's `.readOnly` is `true` and `document.activeElement` is that field |

Three tests. One hundred and seventeen exist, so the suite reports **120**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 120 passed (120)` | the three ran and the hundred-and-seventeen before them still do |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Delete the `if (state.roomCode !== null)` early return → `shows the room code the server named`
   fails with `TestingLibraryElementError: Unable to find an element with the text: ABCDEFGH`, and
   `tsc` additionally reports `TS6133` for the now-unused `state` and `WaitingForRival`. Revert.
2. Delete `autoFocus` from the input → `leaves the invite link selectable and focused for a copy
   by hand` fails with `expected <body><div>…(1)</div></body> to be <input id="invite-link"
   …(2)></input> // Object.is equality`. Revert.
3. Set the input's `value={props.code}` instead of the link → `shows an invite link carrying that
   code` fails with `expected 'ABCDEFGH' to be 'http://localhost:3000/?room=ABCDEFGH'`. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `the lobby > shows the room code the server named` passes
- [ ] `the lobby > shows an invite link carrying that code` passes
- [ ] `the lobby > leaves the invite link selectable and focused for a copy by hand` passes
- [ ] `npm run --silent test` reports `Tests  120 passed (120)`
- [ ] The three `it` blocks from `TASK-030510` are unedited, and their assertions are
      byte-identical
- [ ] `Lobby.tsx` still contains no `useEffect` and no `useRef`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
