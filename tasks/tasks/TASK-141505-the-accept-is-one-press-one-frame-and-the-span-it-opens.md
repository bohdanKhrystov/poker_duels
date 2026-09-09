---
schema: 2
id: TASK-141505
title: The accept is one press, one frame, and the span it opens
type: task
status: backlog
parent: STORY-1415
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, rematch, notice]
depends_on: [TASK-141504]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/result/RematchNotice.test.tsx 2>&1 | grep -qE '^ *Tests +9 passed \(9\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/result/RematchControl.test.tsx 2>&1 | grep -qE '^ *Tests +12 passed \(12\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | grep -qE '^ *Tests +113 passed \(113\)$'
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"useEffect") || index($0,"useLayoutEffect") || index($0,"setTimeout") || index($0,"setInterval") { n++ } END { exit (n != 0) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"type=\"button\"") { n++ } END { exit (n != 1) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"{REMATCH_LABEL}") { n++ } END { exit (n != 1) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"{DEALING_LEAD}") { n++ } END { exit (n != 1) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"{DEALING_TAIL}") { n++ } END { exit (n != 1) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"OfferRematch") { n++ } END { exit (n != 1) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"disabled") { n++ } END { exit (n != 0) }' web-client/src/result/RematchNotice.tsx
  - git diff --quiet develop -- web-client/src/lobby/Lobby.tsx
  - git diff --quiet develop -- web-client/src/App.tsx
  - git diff --quiet develop -- web-client/src/result/RematchControl.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The panel carries the `Rematch` control, one press of it sends exactly one `{ type: "OfferRematch" }`
and nothing else, and between that press and the new duel's `Snapshot` the panel shows the dealing
sentence in place of the button — the span `ADR-0044` §4 sends no frame for.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/RematchNotice.tsx` | modify |
| `web-client/src/result/RematchNotice.test.tsx` | modify |

Read [`ADR-0138`](../../docs/adr/ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md)
§§3 and 4, [`ADR-0044`](../../docs/adr/ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) §§3–4,
[`ADR-0127`](../../docs/adr/ADR-0127-a-control-stands-only-for-a-decision-the-server-has-opened.md)
§1, `web-client/src/result/RematchControl.tsx` (which already holds this span, at
`RematchControl.tsx:32`), and `design/screens/rematch-panel.html` frames 1 and 2. **Nothing outside
the table above is changed**; three `verify:` lines diff `Lobby.tsx`, `App.tsx` and
`RematchControl.tsx` against `develop`.

## Scope

- **The accept is `useSend()` and one frame.** `onClick` sets the span flag and calls
  `send({ type: "OfferRematch" })`. That is the frame `RematchControl` already sends and the only
  frame this whole story causes to be sent (`ADR-0138` §7). A gate holds `OfferRematch` at exactly
  **1** non-comment occurrence.
- **No `disabled` lock.** `ADR-0044` §3 makes `OfferRematch` idempotent on the wire — a repeat is
  answered with the same `RematchOffered`, never an error — so a second press is harmless and a
  lock would be a guard against nothing. `ADR-0127` §1: the control is never drawn faint, disabled
  or as a placeholder. A gate holds `disabled` at **0**.
- **`type="button"`, exactly once so far.** The panel is outside every `<form>` by position
  (`ADR-0138` §1), and this is belt as well as braces. `TASK-141507` adds the second button and
  moves this count.
- **The span is one `useState` boolean, and it is cleared during render when the offer ends.**

  ```tsx
  const [accepted, setAccepted] = useState(false);
  // ADR-0138 §3's clear, applied to the second boolean for §3's own reason: an
  // offer ends and another is made, and a flag that outlived its offer would show
  // the dealing sentence in place of the button on every later offer for the life
  // of the tab. Set during render, React's documented way to adjust state when
  // what it was derived from changes: it terminates (after the set the condition
  // is false), it is pure so StrictMode-safe, and it costs no effect — which
  // keeps §1's "no effect of any kind" true.
  if (accepted && !theirs) setAccepted(false);
  ```

  **This needs no `eslint-disable`.** Probed at `f20d07ed`: `react-hooks/set-state-in-render` is at
  `error` in `eslint-plugin-react-hooks@^7`'s `recommended-latest`, it fires on an unguarded
  `setAccepted(x)` in a render body, and it passes the guarded form above with exit 0. A
  suppression comment here is noise; the explanatory comment is not.
- **`ADR-0138` §4 says the span is held *"exactly as `RematchControl.tsx:32` does"*, and that is
  about where the fact lives, not about its reset.** `RematchControl` never clears its own
  `accepted`; copying that into a component whose lifetime is the whole document reproduces
  `ADR-0123` §7's named failure one field over. The clear is the merged rule applied, and
  `the panel offers the button again after the offer that was accepted has gone` is the test that
  holds it. `RematchControl`'s own stale flag is a different component's defect and is **out of
  scope**, recorded in `STORY-1415`.
- **The order of the returns is: the span, then the standing offer.** `accepted` wins over the
  button, exactly as `RematchControl.tsx:53` orders them, so a press does not leave the button
  under the sentence for a frame.
- **No new words.** `REMATCH_LABEL`, `DEALING_LEAD` and `DEALING_TAIL` come from `rematch-text.ts`,
  with the same `<br />` between the two dealing lines that `RematchControl` uses.

## Out of scope

- **`refusal === "UNKNOWN_ROOM"`** — `TASK-141506`.
- **`Not now`** — `TASK-141507`. The panel is still undismissable and still mounted nowhere.
- **`RematchControl.tsx`, `App.tsx`, `Lobby.tsx`, `rematch-text.ts`.** None is opened.

## Tests

`RematchNotice.test.tsx` grows from **5** to **9**. The five that exist are unchanged: this ticket
adds a control to a panel that already stood, and changes nothing any of them observes.

| Test | Proves |
| --- | --- |
| `offers the rematch control beside the rival's line` | at `#/account` with the offer standing, `getByRole("button", { name: REMATCH_LABEL })` is inside the `[role="status"]` root |
| `one press sends exactly one OfferRematch and nothing else` | the `send` spy is called **once**, and its one argument is `{ type: "OfferRematch" }`. The count is the assertion — a presence check passes on a component that sends it twice |
| `the press opens the dealing span in place of the button` | after the click, the dealing sentence is on screen and `queryByRole("button", { name: REMATCH_LABEL })` is null |
| `the panel offers the button again after the offer that was accepted has gone` | press, then apply a `Snapshot`, a `DuelFinished` and a fresh `RematchOffered` from seat `0` **each in its own `act()`**: the button is back and the dealing sentence is gone. **Without the render-phase clear this test fails**, which is the whole reason it exists |

