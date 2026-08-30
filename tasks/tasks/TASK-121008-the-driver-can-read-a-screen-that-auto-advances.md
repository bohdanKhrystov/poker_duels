---
schema: 2
id: TASK-121008
title: The driver can read a screen that auto-advances between polls
type: task
status: done
parent: STORY-1210
module: qa
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [qa, uat, harness, manual-verify]
depends_on: []
verify:
  - node --check scripts/qa/drive.mjs
  - test "$(grep -c '__pdFrames' scripts/qa/drive.mjs)" -ge 2
  - node scripts/qa/drive.mjs 2>&1 | grep -qF 'record'
  - node scripts/qa/drive.mjs 2>&1 | grep -qF 'frames'
  - grep -qF '`record`' docs/test-plan.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`scripts/qa/drive.mjs` can report what a screen said during a window of time, not only what it says
at the moment of a poll — so a frame that resolves between two polls can be read instead of reported
as unreachable for a third round running.

## The defect — and it is a capability gap, not an `ADR-0089` §4 harness defect

Round 2 of `/qa-cycle uat regression`, 2026-08-30. **Two rounds have now failed the same read**, on
the same two frames:

> the intra-duel showdown/fold winner banner (`duel-table-states.html`'s second and third frames)
> auto-advances faster than repeated `drive.mjs` polls could reliably catch, across three attempts.
> (round 2's report; round 1 reported the same limitation on the same frames)

**The classification is stated carefully rather than conveniently.** `ADR-0089` §4's harness defect
is a case that **fails** without reproducing by hand. No catalogue case asserts either banner, so
nothing is red, nothing is excluded from `B(N)`, and `EPIC-12`'s Definition-of-done box for telling a
harness defect from a product defect on the record stays **unticked**. This ticket does not tick it
by relabelling a reach gap.

**But the cost is now measured rather than predicted.** Check **(a)** on two of
`duel-table-states.html`'s three frames is unreachable by any round with the verbs the driver has.
`waitFor` polls `#root`'s text every 250 ms (`drive.mjs:95–100`); a frame that appears and is
replaced inside one interval is not *slow to catch*, it is **invisible to a poller** at any interval,
because polling samples and the frame is between samples. A fresh `node` process cannot start inside
the window either. Round 1 named it a *gap in reach* and left it for the next pass; round 2 paid the
same cost, and a limitation that recurs is evidence about the tool.

**It is repaired in `scripts/qa/`**, which is where `EPIC-12` §Termination rule 6 puts harness work,
and it is **excluded from `B(2)`** — not under rule 6, which does not reach it, but for the same
reason a card ticket is excluded: `B(N)` counts product defects and a missing driver verb is not one.

## Files

| File | Action |
| --- | --- |
| `scripts/qa/drive.mjs` | modify |
| `docs/test-plan.md` | modify |

## Scope

- **A `record` verb** that installs a `MutationObserver` on `#root` and appends each **distinct**
  rendered text to a page-global buffer, `window.__pdFrames`, clearing it first. Armed **before** the
  action that triggers the transition, so the transition is observed rather than sampled.
- **A `frames [n]` verb** that prints the buffer — the last `n` entries, all of them by default —
  one per line with a separator a reader can see, and exits 1 with a clear message if `record` was
  never armed on this page.
- **Both verbs appear in the usage block** at the bottom of `drive.mjs`, in the same one-line style
  as `wait` and `absent`.
- **`docs/test-plan.md` §UAT gains two sentences** naming `record`/`frames` and saying what they are
  for: a frame that auto-advances is read by arming a recorder, not by polling faster.
- Node built-ins only. **No dependency is added anywhere** — `ADR-0089` §2a, and `ADR-0088` §1's
  sentence about `web-client/package.json` stays byte-unchanged.

## Out of scope

- **Writing application state.** `ADR-0089` §3 lets the driver read anything and write nothing but
  `pd.roomCode`. A `MutationObserver` and a scratch global write **no** store field, no socket frame
  and no row — the buffer is the driver's own notebook, not a game fact — and the implementation must
  keep it that way: no `dispatchEvent`, no store access, no `localStorage` key but the licensed one.
- **Any use in a gate.** `ADR-0089` §2c: no round and no verb of this driver may be cited in an
  epic's `Metrics`, a Definition of done or a ticket's `verify:` as coverage. This ticket's own
  `verify:` block gates the **file**, never a run.
- **Writing catalogue cases for the two frames.** `ADR-0090` §1 makes authoring and running two
  commands; the next `/qa-cases` pass owns it, and this ticket only makes the case writable.
- **The rematch screen's transitional copy**, the other sub-state round 1 could not reach. The same
  verb should reach it; proving that is a round's business, not this ticket's.
- **Image diffing, or any screenshot comparison.** `ADR-0092` forbids image-diff tooling for good;
  `shot` stays a capture a human looks at.

## Tests

**No automated test can prove the behavioural half, and that is said rather than faked.** Proving
`record` catches a transient frame needs a browser, and `ADR-0089` §2a forbids a browser dependency
in any gate — a `verify:` line that started Chrome would break the standing condition the whole
permission rests on. A grep that passed either way would be worse than an honest manual step, and
this repository has been bitten by exactly that.

So the ticket is `labels: [manual-verify]`, and the `verify:` block gates only what a command
honestly can. **Each line fails today:**

| Command | Proves | Today |
| --- | --- | --- |
| `node --check scripts/qa/drive.mjs` | the file still parses | passes; kept so a broken edit cannot land |
| `grep -c '__pdFrames' … -ge 2` | the buffer is both written and read — one occurrence would be a verb that arms nothing or reads nothing | count is `0` |
| `drive.mjs 2>&1 \| grep -qF 'record'` | the usage block names the verb | count is `0` |
| `drive.mjs 2>&1 \| grep -qF 'frames'` | the usage block names the reader | count is `0` |
| the fifth line, over `docs/test-plan.md` | the catalogue tells a round the verb exists | count is `0` |

The greps are structural and they are labelled as such: they prove the verb **exists**, never that it
**works**. The acceptance criteria below carry that half, by hand.

## Acceptance criteria

- [ ] `node scripts/qa/drive.mjs` with no arguments lists `record` and `frames` in its usage block
- [ ] **By hand, on a live stack, and this is the criterion that matters**: with two browsers in a
      duel, `record` on the browser about to see the banner, then play the hand to a showdown, then
      `frames` — and the showdown banner's text is in the output. Run it twice, because a capability
      that works once against a transition is indistinguishable from luck
- [ ] **The same run, for a fold**: `record`, fold the hand, `frames`, and the fold banner's text is
      in the output
- [ ] `record` writes nothing but `window.__pdFrames` — the reviewer greps the diff for
      `localStorage`, `dispatchEvent` and any store access and finds none
- [ ] `git diff --stat` shows no change to any `package.json` anywhere
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
