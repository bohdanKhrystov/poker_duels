---
schema: 2
id: TASK-120401
title: Write the EPIC-04 identity suite into docs/test-plan.md
type: task
status: ready
parent: STORY-1204
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [process, qa, meta]
depends_on: []
verify:
  - python3 .github/scripts/lint_tickets.py
  - awk '/^## /{s=0} /^## EPIC-04 —/{s=1} s && index($0,"**Provisional** — authored") && index($0,"from merged sources, not yet run (`ADR-0090` §5).") {f=1} END{exit f?0:1}' docs/test-plan.md
  - awk -F'|' '/^## /{s=0} /^## EPIC-04 —/{s=1} s && $2 ~ /`04-0[0-9]`/ { n++; if (NF != 7) bad++; for (i=3; i<=6; i++) { c=$i; gsub(/[ \t]/,"",c); if (c=="" || c=="TBD") bad++ } } END{ exit (n==5 && bad==0)?0:1 }' docs/test-plan.md
  - awk '/^## /{s=0} /^## EPIC-04 —/{s=1} s && index($0,"−1"){f=1} END{exit f?0:1}' docs/test-plan.md
  - awk -F'|' '/^### /{s=0} /^### Not yet written/{s=1} s && $2 ~ /EPIC-04/ { seen=1; if ($3 ~ /not written/) bad=1; if ($3 !~ /suite/) bad=1 } END{ exit (seen && !bad)?0:1 }' docs/test-plan.md
  - awk -F'|' '$2 ~ /`(SMK|CORE)-[0-9]+`/ { n++; if (NF != 6) bad++ } END{ exit (n==26 && bad==0)?0:1 }' docs/test-plan.md
  - awk '/^## /{s=0} /^## What this catalogue does not cover/{s=1} s && index($0,"`EPIC-04`"){f=1} END{exit f?0:1}' docs/test-plan.md
---

## Goal

`docs/test-plan.md` carries an `EPIC-04` suite of five cases, each row citing the merged source of
its expectation, under `ADR-0090` §5's `Provisional` line — and the seven promises no case reaches
are named in §*What this catalogue does not cover* rather than silently dropped.

## Every string below is transcribed, never invented

`ADR-0090` §4: *"a case whose expectation has no merged source is not written."* Every literal in
this ticket was read out of a merged module or a merged ADR on 2026-08-29 at commit `7357730d`, and
the `source` cell names where. **Do not paraphrase a quoted string, and do not add a case.** If
something in the table below looks wrong, that is a finding for the report, not a licence to fix it
in the diff — a corrected expectation with no source is the exact failure §4 exists to prevent.

## Files

| File | Action |
| --- | --- |
| `docs/test-plan.md` | modify |
| `tasks/epics/EPIC-04-identity-and-profiles.md` | read — §Definition of done only |
| `docs/adr/ADR-0090-a-skill-may-write-the-catalogue-or-run-it-never-both.md` | read — §§4, 5 |

## Scope

### 1. A new `## EPIC-04 —` section, placed immediately before `## Per-epic suites`

Preceded by a `---` rule, matching how `## CORE` and `## Per-epic suites` are separated today.
Write it exactly like this — the `Provisional` line is copied byte-for-byte from
`docs/test-plan.md` §*Per-epic suites*, em dash, backticks, `§5` and full stop included:

```markdown
## EPIC-04 — Identity and profiles

> **Provisional** — authored 2026-08-29 from merged sources, not yet run (`ADR-0090` §5).

Sources: the epic's Definition of done, and the ADRs its Open decisions table names.

Five of its twelve promises reach a browser; the other seven are under §*What this catalogue does
not cover*. The cases run in order and each leaves state the next uses. **W** is the browser whose
result screen named it the winner of `04-02`'s duel and **L** is the other — decided by reading the
screens, never fixed to a port.

**Read the strip after a reload, never on the first load of a fresh profile.** `pd.deviceId` is
written when the socket's `Welcome` lands (`web-client/src/store/boot.ts`) while the profile read
runs once at mount (`web-client/src/profile/profile-provider.tsx`), and HTTP refuses a device the
socket has not yet minted. A fresh profile's first render is therefore `No profile yet.`, and the
strip is never re-read. A second `open` is deterministic. This is `SMK-03`'s mistake, avoided.

**Three `wait` targets would prove nothing.** `Your duels`, `Leaderboard` and `Account` label the
first screen's own doors as well as the screens behind them, so a wait on one is satisfied before
the swap. Wait on a string that exists only past the door.
```

