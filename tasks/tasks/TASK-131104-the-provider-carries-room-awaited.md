---
schema: 2
id: TASK-131104
title: The provider carries roomAwaited, and main.tsx hands it over
type: task
status: done
parent: STORY-1311
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [client, store]
depends_on: [TASK-131103]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/duel-provider.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 10) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/App.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 36) }'
  - grep -qF 'roomAwaited={client.roomAwaited}' web-client/src/main.tsx
  - awk '{ n += gsub(/useRoomAwaited/, "&") } END { exit (n < 2) }' web-client/src/store/duel-provider.tsx
  - sh -c '! grep -qF "roomAwaited" web-client/src/e2e/drive-arc.tsx'
  - sh -c '! grep -qF "roomAwaited" web-client/src/e2e/drive-duel.tsx'
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh") || index($0, "vite" " preview")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1311*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A component can ask whether this tab is awaiting a room, through the same provider that already
carries the store, `send` and `forgetRoom` — and the real app boot is what answers it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/duel-provider.tsx` | modify |
| `web-client/src/store/duel-provider.test.tsx` | modify |
| `web-client/src/main.tsx` | modify |

Read `docs/adr/ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md` §5. Nothing
else — no screen is opened by this ticket.

## Scope

- `DuelProvider` gains `roomAwaited?: boolean`, carried in the memoised context value beside
  `store`, `send` and `forgetRoom`, **defaulting to `false`** — exactly the shape `forgetRoom` and
  its `NO_FORGET` already use, and for the same reason: a test that is about something else must not
  have to invent one.
- Export `useRoomAwaited(): boolean`, reading the same `useDuelClient()` the other three hooks read.
  KDoc it as the boot-time fact `ADR-0114` §5 names, in one line.
- Add `roomAwaited` to the memo's dependency array. A value left out of it is a value that never
  updates, and no type checker catches that.
- `main.tsx` passes `roomAwaited={client.roomAwaited}` on the one `<DuelProvider>` it renders.

## Out of scope

- **Reading it anywhere.** `Lobby.tsx` is `TASK-131105`'s.
- **The `src/e2e/` harnesses.** `drive-arc.tsx` and `drive-duel.tsx` boot a client and render a
  tree, so wiring this flag into them is tempting and is refused here: both replay frames with no
  round trip, so the window this flag exists to describe is empty in them, and `drive-arc.tsx`
  reuses one `Storage` across two boots — wiring it there would silence the second boot's first
  render and break `claimed-here-recovered-there.test.tsx` for a reason that has nothing to do with
  the product. Two `verify:` gates hold that line. If a later story wants the flag in the harnesses,
  it is that story's diff and its own argument.
- **A live value.** `roomAwaited` is a snapshot (`TASK-131103`). No listener, no setter.

## Tests

Three more `it` blocks in `duel-provider.test.tsx`:

| Test | Proves |
| --- | --- |
| `hands the tree the flag the boot computed` | a provider given `roomAwaited` **true** → `useRoomAwaited()` reads `true` |
| `answers false where the boot passed nothing` | the same provider with the prop omitted → `false`. The pair is the point: one value cannot tell a prop from a default |
| `keeps the value it was last given when it changes` | re-render the same provider with the flag flipped **false → true** → the hook reads `true`, which is what a missing memo dependency would break |

## Acceptance criteria

- [ ] All three tests above exist under those exact names and pass, and `duel-provider.test.tsx`
      reports at least 10 tests in total
- [ ] `main.tsx` contains the literal `roomAwaited={client.roomAwaited}`
- [ ] `useRoomAwaited` is named at least twice in `duel-provider.tsx` (declaration and KDoc/use)
- [ ] Neither `src/e2e/drive-arc.tsx` nor `src/e2e/drive-duel.tsx` mentions `roomAwaited`
- [ ] `App.test.tsx` still reports at least 36 tests
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
