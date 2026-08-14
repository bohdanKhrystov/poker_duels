---
schema: 2
id: TASK-030305
title: The socket URL is derived from the page's own origin
type: task
status: backlog
parent: STORY-0303
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, protocol]
depends_on: [TASK-030304]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +33 passed \(33\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'upgrades to a secure socket on a secure page'
  - cd web-client && npm run check
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

One pure function turns the page's location into the `/ws` URL, so the client never carries an
environment base URL and never needs a CORS carve-out on the server.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/protocol/socket-url.ts` | create |
| `web-client/src/protocol/socket-url.test.ts` | create |

## Scope

- `socket-url.ts` exports exactly one function:

  ```ts
  /**
   * The duel socket's URL on this page's own origin.
   *
   * Relative by construction: `ADR-0026` proxies `/ws` to Ktor in development
   * and production is same-origin, so there is no base-URL knob to configure
   * and nothing for an environment to get wrong.
   */
  export function socketUrl(location: {
    protocol: string;
    host: string;
  }): string;
  ```

- `wss:` when `location.protocol` is `"https:"`, `ws:` otherwise. Path is `/ws`.
- The parameter is a structural type, not `Location`, so a test can call it with a plain object and
  nothing has to stub a global.

## Out of scope

- Constructing a `WebSocket` — `TASK-030311`.
- Query parameters of any kind. `ADR-0027` is explicit that a bearer secret in a URL is written to
  every access log it passes; the session token it adds travels in `Hello`, in band.
- Anything about `/api`. `STORY-0311` owns HTTP, and it is relative for the same reason.

## Tests

`web-client/src/protocol/socket-url.test.ts`, describe block `"the socket URL"`. Two `it` blocks:

| Test | Proves |
| --- | --- |
| `points at /ws on the page's own host` | `socketUrl({ protocol: "http:", host: "localhost:5173" })` is `"ws://localhost:5173/ws"` |
| `upgrades to a secure socket on a secure page` | `socketUrl({ protocol: "https:", host: "duels.example" })` is `"wss://duels.example/ws"` |

Two tests. Thirty-one exist, so the suite reports **33**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 33 passed (33)` | the two tests ran and nothing earlier was displaced |
| the `--reporter=verbose` grep | the secure case exists by name — the case a development-only implementation gets wrong |

**Name the edit that makes it red:** return `` `ws://${location.host}/ws` `` unconditionally →
`upgrades to a secure socket on a secure page` fails, `expected "ws://duels.example/ws" to be
"wss://duels.example/ws"`. Revert, and quote it in the PR. This is the mistake that ships fine on
`localhost` and breaks on the first deployment, which is why it has its own assertion.

## Acceptance criteria

- [ ] `the socket URL > points at /ws on the page's own host` passes
- [ ] `the socket URL > upgrades to a secure socket on a secure page` passes
- [ ] `npm run --silent test` reports `Tests  33 passed (33)`
- [ ] `socket-url.ts` contains no `localhost` and no `8080`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
