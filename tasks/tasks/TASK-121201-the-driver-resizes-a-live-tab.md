---
schema: 2
id: TASK-121201
title: The driver resizes a live tab — a size verb over CDP
type: task
status: ready
parent: STORY-1212
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [process, qa, audit, harness]
depends_on: []
verify:
  - python3 .github/scripts/lint_tickets.py
  - node --check scripts/qa/drive.mjs
  - node scripts/qa/drive.mjs 1 size 390 664 2>&1 | grep -qF "no browser on port 1"
  - node scripts/qa/drive.mjs 1 nosuchverb 2>&1 | grep -qF "usage: node scripts/qa/drive.mjs"
  - "! node scripts/qa/drive.mjs 1 nosuchverb 2>&1 | grep -qF 'no browser on port 1'"
  - awk '/^import /{ if (!index($0,"node:")) bad=1 } /require\(/{bad=1} END{exit bad?1:0}' scripts/qa/drive.mjs
  - awk 'index($0,"size <width> <height>"){f=1} END{exit f?0:1}' scripts/qa/drive.mjs
  - awk 'index($0,"deviceScaleFactor: 0"){a=1} index($0,"mobile: false"){b=1} index($0,"screenOrientation"){c=1} index($0,"mobile: true"){d=1} END{exit (a&&b&&!c&&!d)?0:1}' scripts/qa/drive.mjs
  - grep -rl "Emulation\." scripts/qa | awk 'END{exit (NR==1)?0:1}'
  - awk 'index($0,"innerWidth"){a=1} index($0,"innerHeight"){b=1} END{exit (a&&b)?0:1}' scripts/qa/drive.mjs
---

## Goal

`node scripts/qa/drive.mjs <port> size <width> <height>` sets the attached tab's viewport to those
two numbers in CSS pixels, prints the shape it actually achieved, and **exits 1 if the achieved
shape is not the one requested** — over the DevTools socket the driver already holds, on a tab that
keeps its socket, its seat and its hand.

## The specification is `ADR-0097` §2, and it is binding

> It sends `Emulation.setDeviceMetricsOverride` with **`width`, `height`, `deviceScaleFactor: 0`
> and `mobile: false`, and no other field**. `0` is CDP's *"do not override the scale factor"*;
> `false` is *"do not emulate a mobile device"*; `screenOrientation` is **never** sent. Those three
> pins are the whole of what separates a resize from a fabricated device, and they are written as
> literal values in one verb so that one reviewer can check them against one file.
>
> The verb then **reads the viewport back from the page and prints the shape it achieved** —
> `innerWidth × innerHeight`, measured, not the numbers it was asked for — and **exits 1 if the
> read-back is not the request.**

## Files

| File | Action |
| --- | --- |
| `scripts/qa/drive.mjs` | modify |

You may **read** `docs/adr/ADR-0097-a-resize-is-two-numbers-and-the-observer-is-the-fifth-file.md`
§§1, 2 and 5 — nothing else in it is about this verb.

## Scope

- Add a `case "size":` to the verb switch, between the existing cases, following the shape every
  other case uses: `page = await attach();` then one `page.send(...)`, then a single line of
  stdout.
- Take the two numbers from `args[0]` and `args[1]`, `Number(...)` them, and
  `fail("size needs a width and a height")` when either is absent or not a finite number — the same
  `fail` helper every other verb uses, so the exit code is `1`.
- Send `Emulation.setDeviceMetricsOverride` with **exactly four fields**, spelled as literals:
  `{ width, height, deviceScaleFactor: 0, mobile: false }`. No `screenOrientation`, no
  `screenWidth`, no `screenHeight`, no user agent, no fifth field of any kind.
- Read the achieved viewport back **from the page**, through the existing `page.evaluate` helper:
  `[window.innerWidth, window.innerHeight]`. Do not trust the CDP response.
- Print the achieved shape on one line, in the form `size: <w>x<h>`, so a reader of the round
  transcript can see which shape the walk was in at that moment.
- **Exit 1 when the read-back is not the request**, with an error line naming both — the requested
  pair and the achieved pair. A clamp, a stale target or a call that silently did nothing must be a
  non-zero exit, never a walk that continues at the wrong shape.
- Add one line to the `default:` usage text, in the existing column layout:
  `  size <width> <height>    set this tab's viewport, in CSS pixels`.

## Out of scope

- **`mobile: true`, `deviceScaleFactor` other than `0`, `screenOrientation`, a touch domain, or a
  user-agent override.** `ADR-0097` §2 pins three fields and §5 pins the fourth by omission.
  Measured on 2026-08-31: with `mobile: true` a 390 px request produced a **520 px** layout
  viewport by mobile shrink-to-fit, which turns an `R2` *not met* into a **false pass** — the one
  criterion `ADR-0096` §Consequences predicts will fire hardest. Gate 8 refuses both strings.
- **`Browser.setWindowBounds`, `Page.setDeviceMetricsOverride`, or any other sizing route.**
  `ADR-0097` §Alternatives 1, rejected on measurement: Chrome clamps a window to 500 px wide, so
  390 × 664 came back as a **500 × 577** viewport, and 87 px of chrome made 720 × 900 into
  **720 × 813**.
- **Clearing the override.** Nothing clears it: `ADR-0089` §3's fresh Chrome profile per round
  already ends the tab's life with the round (`ADR-0097` §2).
- **Reloading, re-attaching or opening a fresh tab.** The whole point is that the tab stays alive —
  its JS context, its WebSocket and its seat survive the resize (`ADR-0097` §Context, measured).
  `attach()` with no `fresh` flag is what every other read verb uses; use that.
