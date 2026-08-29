---
schema: 2
id: TASK-120201
title: SMK-03 reads a device id from a profile that has been to the app
type: task
status: ready
parent: STORY-1202
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [process, qa, harness]
depends_on: []
verify:
  - awk -F'|' '/^\| `SMK-03` \|/ { if ($3 ~ /open.*device/) found=1 } END { exit !found }' docs/test-plan.md
  - awk -F'|' '/^\| `SMK-03` \|/ { if ($3 ~ /device/) found=1 } END { exit !found }' docs/test-plan.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`SMK-03`'s `do` column names the navigation that has to happen before a device id can be read, so
the case is executable in the order it is written instead of only by a tester who works out the
ordering for themselves.

## This is a harness defect, and what that means here

Filed under `ADR-0089` §4 and `EPIC-12` §Termination rule 6. It is **excluded from `B(1)`**, and
**no production code may be changed by this ticket** — the fix is one document. A `## Files` table
naming anything outside `docs/` is grounds to reject the diff on sight.

## The defect

`SMK-03` reads:

```
| `SMK-03` | `A device` and `B device` | two non-empty ids, and they differ | they are equal, or empty — two tabs are one player (`ADR-0018`) |
```

Run in that order, B has not been opened yet — the suite's first `B open` is in `SMK-05`. And:

- `scripts/qa/stack.sh chrome-up` starts Chrome on `about:blank`;
- `drive.mjs`'s `attach()` looks for a page whose url contains `localhost:5173` and falls back to
  the first page there is, which on a fresh profile is that `about:blank`;
- `pd.deviceId` is written by the app, under the app's origin. A profile that has never loaded
  `http://localhost:5173/` has nothing to read.

So `B device` prints the empty string, and `SMK-03`'s own `fails if` column — *"they are equal, or
**empty**"* — makes the case red. Red for the harness, not for the product.

**Reproduction.** Round 1 (2026-08-29, commit `7f7b905f`) is the record: the tester reported that
it navigated B to the app root *before* reading the id, and that the case's written steps do not
mention it. The mechanism above is deterministic and was traced in `scripts/qa/stack.sh` and
`scripts/qa/drive.mjs`; it is not timing-dependent and does not need a run to confirm. A run is in
any case not available as evidence in a `verify:` block — `ADR-0089` §2b forbids any `verify:`
waiting on a QA case, which is why the gates below are static.

## Files

| File | Action |
| --- | --- |
| `docs/test-plan.md` | modify |

## Scope

- Rewrite `SMK-03`'s **`do`** cell so an `open` on each profile precedes the two `device` reads.
  The existing wording that makes it run is `` `A open`, `B open`, then `A device` and `B device` ``;
  any phrasing that names the navigation first is acceptable.
- Leave the `expect` and `fails if` cells **unchanged**. *"They are equal, or empty"* is still the
  right failure condition — the point of this ticket is that it stops firing for the wrong reason,
  not that it stops firing.
- Add **one** sentence to the SMOKE section's preamble (or to *How a case is written*) saying that
  a case reading `localStorage` must navigate the profile to the app first, because the id lives
  under the app's origin and a fresh profile sits on `about:blank`. One sentence, so the next case
  that reads storage does not rediscover this.

## Out of scope

- **Any file outside `docs/test-plan.md`.** Named again because this is the rule the ticket exists
  under, not a preference.
- **`scripts/qa/drive.mjs`.** Its `device` verb exits 0 while printing an empty id, which
  `STORY-1202` records and deliberately did not file. Do not fix it here; if it is ever worth
  fixing it is its own ticket.
- **Any other row of the catalogue.** `SMK-02`'s assertion strength was assessed in round 1 and
  judged not a defect, with reasons, in `STORY-1202`.
- **Adding a `verify:` that runs a QA case.** `ADR-0089` §2b forbids it.

## Tests

None — the deliverable is document text, so the gate is a structural check over that text, the
same shape `TASK-060122` uses. The two `awk` commands read `SMK-03`'s `do` cell (the third
pipe-delimited field of the row) and nothing else:

| Gate | Proves | Today |
| --- | --- | --- |
| `$3 ~ /open.*device/` | the `do` cell names an `open` **before** a `device` read | **exits 1** — the cell is `` `A device` and `B device` ``, which has no `open` |
| `$3 ~ /device/` | the `device` reads are still there | exits 0 — it must keep doing so |

The second gate is what stops the cheap fix: deleting the `device` reads would satisfy the first
gate and destroy the case. Both were run against `docs/test-plan.md` at commit `7f7b905f` and
against a patched copy, and they exit `1` then `0` and `0` then `0` respectively.

## Acceptance criteria

- [ ] `SMK-03`'s `do` cell contains an `open` ahead of the first `device`, and still contains the
      `device` reads.
- [ ] `SMK-03`'s `expect` and `fails if` cells are byte-identical to what they are now.
- [ ] `docs/test-plan.md` gained one sentence saying a storage read needs the app's origin.
- [ ] The diff touches exactly one file, and it is `docs/test-plan.md`.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
