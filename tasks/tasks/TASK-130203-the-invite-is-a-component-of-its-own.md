---
schema: 2
id: TASK-130203
title: The invite is a component of its own, and the DOM does not move
type: task
status: done
parent: STORY-1302
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [client, table, lobby]
depends_on: [TASK-130202]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | grep -qE '^ *Tests +80 passed \(80\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/InvitePanel.test.tsx 2>&1 | grep -qE '^ *Tests +5 passed \(5\)$'
  - sh -c 'grep -q "Copy the link" web-client/src/table/InvitePanel.tsx && ! grep -q "<section" web-client/src/table/InvitePanel.tsx'
  - sh -c 'grep -q "roomLink" web-client/src/table/InvitePanel.tsx && ! grep -q "navigator.clipboard" web-client/src/lobby/Lobby.tsx'
  - sh -c '! grep -q "Invite link" web-client/src/lobby/Lobby.tsx'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The invite —
[`ADR-0110`](../../docs/adr/ADR-0110-creating-a-duel-seats-the-host-at-the-table.md) §5's three
parts, the bare code, the labelled read-only box and `Copy the link` with its two feedback lines —
is one exported component with its own test file, so that the ticket that draws it **on the table**
composes it instead of copying it. **Nothing a player sees changes**, and the eighty tests of
`Lobby.test.tsx` pass byte-unchanged.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/InvitePanel.tsx` | create |
| `web-client/src/table/InvitePanel.test.tsx` | create |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/room-link.ts` | read |

## Scope

- **`InvitePanel(props: { code: string })`**, in `web-client/src/table/` because `ADR-0110` §5
  puts the invite on the table. It derives `roomLink(window.location.origin, props.code)` itself —
  the derivation moves with the markup — and renders, **in this order and with these exact class
  lists moved verbatim from `Lobby.tsx`'s `WaitingForRival`**:
  1. the bare code in the `<p>` that today carries
     `font-mono text-display tracking-[var(--pd-track-code)]`;
  2. `<label htmlFor="invite-link">Invite link</label>`;
  3. the `<input autoFocus id="invite-link" readOnly value={link} onFocus={…select()}>`;
  4. today's `CopyLink`, moved into this file, still returning `null` where
     `navigator.clipboard` is absent, still `bg-accent-fill text-on-accent`, still rendering
     `Link copied.` and `Copy it from the box above.`.
- **It renders a fragment, not a wrapper**, and **no `<section>`**. Three tests in
  `Lobby.test.tsx` scope themselves with `.closest("section")`; the nearest section must stay the
  screen's own, and a `verify:` gate refuses `<section` in this file.
- **Both docstrings move with the code they describe.** *"The invite is selectable text before it
  is anything else: the one interaction this product depends on cannot need a working clipboard"*
  belongs on `InvitePanel`; *"Absent where the clipboard API is: the box above is always the
  fallback"* on `CopyLink`. `ADR-0022`'s no-oracle discipline is the reason they exist.
- **`Lobby.tsx`'s `WaitingForRival` keeps its `<section>`, its `<h2>`, `Back to the lobby` and the
  promise line**, and renders `<InvitePanel code={props.code} />` where the four invite nodes were.
  `CopyLink` and the `roomLink` import leave the file.
- **`autoFocus` stays.** `Lobby.test.tsx`'s *leaves the invite link selectable and focused for a
  copy by hand* asserts `document.activeElement` is the box. It is shipped behaviour and no merged
  source retires it.

## Out of scope

- **Any change to what renders.** The gate is `Lobby.test.tsx` still passing **80** tests with no
  edit to that file. If a test in it needs changing, the extraction was not faithful — fix the
  extraction, not the test.
- **Moving the invite to the table.** `TASK-130204` composes this component into `WaitingTable`;
  `TASK-130205` is where `Lobby.tsx`'s branch changes.
- **`web-client/src/lobby/room-link.ts`.** Read only. `normalizeRoomCode`, `roomCodeFromSearch`
  and `roomLink` are untouched, and so is `room-link.test.ts`.
- **The heading `<h2>Waiting for your rival</h2>`.** It is still a heading after this ticket;
  `TASK-130205` is what relocates it to the seat.

## Tests

`InvitePanel.test.tsx` — five tests, and the file's own `afterEach` must
`Reflect.deleteProperty(navigator, "clipboard")` the way `Lobby.test.tsx` does, or a clipboard
installed by one test leaks into the next.

| Test | Proves |
| --- | --- |
| `shows the bare code, the labelled box and the copy control` | all three of `ADR-0110` §5's parts render together when a clipboard exists |
| `builds the link from this window's origin and the code` | the box holds `http://localhost:3000/?room=7Q4M9K2T` — the derivation moved with the markup and did not become a prop |
| `leaves the box read-only and focused for a copy by hand` | `readOnly` is true and `document.activeElement` is the box |
| `says so when the copy succeeds` | `writeText` is called with the link, and `Link copied.` appears |
| `keeps the box as the whole invite when the clipboard refuses, and when there is none` | **two inputs**: a rejecting clipboard renders `Copy it from the box above.` with the button still standing; no clipboard at all renders no button, and in both the box still holds the link |

The last test carries both fallback cases deliberately: one of them alone would pass against a
component that had dropped the other, which is the exact severing `ADR-0110` §5 forbids.

## Acceptance criteria

- [ ] `InvitePanel.test.tsx` reports `Tests  5 passed (5)`
- [ ] `Lobby.test.tsx` reports `Tests  80 passed (80)` — the same eighty, with no edit to the file
- [ ] `web-client/src/table/InvitePanel.tsx` contains `Copy the link` and `roomLink`, and no
      `<section`
- [ ] `web-client/src/lobby/Lobby.tsx` contains neither `navigator.clipboard` nor `Invite link`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
