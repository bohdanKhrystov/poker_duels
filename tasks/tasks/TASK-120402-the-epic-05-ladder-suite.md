---
schema: 2
id: TASK-120402
title: Write the EPIC-05 ladder suite into docs/test-plan.md
type: task
status: ready
parent: STORY-1204
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [process, qa, meta]
depends_on: [TASK-120401]
verify:
  - python3 .github/scripts/lint_tickets.py
  - awk '/^## /{s=0} /^## EPIC-05 —/{s=1} s && index($0,"**Provisional** — authored") && index($0,"from merged sources, not yet run (`ADR-0090` §5).") {f=1} END{exit f?0:1}' docs/test-plan.md
  - awk -F'|' '/^## /{s=0} /^## EPIC-05 —/{s=1} s && $2 ~ /`05-0[0-9]`/ { n++; if (NF != 7) bad++; for (i=3; i<=6; i++) { c=$i; gsub(/[ \t]/,"",c); if (c=="" || c=="TBD") bad++ } } END{ exit (n==5 && bad==0)?0:1 }' docs/test-plan.md
  - awk '/^## /{s=0} /^## EPIC-05 —/{s=1} s && index($0,"You have no place on this season"){a=1} s && index($0,"s leaderboard."){b=1} s && index($0,"You are rank "){c=1} s && index($0,"on −1 duel coins"){d=1} END{exit (a&&b&&c&&d)?0:1}' docs/test-plan.md
  - awk -F'|' '/^### /{s=0} /^### Not yet written/{s=1} s && $2 ~ /EPIC-05/ { seen=1; if ($3 ~ /not written/) bad=1; if ($3 !~ /suite/) bad=1 } END{ exit (seen && !bad)?0:1 }' docs/test-plan.md
  - awk -F'|' '/^## /{s=0} /^## EPIC-04 —/{s=1} s && $2 ~ /`04-0[0-9]`/ { n++; if (NF != 7) bad++ } $2 ~ /`(SMK|CORE)-[0-9]+`/ { c++; if (NF != 6) bad++ } END{ exit (n==5 && c==26 && bad==0)?0:1 }' docs/test-plan.md
  - awk '/^## /{s=0} /^## What this catalogue does not cover/{s=1} s && index($0,"`EPIC-05`"){f=1} END{exit f?0:1}' docs/test-plan.md
---

## Goal

`docs/test-plan.md` carries an `EPIC-05` suite of five cases, each row citing the merged source of
its expectation, under `ADR-0090` §5's `Provisional` line — and the four promises no case reaches
are named in §*What this catalogue does not cover*.

## Every string below is transcribed, never invented

`ADR-0090` §4. Every literal here was read out of a merged module or a merged ADR on 2026-08-29 at
commit `7357730d`, and the `source` cell names where. **Do not paraphrase a quoted string, and do
not add a case.**

## This ticket edits a file `TASK-120401` just wrote

It runs **after** that ticket merges, never beside it — one file, two tickets, no overlap. Gate 6
reddens if the `EPIC-04` suite loses a row or a column while this one is added, which is the whole
of what a sequenced pair needs.

## Files

| File | Action |
| --- | --- |
| `docs/test-plan.md` | modify |
| `tasks/epics/EPIC-05-ranking-duel-coins-and-leaderboard.md` | read — §Definition of done only |
| `docs/adr/ADR-0065-the-ladder-hands-a-player-their-own-row.md` | read — §§1, 4 |
| `docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md` | read — §§4, 6 |

## Scope

### 1. A new `## EPIC-05 —` section, immediately after the `EPIC-04` suite

Preceded by a `---` rule. The `Provisional` line is copied byte-for-byte:

