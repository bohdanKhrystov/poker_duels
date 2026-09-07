---
schema: 2
id: TASK-140403
title: The host's plate goes bare and the table says the duel starts by itself
type: task
status: done
parent: STORY-1404
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 4
atomic:
  - cd web-client && npm run check
labels: [client, table, lobby]
depends_on: [TASK-140402]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - perl -ne 'print "$1\n" while m{<p class="autostart">([^<]*)</p>}g' design/screens/duel-table.html | sort -u > "${TMPDIR:-/tmp}/pd-autostart.txt"
  - test "$(wc -l < "${TMPDIR:-/tmp}/pd-autostart.txt")" -eq 1
  - grep -qFf "${TMPDIR:-/tmp}/pd-autostart.txt" web-client/src/table/WaitingTable.tsx
  - grep -qFf "${TMPDIR:-/tmp}/pd-autostart.txt" web-client/src/table/WaitingTable.test.tsx
  - grep -qFf "${TMPDIR:-/tmp}/pd-autostart.txt" web-client/src/lobby/Lobby.test.tsx
  - grep -qFf "${TMPDIR:-/tmp}/pd-autostart.txt" web-client/src/table/null-view.test.tsx
  - cd web-client && test "$(grep -c 'You' src/table/WaitingTable.tsx || true)" = "0"
  - cd web-client && test "$(grep -ci 'no clipboard' src/table/WaitingTable.tsx || true)" = "0"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/WaitingTable.test.tsx > "${TMPDIR:-/tmp}/wt-after.txt" 2>&1; grep -qF 'Tests  7 passed (7)' "${TMPDIR:-/tmp}/wt-after.txt"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx > "${TMPDIR:-/tmp}/lb-after.txt" 2>&1; grep -qF 'Tests  102 passed (102)' "${TMPDIR:-/tmp}/lb-after.txt"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/null-view.test.tsx > "${TMPDIR:-/tmp}/nv-after.txt" 2>&1; grep -qF 'Tests  10 passed (10)' "${TMPDIR:-/tmp}/nv-after.txt"
  - cd web-client && cp src/table/WaitingTable.tsx "${TMPDIR:-/tmp}/wt.fixed.tsx" && perl -0pi -e 's{bg-surface px-5 py-4"></div>}{bg-surface px-5 py-4"><span className="block font-medium">You</span></div>}' src/table/WaitingTable.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/WaitingTable.test.tsx > "${TMPDIR:-/tmp}/m3a.txt" 2>&1; FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx > "${TMPDIR:-/tmp}/m3b.txt" 2>&1; FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/null-view.test.tsx > "${TMPDIR:-/tmp}/m3c.txt" 2>&1; cp "${TMPDIR:-/tmp}/wt.fixed.tsx" src/table/WaitingTable.tsx; cmp -s "${TMPDIR:-/tmp}/wt.fixed.tsx" src/table/WaitingTable.tsx && grep -qF 'Tests  1 failed | 6 passed (7)' "${TMPDIR:-/tmp}/m3a.txt" && grep -qF 'Tests  1 failed | 101 passed (102)' "${TMPDIR:-/tmp}/m3b.txt" && grep -qF 'Tests  1 failed | 9 passed (10)' "${TMPDIR:-/tmp}/m3c.txt"
  - cd web-client && cp src/table/WaitingTable.tsx "${TMPDIR:-/tmp}/wt.fixed.tsx" && perl -0pi -e 's{\n      \{/\* ADR-0129 §1[^\n]*\n      <p className="text-small text-text-muted">\{DUEL_STARTS_BY_ITSELF\}</p>\n}{\n}' src/table/WaitingTable.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/WaitingTable.test.tsx > "${TMPDIR:-/tmp}/m4a.txt" 2>&1; FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx > "${TMPDIR:-/tmp}/m4b.txt" 2>&1; FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/null-view.test.tsx > "${TMPDIR:-/tmp}/m4c.txt" 2>&1; cp "${TMPDIR:-/tmp}/wt.fixed.tsx" src/table/WaitingTable.tsx; cmp -s "${TMPDIR:-/tmp}/wt.fixed.tsx" src/table/WaitingTable.tsx && grep -qF 'Tests  1 failed | 6 passed (7)' "${TMPDIR:-/tmp}/m4a.txt" && grep -qF 'Tests  2 failed | 100 passed (102)' "${TMPDIR:-/tmp}/m4b.txt" && grep -qF 'Tests  2 failed | 8 passed (10)' "${TMPDIR:-/tmp}/m4c.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The host waiting alone sees their own plate carrying no `You`, and one sentence — the merged card's,
transcribed byte-for-byte — saying that the duel starts by itself when the rival arrives. The three
merged assertions that enumerate this state's strings move with it, deliberately and by name.

