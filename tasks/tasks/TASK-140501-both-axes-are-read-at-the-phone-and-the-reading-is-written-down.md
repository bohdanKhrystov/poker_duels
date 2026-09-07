---
schema: 2
id: TASK-140501
title: Both axes are read at the phone, and the reading is written down
type: task
status: done
parent: STORY-1405
module: docs
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [table, client, docs, manual-verify, bug]
depends_on: []
verify:
  - python3 -c "import re,sys;t=open('tasks/epics/EPIC-14-the-name-the-showdown-and-the-fit.md',encoding='utf-8').read();p=t.split('### The 390 × 664 reading');sys.exit(0 if len(p)==2 and 'Commit:' in p[1] and len(re.findall(r'[|] *[0-9]+ */ *[0-9]+ *[|] *[0-9]+ */ *[0-9]+ *[|]', p[1]))>=3 else 1)"
  - sh -c 'test "$(grep -c "^- \[x\]" tasks/epics/EPIC-14-the-name-the-showdown-and-the-fit.md)" = "6"'
  - sh -c 'test "$(grep -c "^- \[ \]" tasks/epics/EPIC-14-the-name-the-showdown-and-the-fit.md)" = "9"'
  - grep -qF -e '- [ ] The duel table is walked on a **real iPhone** over the local network and, at every beat,' tasks/epics/EPIC-14-the-name-the-showdown-and-the-fit.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`EPIC-14` carries the reading `ADR-0121` §3 asks for: `scrollHeight ≤ clientHeight` **and**
`scrollWidth ≤ clientWidth`, at 390 × 664, on the **running** client, at three named beats — written
down in `ADR-0088` §3's form, with the row for the human's iPhone left for the human. Nothing is
repaired here. This is the reproduction the repair is measured against, and
[`ADR-0121`](../../docs/adr/ADR-0121-the-table-reflows-never-scales-and-the-shape-is-measured-on-the-device.md)
§4 says nothing else establishes the shape it is aimed at.

## Why this is a ticket and not a paragraph

`ADR-0088` §3: *"A run that is not written down did not happen."* The receipt is *"one line appended
to the epic's Definition of done … written by the person who ran it."* `EPIC-14`'s Definition of
done already carries the unchecked line this closes on — *"The duel table is walked on a **real
iPhone** over the local network and, at every beat, shows every control and every number without a
scroll"* — and has nowhere to put the numbers that answer it.

`ADR-0089` §2 bars a browser from any `verify:` block, so **the numbers below are acceptance criteria
with a named runner, never gates.** The `verify:` block checks only that the receipt has the shape a
later reader can use.

## Files

| File | Action |
| --- | --- |
| `tasks/epics/EPIC-14-the-name-the-showdown-and-the-fit.md` | modify |

Read [`ADR-0121`](../../docs/adr/ADR-0121-the-table-reflows-never-scales-and-the-shape-is-measured-on-the-device.md)
§§2–4 for what is measured and against what, and
[`ADR-0088`](../../docs/adr/ADR-0088-the-two-browser-proof-is-a-written-hand-check.md) §§2–3 for the
receipt's form. **Nothing else is opened, and no file outside `tasks/` is edited.** In particular no
file under `web-client/`, `design/` or `docs/` changes: this ticket repairs nothing.

## Scope

### 1. Take the reading

Bring the stack up and drive it. `scripts/qa/stack.sh` owns the database and the browsers; the JVM
server and the Vite dev server are background tasks you start yourself (the script says so in its
own head comment, and says why). The driver is `node scripts/qa/drive.mjs <port> <verb>`; use
**two** profiles, `A` on 9232 and `B` on 9233, because two tabs of one profile are one player
(`ADR-0018`).

**Each browser's first act is `size 390 664`, before `open`** — the audit's own rule
(`.claude/agents/audit.md`, *Two shapes, one live tab*). Never claim a device: no `mobile: true`, no
`deviceScaleFactor` above 0, no fabricated `screen`. A finding built on one of those is a harness
defect (`ADR-0089` §4), not a product defect.

The three beats:

| beat | how to reach it | why |
| --- | --- | --- |
| **B1** | `A size 390 664`, `A open` | the front door — the control that rules out a broken instrument |
| **B2** | `A click "Play duel"`, `A link`, `B size 390 664`, `B open <link>`, `B wait "Blinds"` | the **first** decision of hand 1. No seat has acted, so no plate carries a last-act mark |
| **B3** | read `A text` and `B text`, find the tab showing `Your turn`, and on that tab click the button whose label starts with `Raise to` | the raiser's plate now carries a mark **with a figure**, the dealer `D`, `Timebank 3:00`, the pile and the stack — all at once |

At **each** beat, run this on **both** tabs and record what it prints:

```js
(() => { const d = document.documentElement; const plates = [...document.querySelectorAll(".border-l-2")].map((p) => { const k = p.children[p.children.length - 1].getBoundingClientRect(); return { text: p.innerText.replace(/\s+/g, " ").trim(), scrollW: p.scrollWidth, clientW: p.clientWidth, overhang: +(k.right - d.clientWidth).toFixed(2) }; }); return JSON.stringify({ h: [d.scrollHeight, d.clientHeight], w: [d.scrollWidth, d.clientWidth], plates }); })()
```

