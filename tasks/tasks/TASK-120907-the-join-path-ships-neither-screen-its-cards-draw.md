---
schema: 2
id: TASK-120907
title: The join path ships neither of the two screens its cards draw
type: task
status: done
parent: STORY-1209
module: design
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [qa, uat, bug, medium, design]
depends_on: [TASK-120911]
verify:
  - sh -c '! test -f design/screens/join-duel.html'
  - sh -c '! grep -q join-duel docs/test-plan.md'
  - grep -q 'joining by a shared invite link.*design/screens/duel-table.html' docs/test-plan.md
  - grep -qF '<title>Joining by code — Poker Duels</title>' design/screens/enter-code.html
  - grep -qF '<h1>Joining by code</h1>' design/screens/enter-code.html
  - grep -qF 'Room code' design/screens/enter-code.html
  - grep -qF 'Join the duel' design/screens/enter-code.html
  - sh -c '! grep -qF "Open the duel" design/screens/enter-code.html'
  - sh -c '! grep -qF "btn ghost" design/screens/enter-code.html'
  - sh -c '! grep -q "class=.rest." design/screens/enter-code.html'
  - grep -qF 'No duel room has that code.' design/screens/enter-code.html
  - sh design/check-drift.sh
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The two cards that draw a join path this product does not have stop drawing it: `join-duel.html`
is deleted, `enter-code.html` becomes the first screen's join-by-code states, and the screen
table's row for the invite link names the card that draws what the joining player actually sees.

## The reversal — read this before anything else

This ticket was filed by round 1 of `/qa-cycle uat regression` as a **client** ticket, with
`web-client/src/lobby/Lobby.tsx` in its *Files* table and a `DEC` to be routed first. That `DEC`
is `DEC-092`, it was answered on 2026-08-30 by
[`ADR-0094`](../../docs/adr/ADR-0094-opening-the-invite-is-taking-the-seat.md), and the answer went
**against the cards**:

- **§1 — the invite path renders no screen.** Opening `…/?room=CODE` seats the player, the duel
  starts, and the first thing they see is the table. Presenting the code *is* taking the seat
  (`ADR-0022`). There is no *Take the seat*, no accept-or-decline, no pre-join view of the room.
- **§2 — a room code is typed on the first screen, and nowhere else.** The join-by-code field and
  its refusals live beside the create control on the first screen, which is what `ADR-0060` §§1 and
  4 already said. There is no second screen for entering a code.
- **§3 — the cards are what changes; the client does not.** `web-client/` is untouched, so is the
  server, so is the wire, and so is every `expect` column in `docs/test-plan.md`. `SMK-05`,
  `CORE-02` and `CORE-05` describe the blessed product and keep passing.

So the client is right, the two cards are wrong, and **this is a `module: design` ticket**. The
manual reproduction the old text carried is not a defect to fix: it is the product, and
`ADR-0094` blessed it.

**A diff that touches any file under `web-client/` fails this ticket on sight.** Nothing on the
first screen is repaired here, dressed here, or re-worded here.

## Files

| File | Action |
| --- | --- |
| `design/screens/join-duel.html` | delete |
| `design/screens/enter-code.html` | modify |
| `docs/test-plan.md` | modify |
| `docs/adr/ADR-0094-opening-the-invite-is-taking-the-seat.md` | read |

**Size.** `S`, and the diff reads larger than it is: the deletion is one `git rm` of a 122-line
file with no line to author, the authored change is roughly 70 lines inside `enter-code.html`, and
`docs/test-plan.md` moves one table cell.

## Why `join-duel.html` is deleted rather than corrected

`ADR-0094` §4a leaves the choice to this ticket — *"The ticket may delete it or repurpose its path,
on one condition: no register may be left citing a path that is gone"* — and three facts settle it
as a deletion:

1. **It has no subject.** §4a says so in as many words: the invite path has no screen, so it has
   nothing for a screen card to draw. A card is *"a versioned, rendered, human-accepted reference
   that a coder transcribes"* (`ADR-0091` §1), and a reference to something that does not exist is
   not a reference.