### 2. The five rows, verbatim

```markdown
| id | do | expect | fails if | source |
| --- | --- | --- | --- | --- |
| `04-01` | fresh `A`: `A open`, `A open`, `A wait "Duel coins"`, `A click "Your duels"`, `A wait "Opponent name"` | the strip states a balance as `<n> Duel coins`, and the duels screen renders with `No duels yet.` — neither read refuses a device that never made an account | the strip still reads `No profile yet.` after the reload, or the screen reads `Your duels did not load. Reload the page to try again.` — `GET /api/me` or `GET /api/me/duels` answered `401` to an account-less device | `web-client/src/profile/ProfileStrip.tsx`; `web-client/src/history/history-text.ts` |
| `04-02` | play a duel to a winner (`CORE-12`'s sequence); on the loser **L**, read the balance, claim the profile with a handle and a password, reload, read it again — steps below the table | both readings are the same string, and it is `−1 Duel coins` for a browser whose only duel was a loss — U+2212, not a hyphen | the two differ, or the second reads `0 Duel coins` — the claim rewrote the balance, or clamped it (`ADR-0014`) | `ADR-0014`; `web-client/src/profile/profile-text.ts` (`coinBalanceText`) |
| `04-03` | on **L**: set a display name that is not L's handle, then try to sign in with that display name and L's own password — steps below the table | the sign-in is refused with `That handle and password do not match an account.` | it signs in, or the account screen afterwards reads `Your password signs in to this account.` — something resolved a player from a display name | `ADR-0031` §1; `web-client/src/account/account-text.ts` (`SIGN_IN_REFUSED`) |
| `04-04` | on **L**, sign in twice from the sign-in screen — once with L's own handle and a wrong password, once with a handle no account holds — capturing `L text` after each | both render `That handle and password do not match an account.`, and the two captures are identical | the two differ in any character, or either names a field, a handle or an account — the wire's indistinguishability was undone in words | `ADR-0027` §6; `web-client/src/account/account-text.ts` (`SIGN_IN_REFUSED`) |
| `04-05` | on **W**: set a display name, then claim the profile with a second handle and password. On **L**: sign in with W's handle and password — steps below the table. **Runs last** | L lands on the account screen reading `Your password signs in to this account.`, and behind *Back* it shows W's display name, W's balance and W's duel — on a browser whose `device` differs from W's | any of the three differs from what W's own screen shows, or L is left on the sign-in screen | `ADR-0083` §5; `web-client/src/profile/ProfileStrip.tsx` |
```

### 3. The verb sequences, in a note under the table

```markdown
**`04-02`, `04-03` and `04-05` in full.** A duel leaves both browsers in a room (`ADR-0072`), so
each begins `forget-room` then `open`. On the first screen the room-code box is input `0` and the
name box is input `1`; on the account screen the sign-up handle is `0` and its password is `1`; on
the sign-in screen the handle is `0` and the password is `1`.

- `04-02` — `L forget-room`, `L open`, `L text` (read `… Duel coins`), `L click "Account"`,
  `L wait "Give this profile a password"`, `L type 0 <handle>`, `L type 1 <password>`,
  `L click "Give this profile a password"`, `L wait "This profile now has a password."`,
  `L open`, `L text` (read it again).
- `04-03` — `L type 1 <a display name with a space in it, so it cannot be a handle>`,
  `L click "Set my name"`, `L click "Account"`, `L click "Sign in"`,
  `L wait "Forgot your password?"`, `L type 0 <that display name>`, `L type 1 <L's password>`,
  `L click "Sign in"`, `L wait "That handle and password do not match an account."`.
- `04-05` — the same two flows on **W** with a second handle and a second display name, then on
  **L**: `L click "Account"`, `L click "Sign in"`, `L wait "Forgot your password?"`,
  `L type 0 <W's handle>`, `L type 1 <W's password>`, `L click "Sign in"`,
  `L wait "Your password signs in to this account."`, `L click "Back"`, `L text`, and
  `L device` against `W device`.

