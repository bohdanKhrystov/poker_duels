---
schema: 2
id: TASK-030807
title: The way on from the result is back to the lobby, and there is no dead rematch
type: task
status: backlog
parent: STORY-0308
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, ui, result]
depends_on: [TASK-030806]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +270 passed \(270\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers a way back to the lobby'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers no rematch it cannot honour'
  - cd web-client && npm run check
---

## Goal

The result panel ends in the one bright control the design allows: a link back to the lobby, where a
new room can be created. Nothing else is offered.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/result/DuelResult.tsx` | modify — one element added |
| `web-client/src/result/DuelResult.test.tsx` | modify — two tests appended |
| `web-client/src/lobby/room-link.ts` | read — the `?room=` parameter this deliberately drops |

## Scope

- One element, last inside the `section`:

  ```tsx
  <a
    className="rounded-medium bg-accent-fill px-5 py-4 leading-tight font-medium text-on-accent"
    href="/"
  >
    Back to the lobby
  </a>
  ```

- **A link, not a button, and to `/` rather than to history.** A finished duel leaves `roomCode`,
  `view` and `outcome` set — the reducer clears none of them, because state *is* the last frame the
  server sent — so returning to the lobby means starting from nothing. A plain navigation does that
  the way this client is built to do it: `main.tsx` boots one connection per tab (`ADR-0032`), and
  loading `/` boots a fresh one with an empty store. It also drops the invite's `?room=` parameter,
  which a soft reset would leave in the address bar for `TASK-030504` to rejoin the finished room
  from on the next refresh.
- No handler, no `useEffect`, no store mutation, no router. There is no router in this client and
  this story does not invent one (`TASK-030711` said the same about the screen it added).
- The design's `.btn.fill` treatment: this is *"the only bright action"* on the screen.

## Out of scope

- **Rematch.** `STORY-0309` owns it, blocked on `DEC-023`. The design shows the button; the story
  says plainly not to stub it — a dead control is worse than an absent one, and one faked with
  `CreateRoom` would lose the button-seat alternation the room owns. The second test below is what
  keeps a later coder from adding it here by kindness.
- Leaving the room politely. There is no `LeaveRoom` frame and the duel is over; the socket closes
  when the page unloads.
- Anything the lobby does after the navigation. That it creates rooms is `TASK-030510`'s, still
  green.

## Tests

`web-client/src/result/DuelResult.test.tsx`, appended to the `"the result screen"` block.

| Test | Proves |
| --- | --- |
| `offers a way back to the lobby` | `getByRole("link", { name: "Back to the lobby" })` is found and its `href` attribute is exactly `/` |
| `offers no rematch it cannot honour` | no element with an accessible name matching `/rematch/i` exists — neither `queryByRole("button", …)` nor `queryByRole("link", …)` finds one — and the panel's `textContent` does not match `/rematch/i` |

```tsx
it("offers a way back to the lobby", () => {
  render(<DuelResult outcome={anOutcome()} mySeat={0} />);

  const back = screen.getByRole("link", { name: "Back to the lobby" });
  expect(back.getAttribute("href")).toBe("/");
});
```

Read the `href` attribute, not `HTMLAnchorElement.href`: the property resolves against the test
page's origin and would pass for `/anything`.

Two tests. Two hundred and sixty-eight exist, so the suite reports **270**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 270 passed (270)` | the two ran and the two hundred and sixty-eight before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, format-checks |

**Name the edit that makes each assertion red:**

1. Change the `href` to `/lobby` → `offers a way back to the lobby` fails with `expected '/lobby' to
   be '/'`. Revert.
2. Add `<button type="button">Rematch</button>` beside the link → `offers no rematch it cannot
   honour` fails. Revert — that is the point of the test.

Quote both in the PR.

## Acceptance criteria

- [ ] `the result screen > offers a way back to the lobby` passes
- [ ] `the result screen > offers no rematch it cannot honour` passes
- [ ] The seven tests `TASK-030805` and `TASK-030806` wrote are byte-identical
- [ ] `DuelResult.tsx` contains no `onClick`, no `useEffect`, no `useRef` and no `useState`
- [ ] `npm run --silent test` reports `Tests  270 passed (270)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