2. **Neither of its frames is the only card of anything.** The *offered seat* frame draws a screen
   §1 abolishes. Its *When the code refuses* frame is carded twice over already —
   `design/components/flow-actions.html` §*The refusals* carries both refusal panels and the rate
   limit as shared vocabulary, and `enter-code.html` carries the unknown-room refusal as a first
   screen state. **Deleting this card leaves no product state uncarded**, which is what
   `ADR-0094`'s *Alternatives considered* rejected the delete-both option for.
3. **The one live register that cites the path moves in this same diff** — `docs/test-plan.md`'s
   screen table, below.

`enter-code.html` **keeps its path**, per §4b, and for the reasons §4b gives: `docs/test-plan.md`
cites it, and `ADR-0092` §4's dedupe key for a missing-card finding *is* the card's own path.

## Scope

- **Delete `design/screens/join-duel.html`.**
- **Move the screen table's invite-link row.** In `docs/test-plan.md` §*The screen inventory*, the
  row `joining by a shared invite link — seated with no code ever typed` changes its **card** cell
  from `design/screens/join-duel.html` to `design/screens/duel-table.html` — the card that draws
  what she actually sees (`ADR-0094` §4a). The state text, the `walk` cell and the `routes` cell
  (`SMK-05`, `CORE-02`) are unchanged. **No `expect` column anywhere in the file is touched.**
