---
schema: 2
id: TASK-130912
title: The presence notice stops testing a sentence that left, and tests the one that stayed
type: task
status: done
parent: STORY-1309
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [client, table, copy]
depends_on: [TASK-130911]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/PresenceNotice.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 4) }'
  - sh -c '! grep -qiF "paused" web-client/src/table/PresenceNotice.test.tsx'
  - sh -c '! grep -qF "useFakeTimers" web-client/src/table/PresenceNotice.test.tsx'
  - sh -c 'test -f web-client/src/table/PresenceNotice.tsx && ! grep -qiF "paused" web-client/src/table/PresenceNotice.tsx'
  - sh -c '! grep -rqF "The duel is paused" web-client/src'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`PresenceNotice.test.tsx` asserts the away line the register now carries, instead of asserting that
a sentence which no longer exists is not followed by a digit that no longer exists either.

## Why a green test is the problem

`the countdown is separated from the line it counts under` was written for a real defect —
`paused.47` with no space, when the notice carried a grace countdown. `TASK-130805` took the
countdown off the wire and out of the component; `TASK-130911` took *The duel is paused.* out of the
copy. The test's regex, `/paused\.\d/`, can now match nothing whatever the code does: it is green by
construction, it installs fake timers for a component with no timer in it, and it is the last place
in the client that mentions a pause as though the product had one. A vacuous assertion that reads
like a guard is worse than no assertion, because the next reader trusts it.

It is a separate diff from `TASK-130911` for one reason and it is not taste: that ticket already
touches three files, and this is the fourth. No merged gate forbids splitting them, so they split.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/PresenceNotice.test.tsx` | modify |
| `web-client/src/table/presence-text.ts` | read — the four lines as they now stand |

## Scope

- **Replace `the countdown is separated from the line it counts under`** with
  `says the away line, and counts nothing`: render `<PresenceNotice presence="AWAY" returned={false} />`,
  assert `getByText("Your rival is away.")` resolves, and assert `container.textContent` matches no
  digit — the same `not.toMatch(/\d/)` the file's other three tests already carry, which is what
  makes *the notice counts nothing* a property of all four states rather than of three.
- **Delete the `afterEach` and the `vi.useFakeTimers()` call.** No timer is reachable from this
  component; the import of `vi` goes with them if nothing else uses it.
- **The file's count does not change** — one test replaces one test — and the other three are
  untouched.
- **`paused` appears nowhere in the file afterwards**, in a test name, an assertion or a comment,
  and a gate says so. Rewrite the comment rather than keeping it as history: `git log` is where the
  defect's story lives.

## Out of scope

- **`PresenceNotice.tsx`.** The component is already correct — it renders `presenceLine` and nothing
  else — and its KDoc records, accurately, that the countdown left in `TASK-130805` and that
  `STORY-1309` moved a countdown onto the turn clock. It says nothing about a pause today, and the
  gate below only holds it that way; it is not a licence to edit it.
- **`presence-text.ts` and `presence-copy.test.tsx`.** `TASK-130911`, merged.
- **Any new string.** `Your rival is away.` is the register's, landed one ticket ago.

## Tests

`PresenceNotice.test.tsx` — one replaced, so the file still reports **4**.

| Test | Proves |
| --- | --- |
| `says the away line, and counts nothing` | `AWAY` renders exactly `Your rival is away.` and the notice contains no digit — replacing an assertion that could no longer fail with one that can |
| the other three | unchanged: the `ABSENT` sentence, the silence on a fresh `PRESENT`, and `Your rival is back.` |

## Acceptance criteria

- [ ] `PresenceNotice.test.tsx` reports at least **4** passing tests and none failing
- [ ] `says the away line, and counts nothing` passes, by name
- [ ] `PresenceNotice.test.tsx` contains no `paused`, in any case, and no `useFakeTimers`
- [ ] `PresenceNotice.tsx` contains no `paused`, in any case, and still exists
- [ ] No file under `web-client/src` contains the string `The duel is paused`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