## Files

Four, because a merged gate refuses every intermediate state — see *Why this is `atomic:`* below.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `web-client/src/table/WaitingTable.tsx` | modify | the only file that renders `You` on this screen and the only place the sentence can be added |
| `web-client/src/table/WaitingTable.test.tsx` | modify | *"names the empty seat and the host's seat exactly once each"* asserts `getAllByText("You")` has length 1; `npm run check` goes red the moment the plate is bare |
| `web-client/src/lobby/Lobby.test.tsx` | modify | two tests enumerate this state and both name `"You"` — the string-set test at line 1332 and the recovering-browser test at line 424 |
| `web-client/src/table/null-view.test.tsx` | modify | the `BASELINE` array of *"the copy control's feedback adds one named string and no other"* names `"You"` and is compared with `toEqual`, so it fails on both the removal and the addition |

Read [`ADR-0129`](../../docs/adr/ADR-0129-the-host-is-told-the-duel-starts-by-itself.md) §§1–6 and
`design/screens/duel-table.html`'s three `Host alone` frames — the card is where the sentence comes
from, and `ADR-0129` §4 deliberately pins no literal of its own. **Nothing else is opened, and no
other file in the repository is changed.**

## Why this is `atomic:`

`npm run check` runs the whole vitest suite, and the three test files above assert today's strings.
Landing `WaitingTable.tsx` alone leaves the gate red; landing any one test file alone leaves it red
the other way. A prototype built in this worktree and reverted measured exactly this: removing `You`
and adding the sentence, with no test touched, gives **4 failures across 3 files** — nothing more
and nothing less, which is why the count is four and not five.

## Scope

- **`WaitingTable.tsx` — the sentence.** Above the component, a module-level constant:

  ```tsx
  /**
   * ADR-0129 §1: the host-alone table says, in one sentence, that the duel begins
   * by itself when the rival arrives. §4 pins no literal — the glyphs are the card's
   * (design/screens/duel-table.html, the <p class="autostart"> node), so this is a
   * transcription and never a re-wording. A verify: gate compares the two.
   */
  const DUEL_STARTS_BY_ITSELF = "«the card's sentence, byte-for-byte»";
  ```

  and, in the JSX, immediately after the rival's empty-seat `<div>` and before `<InvitePanel …/>` —
  the placement the card draws — exactly these two lines:

  ```tsx
        {/* ADR-0129 §1: one sentence, once, in this state only. */}
        <p className="text-small text-text-muted">{DUEL_STARTS_BY_ITSELF}</p>
  ```

  Write those two lines verbatim, at that indentation: the second mutation gate finds them by that
  text, and a differently spelled equivalent makes it red.
- **`WaitingTable.tsx` — the plate.** The host's seat keeps its `<div>` and loses its only child:

  ```tsx
        {/* The host's seat — the same plate, solid, and bare: ADR-0110 §§2-3 make
            the shipped name optional, and STORY-1404 takes it off. */}
        <div className="flex items-center gap-4 rounded-medium border border-hairline bg-surface px-5 py-4"></div>
  ```

  The plate stands. `ADR-0110` §§2–3 make the seat's *name* optional, not the seat.
- **`WaitingTable.tsx` — one stale comment.** *"The invite panel — code, link box, copy button with
  three states (at rest, copied, refused, no clipboard)"* stopped being true when `TASK-140402`
  merged. Replace the parenthesis so it names what the panel now has — the control that is never
  absent, at rest or after either of its two outcomes — and cite `ADR-0110 §5 as ADR-0128 §§1 and 3
  amend it`. A `verify:` gate holds `no clipboard` to zero occurrences in this file.
- **`WaitingTable.test.tsx`.** *"names the empty seat and the host's seat exactly once each"* becomes
  *"names the empty seat once and leaves the host's plate standing and bare"*: the rival's line is
  still found exactly once, `queryByText("You")` is `null`, and
  `container.querySelectorAll("div.border-hairline.bg-surface")` has length **1** with `textContent`
  `""`. That selector was measured on `develop` — it matches the host's plate and nothing else in
  this component, the code well being a `<p>` and the link box an `<input>` — and it is what keeps
  the plate's removal from passing as its emptying. Then one new test,
  *"says the duel starts by itself, once"*, asserting `getAllByText(<the literal>)` has length 1.
  Seven tests after this ticket.
- **`Lobby.test.tsx`, two edits and no third.** In *"still shows the whole waiting screen to a
  recovering browser told only that it holds a room"* (line 424), `expect(screen.getByText("You"))`
  becomes the same assertion on the sentence — the test enumerates the whole screen, and the
  sentence is now part of it. In the string-set test, `"You"` is replaced by the literal in the
  array. **Its title does not change**: `TASK-140402` already made it *"states the seven strings …
  and no eighth"*, and this ticket removes one member and adds one, so seven it stays. Test count
  unchanged at 102.