- **`enter-code.html` stops claiming to be a screen** and becomes the first screen's join-by-code
  states. Exactly these edits:
  - `<title>Joining by code — Poker Duels</title>` and `<h1>Joining by code</h1>`.
  - The lede says what the card now is: not a screen but a state of the first one — the field and
    its refusals live on the first screen (`ADR-0060` §§1, 4; `ADR-0094` §2), and an opened invite
    link draws nothing at all, because presenting the code is taking the seat (`ADR-0022`) and what
    she sees next is the table (`ADR-0094` §1).
  - The first frame is headed for where it lives — *The code field, on the first screen* — and
    carries the field's label `Room code`, the code well, the *Eight characters, letters and
    numbers* hint, and one control, `Join the duel`.
  - **`Open the duel` and `Back` go, with the `.btn.ghost` rule that dressed `Back`.** They are the
    controls of a screen a player travels to, and §2 abolished the travel.
  - **The `<span class="rest">····</span>` placeholder goes, with its `.code .rest` rule.** Untyped
    glyphs holding their place is a mechanic an inline field does not have — `ADR-0094`'s own words
    for it are *"which an inline field cannot be"*.
  - The two frames' margin notes stop citing *the join screen* and cite `ADR-0094` §§1–2 instead.
    The existing note recording that the refusal string is `ADR-0072`/`ADR-0073`'s correction
    (`TASK-120911`'s trail) stays.
- **The code well's typography stays**, on the first screen: the mono family, `1.875rem`, the
  `--pd-track-code` tracking and the centring. `ADR-0094` reserved it — *"If the code well's
  typography is worth keeping, it is worth keeping on the first screen, which §4b leaves the design
  ticket free to do"* — and it is the same well `create-duel.html` and `flow-actions.html` draw, so
  a code read aloud and a code typed in stay one object.

## Out of scope

- **Every file under `web-client/`.** `ADR-0094` §3 blessed the client; there is nothing to repair.
  A *Files* table that names one is grounds to reject the diff.
- **Every string a merged source owns.** `ADR-0094` §5: this decision is about *which screens
  exist*, not *what words are on them*. In particular the room-full refusal — the deleted card said
  *This duel already has two players.* where the client says *That duel room already has a rival in
  it.* — is **not** settled here, is not carried into `enter-code.html`, and stays carded exactly
  where it already is, in `design/components/flow-actions.html`. `ADR-0094` §*Leaves open* puts the
  refusal-copy divergence with `TASK-120911`, which is `done` and settled only the unknown-room
  string; the room-full one is live, and **not yet ticketed**. Deleting `join-duel.html` neither
  decides it nor hides it.
- **`design/screens/create-duel.html`'s front-door and waiting frames** — the *Create a duel* /
  *I have a code* pair, and the seat plates. `ADR-0094` §*Leaves open* keeps them out on purpose,
  even though `TASK-121004` and `TASK-121012` deferred them here while the `DEC` was open. **Not
  yet ticketed**; it needs a planner run of its own against `ADR-0094` §§1–2.
- **Whether the client's `#room-code` input takes the code well's type treatment.** That is a
  client question and it runs in `ADR-0091` §2's direction — card leads, client transcribes. It is
  not filed here and no client file is touched for it.
- **A row in `docs/test-plan.md` §*Settled, and not a finding*.** Considered and deliberately not
  added: `ADR-0092` §5 already makes the answered `DEC-092` a merged source, and once the card is
  gone and `enter-code.html` is corrected there is no divergence left for a round to observe.
- **The claude.ai/design mirror.** The deleted card also leaves the cloud project and its
  `_ds_manifest.json`, but `DesignSync` is human-invoked and pushes the whole tree
  (`design/README.md` §*Push*). Not the coder's, and not gated here.
- **`TASK-060402`, the done ticket that created `join-duel.html`.** Its `verify:` block greps the
  file it created and was green when it merged. A dated record is not re-run and is not rewritten;
  neither is `ADR-0094`'s own prose, or the answered `DEC-092` row in `docs/adr/README.md`, which
  cite the path as it stood on 2026-08-30.

## Tests

**No test file.** Cards are rendered artefacts and `ADR-0024` §3 puts their taste judgment with the
human at the design pane; `ADR-0091` §3 lets that verdict trail the merge. So the `verify:` block
gates what a command honestly can, and every command in it was **run in the tree before this
rewrite, as written** — **ten of the thirteen fail today**, which is what makes them a gate rather
than a decoration. The whole block was then run a second time against a scratch implementation of
this ticket, and all thirteen exited 0; the scratch was reverted.

| `verify:` command | exit code today | what a green run proves |
| --- | --- | --- |
| `sh -c '! test -f design/screens/join-duel.html'` | **1** | the card with no subject is gone |
| `sh -c '! grep -q join-duel docs/test-plan.md'` | **1** | no register cites the path that went |
| `grep -q 'joining by a shared invite link.*design/screens/duel-table.html' docs/test-plan.md` | **1** | the invite row names the card that draws the table — the move, not just the removal |
| `grep -qF '<title>Joining by code — Poker Duels</title>' design/screens/enter-code.html` | **1** | the card's title stopped naming a screen |
| `grep -qF '<h1>Joining by code</h1>' design/screens/enter-code.html` | **1** | so did its heading |
| `grep -qF 'Room code' design/screens/enter-code.html` | **1** | the field is drawn with the label the first screen gives it |
| `grep -qF 'Join the duel' design/screens/enter-code.html` | **1** | and with the first screen's control |
| `sh -c '! grep -qF "Open the duel" design/screens/enter-code.html'` | **1** | the abolished screen's action is gone |
| `sh -c '! grep -qF "btn ghost" design/screens/enter-code.html'` | **1** | so is its *Back* — there is nowhere to go back from |
| `sh -c '! grep -q "class=.rest." design/screens/enter-code.html'` | **1** | so is the placeholder mechanic an inline field cannot have |
| `grep -qF 'No duel room has that code.' design/screens/enter-code.html` | **0** | the refusal `TASK-120911` settled survived the reframing — without this line, a rewritten body could silently undo a merged ticket |
| `sh design/check-drift.sh` | **0** | still 0 with a card removed and two rules deleted — probed, and it reports 19 cards instead of 20 |
| `python3 .github/scripts/lint_tickets.py` | **0** | the board register still agrees with this file |

The last three are green today **and must stay green**. They are not this ticket's own gate — they
are what it must not break — and a red one means the change hit something it was not aiming at.

## Acceptance criteria

- [ ] `design/screens/join-duel.html` does not exist, and `docs/test-plan.md` contains the string
      `join-duel` nowhere
- [ ] `docs/test-plan.md`'s `joining by a shared invite link` row names
      `design/screens/duel-table.html`, with its state, `walk` and `routes` cells unchanged
- [ ] `design/screens/enter-code.html` carries `<h1>Joining by code</h1>` and the matching
      `<title>`, and carries neither `Open the duel`, nor a `btn ghost` control, nor a
      `class="rest"` span
- [ ] `design/screens/enter-code.html` carries `Room code` and `Join the duel`
- [ ] `design/screens/enter-code.html` still carries `No duel room has that code.` — `TASK-120911`
      settled that string and this ticket does not move it
- [ ] The diff changes **no file under `web-client/`**, and no `expect` column in
      `docs/test-plan.md`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged. The human's visual verdict on the corrected card may
trail it (`ADR-0091` §3).
