---
schema: 2
id: TASK-131008
title: P6a and P6b — a mailed link opened while a room is held
type: task
status: ready
parent: STORY-1310
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [qa, refresh, manual-verify]
depends_on: [TASK-131007]
verify:
  - awk '/^\| `P6a`/ { if (index($0, "NOT-YET-DRIVEN")) bad = 1; else ok = 1 } END { exit (bad || !ok) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '/^\| `P6b`/ { if (index($0, "NOT-YET-DRIVEN")) bad = 1; else ok = 1 } END { exit (bad || !ok) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '{ n += gsub(/NOT-YET-DRIVEN/, "&") } END { exit (n > 0) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '/^\| `P[1-6]/ { n++ } END { exit (n != 7) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1310*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A mailed verification address is opened on a browser that already holds a room — once **waiting**,
once **playing** — and `STORY-1310`'s `P6a` and `P6b` rows say whether the mailed screen mounted, and
therefore whether a real token would have been spent.

## The half of this path that cannot be driven, and why that is a result

`ADR-0112` §5's hard requirement is that *"a mailed link refused mid-duel **must not spend its
token** — the same mail must work after the duel."* **The second clause cannot be observed on this
stack, and the reason is merged in three places:**

- a machine with no mail transport binds `NoRecoveryMailer` (`ADR-0031` §7), whose two members are
  empty bodies — **no mailed link ever arrives**;
- the verification and reset tokens are stored only as `BYTEA` **hashes** (`V8__recovery_email.sql`),
  so no real token can be read back out of the database either;
- and minting one by inserting a row would be `ADR-0089` §3's forbidden write — *"no seeded row to
  reach a screen the product would not otherwise have shown"* — after which the case would prove a
  fiction.

`docs/test-plan.md` already states the same conclusion for the whole of recovery: *"no mailed link
ever arrives for a driver to follow."* **So `P6a` and `P6b` record that half as undrivable, with that
reason, and the story's third acceptance criterion is met by saying so** — the story licenses exactly
this: *"A path that cannot be driven says so and says why — that is a result, not a gap."*

## The half that can be driven, and it is the half that matters

Whether the token survives is decided by whether the client **asks** at all.
`web-client/src/account/VerifyScreen.tsx` submits its token in a **mount effect**, guarded by a ref —
so the screen rendering, even for one paint, is the token being spent. `ADR-0114` §5 is built on
exactly that: *"a booting tab reads *no room* for the whole rejoin round trip"*, and `hold` exists so
the screen does not mount before the frames arrive.

**A fabricated token makes that observable without inventing anything.** A token no account holds
comes back dead, and `VerifyScreen` renders `recovery-text.ts`'s `VERIFY_LINK_DEAD` —
`"That link has expired or has already been used. Ask for a new one from the account screen."` Seeing
that sentence is proof the mount effect ran and the call completed; seeing `VERIFY_HEADING`
(`"Finish verifying an address"`) alone is proof it mounted. Seeing neither, ever, is proof nothing
was submitted. **A real token would have been spent in exactly the cases where the fabricated one
produces those sentences**, and the row says it in those terms rather than claiming more.

`ADR-0089` §5: the module owning both literals is `web-client/src/account/recovery-text.ts`, and the
row cites it.

## A mailed link is a page load, not a hash write

`Lobby.tsx` reads the token **once**, with `useState(() => tokenFromHash(window.location.hash))`, at
first mount. So assigning `location.hash` in a tab that is already running captures **no token** and
measures nothing. The act being reproduced is a player clicking a link in their mail client, which is
a fresh document: `node scripts/qa/drive.mjs <port> open "http://localhost:5173/#/verify/<token>"`.
`screen.ts`'s `tokenFromHash` takes the fragment's **second** path segment, so the address is
`#/verify/<token>` and not a query string.

## The stack

As `TASK-131003` sets it out, and both layouts matter here more than anywhere: bare, and
`delayed 300ms` with Vite on `5273` and `node scripts/qa/delay.mjs 5173 5273 300 6173` on `5173`.
The delayed run is what widens the boot race the mount effect sits inside. Fresh Chrome profiles from
`mktemp -d` every time (`ADR-0089` §3).

## Files

| File | Action |
| --- | --- |
| `tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md` | modify |

Read: `docs/adr/ADR-0112-only-a-running-duel-refuses-another-screen.md` §5,
`web-client/src/account/VerifyScreen.tsx`, `web-client/src/account/recovery-text.ts`, and
`scripts/qa/drive.mjs`. No source is changed.

## Scope

- **`P6a` — the room waiting.** One fresh profile creates a room and stops there, so the tab holds a
  `WAITING` room and `pd.roomCode` is set. Confirm the memory with
  `X eval "localStorage['pd.roomCode']"` before going further — a drive against a tab that holds no
  room is measuring the ordinary mailed-link path and proves nothing about this one. Then
  `X open "http://localhost:5173/#/verify/deadbeefdeadbeef"`, keeping the first paint verbatim;
  `X record` immediately; `X frames`; `X text`; `X eval "location.hash"`.
- **`P6b` — the room playing.** A second profile joins by link so a duel is running, then the same
  sequence on a browser whose room is `PLAYING`.
- **Both at both layouts.** Four readings in all, and the two rows name which layout each came from.
- **Write both rows.** Each says: whether the mailed screen appeared at all; whether
  `VERIFY_LINK_DEAD` appeared, which is the call completing; what stood on screen afterwards; what
  `location.hash` read afterwards; that the *token survives* half is undrivable and why; the layout;
  and the short commit.

**What each outcome means, so the rows are not written as a guess.** The screen never appears →
nothing is submitted and a real token survives on this path today. The heading appears and is then
replaced → the effect ran, and a real token would have been spent and the screen taken away, which is
`ADR-0112` §5's silent permanent failure and `ADR-0114` §5's reason for `hold`. The screen appears
and **stays** over a held room → a third thing, and a finding of its own.

## Out of scope

- **`#/reset`.** `ADR-0114` §5 treats the two mailed screens alike and `ResetScreen` spends nothing on
  mount, so the token question does not arise there. One screen, driven twice.
- **Requesting a real verification mail, configuring a transport, or reading the token from anywhere.**
  Undrivable is the finding; making it drivable is a change to the product's wiring and is nobody's
  ticket here.
- **Seeding a row, patching `fetch`, or dispatching into the store** to reach the screen or count the
  call. `ADR-0089` §3 forbids all three, and `ADR-0114` §7 already fixes a `verifyEmail` **call
  count** as a jsdom proof for `STORY-1311` — that is where a counted assertion belongs, in a test,
  not in a drive.
- **Any repair.** `hold` is `ADR-0114` §5's mechanism and lands in `STORY-1311`.
- **`/qa-cycle`, `docs/test-plan.md`, `A(N)`/`B(N)`, and any coverage claim** (`ADR-0089` §§2b, 2c).

**If the reading needs a decision no merged source settles**, write both rows, register the next free
`DEC` in `docs/adr/README.md`'s `## Open decisions`, and leave this ticket `blocked`.

## Tests

No test can be written, for `TASK-131003`'s merged reason. Five gates, and what each is worth:

| Gate | Proves | Today |
| --- | --- | --- |
| the `P6a` row is filled | the waiting-room reading was written down | **red** |
| the `P6b` row is filled | the playing-room reading was written down, separately | **red** |
| **zero** placeholders remain anywhere in the story | every one of the seven rows now carries a result — the story's first acceptance criterion, checked | **red** |
| the table still has seven `P` rows | no row was lost on the way — the two row gates already fail on their **own** row being missing (`exit (bad || !ok)`, probed), so this one covers the other five | green — a regression guard |
| no `verify:` block under `tasks/tasks/TASK-1310*.md` names `drive.mjs` or `stack.sh` | `ADR-0089` §2b holds across the whole story | green — a regression guard |

The third gate is an upper bound on placeholders and therefore a **lower** bound on rows filled: it
forbids leaving one, and permits a drive that answered more than it was asked.

## Acceptance criteria

**Who runs the measurement:** the implementer, before opening the PR, on a running stack.
**Paste every reading into the PR body as text**, unedited — four `open` first paints, four `frames`
transcripts, four `location.hash` reads.

- [ ] For each of the four runs, `pd.roomCode` was read **before** the mailed link was opened, and
      the value is in the PR body — a drive on a tab holding no room measures a different path
- [ ] Each mailed link was reached by a **page load** to `#/verify/<token>`, never by assigning
      `location.hash` in a running tab, and the PR body shows the `open` command
- [ ] The PR body says, for each run, whether `Finish verifying an address` appeared and whether
      `That link has expired or has already been used.` appeared
- [ ] The `P6a` and `P6b` rows each state whether the mailed screen mounted, and therefore whether a
      real token would have been spent, naming `web-client/src/account/recovery-text.ts` as the
      module owning the sentences (`ADR-0089` §5)
- [ ] Both rows record that *the token is still usable afterwards* is **undrivable on this stack**,
      with `ADR-0031` §7, `NoRecoveryMailer` and the `BYTEA` hashes as the reason
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