- **`null-view.test.tsx`, two edits.** In the `BASELINE` array, `"You"` is replaced by the literal.
  Then one new test, *"takes the sentence away with the state it stood on"* (`ADR-0129` §6): render
  the null view, find the sentence, `act(() => store.apply({ type: "Snapshot", view: aView() }))`,
  and `queryByText` for it is `null`. `act`, `screen`, `aView` and `renderNullView` are all already
  in that file. Ten tests after this ticket.

## Out of scope

- **The literal itself.** It is not written here, chosen here, or re-worded here. It is copied out of
  `design/screens/duel-table.html`'s `<p class="autostart">` node, which `TASK-140401` merged, and
  four `verify:` gates compare the two. If the card's node does not exist or the three copies of it
  disagree, this ticket is not startable — say so rather than inventing a sentence, which is the one
  thing `ADR-0129` §4 forbids.
- **A second string.** `ADR-0110` §6's set gains exactly one member (`ADR-0129` §5) and loses `You`.
  Nothing else is added anywhere, in any state.
- **The arrival.** `ADR-0110` §7 stands: the rival's joining is silent, no announcement string
  exists, and the sentence simply leaves with the state — the new `null-view` test is that, and it
  is the whole of it.
- **The live table's `You`.** `DuelTable.tsx:107` passes `name="You"` to the hero's plate and stays
  exactly as it is; `DuelTable.test.tsx`, `SeatPlate.test.tsx` and `App.test.tsx`'s two `getByText("You")`
  assertions are all about a table with a `Snapshot` and none of them moves.
- **`InvitePanel.tsx` and `InvitePanel.test.tsx`** — `TASK-140402`, merged before this starts.
- **`design/`.** `TASK-140401` merged it; this ticket **reads** the card and changes nothing in it.

## Tests

`WaitingTable.test.tsx` — 7

| Test | Proves |
| --- | --- |
| `names the empty seat once and leaves the host's plate standing and bare` | rewritten: the rival's line once, no `You` anywhere, and the host's plate present with empty text |
| `says the duel starts by itself, once` | new: the card's literal renders exactly once |
| the four existing tests | unchanged bodies — the invite, the way back, the promise, the single section, the column |

`Lobby.test.tsx` — 102, two rewritten in place. `null-view.test.tsx` — 10, one array member replaced
and one test added.

**The mutation gates, measured on a prototype in this worktree and reverted.**

| Mutation | Requires |
| --- | --- |
| `<span className="block font-medium">You</span>` put back inside the host's plate | `WaitingTable.test.tsx` `1 failed \| 6 passed (7)`, `Lobby.test.tsx` `1 failed \| 101 passed (102)`, `null-view.test.tsx` `1 failed \| 9 passed (10)` |
| the two-line comment and `<p>` that render `DUEL_STARTS_BY_ITSELF` deleted, the constant left in place | `WaitingTable.test.tsx` `1 failed \| 6 passed (7)`, `Lobby.test.tsx` `2 failed \| 100 passed (102)`, `null-view.test.tsx` `2 failed \| 8 passed (10)` |

The second mutation deliberately leaves the constant behind, so the four card-to-client literal
gates stay green while the render is gone: what it isolates is that the sentence is **rendered**,
not merely declared. Both gates restore the file and `cmp` it before judging.

## Acceptance criteria

- [ ] `design/screens/duel-table.html` yields exactly **one** distinct `<p class="autostart">` text,
      and that exact string appears in all four files of the *Files* table
- [ ] `src/table/WaitingTable.tsx` contains `You` zero times and `no clipboard` zero times
- [ ] `WaitingTable.test.tsx` reports `Tests  7 passed (7)`, with
      `names the empty seat once and leaves the host's plate standing and bare` and
      `says the duel starts by itself, once` among them
- [ ] `Lobby.test.tsx` reports `Tests  102 passed (102)` and `null-view.test.tsx` reports
      `Tests  10 passed (10)`
- [ ] With `You` put back on the plate, the three files report `1 failed | 6 passed (7)`,
      `1 failed | 101 passed (102)` and `1 failed | 9 passed (10)`
- [ ] With the sentence's `<p>` deleted, they report `1 failed | 6 passed (7)`,
      `2 failed | 100 passed (102)` and `2 failed | 8 passed (10)`
- [ ] `Back to the lobby` and
      `The room stays open. That link still works for your rival, and it brings you back.` still
      render byte-identically — the two `WaitingTable.test.tsx` tests that assert them are unchanged
- [ ] `cd web-client && NO_COLOR=1 npm run --silent check` exits 0
- [ ] The diff touches exactly the four files in the *Files* table
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