`.border-l-2` is `SeatPlate.tsx`'s own root class and is on no other element in the client.
`overhang` is how far past the screen edge the plate's **last** child — the stack figure — is
painted; a positive number is `R2` and `R3` `not met`.

### 2. Write the receipt

Append to `EPIC-14`'s **Definition of done**, immediately after the last `- [ ]` line, a block in
exactly this shape. The `verify:` gate reads it, so the table's cells must carry both axis pairs:

```markdown
### The 390 × 664 reading

`ADR-0121` §3's two numbers, taken under §2 at the shape §4 fixes. `ADR-0089` §2 keeps this out of
every `verify:` block, so it is recorded rather than gated.

- **Taken:** YYYY-MM-DD by TASK-140501, on the running stack, two Chrome profiles at `size 390 664`
- **Commit:** <the sha this was read at>
- **Runner:** `node scripts/qa/drive.mjs <port> size 390 664` then `… eval '<the expression above>'`

| beat | `scrollHeight` / `clientHeight` | `scrollWidth` / `clientWidth` | worst plate overhang |
| --- | --- | --- | --- |
| B1 the front door | … / … | … / … | — |
| B2 hand 1, first decision, no mark on either plate | … / … | … / … | … |
| B3 the same hand after the first raise | … / … | … / … | … |

**The device reading is the human's** (`ADR-0121` §4, `ADR-0088`): no CI job and no agent owns an
iPhone. It is taken on the phone in the photograph, in Safari, with the URL bar and tab bar fully
expanded, and it may correct 390 × 664 **downward on either axis** and may never raise it.

| device · browser · beat | judged height | judged width | fits |
| --- | --- | --- | --- |
| _to be filled by the human_ | | | |
```

Both axis columns must be filled for all three beats and on both tabs — where the two tabs differ,
record the **worse** number and say which seat it came from.

## Out of scope

- **Any repair.** `TASK-140502` owns it. If the reading is worse than the prediction below, that
  does not license fixing anything here: record it and say so.
- **Correcting 390 × 664.** Only the device reading may, and only downward (`ADR-0121` §4). The
  three things that move with it — the round's shape in `.claude/skills/qa-cycle/SKILL.md`, the
  headless override, and `design/screens/duel-table.html`'s `.viewport.phone` — are **not** touched
  by this ticket under any reading.
- **Ticking the iPhone checkbox.** It stays `- [ ]`; a `verify:` gate counts the boxes at 6 and 9.
- **720 × 900.** `ADR-0121` §2 measures the smallest viewport the browser presents. The laptop shape
  is not re-read here.
- **The result screen.** `STORY-1405`'s *Out of scope* explains: it is `STORY-1408`'s.

## Tests

**None.** This ticket adds no code and no test. Its `verify:` block checks the receipt's shape — that
the block exists, names a commit, and carries three rows each holding both axis pairs — and that the
epic's checklist is otherwise untouched. What the numbers *are* is the acceptance criteria below,
under `ADR-0089` §2.

## Acceptance criteria

### Gated

- [ ] `EPIC-14` carries a `### The 390 × 664 reading` block naming a commit and holding at least
      three table rows of the form `| … | N / N | N / N | … |`
- [ ] The epic still has exactly **6** `- [x]` lines and **9** `- [ ]` lines, and the `real iPhone`
      line is still one of the `- [ ]`
- [ ] `python3 .github/scripts/lint_tickets.py` exits 0
- [ ] The diff touches exactly one file

### Measured on the running stack, and pasted into the PR body

**Who runs it:** the implementer, before opening the PR, on the running stack.

- [ ] **B1** and **B2** read `scrollHeight ≤ clientHeight` **and** `scrollWidth ≤ clientWidth` on
      both tabs, with every plate's `overhang` negative. Predicted **664 / 664** and **390 / 390**.
      A failure at B2 is still a true reading — record it — but it means the cause is wider than
      `STORY-1405` measured, and `TASK-140502` must be re-sized before it starts
- [ ] **B3** reads `scrollWidth > clientWidth` on the raiser's tab. **This is the reproduction.**
      Predicted `scrollWidth` between **415 and 450** against 390, with the raiser's plate's
      `overhang` between **+25 and +55**. For calibration, `STORY-1405`'s *Design notes* measured
      `+21.36` for the narrowest reproducing row and `+53.19` for the widest, on the real component
      against the built stylesheet
- [ ] If B3 reads `scrollWidth ≤ clientWidth` — the defect did **not** reproduce on the running
      stack — **stop**, paste the reading, and say so in the PR body. Do not go looking for a
      different thing to measure and do not change any file but the epic (`ADR-0103` §3's stop rule)
- [ ] `scrollHeight ≤ clientHeight` at all three beats, or the height axis is a second finding and
      the PR body says which beat and by how much
- [ ] The PR body names where every `size` was issued. Nothing catches a resize you forgot
      (`ADR-0097` §Consequences), and a reading taken at the wrong shape is worse than no reading
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.

The **device** row stays empty at merge and is filled by the human whenever they next hold the
phone (`ADR-0121` §4, `ADR-0088` §3). This ticket does not wait for it, and `EPIC-14`'s own
Definition of done is what closes on it.