- **Any npm package, any `package.json`, any lockfile.** `ADR-0089` §2a and `ADR-0088` §1 forbid a
  browser dependency by name, and gate 6 refuses any `import` that is not `node:`-prefixed.
- **Changing any existing verb**, the `attach`/`waitFor` helpers, the `finally` block, or the
  `shot` verb's capture parameters.
- **Deciding when a resize happens in a round.** That is `.claude/agents/audit.md`'s
  (`TASK-121202`) and `.claude/skills/qa-cycle/SKILL.md`'s (`TASK-121204`). This ticket ships a
  verb, not a policy.
- **`docs/test-plan.md`.** Its verb list is the UAT catalogue's and the audit adds no case
  (`STORY-1212` §*Design notes*).

## Tests

No test class — `scripts/qa/` has none and this ticket does not invent a harness for a harness
(`ADR-0089` §2b: no `verify:` may wait on a QA case). The gates are what the binary does, and every
row below was run on 2026-08-31 at commit `f8383c4e`, against the tree as it stands.

| # | Gate | Proves | Today | With the verb |
| --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | the ticket, story and board rows agree | 0 | 0 |
| 2 | `node --check` | the file still parses as an ES module | 0 | 0 |
| 3 | `size 390 664` against port **1** | `size` is **a verb**, not the usage fallback: it reaches `attach()` → `targets()`, whose `fetch` to `localhost:1` is refused, and the shared `fail` prints `drive: no browser on port 1` | **1** — `size` falls through to `default`, which prints usage and exits 2, so the string never appears | 0 |
| 4 | `nosuchverb` against port 1 | the control for gate 3: the usage fallback still exists and is still where an unknown verb lands | 0 | 0 |
| 5 | `nosuchverb` does **not** print the attach failure | the second half of gate 3's control — exit 2 and exit 1 are distinguishable outcomes, not one message either way | 0 — a guard | 0 |
| 6 | `awk` over `import`/`require(` | Node built-ins only — every `import` names `node:`, nothing calls `require(` | 0 — a guard | 0 |
| 7 | `awk` over `size <width> <height>` | the usage text lists the verb | **1** | 0 |
| 8 | `awk` over four field literals | `deviceScaleFactor: 0` and `mobile: false` are present, and **`mobile: true` and `screenOrientation` are absent from the whole file** | **1** | 0 |
| 9 | `grep -rl "Emulation\."` over `scripts/qa` | **exactly one** file under `scripts/qa/` names the `Emulation.` domain, so a reviewer checking the field discipline has one file to read (`ADR-0097` §Consequences) | **1** — no file names it | 0 |
| 10 | `awk` over `innerWidth`/`innerHeight` | the read-back is taken from the page, not from the CDP response | **1** | 0 |

**Gate 3 is the one that cannot be faked, and gates 7 and 10 are the ones that can.** Gate 3
distinguishes exit 1 (the verb ran and could not attach) from exit 2 (the verb does not exist), and
a `size` that only printed a usage line, or that `fail`ed before touching the network, would not
produce that message. Measured on 2026-08-31: `size` exits **2** today and prints nothing about
port 1, while `open` — an existing verb — exits **1** and prints `drive: no browser on port 1`.
Gates 7 and 10 pass the moment the coder types the strings, whether or not the behaviour is there;
they are documentation checks and are worth exactly what `TASK-120702` said its gate 6 was worth.

**Gate 8 is the load-bearing refusal and it is still only a convention with a gate on it.**
`ADR-0097` §Consequences prices it honestly: adding `mobile: true` is a two-word change that would
flip an `R2` *not met* to a **false pass**, silent and measured, and **no CI job may catch it**
because `ADR-0089` §2b forbids one. What stands between the harness and that edit is this gate, the
literal values in one verb, and a reviewer reading one file. Say so in review rather than treating
the gate as a proof.

**What no gate here sees** is whether the viewport actually changed, or whether the read-back
assert fires on a clamp. Both need a live browser, and a `verify:` that waited on one would be a
gate waiting on a QA case (`ADR-0089` §2b). That half is the reviewer's, against `ADR-0097` §2's
four words — `setDeviceMetricsOverride`, two pinned fields, a read-back from the page, exit 1 on a
mismatch.

Port **1** is chosen because it is privileged, unassignable to a Chrome debug endpoint, and certain
to refuse.

## Acceptance criteria

- [ ] `node scripts/qa/drive.mjs 1 size 390 664` exits **1** and prints
      `drive: no browser on port 1` (gate 3).
- [ ] `node scripts/qa/drive.mjs 1 nosuchverb` still prints the usage block and does **not** print
      the attach failure (gates 4 and 5).
- [ ] `scripts/qa/drive.mjs` imports only from `node:` and calls `require(` nowhere (gate 6).
- [ ] The usage block lists `size <width> <height>` (gate 7).
- [ ] `scripts/qa/drive.mjs` contains `deviceScaleFactor: 0` and `mobile: false`, and contains
      neither `mobile: true` nor `screenOrientation` (gate 8).
- [ ] Exactly one file under `scripts/qa/` names `Emulation.` (gate 9).
- [ ] The verb reads `innerWidth` and `innerHeight` back from the page (gate 10).
- [ ] `node --check scripts/qa/drive.mjs` exits 0 (gate 2).
- [ ] The diff touches exactly one file besides this ticket's own status and its `BOARD.md` cell.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
