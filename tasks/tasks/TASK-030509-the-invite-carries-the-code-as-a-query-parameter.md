---
schema: 2
id: TASK-030509
title: The invite carries the code as a query parameter, trimmed and upper-cased
type: task
status: done
parent: STORY-0305
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, lobby, rooms]
depends_on: [TASK-030508]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +114 passed \(114\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'trims and upper-cases a pasted code'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads the code the link carried'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'has no code when the link carried none'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'has no code when the room parameter is blank'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF "builds the invite from this page's own origin"
  - cd web-client && npm run check
---

## Goal

Three pure functions the lobby and `main.tsx` both need: normalise a code, read one out of a query
string, and build the invite link that carries it.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/lobby/room-link.ts` | create |
| `web-client/src/lobby/room-link.test.ts` | create |
| `docs/adr/ADR-0022-the-room-code-is-the-invite.md` | read — why the client does not validate the alphabet |

## Scope

- `web-client/src/lobby/` is new, and is where this story's screen lives. `ADR-0032` §5 places the
  store and its wiring and leaves screen location to the screen story; this is that choice.
- Create `web-client/src/lobby/room-link.ts` with exactly these three exports:

  ```ts
  /**
   * Trim and upper-case, and nothing else: `ADR-0022` has the server answer an
   * unparseable code and an unknown room identically on purpose, so checking the
   * alphabet here would hand back the shape oracle it deliberately withholds.
   */
  export function normalizeRoomCode(raw: string): string {
    return raw.trim().toUpperCase();
  }

  /** The room code this tab's URL carried, or `null` when it carried none. */
  export function roomCodeFromSearch(search: string): string | null {
    const code = normalizeRoomCode(new URLSearchParams(search).get("room") ?? "");
    return code === "" ? null : code;
  }

  /**
   * The invite. The code is a query parameter because a path segment would 404 on
   * reload against a static host with no rewrite rule, and `EPIC-07` has not
   * chosen one.
   */
  export function roomLink(origin: string, code: string): string {
    return `${origin}/?room=${code}`;
  }
  ```

- **Every function is pure and takes its input as a parameter.** Nothing here reads
  `window.location`; the callers pass `window.location.search` and `window.location.origin`. That
  is what makes this file testable with no DOM and no stubbing.
- **No length check, no alphabet check, no eight-character assertion.** Eight Crockford base32
  characters is what the server mints; what the *client* accepts is "whatever the player typed,
  trimmed and upper-cased", because a client-side rejection tells an attacker which codes are
  well-formed and `ADR-0022` withholds exactly that.

## Out of scope

- Any component — `TASK-030510` onwards.
- `main.tsx` calling `roomCodeFromSearch` — `TASK-030515`.
- `history.pushState`, routing, or removing `?room=` from the address bar after joining. There is
  no router in this client and this story does not add one.

## Tests

`web-client/src/lobby/room-link.test.ts`, one `describe("the room link")`.

| Test | Proves |
| --- | --- |
| `trims and upper-cases a pasted code` | `normalizeRoomCode("  abcdefgh  ")` is `"ABCDEFGH"` |
| `reads the code the link carried` | `roomCodeFromSearch("?room=abcdefgh")` is `"ABCDEFGH"` |
| `has no code when the link carried none` | `roomCodeFromSearch("")` and `roomCodeFromSearch("?rematch=1")` are both `null` |
| `has no code when the room parameter is blank` | `roomCodeFromSearch("?room=")` and `roomCodeFromSearch("?room=%20%20")` are both `null` — `URLSearchParams` decodes `%20`, and the trim then empties it |
| `builds the invite from this page's own origin` | `roomLink("https://duels.example", "ABCDEFGH")` is exactly `"https://duels.example/?room=ABCDEFGH"` |

Five tests. One hundred and nine exist, so the suite reports **114**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 114 passed (114)` | the five ran and the hundred-and-nine before them still do |
| the five `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Drop `.trim()` from `normalizeRoomCode` → `trims and upper-cases a pasted code` fails with
   `expected '  ABCDEFGH  ' to be 'ABCDEFGH' // Object.is equality`, and `has no code when the
   room parameter is blank` fails with `expected '  ' to be null`. Revert.
2. Return `code` unconditionally from `roomCodeFromSearch` → `has no code when the link carried
   none` fails with `expected '' to be null`. Revert.
3. Change `roomLink` to `` `${origin}/r/${code}` `` → `builds the invite from this page's own
   origin` fails with `expected 'https://duels.example/r/ABCDEFGH' to be
   'https://duels.example/?room=ABCDEFGH' // Object.is equality`. Revert.

Quote all three in the PR. The third is the story's design note made executable: a path segment
404s on reload against a static host.

## Acceptance criteria

- [ ] `the room link > trims and upper-cases a pasted code` passes
- [ ] `the room link > reads the code the link carried` passes
- [ ] `the room link > has no code when the link carried none` passes
- [ ] `the room link > has no code when the room parameter is blank` passes
- [ ] `the room link > builds the invite from this page's own origin` passes
- [ ] `npm run --silent test` reports `Tests  114 passed (114)`
- [ ] `room-link.ts` contains no reference to `window`, and no length or alphabet check
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
