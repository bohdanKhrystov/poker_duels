---
schema: 2
id: TASK-140402
title: The copy control is never absent, and a press that cannot copy hands over the selection
type: task
status: backlog
parent: STORY-1404
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [client, table, lobby]
depends_on: [TASK-140401]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/InvitePanel.test.tsx > "${TMPDIR:-/tmp}/invite-after.txt" 2>&1; grep -qF 'Tests  6 passed (6)' "${TMPDIR:-/tmp}/invite-after.txt"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx > "${TMPDIR:-/tmp}/lobby-after.txt" 2>&1; grep -qF 'Tests  102 passed (102)' "${TMPDIR:-/tmp}/lobby-after.txt"
  - cd web-client && cp src/table/InvitePanel.tsx "${TMPDIR:-/tmp}/ip.fixed.tsx" && perl -0pi -e 's/(const \[outcome, setOutcome\] = useState<"none" \| "copied" \| "refused">\("none"\);\n)/$1  if (!navigator.clipboard) {\n    return null;\n  }\n/' src/table/InvitePanel.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/InvitePanel.test.tsx > "${TMPDIR:-/tmp}/m1a.txt" 2>&1; FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx > "${TMPDIR:-/tmp}/m1b.txt" 2>&1; cp "${TMPDIR:-/tmp}/ip.fixed.tsx" src/table/InvitePanel.tsx; cmp -s "${TMPDIR:-/tmp}/ip.fixed.tsx" src/table/InvitePanel.tsx && grep -qF 'Tests  2 failed | 4 passed (6)' "${TMPDIR:-/tmp}/m1a.txt" && grep -qF 'Tests  3 failed | 99 passed (102)' "${TMPDIR:-/tmp}/m1b.txt"
  - cd web-client && cp src/table/InvitePanel.tsx "${TMPDIR:-/tmp}/ip.fixed.tsx" && perl -0pi -e 's/    props\.box\.current\?\.focus\(\);\n    props\.box\.current\?\.select\(\);\n//' src/table/InvitePanel.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/InvitePanel.test.tsx > "${TMPDIR:-/tmp}/m2a.txt" 2>&1; FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx > "${TMPDIR:-/tmp}/m2b.txt" 2>&1; cp "${TMPDIR:-/tmp}/ip.fixed.tsx" src/table/InvitePanel.tsx; cmp -s "${TMPDIR:-/tmp}/ip.fixed.tsx" src/table/InvitePanel.tsx && grep -qF 'Tests  2 failed | 4 passed (6)' "${TMPDIR:-/tmp}/m2a.txt" && grep -qF 'Tests  1 failed | 101 passed (102)' "${TMPDIR:-/tmp}/m2b.txt"
  - cd web-client && test "$(grep -c 'return null' src/table/InvitePanel.tsx || true)" = "0"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`Copy the link` renders in every browser, and a press that cannot copy — because the browser exposes
no Clipboard API, or because the write was refused — puts focus on the invite-link box with the
whole link selected and renders `Copy it from the box above.` `Link copied.` is said only when a
copy actually happened.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/InvitePanel.tsx` | modify |
| `web-client/src/table/InvitePanel.test.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read [`ADR-0128`](../../docs/adr/ADR-0128-the-copy-control-is-never-absent-and-a-press-that-cannot-copy-hands-over-the-selection.md)
§§1–5. In `Lobby.test.tsx` — a 1400-line file — only two tests are of interest: the one at line 710
today, *"offers no copy button when the browser has no clipboard"*, and the one at line 1332,
*"states the six strings the host-alone table renders with no clipboard, and no seventh"*.
**Nothing else is opened, and no other file in the repository is changed** — `WaitingTable.tsx` and
`null-view.test.tsx` in particular belong to `TASK-140403`.

## Scope

