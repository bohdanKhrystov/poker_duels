---
schema: 2
id: TASK-030507
title: A StrictMode double mount sends no second JoinRoom
type: task
status: ready
parent: STORY-0305
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [client, store, test]
depends_on: [TASK-030506]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +106 passed \(106\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'is really mounted twice'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends no JoinRoom of its own after the reaction already fired'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends exactly one JoinRoom when Welcome arrives after it mounted'
  - cd web-client && npm run check
---

## Goal

The story's second acceptance criterion, proved where it is actually at risk: a real React 18
`StrictMode` double mount over a booted client sends exactly one `JoinRoom`, whichever side of
`Welcome` the mount happens on.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/store/boot-strict-mode.test.tsx` | create |
| `web-client/src/store/boot.test.ts` | read — `inMemoryStorage`, `bootOverFakeSocket` to copy from |
| `web-client/src/store/duel-provider.test.tsx` | read — how a provider is rendered in a test |

## Scope

- **No production code changes at all.** This ticket adds one test file. If any assertion here
  requires touching `boot.ts`, `duel-store.ts` or `duel-provider.tsx`, stop and report it: the
  design under test is already merged and this ticket exists to show that it holds.
- Create `web-client/src/store/boot-strict-mode.test.tsx`, one
  `describe("a screen mounted twice by StrictMode")`, built from four pieces:
  - `inMemoryStorage(): Storage` and a `WELCOME` frame built with `PROTOCOL_VERSION`, copied from
    `boot.test.ts`. **Never the real `localStorage`** — Node 24+ shadows jsdom's with an inert
    one under Vitest.
  - `bootOverFakeSocket(joinRoomCode: string | null)` returning `{ socket, client }`, over
    `openConnection({ socket: socket.asWebSocket(), storage: inMemoryStorage(), onMessage })`.
    `FakeSocket` is not in the barrel: `import { FakeSocket } from "../protocol/fake-socket";`.
  - `joinRoomCount(socket)` — `socket.sent` parsed and filtered to `type === "JoinRoom"`, length.
  - the probe and the mount:

    ```tsx
    const rendered = vi.fn();

    function RoomCode(): ReactElement {
      const state = useDuelState();
      rendered();
      return <p>{state.roomCode ?? "no room yet"}</p>;
    }

    function mountUnderStrictMode(client: DuelClient): void {
      render(
        <StrictMode>
          <DuelProvider store={client.store} send={client.send}>
            <RoomCode />
          </DuelProvider>
        </StrictMode>,
      );
    }
    ```

    `StrictMode` is imported from `react`. The probe **calls** `rendered()`; it must not assign to
    an outer variable, which eslint's `react-hooks/globals` rejects and `npm run check` fails on.
- **Do not write the token `WebSocket` anywhere in the file.** `fake.asWebSocket()` is fine — the
  boundary guard's regex needs a word boundary before `W` and there is none — but an annotation is
  not.

## Out of scope

- Any `useEffect` in the probe. The point is that the tree has no effects to double-run.
- Testing `main.tsx`. It is outside the test net by design (`ADR-0032`); `TASK-030516` guards what
  can be guarded about it structurally.
- Reconnection, a second socket, or a second `Welcome` — `STORY-0310`.

## Tests

| Test | Proves |
| --- | --- |
| `is really mounted twice` | after `rendered.mockClear()` and one `mountUnderStrictMode`, `rendered.mock.calls.length` is greater than 1 (it is exactly 2 today), and `"no room yet"` is on screen — so the two tests below are not vacuous |
| `sends no JoinRoom of its own after the reaction already fired` | boot with `"ABCDEFGH"`, `socket.open()`, `socket.receive(WELCOME)`, **then** mount → `joinRoomCount(socket)` is 1 |
| `sends exactly one JoinRoom when Welcome arrives after it mounted` | boot with `"ABCDEFGH"`, mount, **then** `socket.open()` and `socket.receive(WELCOME)` → `joinRoomCount(socket)` is 1 |

Three tests. One hundred and three exist, so the suite reports **106**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 106 passed (106)` | the three tests ran and the hundred-and-three before them still do |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. In the test file, give `RoomCode` the effect `ADR-0032` rejects — `const send = useSend();
   useEffect(() => { send({ type: "JoinRoom", code: "ABCDEFGH" }); }, [send]);` → both count tests
   fail with `expected 3 to be 1 // Object.is equality`: StrictMode ran the effect twice, on top
   of the one boot reaction. Revert. **Quote this one in the PR body** — it is the whole ADR in
   one assertion.
2. In the test file, drop `<StrictMode>` from `mountUnderStrictMode` → `is really mounted twice`
   fails with `expected 1 to be greater than 1`. Revert.
3. In `boot.ts`, react to `RoomJoined` instead of `Welcome` → both count tests fail with
   `expected +0 to be 1 // Object.is equality`. Revert.

## Acceptance criteria

- [ ] `a screen mounted twice by StrictMode > is really mounted twice` passes
- [ ] `a screen mounted twice by StrictMode > sends no JoinRoom of its own after the reaction already fired` passes
- [ ] `a screen mounted twice by StrictMode > sends exactly one JoinRoom when Welcome arrives after it mounted` passes
- [ ] `npm run --silent test` reports `Tests  106 passed (106)`
- [ ] `git diff --name-only` for the PR lists exactly one file
- [ ] The test file contains no `useEffect` and no `useRef`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