**Two things about these two tests were measured against a prototype, and both bite silently.**

- **`getByText(DEALING_TAIL)` does not find it.** The two lines share one element across a `<br />`,
  and Testing Library matches an element's whole normalised text — so a `getByText` on either
  constant throws *"Unable to find an element with the text"*. Use the regex the merged
  `RematchControl.test.tsx:133` already uses over the whole node:
  `getByText(/The button changes sides.*dealing hand 1…/)`. A coder who reaches for the constant
  gets a red test against correct code and will be tempted to change the code.
- **The three frames must arrive in three separate `act()` calls.** The clear fires only in a render
  where `theirs` is false, and that render exists only if React commits between the `Snapshot` and
  the `RematchOffered`. Measured: with the three `store.apply` calls inside **one** `act()`, React
  batches them, the intermediate render never happens, and the button does **not** come back — the
  test fails against a correct component. With one `act()` each, it does. In production the frames
  arrive on separate socket messages in separate tasks, so React does not batch them, and this is a
  fact about the test harness rather than about the design.

## What would still pass if this were built wrong

`one press sends exactly one OfferRematch and nothing else` asserts the **call count** and the
argument, not that a call happened: a handler that also sent a second frame, or that sent on every
render, passes a presence check and fails this.

`the press opens the dealing span…` asserts the button is **gone** as well as the sentence being
present. A component that rendered the sentence beside the button would otherwise pass, and that is
the state `ADR-0044` §4 says does not exist.

`the panel offers the button again…` is the only test in this story that fails on a correct-looking
component: `accepted` never resets, so the panel would be permanently in the dealing state after one
press, and every other test here — each of which presses at most once inside its own render — stays
green. Deliver the three frames in the order `Snapshot`, `DuelFinished`, `RematchOffered`; a
`Snapshot` alone clears `rematchOffers` and a `DuelFinished` alone does too, but only the pair
returns the room to a finished standing that can carry a new offer.

## Acceptance criteria

- [ ] `npx vitest run src/result/RematchNotice.test.tsx` reports **9 passed (9)**, the four new ones
      being the four named above and the original five unedited
- [ ] `src/result/RematchControl.test.tsx` reports **12 passed (12)** and `src/lobby/Lobby.test.tsx`
      **113 passed (113)** — measured on `develop` at `f20d07ed`
- [ ] `RematchNotice.tsx` has, on non-comment lines, `OfferRematch` **1**, `type="button"` **1**,
      `{REMATCH_LABEL}` **1**, `{DEALING_LEAD}` **1**, `{DEALING_TAIL}` **1**, `disabled` **0**, and
      still **0** of `useEffect`, `useLayoutEffect`, `setTimeout`, `setInterval`
- [ ] `git diff --quiet develop` is clean for `Lobby.tsx`, `App.tsx` and `RematchControl.tsx`
- [ ] **Shown red, then reverted:** deleting the line `if (accepted && !theirs) setAccepted(false);`
      makes `the panel offers the button again after the offer that was accepted has gone` fail and
      leaves the other eight green. The mutation was run, observed red by that name, and reverted
- [ ] `npm run check` exits 0, with no `eslint-disable` added anywhere in this diff

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
