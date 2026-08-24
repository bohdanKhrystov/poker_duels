---
schema: 2
id: TASK-031306
title: The notice says the state and counts the window down
type: task
status: done
parent: STORY-0313
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, duel, ui, presence]
depends_on: [TASK-031305]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +[0-9]+ passed \([0-9]+\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says the duel is paused and starts from the number the frame carried'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'counts down as time passes'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'holds at zero, and says nothing new there'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'counts nothing once the window has run out'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says nothing at all to a client whose rival never left'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says the rival is back to a client that saw them go'
  - cd web-client && npm run check
---

## Goal

One component turns the three fields the store holds into `ADR-0046` §2's line and, while the
window is running, a number that counts down in whole seconds and stops at zero.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/PresenceNotice.tsx` | create |
| `web-client/src/table/PresenceNotice.test.tsx` | create |
| `web-client/src/table/ActionBar.tsx` | read — `Notice`, for the reserved-height line idiom |

## Scope

- One exported component, `PresenceNotice`, taking exactly three props: `presence:
  SeatPresence | null`, `returned: boolean`, `graceRemainingMillis: number | null`.
- **The deadline is anchored once, on mount**, with lazy initial state:
  `useState(() => props.graceRemainingMillis === null ? null : Date.now() + props.graceRemainingMillis)`.
  The parent remounts this component per presence frame (`key={presenceCount}`, `TASK-031309`), so a
  second window starts a second countdown even when it carries the same remaining as the first.
- **A second piece of state holds the current instant**, `useState(() => Date.now())`, and one
  `useEffect` keyed on the deadline installs `setInterval(() => setNow(Date.now()), 1000)` and
  clears it on teardown. The effect returns early when the deadline is `null`, so a notice with no
  window installs no timer at all.
- The body is a reserved-height `<p>` in `ActionBar`'s `Notice` idiom, holding
  `presenceLine(props.presence, props.returned)` and, when the deadline is not `null`, a
  `<span className="font-mono tabular-nums">` carrying `secondsRemaining(deadline, now)`.
- The KDoc-equivalent comment on the component states the rule it exists to keep:

  ```
  The countdown is started once and never acted upon (`ADR-0028` §3). Reaching zero enables no
  control, sends nothing, marks no hand lost and assumes no resumption: the duel is paused until
  an `OpponentPresence` says otherwise. This component sends nothing, because it is handed no
  way to.
  ```

- **`Date.now()`, not `performance.now()`.** Measured on this toolchain: under
  `vi.useFakeTimers()`, `Date.now()` advances with `vi.advanceTimersByTime` and `performance.now()`
  does **not** — a probe asserting a 5 000 ms advance got `5000` from the first and
  `0.0032920000000444816` from the second. A monotonic source would be nicer in principle and is
  untestable under `TASK-031013`'s no-real-clock rule, which is the discipline this story's criteria
  rest on.
- The interval period is **not** a product fact and no test pins it. `ADR-0028` §2 forbids a frame
  per second on the wire; it says nothing about how often a browser repaints its own number.
- **No new string.** The only text this component renders is whatever `presenceLine` returned and a
  bare numeral. No label, no `aria-label`, no unit, no *time's up*, no second string at zero —
  `The duel is paused.` above the number is its label (`ADR-0046` §3).

## Out of scope

- The numeral's shape. `ADR-0046` §3 gives `0:45` and `45s` as the design's to choose; this renders
  the whole-second integer and adds nothing to it. Placement, colour and typography are `EPIC-06`'s.
- The remount key. The parent passes it — `TASK-031309`.
- The seat plate's two words, which reach the plate through `DuelTable` (`TASK-031307`,
  `TASK-031308`).
- Any send. This component is handed no `send` and must not acquire one.

## Tests

`web-client/src/table/PresenceNotice.test.tsx`, one describe block: `"the presence notice"`.

Fake timers are installed **inside** each test that needs them and released in an `afterEach`
(`vi.useRealTimers()`), and they are installed **before** `render`, because the interval is created
by an effect that runs on mount. `virtual-time.test.ts` requires any test file that touches a timer
to install fake ones first, and this file does. Every advance is wrapped in `act`.

`the presence notice`

| Test | Proves |
| --- | --- |
| `says the duel is paused and starts from the number the frame carried` | with `presence: "AWAY"`, `graceRemainingMillis: 47_000`, the exact text `Your rival is away. The duel is paused.` is on screen and so is `47`. **47 000 on purpose**: the server's default window is 60 000, so a countdown seeded from a constant reads `60` and fails here |
| `counts down as time passes` | same props; advance virtual time by `3_000` and the number reads `44`; advance `20_000` more and it reads `24`. Two advances, because one is satisfied by a component that subtracts a constant |
| `holds at zero, and says nothing new there` | same props; advance `120_000` and the number reads `0`; advance `120_000` again and it still reads `0`, the line is still `Your rival is away. The duel is paused.`, and the rendered text matches none of `/expired|time.s up|too late|gone/i` |
| `counts nothing once the window has run out` | with `presence: "ABSENT"`, `graceRemainingMillis: null`, the exact text `Your rival did not come back. The duel continues, and the server acts for them.` is on screen and the container's text contains **no digit at all** |
| `says nothing at all to a client whose rival never left` | with `presence: "PRESENT"`, `returned: false`, `graceRemainingMillis: null`, the container's text is empty and no digit is present |
| `says the rival is back to a client that saw them go` | with `presence: "PRESENT"`, `returned: true`, the exact text `Your rival is back.` is on screen, and no digit |

Six tests. The suite grows by that many on top of whatever `TASK-031305` left, and every one of them passes.

## Proof

| Command | Proves |
| --- | --- |
| a green `Tests N passed (N)` line | six ran and every test before them still does |
| the six `--reporter=verbose` greps | each exists by name |
| `npm run check` | the component typechecks and the `react-hooks` rules pass on the effect |

**Name the edit that makes each assertion red:**

1. Seed the deadline from a literal — `Date.now() + 60_000` in place of
   `props.graceRemainingMillis` → `says the duel is paused and starts from the number the frame
   carried` fails, `60` on screen against the `47` it looks for. Revert.
2. Render `deadline - now` in milliseconds in place of `secondsRemaining(deadline, now)` → `holds at
   zero, and says nothing new there` fails: the number keeps falling past zero into the negatives
   instead of stopping. Revert.
3. Delete the `useEffect` entirely → `counts down as time passes` fails on its first advance, `47`
   against `44`, while `says the duel is paused and starts from the number the frame carried` still
   passes. That difference is the point: the anchoring and the ticking are two separate claims, and
   a single mutation kills only one of them.

Quote all three in the PR.

## Acceptance criteria

- [ ] `the presence notice > says the duel is paused and starts from the number the frame carried` passes
- [ ] `the presence notice > counts down as time passes` passes
- [ ] `the presence notice > holds at zero, and says nothing new there` passes
- [ ] `the presence notice > counts nothing once the window has run out` passes
- [ ] `the presence notice > says nothing at all to a client whose rival never left` passes
- [ ] `the presence notice > says the rival is back to a client that saw them go` passes
- [ ] Every word `PresenceNotice.tsx` renders comes out of `presenceLine` — the file contains no
      rendered string literal of its own
- [ ] `PresenceNotice.tsx` mentions `performance` nowhere, and `PresenceNotice.test.tsx` calls
      `vi.useFakeTimers()` before every `render` whose test advances time
- [ ] `PresenceNotice.tsx` takes no `send` prop and calls nothing that could reach a socket
- [ ] `npm run --silent test` reports `Tests  N passed (N)` with no failures
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
