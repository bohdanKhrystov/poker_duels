---
schema: 2
id: TASK-030108
title: The dev server proxies /api and /ws to the Ktor server
type: task
status: ready
parent: STORY-0301
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, build, toolchain]
depends_on: [TASK-030107]
verify:
  - cd web-client && npm test
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +3 passed \(3\)'
  - cd web-client && npm run lint
  - cd web-client && npm run typecheck
  - cd web-client && npm run format:check
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

`vite.config.ts` carries `/api` and `/ws` to the Ktor server on port 8080 in development, and a test
asserts both targets so the routing cannot rot unnoticed.

## Files

| File | Action |
| --- | --- |
| `web-client/vite.config.ts` | modify |
| `web-client/src/dev-proxy.test.ts` | create |

## Scope

- Add to `vite.config.ts`, exactly as `ADR-0026` specifies:

  ```ts
    server: {
      proxy: {
        '/api': 'http://localhost:8080',
        '/ws': { target: 'ws://localhost:8080', ws: true },
      },
    },
  ```

  This is what lets the client speak relative URLs only, with no CORS configuration on the server
  and no environment base-URL knob in the client.
- `src/dev-proxy.test.ts` imports the config and asserts both entries. It starts with
  `// @vitest-environment node`, because the config pulls in `@vitejs/plugin-react`, which is
  Node-side and has no business loading into `jsdom`.
- Read the values off the imported object — `config.server?.proxy?.['/api']` — never off the file's
  text. A grep over source would pass on a commented-out block.

## Out of scope

- Starting a Vite dev server, binding a port or making a request through the proxy. No test in
  `EPIC-03` may reach the network; the configuration is the contract this ticket checks.
- Any client code that builds a URL. Deriving the socket URL from `location.host` belongs to
  `STORY-0303`, which owns the socket.
- Any server-side change. `ADR-0026` chose a proxy precisely so the Ktor server needs none.

## Tests

`src/dev-proxy.test.ts`

| Test | Proves |
| --- | --- |
| `sends /api to the Ktor server` | `config.server.proxy['/api']` is `http://localhost:8080` |
| `sends /ws to the Ktor server as a websocket` | `config.server.proxy['/ws']` equals `{ target: 'ws://localhost:8080', ws: true }` — the `ws` flag is the half that is easiest to drop and hardest to notice |

## Proof the assertions bite

The whole test file is worthless if it passes with the proxy absent, so check it once: delete the
`server` block, run `npm test`, and both tests must fail (`undefined` is not the target). Restore it
and say in the PR what the failures looked like.

The run is expected to report **three** passing tests — the heading test from `TASK-030106` plus
these two. The verify block asserts that count, so a file that was never discovered fails here even
though `vitest run` would exit 0 on one test.

## Acceptance criteria

- [ ] `cd web-client && npm test` exits 0 and prints `Tests  3 passed (3)`
- [ ] `sends /api to the Ktor server` passes
- [ ] `sends /ws to the Ktor server as a websocket` passes, including the `ws: true` flag
- [ ] Both tests read the imported config object, not the file's source text
- [ ] `npm run lint`, `npm run typecheck` and `npm run format:check` all exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
