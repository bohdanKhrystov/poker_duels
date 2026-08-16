---
schema: 2
id: TASK-031101
title: One GET, carrying the device id, with three answers and no network in the test
type: task
status: ready
parent: STORY-0311
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, http, profile]
depends_on: [TASK-031015]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +326 passed \(326\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends the device id it was given'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asks for the path it was given'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asks for nothing at all with no device id in hand'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers with the body of a 200'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers no-profile on a 401'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers unavailable on a 500, on a refused request and on a body that is not JSON'
  - cd web-client && npm run check
---

## Goal

The client can make one authenticated `GET` under `/api/me`, and it turns every reply the server can
send into exactly one of three answers: a body, *no profile yet*, or *unavailable* — with the
`fetch` injected, so the test touches no network.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/api.ts` | create |
| `web-client/src/profile/api.test.ts` | create |
| `docs/protocol.md` | read — the two endpoints, lines 60–99: the header, the `401` rule, the fields |
| `web-client/src/protocol/connection.ts` | read — the precedent for injecting the browser thing rather than naming a global |

`web-client/src/profile/` is a new directory, a sibling of `src/result/` and `src/table/`. It is
*the HTTP module* `EPIC-03`'s non-negotiables name — the one place in this client allowed to declare
a wire shape by hand, because these two endpoints are not `ServerMessage`s and are contracted in
`docs/protocol.md` rather than generated.

## Scope

- `api.ts` exports one type per idea and one function, and imports nothing:

  ```ts
  /** The part of a `Response` this module reads. */
  export interface ApiResponse {
    readonly status: number;
    json(): Promise<unknown>;
  }

  /** The part of `fetch` this module uses, injected so no test reaches the network. */
  export type ApiFetch = (
    path: string,
    init: { readonly headers: Readonly<Record<string, string>> },
  ) => Promise<ApiResponse>;

  /** What one read under `/api/me` came back with. */
  export type ApiRead =
    | { readonly kind: "body"; readonly body: unknown }
    | { readonly kind: "no-profile" }
    | { readonly kind: "unavailable" };

  export async function readFromApi(request: {
    readonly fetch: ApiFetch;
    readonly deviceId: string | null;
    readonly path: string;
  }): Promise<ApiRead>;
  ```

- The rules, in this order:
  - `deviceId === null` → `{ kind: "no-profile" }`, **without calling `fetch` at all**. A browser
    that has never held an id is the first visit `docs/protocol.md` says answers `401`; asking is a
    round trip whose answer is already known.
  - otherwise one call: `request.fetch(request.path, { headers: { "X-Device-Id": deviceId } })`.
  - `status === 200` → `{ kind: "body", body: await response.json() }`.
  - `status === 401` → `{ kind: "no-profile" }`. The server answers `401` identically for absent,
    blank and unknown **on purpose**, so nothing here tries to tell them apart.
  - any other status → `{ kind: "unavailable" }`.
  - a `fetch` that rejects, or a `json()` that rejects → `{ kind: "unavailable" }`. Caught, never
    rethrown: a lobby must not fall over because a profile read did.
- `deviceId` is a parameter, not a `Storage`. Reading the browser's key is `TASK-031102`'s job, and
  keeping it out of here is what lets this test file exist without a fake `Storage`.

## Out of scope

- Knowing what a profile or a duel *is*. This function answers `unknown` and validates nothing —
  `TASK-031102` and `TASK-031103` each name their own shape.
- The `limit` parameter. `TASK-031103` decides whether one is sent at all.
- Retrying, caching, timing out, or aborting. One call, one answer.
- `window.fetch`, `localStorage` or any other global. Naming one here would put the network inside
  the unit test the moment a caller forgot to inject.

## Tests

`web-client/src/profile/api.test.ts`, describe block `"the /api/me read"`. Two helpers, declared in
the file:

```ts
interface Call {
  readonly path: string;
  readonly headers: Record<string, string>;
}

/** Records every call and answers them in order. No network, no globals. */
function answering(...answers: readonly ApiResponse[]): {
  readonly calls: Call[];
  readonly fetch: ApiFetch;
} { … }

function ok(body: unknown): ApiResponse { … }   // { status: 200, json: async () => body }
function refusedWith(status: number): ApiResponse { … }
```

| Test | Proves |
| --- | --- |
| `sends the device id it was given` | **two distinct ids** — `"d-1"` then `"d-2"` — reach the header, whose name is asserted as the literal `"X-Device-Id"`. One id could not tell a header from a constant |
| `asks for the path it was given` | **two distinct paths** — `"/api/me"` then `"/api/me/duels"` — arrive verbatim as the first argument |
| `asks for nothing at all with no device id in hand` | `deviceId: null` answers `{ kind: "no-profile" }` **and** `calls` is empty. Both halves: the answer, and that no request was made |
| `answers with the body of a 200` | two different bodies come back as `{ kind: "body", body }`, deep-equal to what the response carried |
| `answers no-profile on a 401` | `401` answers `{ kind: "no-profile" }` |
| `answers unavailable on a 500, on a refused request and on a body that is not JSON` | three cases, each written out: `status: 500`; a `fetch` whose promise rejects; a `200` whose `json()` rejects. A name that claims three must show three |

Six tests added. Three hundred and twenty exist, so the suite reports **326**.

Use `await` on the returned promise. Do not reach for a timer: `virtual-time.test.ts` fails the
build on a test file that names one without installing fake ones.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 326 passed (326)` | six ran and nothing else moved |
| the six `--reporter=verbose` greps | every name above exists |
| `npm run check` | typechecks under `strict`, lints, is formatted |

**Name the edit that makes each assertion red** — run each, quote two in the PR, revert:

1. Hardcode the header value to `"d-1"` → `sends the device id it was given` fails on its second id.
2. Delete the `deviceId === null` branch so the call is always made → `asks for nothing at all with
   no device id in hand` fails on `calls` being non-empty, and its `kind` half still passes.
3. Return `unavailable` for `401` → `answers no-profile on a 401` fails.

## Acceptance criteria

- [ ] `the /api/me read > sends the device id it was given` passes
- [ ] `the /api/me read > asks for the path it was given` passes
- [ ] `the /api/me read > asks for nothing at all with no device id in hand` passes
- [ ] `the /api/me read > answers with the body of a 200` passes
- [ ] `the /api/me read > answers no-profile on a 401` passes
- [ ] `the /api/me read > answers unavailable on a 500, on a refused request and on a body that is not JSON` passes
- [ ] The first two tests each assert **two different** inputs
- [ ] Neither `api.ts` nor `api.test.ts` names `window`, `fetch` as a global, or `localStorage`
- [ ] No file outside `web-client/src/profile/` differs
- [ ] `npm run --silent test` reports `Tests  326 passed (326)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
