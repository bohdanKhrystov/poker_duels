---
schema: 2
id: TASK-120603
title: 05-02's standings read carries the identity the app sends
type: task
status: backlog
parent: STORY-1206
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [process, qa, harness]
depends_on: [TASK-120602]
verify:
  - awk -F'|' '/^\| `05-02` \|/ { if ($3 !~ /X-Device-Id/) bad=1 } END { exit bad }' docs/test-plan.md
  - awk -F'|' '/^\| `05-02` \|/ { if ($4 !~ /self line/) bad=1 } END { exit bad }' docs/test-plan.md
  - grep -qF '`05-02`' docs/test-plan.md && grep -qF '`05-05`' docs/test-plan.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`05-02` compares the ladder's self line against a response that was fetched **as this player**, so
running the case exactly as written stops manufacturing a failure the product did not cause.

## This is a harness defect, and what that means here

Filed under `ADR-0089` §4 and `EPIC-12` §Termination rule 6. It is **excluded from `B(2)`**, and
**no production code may be changed by this ticket** — the fix is one document. A `## Files` table
naming anything outside `docs/` is grounds to reject the diff on sight.

## The defect

Round 2 of `/qa-cycle regression`, 2026-08-29, commit `c7b35f4b`. The tester **nearly filed this as
a `05-02` product failure** and caught it before writing it up. That is the second round running in
which a case's own recipe produced a phantom, and it is the reason this is a ticket rather than a
note.

`05-02`'s `do` cell reads:

> on that same ladder screen: `A eval` `GET /api/standings` (`05-05`'s call) to learn whether this
> device already has a place, then read the self line with `A text`

`05-05`'s call is `fetch('/api/standings')` with no headers. The app's own request carries
`X-Device-Id`, and `Authorization: Bearer <pd.sessionToken>` when the browser holds a session. The
server resolves `self` from those, so **the headerless call answers `"self": null` for a player who
plainly has a place** — and the case's `fails if` then fires on *"a rank shown where the response
has no entry"*.

Measured this round on the live stack, from the browser, one profile, one variable changed:

| call | `self` |
| --- | --- |
| `fetch('/api/standings')` | `null` |
| `fetch('/api/standings', {headers:{'X-Device-Id': localStorage['pd.deviceId']}})` | `{"playerId":"84e8e12a-…","rank":4,"coins":0}` |

The ladder screen for that same browser read `You are rank 4 this season, on 0 duel coins.` So the
case as written compares a true screen against an anonymous response and calls the screen wrong.

**`05-05` is unaffected and is not changed here.** It reads `season`, which is not identity-scoped,
so its headerless call answers correctly. What is wrong is `05-02` **borrowing** it for a read that
is identity-scoped.

## Files

| File | Action |
| --- | --- |
| `docs/test-plan.md` | modify |

## Scope

- **Rewrite `05-02`'s `do` cell so the read carries the identity the app carries**: `X-Device-Id`
  from `pd.deviceId`, and `Authorization: Bearer` from `pd.sessionToken` when one is held. Both are
  reads of browser storage, which `ADR-0089` §3 permits; nothing is written.
- **Stop citing `05-05`'s call as the recipe.** Cite `05-05` for the season read if it helps, but
  `05-02` must state its own call, or the next person copies the anonymous one again.
- **Say why, in one sentence in the `EPIC-05` preamble**: a read that is identity-scoped is
  anonymous unless the case sends what the app sends, and an anonymous read of `/api/standings`
  answers `self: null` for everyone. One sentence, so the next authored suite does not rediscover it.
- Keep `05-02`'s subject and its `source` column exactly as they are — the case is about the self
  line agreeing with the response in **both** directions, and no `source` may be weakened to make a
  rewrite easier (`ADR-0090` §4).

## Out of scope

- **Any file outside `docs/test-plan.md`.**
- **`05-05`.** Its call is correct for what it reads. It is named in the gates only so a rewrite
  cannot delete it.
- **A new `drive.mjs` verb for an authenticated fetch.** It would be a reasonable convenience and it
  is not this ticket; `eval` already reads storage and issues the call, and adding a verb touches
  `scripts/qa/` while this ticket touches one document. Not yet ticketed.
- **The other rows' calls.** Only `05-02`'s is identity-scoped and headerless.

## Tests

None — the deliverable is document text, so the gates are structural checks over one row of it.

| Gate | Proves | Today |
| --- | --- | --- |
| `05-02` `$3 ~ /X-Device-Id/` | the `do` cell names the header the app sends | **exits 1** — the cell names no header |
| `05-02` `$4 ~ /self line/` | the `expect` still compares the self line, rather than being rewritten around the problem | exits 0 — it must keep doing so |
| `05-02` and `05-05` still present | neither row was deleted to satisfy the gates | exits 0 — it must keep doing so |

All three were run against `docs/test-plan.md` at commit `c7b35f4b`; the first exits `1`, the other
two exit `0`.

**What these gates cannot prove**, said plainly rather than implied: that the rewritten sentence is
*correct*. A structural check over prose can see that a header is named and not that the recipe
works — the proof of that is the next round running the case, and `ADR-0089` §2b forbids a `verify:`
block from waiting on a QA case.

## Acceptance criteria

- [ ] `05-02`'s `do` cell states a `/api/standings` read carrying `X-Device-Id`, and
      `Authorization: Bearer` when a session token is held.
- [ ] `05-02`'s `do` cell no longer instructs the reader to reuse `05-05`'s call.
- [ ] The `EPIC-05` preamble gained the sentence about identity-scoped reads.
- [ ] `05-02` keeps its `expect`, its `fails if` and its `source` column unweakened.
- [ ] The diff touches exactly one file, and it is `docs/test-plan.md`.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