```markdown
## EPIC-05 — Ranking, duel coins and leaderboard

> **Provisional** — authored 2026-08-29 from merged sources, not yet run (`ADR-0090` §5).

Sources: the epic's Definition of done, and the ADRs its Open decisions table names.

Five of its nine promises reach a browser; the other four are under §*What this catalogue does not
cover*.

**No case asserts an absolute rank.** The database persists between rounds, so every rank on the
ladder depends on every duel ever played on that machine. What is deterministic is that a **fresh**
profile's first finished duel moves its standing by exactly one (`ADR-0014`, `ADR-0061` §4), and
`ADR-0089` §3 already requires a fresh Chrome profile per round. A case pinning `rank 1` would be
red for the machine's history rather than for a defect.

**The season line is compared against the response, never merely read.** A client that worked the
season out from the browser's clock would print the right month on the day a round runs — so *"the
screen shows a month"* is an assertion that passes on the defect `ADR-0061` §6 forbids. `05-05`
reads `GET /api/standings` and compares.
```

### 2. The five rows, verbatim

```markdown
| id | do | expect | fails if | source |
| --- | --- | --- | --- | --- |
| `05-01` | fresh `A`: `A open`, `A open`, `A click "Leaderboard"`, `A wait "You have no place on this season's leaderboard."`, `A click "Back"`, `A wait "Create a duel room"` | a browser that never signed up opens the ladder from the first screen and leaves it again | the `Leaderboard` control is absent, the screen reads `The leaderboard did not load. Reload the page to try again.`, or *Back* does not return to `Create a duel room` | `web-client/src/ladder/ladder-text.ts` (`LADDER_FAILED`); `web-client/src/lobby/Lobby.tsx` |
| `05-02` | on that same ladder screen: `A absent "You are rank "` and read `A text` | the self line is exactly `You have no place on this season's leaderboard.` — no rank, and no `0` standing anywhere in it | the self line reads `You are rank …`, or a `0` stands where a standing would — a player who finished no duel was given a place | `ADR-0065` §4; `web-client/src/ladder/ladder-text.ts` (`selfLine`, `NO_PLACE_THIS_SEASON`) |
| `05-03` | read both self lines (both no-place); play a duel to a winner (`CORE-12`'s sequence); `forget-room`, reload and open the ladder on each; then the `duel_result` read below the table | **W**'s self line reads `on 1 duel coin.` and **L**'s reads `on −1 duel coins.` — U+2212, singular for one — and each player's season sum in `duel_result` is the number their own ladder printed | either standing moved by anything other than one, or a rendered standing disagrees with its `duel_result` sum — a coin was minted or destroyed between the row and the screen | `ADR-0014`; `ADR-0061` §4; `web-client/src/ladder/ladder-text.ts` (`selfLine`) |
| `05-04` | on **L**'s ladder, walk the pages with `Show more`, reading each page's row lines with `A eval`, until L's own standing appears | a row whose standing is `−1` is listed, and the standings down the walk never increase — the negative sits below every larger one, since `rank = 1 + the number standing strictly higher` | no `−1` row appears at all, or it reads `0`, or its minus is an ASCII hyphen, or a standing later in the walk is greater than an earlier one | `ADR-0061` §4; `ADR-0064` §1; `web-client/src/profile/profile-text.ts` (`coinBalanceText`) |
| `05-05` | on the ladder: `A eval "(async()=>(await (await fetch('/api/standings')).json()).season)()"`, then read the season line with `A text` | the screen prints the month and the year in English for exactly the season the response carried — `August 2026` for `2026-08` | the screen prints `2026-08`, prints no season line, or prints a month the response did not carry — the client worked the season out from the browser's clock (`ADR-0002`) | `ADR-0061` §6; `web-client/src/ladder/ladder-text.ts` (`seasonName`) |
```

### 3. `05-03`'s database read, in a note under the table

```markdown
**`05-03` reads the database, and only as a second witness.** `ADR-0089` §3 names the database in
what the harness may read, and this is `CORE-13`'s shape: the ladder's rendered number is still the
observation, the row is what it is checked against, and the case is red when they disagree. A case
whose *only* observation was a row would be a test of the server — which
`poker-server/.../e2e/` already covers against real Postgres — and a `PASS` over it would read as
the coverage claim `ADR-0089` §2c forbids. It writes nothing.

A season is a calendar month in UTC and a duel belongs to the season its **finish** falls in
(`ADR-0061` §§1, 2), so the read is bounded the same way:

    docker exec poker_duels-postgres-1 psql -U poker -d poker_duels -At -c \
      "SELECT p.device_id, SUM(dr.coin_delta)
         FROM player p
         JOIN duel_result dr ON dr.player_id = p.id
         JOIN duel d ON d.id = dr.duel_id
        WHERE p.device_id IN ('<A device>', '<B device>')
          AND d.finished_at >= date_trunc('month', now() AT TIME ZONE 'UTC') AT TIME ZONE 'UTC'
        GROUP BY p.device_id"

The device ids come from `A device` and `B device`; `player.device_id` is unique
(`V1__initial_schema.sql`), so each browser resolves to one row without the driver ever learning a
`player_id`.
```

### 4. §*Not yet written*'s `EPIC-05` row

```markdown
| `EPIC-05` ranking and leaderboard | **the `EPIC-05` suite above is it** — authored 2026-08-29 from merged sources, provisional until its first round |
```

### 5. §*What this catalogue does not cover* gains one bullet

Beside the one `TASK-120401` added:

```markdown
- **Four of `EPIC-05`'s nine Definition-of-done promises**, and one clause of a fifth. *Every story
  is `done` or `dropped`*, *`docs/protocol.md` contracts every endpoint this epic adds* and
  *`poker-engine` is untouched by every commit in the epic* are facts about the repository and the
  documents; no browser can see one. *`ADR-0012`'s gate is discharged in writing* was discharged by
  [`ADR-0063`](adr/ADR-0063-nothing-gates-a-place-and-the-farm-is-accepted-until-the-ladder-is-public.md)
  as an accepted risk with a named expiry, which is a written record rather than a behaviour. And
  within the self-line promise, ***tied with a hundred others*** is the scenario `ADR-0065` §1 was
  written for and the one a round cannot build: `05-02` tests that the self line exists and what it
  says, never that it survives a tie.
```

## Out of scope

- **Every file except `docs/test-plan.md`**, besides this ticket's own `status` and its `BOARD.md`
  cell.
- **Any edit to the `EPIC-04` suite, `SMOKE` or `CORE`.** Gate 6 reddens on either. `ADR-0090` §4
  refuses the retrofit by name.
- **A sixth case**, or a case for a promise the bullet in scope item 5 calls uncovered.
- **A case asserting an absolute rank**, or one asserting that the ladder is empty. Both are false
  on a machine that has run a round before.
- **Any `psql` that writes**, and any `UPDATE`, `INSERT` or `DELETE` anywhere in this diff.
  `ADR-0089` §3 licenses reads only, and a seeded row is a client asserting a game fact
  (`ADR-0002`).
- **Running anything.** No stack, no browser, and no `verify:` command that waits on a QA case
  (`ADR-0089` §2b).

## Tests

No test class — one markdown section, so the gates are structural checks over the document.
**Every row below was run on 2026-08-29 at commit `7357730d`**, against the tree as it stands and
against a draft suite built to satisfy them: seven red, then seven green.

| # | Gate | Proves | Today | With the suite |
| --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | the ticket, story, epic row and board rows agree | exits 0 — and must keep doing so once this ticket's status and board cell move | 0 |
| 2 | `awk` over the `Provisional` literal, **scoped to `## EPIC-05 —`** | `ADR-0090` §5's marker is on **this** suite — the copies in §*Per-epic suites* and in the `EPIC-04` suite are outside the scope and do not satisfy it | **exits 1** | 0 |
| 3 | `awk -F'\|'` over the `05-0N` rows | exactly **five** rows, each **seven fields**, and no empty or `TBD` `do`, `expect`, `fails if` or `source` cell | **exits 1** | 0 |
| 4 | `awk` over four ladder literals | the self line's two sentences are transcribed rather than paraphrased — `You have no place on this season`, `s leaderboard.`, `You are rank `, and `on −1 duel coins` with **U+2212** | **exits 1** | 0 |
| 5 | `awk -F'\|'` over §*Not yet written* | the `EPIC-05` row exists, no longer says *not written*, and points at a suite | **exits 1** | 0 |
| 6 | `awk -F'\|'` over `04-0N` and `SMK-`/`CORE-` rows | this diff **left `TASK-120401`'s suite alone** — five five-column rows — and left `SMOKE` and `CORE` at 26 four-column rows | **exits 1** today, because the `EPIC-04` suite does not exist yet; it is this ticket's dependency, made mechanical | 0 |
| 7 | `awk` over §*What this catalogue does not cover* | the four unreached promises are written down | **exits 1** | 0 |

**Gate 4 is split around the apostrophe on purpose.** `You have no place on this season's
leaderboard.` carries a `'`, and embedding one inside a single-quoted `awk` program needs the
`'"'"'` idiom — a string every later editor gets wrong once. Matching the two halves either side of
it pins the same sentence with no quoting trap, and a paraphrase still reddens the gate: the
mutation that rewrote the two sentences as *"no place"* and *"a rank"* was run, and gate 4 exits 1.

**Every gate is `awk` and reads a file directly.** The bare `grep` in an agent shell is a function
shimming `ugrep`, and it disagrees with `/usr/bin/grep` about an anchor inside a mid-pattern
alternation and about `-v -q` over a piped stream; and a pipe would make `$?` the pipe's rather than
the gate's. Neither hazard is present here.

### The mutations, and what each proves

| Mutation | Gate that went red |
| --- | --- |
| the marker's em dash changed to a hyphen | 2 |
| four columns on one row, or a `TBD` source, or an empty `expect`, or four rows | 3 |
| the two self-line sentences paraphrased | 4 |
| the `EPIC-04` suite cut to four rows while this suite is correct | 6 |

Gate 6's mutation is the one worth reading twice: it is the failure a two-ticket, one-file story
actually has, and it is the reason this ticket carries a gate about somebody else's rows.

### What these gates cannot see

They cannot see that a `source` cell is true, that the ladder screen exists, that `Show more` is
reachable, or that `selfLine`'s sentence has not moved since it was read. `ADR-0090` §5 says so in
its own words, which is why the suite carries the `Provisional` line: **the first round is expected
to correct cases here, those corrections are harness tickets against `EPIC-12`, and no production
code may be changed to satisfy one of these rows until it has reproduced by hand** (`ADR-0089` §4).

The review instruction is the same one `TASK-120401` carries: **read each row's `source` against the
file it names, and read the five rows against `EPIC-05`'s Definition of done.** Five of nine
promises are claimed; a reviewer who cannot map each row to one of them has found a defect.

## Acceptance criteria

- [ ] A `## EPIC-05 — Ranking, duel coins and leaderboard` section exists after the `EPIC-04` suite,
      carrying the `Provisional` line byte-for-byte (gate 2).
- [ ] It holds exactly five rows, `05-01` … `05-05`, each five columns wide, with no empty and no
      `TBD` cell in `do`, `expect`, `fails if` or `source` (gate 3).
- [ ] `05-02`'s and `05-03`'s expectations quote `You have no place on this season's leaderboard.`,
      `You are rank `, and `on −1 duel coins` with U+2212 (gate 4).
- [ ] The note under the table carries `05-03`'s read, states that it writes nothing, and says why
      the rendered number is the observation and the row the second witness.
- [ ] §*Not yet written*'s `EPIC-05` row no longer reads *not written* and points at the suite
      (gate 5).
- [ ] §*What this catalogue does not cover* names `EPIC-05`, its four unreached promises, and the
      tie clause `05-02` does not reach (gate 7).
- [ ] `TASK-120401`'s `EPIC-04` suite is byte-unchanged, and `SMOKE` and `CORE` still have 26
      four-column rows (gate 6).
- [ ] The diff touches exactly one file besides this ticket's own status and its `BOARD.md` cell.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