- **`InvitePanel.tsx` — the whole file after this ticket is exactly this**, so that the two mutation
  gates in `verify:` match the text they rewrite:

  ```tsx
  import { useRef, useState, type ReactElement, type RefObject } from "react";
  import { roomLink } from "../lobby/room-link";

  /**
   * The invite is selectable text before it is anything else: the one interaction
   * this product depends on cannot need a working clipboard.
   */
  export function InvitePanel(props: { readonly code: string }): ReactElement {
    const link = roomLink(window.location.origin, props.code);
    const box = useRef<HTMLInputElement>(null);
    return (
      <>
        <p className="rounded-medium border border-hairline bg-surface px-5 py-4 text-center font-mono text-display tracking-[var(--pd-track-code)] text-text">
          {props.code}
        </p>
        <label htmlFor="invite-link">Invite link</label>
        <input
          autoFocus
          ref={box}
          id="invite-link"
          className="rounded-medium border border-hairline bg-surface px-5 py-4 text-text"
          readOnly
          value={link}
          onFocus={(event) => event.currentTarget.select()}
        />
        <CopyLink link={link} box={box} />
      </>
    );
  }

  /** ADR-0128 §1: never absent. §3: a press that cannot copy hands over the selection. */
  function CopyLink(props: {
    readonly link: string;
    readonly box: RefObject<HTMLInputElement>;
  }): ReactElement {
    const [outcome, setOutcome] = useState<"none" | "copied" | "refused">("none");
    const handOver = (): void => {
      props.box.current?.focus();
      props.box.current?.select();
      setOutcome("refused");
    };
    return (
      <>
        <button
          type="button"
          className="rounded-medium border border-transparent bg-accent-fill px-5 py-4 leading-tight font-medium text-on-accent"
          onClick={() => {
            if (!navigator.clipboard) {
              handOver();
              return;
            }
            void navigator.clipboard
              .writeText(props.link)
              .then(() => setOutcome("copied"), handOver);
          }}
        >
          Copy the link
        </button>
        {outcome === "copied" && <p>Link copied.</p>}
        {outcome === "refused" && <p>Copy it from the box above.</p>}
      </>
    );
  }
  ```

  Three things in it are load-bearing. `CopyLink` returns `ReactElement`, not `ReactElement | null`
  — the type is what makes a re-added early return a compile error as well as a red test. The two
  no-copy routes share **one** function, `handOver`, because `ADR-0128` §3 makes them one outcome.
  And the box is reached through a `ref` threaded from `InvitePanel`, not through
  `document.getElementById`: the panel already owns that input, and a DOM lookup would work in this
  tree by accident and stop working in any tree that mounts two.
- **`InvitePanel.test.tsx` — six tests, named exactly as the *Tests* table below.** The three
  unchanged ones keep their bodies. The one asserting the control's absence is **rewritten, never
  deleted** (`ADR-0128`'s *Consequences* names its inversion as the point), and it becomes the two
  hand-over tests.
- **The hand-over tests must break the box's own precondition before the press.** The input carries
  `autoFocus` with `onFocus` → `select()`, so at rest it is **already** focused with the whole link
  already selected: an assertion made without breaking that would pass on a press that did nothing.
  Every hand-over test therefore calls `box.setSelectionRange(0, 0)` and focuses the button first,
  and checks that the focus moved, before pressing.
- **`Lobby.test.tsx`, three edits and no fourth.**
  1. The test at line 710 becomes *"offers the copy button and hands over the selection with no
     clipboard"*: the same store and render, then the button is found rather than refused, the
     precondition is broken as above, the press is fired, and `Copy it from the box above.` renders
     while `Link copied.` does not, with focus on the box and `selectionEnd` at `value.length`.
  2. The test at line 1332 is renamed *"states the seven strings the host-alone table renders with
     no clipboard, and no eighth"*, and `"Copy the link"` joins its array. Nothing else in that array
     moves — `"You"` is still there after this ticket and leaves in `TASK-140403`.
  3. In *"the waiting frame's controls are dressed, not bare"* (line ~632), the two-line comment
     *"The copy button only renders with a clipboard to call (see CopyLink), and it is one of the
     three controls this test names"* and the `withClipboard(() => Promise.resolve());` line under
     it are both deleted. The comment is false after §1 and the call is now noise; the test's three
     assertions do not change.

## Out of scope

- **`WaitingTable.tsx` and its stale comment** — *"copy button with three states (at rest, copied,
  refused, no clipboard)"* is wrong after this merges, and `TASK-140403` corrects it. It is the one
  known-stale thing this ticket leaves behind, deliberately, because `WaitingTable.tsx` cannot enter
  this budget without dragging three test files with it.
