---
schema: 2
id: TASK-120702
title: The driver captures a screen — a shot verb over CDP
type: task
status: done
parent: STORY-1207
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [process, qa, uat, harness]
depends_on: [TASK-120701]
verify:
  - python3 .github/scripts/lint_tickets.py
  - node --check scripts/qa/drive.mjs
  - node scripts/qa/drive.mjs 1 shot /tmp/pd-shot-probe.png 2>&1 | grep -qF "no browser on port 1"
  - node scripts/qa/drive.mjs 1 nosuchverb 2>&1 | grep -qF "usage: node scripts/qa/drive.mjs"
  - "! node scripts/qa/drive.mjs 1 nosuchverb 2>&1 | grep -qF 'no browser on port 1'"
  - awk '/^import /{ if (!index($0,"node:")) bad=1 } /require\(/{bad=1} END{exit bad?1:0}' scripts/qa/drive.mjs
  - awk 'index($0,"shot <path>"){f=1} END{exit f?0:1}' scripts/qa/drive.mjs
---

## Goal

`node scripts/qa/drive.mjs <port> shot <path>` writes the attached page's rendered screen to
`<path>` as a PNG, over the DevTools socket the driver already holds.

## The specification is `ADR-0092` §2a, and it is binding

> CDP `Page.captureScreenshot` over the WebSocket it already holds, the PNG written with
> `node:fs` into the round's own `mktemp -d` directory. Node built-ins only; no module's
> dependency set changes; Chrome remains a machine-local binary this repository does not vendor,
> pin or ship. **No image-comparison tooling enters this repository under this ADR** — a
> screenshot is read by a reader, never diffed by a program — and the day pixel tooling is
> wanted, §2a is failing and the question returns as a new `DEC`.

## Files

| File | Action |
| --- | --- |
| `scripts/qa/drive.mjs` | modify |

You may **read** `docs/adr/ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md`
§2a and §3 — nothing else in it is about this verb.

## Scope

- Add a `case "shot":` to the verb switch, between the existing cases, following the shape every
  other case uses: `page = await attach();` then one `page.send(...)`, then a single line of
  stdout.
- Take the output path from `args[0]`, and `fail("shot needs a path")` when it is absent — the
  same `fail` helper every other verb uses, so the exit code is `1`.
- `Page.captureScreenshot` returns base64 in `result.data`. Decode it with
  `Buffer.from(data, "base64")` and write it with `writeFileSync` from **`node:fs`** — the file's
  first and only `import`, spelled `import { writeFileSync } from "node:fs";` at the top.
- Print the path written, one line, so a caller can read it from stdout.
- Add one line to the `default:` usage text, in the existing column layout:
  `  shot <path>             write the rendered screen to <path> as a PNG`.

## Out of scope

- **Any npm package, any `package.json`, any lockfile.** `ADR-0089` §2a and `ADR-0088` §1 forbid
  a browser dependency by name, and gate 5 refuses any `import` that is not `node:`-prefixed.
- **Any image comparison, screenshot diff, pixel threshold, or PNG parsing beyond writing the
  bytes.** `ADR-0092` §2a: *no image-comparison tooling enters this repository under this ADR.*
  A diff threshold is an opinion in a verify block, which `ADR-0024` §3 says a verify block cannot
  hold. Wanting one is a new `DEC`, not a wider ticket.
- **Committing a screenshot, or adding a `.gitignore` entry for one.** The caller supplies an
  absolute path under the round's `mktemp -d`, outside the repository; the verb writes exactly
  where it is told and nowhere else.
- **Choosing or creating the round's temp directory.** That is `qa-cycle`'s, which already
  `mktemp -d`s browser profiles.
- **A full-page, clipped, scaled or device-emulated capture.** The viewport as rendered is the
  observation. Capture parameters are a decision nothing has made.
- **Changing any existing verb**, the `attach`/`waitFor` helpers, or the `finally` block.
- **`docs/test-plan.md`.** Its verb list gains `shot` in `TASK-120703`, which owns that file and
  gates the two lists against each other.

## Tests

No test class — `scripts/qa/` has none and this ticket does not invent a harness for a harness
(`ADR-0089` §2b: no `verify:` may wait on a QA case). The gates are what the binary does, and
every row was run on 2026-08-30 at commit `cfcc6a4e`.

| # | Gate | Proves | Today | With the verb |
| --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | the ticket, story and board rows agree | 0 | 0 |
| 2 | `node --check` | the file still parses as an ES module | 0 | 0 |
| 3 | `shot` against port **1** | `shot` is **a verb**, not the usage fallback: it reaches `attach()` → `targets()`, whose `fetch` to `localhost:1` is refused, and the shared `fail` prints `drive: no browser on port 1` | **exits 1** — `shot` falls through to `default`, which prints usage and exits 2, so the string never appears | 0 |
| 4 | `nosuchverb` against port 1 | the control for gate 3: the usage fallback still exists and is still where an unknown verb lands | 0 | 0 |
| 5 | `awk` over `import`/`require(` | Node built-ins only — every `import` line names `node:`, and nothing calls `require(` | 0 — the file has no imports at all today, so this is a **guard**, not a progress gate | 0 |
| 6 | `awk` over `shot <path>` | the usage text lists the verb | **exits 1** | 0 |

**Gate 3 is the one that cannot be faked, and gate 6 is the one that can.** Gate 6 passes the
moment the coder types the string into the usage block, whether or not a verb exists; it is a
documentation check and nothing more. Gate 3 is different in kind: it distinguishes exit 1 (the
verb ran and could not attach) from exit 2 (the verb does not exist), and a `shot` that only
printed a usage line, or that `fail`ed before touching the network, would not produce that
message. **Measured both ways on 2026-08-30**: `shot` exits **2** today and prints nothing about
port 1, while `open` — an existing verb — exits **1** and prints `drive: no browser on port 1`.

Port **1** is chosen because it is privileged, unassignable to a Chrome debug endpoint, and
certain to refuse. `/tmp/pd-shot-probe.png` is never written, because the verb dies at attach.

**What no gate here sees** is the capture itself: whether the PNG has the page in it needs a live
browser, and a `verify:` that waited on one would be a gate waiting on a QA case (`ADR-0089`
§2b). That half is the reviewer's, against `ADR-0092` §2a's four words — `Page.captureScreenshot`,
`node:fs`, no dependency, never committed.

## Acceptance criteria

- [ ] `node scripts/qa/drive.mjs 1 shot /tmp/pd-shot-probe.png` exits **1** and prints
      `drive: no browser on port 1` (gate 3).
- [ ] `node scripts/qa/drive.mjs 1 nosuchverb` still prints the usage block (gate 4).
- [ ] `scripts/qa/drive.mjs` imports only from `node:` and calls `require(` nowhere (gate 5).
- [ ] The usage block lists `shot <path>` (gate 6).
- [ ] `node --check scripts/qa/drive.mjs` exits 0 (gate 2).
- [ ] The diff touches exactly one file besides this ticket's own status and its `BOARD.md` cell.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