**A handle is 3–32 of `[a-z0-9._-]` starting with `[a-z0-9]`, and a password is 8–128 characters**
(`web-client/src/account/account-text.ts`, `HANDLE_REFUSED` and `PASSWORD_REFUSED`). A display
name containing a space cannot be a handle, which is what makes `04-03` unambiguous.

**`04-05` runs last because it ends L's own identity**: signing in binds that browser to W's
player, so every case after it would read W's profile.
```

### 4. §*Not yet written*'s `EPIC-04` row

Replace its Status cell. Keep the row — `EPIC-03`'s row is the precedent for a table that keeps a
row and changes what it says:

```markdown
| `EPIC-04` identity and profiles | **the `EPIC-04` suite above is it** — authored 2026-08-29 from merged sources, provisional until its first round |
```

### 5. §*What this catalogue does not cover* gains one bullet

Added before the existing **A real network** bullet:

```markdown
- **Seven of `EPIC-04`'s twelve Definition-of-done promises**, each for a stated reason rather than
  by omission. *Every story is `done` or `dropped`*, *`V1` and `V2` are byte-unchanged*,
  *`poker-engine` declares no dependency outside the `ADR-0010` allowlist* and *`verifyProtocolTypes`
  still passes* are facts about the repository, answered by the board and by Gradle; no browser can
  see one. *No response body, log line or `ServerMessage` contains a password or a password hash* is
  promised **"asserted structurally rather than by inspection"** — the epic names its own method, and
  it is not a browser: `drive.mjs` reads `#root.innerText`, which never contains an input's value, so
  a DOM assertion for it would be green on any product. *History paging is total and disjoint* needs
  more finished duels than a round can play — `DEFAULT_DUEL_LIMIT` is 10, so a second page needs
  eleven — and its concurrent-insert half needs a duel to finish between two page reads, which two
  browsers cannot do while one of them is paging. *Checked by hand, once, and recorded* asks for a
  receipt rather than a repeatable case, and its recovery half is unreachable: a machine with no mail
  transport binds `NoRecoveryMailer` (`ADR-0031` §7), the verification and reset tokens are stored
  only as `BYTEA` hashes (`V8__recovery_email.sql`), and no mailed link ever arrives for a driver to
  follow. **The whole of recovery — `#/verify`, `#/reset`, and *Forgot your password?* past its
  acknowledgement — is outside this catalogue for that reason.**
```

## Out of scope

- **Every file except `docs/test-plan.md`**, besides this ticket's own `status` and its `BOARD.md`
  cell. A `## Files` row naming anything else is grounds to reject the diff on sight.
- **The `EPIC-05` suite.** `TASK-120402` writes it, into the same file, after this merges. Adding it
  here makes both tickets one and makes the second unreviewable.
- **Retrofitting `SMOKE` or `CORE` with a `source` column**, or touching one of their 26 rows.
  `ADR-0090` §4 refuses that churn by name, and gate 6 reddens on it.
- **`EPIC-06`'s row in §*Not yet written*.** It stays as it is.
- **Adding a sixth case, or a case for a promise the bullet in scope item 5 calls uncovered.** Both
  are how a catalogue acquires an invented expectation.
- **Running anything.** No stack, no browser, no `scripts/qa/` invocation, and no `verify:` command
  that waits on a QA case (`ADR-0089` §2b). Every gate here reads a committed file.

## Tests

No test class — the deliverable is one markdown section, so the gates are structural checks over
that document, the shape [`TASK-120201`](TASK-120201-smk-03-reads-a-device-id-from-a-profile-that-has-been-to-the-app.md)
and [`TASK-120301`](TASK-120301-the-qa-cases-skill-file.md) use. **Every row below was run on
2026-08-29 at commit `7357730d`**, against the tree as it stands and against a draft suite built to
satisfy them: six red, then six green, with six mutations each reddening exactly one gate.