- **`null-view.test.tsx`.** Measured, not assumed: its only exhaustive text sweep is *"the copy
  control's feedback adds one named string and no other"*, which installs a clipboard in **both** of
  its arms, and every other test in that file sweeps digits and `aria-label`/`title` only — and
  `Copy the link` carries neither. The prototype ran the whole client suite green with this file
  untouched.
- **`You`, and the auto-start sentence.** `TASK-140403`, which merges after this one.
- **Any attempt to copy without `navigator.clipboard`.** `document.execCommand("copy")` and its
  relatives are `DEC-150`'s, the architect's, and `ADR-0128` §7 says outright that no ticket of this
  story waits on it. Writing one here would also break §4, because its success cannot be read.
- **The strings.** `Copy the link`, `Link copied.` and `Copy it from the box above.` are shipped and
  byte-identical; `ADR-0128` §5 adds none and spends none of `ADR-0110` §6's escape clause.
- **`design/`.** `TASK-140401` merged the frames already.

## Tests

`InvitePanel.test.tsx` — six tests, in this order.

| Test | Proves |
| --- | --- |
| `shows the bare code, the labelled box and the copy control` | modified: installs **no** clipboard, and the control renders anyway (`ADR-0128` §1) |
| `builds the link from this window's origin and the code` | unchanged |
| `leaves the box read-only and focused for a copy by hand` | unchanged |
| `says so when the copy succeeds` | `Link copied.` on a resolved write, and `Copy it from the box above.` absent |
| `hands over the selection when the write is refused` | the async route: focus on the box, `selectionStart` 0 and `selectionEnd` at the link's length, the sentence rendered, `Link copied.` absent |
| `offers the control and hands over the selection with no Clipboard API` | the sync route — **the path no gate in this project has ever exercised** |

`Lobby.test.tsx` — 102 tests, unchanged in number; two rewritten in place and named as *Scope* says.

**How the fixture removes the API, and why that is the honest fixture.** It does not remove it: it
never installs it. jsdom 24 implements no Clipboard API at all — `navigator.clipboard` is
`undefined` and `"clipboard" in navigator` is `false` — so a test that skips `withClipboard` **is**
the browser `ADR-0128` was written for. Both files already carry
`afterEach(() => Reflect.deleteProperty(navigator, "clipboard"))`, which is the guard against a
clipboard installed by an earlier test leaking into this one; keep it. Both no-API tests open with
`expect(navigator.clipboard).toBeUndefined()` so that a future jsdom which grows a stub cannot let
them pass while testing the wrong browser. This matters because `ADR-0117` drives the proofs of
record from `http://localhost:4173`, a potentially-trustworthy origin where the API **is** defined —
so these two tests are the only automated proof of this path that this project has.

**The mutation gates, measured on a prototype in this worktree and reverted.**

| Mutation | Requires |
| --- | --- |
| the `if (!navigator.clipboard) return null;` guard put back at the top of `CopyLink` | `InvitePanel.test.tsx` `2 failed \| 4 passed (6)` and `Lobby.test.tsx` `3 failed \| 99 passed (102)` |
| the two `props.box.current?…` lines deleted from `handOver` | `InvitePanel.test.tsx` `2 failed \| 4 passed (6)` and `Lobby.test.tsx` `1 failed \| 101 passed (102)` |

Both restore the file and `cmp` it before judging, so a gate that failed to revert fails loudly
instead of leaving a mutated tree. The second is why the hand-over assertions cannot be satisfied by
the box's own `autoFocus`: with the two lines gone the box is still focused at render, and the tests
still go red.

## Acceptance criteria

- [ ] `InvitePanel.test.tsx` reports `Tests  6 passed (6)`, with the six test names above
- [ ] `Lobby.test.tsx` reports `Tests  102 passed (102)`
- [ ] `src/table/InvitePanel.tsx` contains no `return null`
- [ ] With the absence guard restored, `InvitePanel.test.tsx` reports `2 failed | 4 passed (6)` and
      `Lobby.test.tsx` reports `3 failed | 99 passed (102)`
- [ ] With `handOver`'s two `props.box.current?` lines deleted, `InvitePanel.test.tsx` reports
      `2 failed | 4 passed (6)` and `Lobby.test.tsx` reports `1 failed | 101 passed (102)`
- [ ] `cd web-client && NO_COLOR=1 npm run --silent check` exits 0
- [ ] The diff touches exactly the three files in the *Files* table
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