| # | Gate | Proves | Today | With the suite |
| --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | the ticket, story, epic row and board rows agree | exits 0 — and must keep doing so once this ticket's status and board cell move | 0 |
| 2 | `awk` over the `Provisional` literal, **scoped to the `## EPIC-04 —` section** | `ADR-0090` §5's marker is on this suite, byte-for-byte | **exits 1** — no such section, and the two merged copies of the string are outside it | 0 |
| 3 | `awk -F'\|'` over the `04-0N` rows | there are **exactly five**, each **seven fields** (five columns), and no `do`, `expect`, `fails if` or `source` cell is empty or `TBD` | **exits 1** — zero rows | 0 |
| 4 | `awk` over the literal `−1`, scoped to the section | the minus sign is **U+2212**, bytes `e2 88 92`, not an ASCII hyphen | **exits 1** | 0 |
| 5 | `awk -F'\|'` over §*Not yet written* | the `EPIC-04` row exists, no longer says *not written*, and points at a suite | **exits 1** — it says *not written* today | 0 |
| 6 | `awk -F'\|'` over `SMK-`/`CORE-` rows | `SMOKE` and `CORE` keep **26 rows of four columns** — not retrofitted (`ADR-0090` §4) | exits 0 — a guard, not a progress gate | 0 |
| 7 | `awk` over §*What this catalogue does not cover* | the seven unreached promises are written down | **exits 1** | 0 |

**Every gate is `awk`, and that is deliberate.** The bare `grep` in an agent shell is a function
shimming `ugrep` and it disagrees with `/usr/bin/grep` about an anchor inside a mid-pattern
alternation and about `-v -q` over a piped multi-line stream. `awk` is one binary with one answer,
and every gate here reads a file directly rather than through a pipe — so no gate's exit code can
be swallowed by a pipeline.

### The mutations, and what each proves

Run against a draft satisfying all six, one at a time:

| Mutation | Gate that went red |
| --- | --- |
| the marker's em dash changed to a hyphen | 2 |
| one row written with four columns | 3 |
| one `source` cell set to `TBD` | 3 |
| one `expect` cell emptied | 3 |
| four rows instead of five | 3 |
| `−1` rewritten with an ASCII hyphen | 4 |

Gate 6 was also run against a copy in which the `EPIC-04` suite is correct but a `CORE` row has
gained a fifth column: it goes red, which is what makes it a guard rather than decoration.

### What gate 3 can and cannot see

Its row count is, on its own, the tautology this repository keeps rediscovering — a gate counting
rows in a table the same diff writes proves only that rows were written. What makes it worth having
is the other three checks in the same pass: **a wrong suite gets the column count wrong** (four
columns is the shape every other table in this file has, and the shape a copy-paste produces), and
**a case with nothing to say fills its `source` or `fails if` with nothing or with `TBD`**, which is
exactly `ADR-0090` §4's failure wearing a table row.

It **cannot** see whether a `source` cell is true. A row citing `ADR-0099` §12 passes. That half is
the reviewer's, and the review instruction is: **read each row's `source` against the file it names,
and read the five rows against `EPIC-04`'s Definition of done.** The suite claims five of twelve
promises; a reviewer who cannot map each row to one of them has found a defect.

### What gate 4 can and cannot see

It can see that the U+2212 glyph is in the section — `printf '%s' '−1' | od -An -tx1` prints
`e2 88 92 31`, and the ASCII form prints `2d 31`. It **cannot** see that the surrounding sentence is
right, and it cannot see a *second* balance written with a hyphen elsewhere in the section. One
correct occurrence satisfies it. That is accepted: the gate exists to stop the one substitution a
keyboard makes by default, not to audit prose.

## Acceptance criteria

- [ ] A `## EPIC-04 — Identity and profiles` section exists immediately before `## Per-epic suites`,
      carrying the `Provisional` line byte-for-byte (gate 2).
- [ ] It holds exactly five rows, `04-01` … `04-05`, each five columns wide, with no empty and no
      `TBD` cell in `do`, `expect`, `fails if` or `source` (gate 3).
- [ ] `04-02`'s expectation writes the balance as `−1` with U+2212 (gate 4).
- [ ] The verb-sequence note under the table gives the full steps for `04-02`, `04-03` and `04-05`,
      and says `04-05` runs last because signing in ends L's own identity.
- [ ] §*Not yet written*'s `EPIC-04` row no longer reads *not written* and points at the suite
      (gate 5).
- [ ] §*What this catalogue does not cover* names `EPIC-04` and all seven unreached promises with a
      reason for each (gate 7).
- [ ] `SMOKE` and `CORE` are byte-unchanged — 26 rows, four columns (gate 6).
- [ ] The diff touches exactly one file besides this ticket's own status and its `BOARD.md` cell.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
