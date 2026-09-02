---
id: STORY-1302
title: The host waits at the table, and both promises move with them
type: story
status: done
parent: EPIC-13
module: web-client
labels: [client, design, table, lobby]
depends_on: [STORY-1301]
---

## Goal

Creating a duel puts the host at the duel table with the rival's seat empty and the invite drawn on
it; the dedicated waiting screen is retired, and `ADR-0073` §§1–3's two promises keep being made,
verbatim, at the same moment.

## Why

**It is second because it creates a table state every later surface in this epic has to answer
for.** [`ADR-0110`](../../docs/adr/ADR-0110-creating-a-duel-seats-the-host-at-the-table.md)
*Consequences* names it: *"The table acquires a state in which it is not a duel. Every future table
surface — the turn clock, the chips, the act-just-made line, all of them this same epic — must now
say what it shows when `view === null`, or be absent from that state on purpose."* Paying that tax
once, before four more surfaces are drawn, is cheaper than retro-fitting each of them and cheaper
than discovering at the end that a card drew a clock at an empty seat.

It is also the one item in the epic that removes a screen, and `ADR-0105` §2 leans on a sentence
that screen renders. If the sentence stopped being said, a merged ADR's reasoning would point at
copy nothing renders — `ADR-0110`'s own named worst outcome.

## Design notes

Everything below is merged and is not re-litigated by a ticket.

- **The state already exists; only its rendering changes.** `state.roomCode !== null && state.view
  === null` is the branch (`ADR-0110` §1). `Lobby.tsx` already renders `state.view !== null` ahead
  of it, so the opening `Snapshot` ends the wait with no navigation. **No Kotlin, no wire type, no
  `PROTOCOL_VERSION`, no stored data** — `RoomJoined(code, seat)` carries everything.
- **The seat says `Waiting for your rival`** — verbatim, capital *W*, no full stop, byte-identical
  to today's heading, relocated to the seat, rendered **once** and not additionally as a heading
  (`ADR-0110` §2). The word is *rival*, not *opponent*; §2 records that being overruled is one
  string and one card frame.
- **The table states no game fact before the `Snapshot`** (`ADR-0110` §3): no stack, blind, card,
  pot, dealer button or action bar. This is `ADR-0002` applied, not a styling choice. The host's own
  seat may carry its shipped `You` and nothing more.
- **Both promises move verbatim** (`ADR-0110` §4): `Back to the lobby` with `forgetRoom()` from an
  event handler, and `The room stays open. That link still works for your rival, and it brings you
  back.` placed with the control. `ADR-0073` §5's refused words stay refused.
- **The invite moves whole** (`ADR-0110` §5): the bare code, the `Invite link` label with the
  selectable read-only box, and `Copy the link` with `Link copied.` and `Copy it from the box
  above.` — the control absent where `navigator.clipboard` is. Dropping any part severs a shipped
  way in. Nothing is said *about* the code (`ADR-0022`), and nothing prints a duration or countdown
  (`ADR-0072` §6).
- **The state adds no new string** (`ADR-0110` §6). The enumeration in §6 is exhaustive. **If the
  card or a ticket finds it needs one more, that is a stop and a new ADR — never an invented
  sentence.**
- **The arrival is the `Snapshot` and it is silent** (`ADR-0110` §7). No *Your rival has joined*
  notice. The rival never sees this state at all (`ADR-0094` §1).

**The card is the first ticket and merges before the implementing ticket is startable**
(`EPIC-13` *Design first*, `ADR-0091` §2). `ADR-0110` §8 fixes the states it owes:

1. **Host alone at the table**, in four named variants — at rest; after `Link copied.`; after
   `Copy it from the box above.`; and with **no clipboard API**, where the copy control is absent
   and the box is the invite.
2. **The moment the rival arrives** — the live table `duel-table.html` already draws; the card owes
   only that the waiting furniture is gone from it.

The card ticket names the file it lands in — frames appended to an existing screens card or a new
card file — under `design/README.md`'s conventions, with `design/check-drift.sh` green either way.
`ADR-0073` §6's *Created — waiting for your rival* frame in `design/screens/create-duel.html` is
retired with the screen; that file's front-door frame is untouched. **`ADR-0110` §8 asserts the fit
at 390 × 664 as an argument, not a reading** — the card proves it under `ADR-0103`, and if all three
invite parts do not fit, that finding **reopens `ADR-0110` §5** and does not license quietly
dropping one.

Taste — how the empty seat is drawn, where the invite sits, what weight each element carries — is
the human's by `ADR-0024` §3, given by looking at the rendered card, and may trail the merge.

## Tasks

Split on 2026-09-02 into seven, in one chain. Two decisions the split made and the story did not:

- **The frames land in `design/screens/duel-table.html`, not in a new card file.** `ADR-0110` §8.2
  makes the arrival frame *that file's existing `Phone — 390 × 664` drawing*, so putting the
  host-alone frames beside it turns the transition into an adjacency a reader can see; it is also
  the only card declaring a 390 × 664 box, which is where §8 puts the fit; and a new file would
  copy about ninety lines of preamble, column and seat plate before drawing anything.
- **The null-view contract lands as a file, `web-client/src/table/null-view.test.tsx`**
  (`TASK-130206`), rendering the real branch and sweeping text nodes *and* `aria-label`/`title`.
  A clock, a chip pile or a last-act figure drawn at the empty seat prints a digit and fails it.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-130201](../tasks/TASK-130201-the-card-seats-the-host-alone-at-the-phone.md) | The card seats the host alone at the phone, at rest | ready |
| [TASK-130202](../tasks/TASK-130202-the-cards-three-remaining-variants-and-the-arrival.md) | The card's three remaining host-alone variants, and the arrival | backlog |
| [TASK-130203](../tasks/TASK-130203-the-invite-is-a-component-of-its-own.md) | The invite is a component of its own, and the DOM does not move | backlog |
| [TASK-130204](../tasks/TASK-130204-the-host-alone-table-is-a-component.md) | The host-alone table is a component, drawn as the card draws it | backlog |
| [TASK-130205](../tasks/TASK-130205-creating-a-duel-lands-the-host-at-the-table.md) | Creating a duel lands the host at the table, and the waiting screen is gone | backlog |
| [TASK-130206](../tasks/TASK-130206-what-the-table-shows-when-there-is-no-view.md) | What the table shows when there is no view, written down as a gate | backlog |
| [TASK-130207](../tasks/TASK-130207-the-retired-frame-leaves-the-card-and-the-inventory.md) | The retired frame leaves the card, and the inventory names where the host waits | backlog |

## Acceptance criteria

- [ ] A card under `design/` draws the host-alone table in `ADR-0110` §8's **four named variants**
      plus the arrival frame, at `ADR-0103`'s phone size, with `design/check-drift.sh` exiting 0 —
      and it is merged before any implementing ticket is startable
- [ ] Creating a duel renders the duel table with the rival's seat empty; no dedicated waiting
      screen renders at any point
- [ ] The empty-table render contains **no** stack, blind, card, pot, dealer button or action bar —
      asserted by a named test that fails if any of them appears
- [ ] `Waiting for your rival` renders exactly once, at the rival's seat
- [ ] `Back to the lobby` and `The room stays open. That link still works for your rival, and it
      brings you back.` both render in the host-alone state, byte-identical to today's strings
- [ ] All three invite parts render, and the copy control is absent when `navigator.clipboard` is
- [ ] The set of strings the state renders equals `ADR-0110` §6's enumeration — no more
- [ ] The rival's arrival replaces the empty seat with the live table and renders no announcement

## Out of scope

- **`DEC-111`** — whether one player may hold several `WAITING` rooms. `ADR-0110` §7 leaves it
  exactly as open as it was; `EPIC-13` *Out of scope* names it by name.
- **`DEC-119`'s address question** — what the URL says while a room is held is
  [`ADR-0112`](../../docs/adr/ADR-0112-only-a-running-duel-refuses-another-screen.md)'s and lands in
  `STORY-1311`. `ADR-0112` §3 is phrased in room states precisely so this story may put the waiting
  host anywhere.
- **The one-render `waiting` window on a resume into a `PLAYING` room.** `ADR-0114` §6 measured it
  off merged source — `RoomJoined` and `Snapshot` are two socket frames, so the standing reads
  `waiting` for one render — and settles ownership in the same breath: it *"cannot be closed from
  the client"*, closing it exactly would need the server to name the room's state in the join
  answer, which `ADR-0112` §7 forbids, and any observation of it belongs to `ADR-0114` §7's drive.
  That drive is **`STORY-1311`'s**. This story changes what that one render *draws* and nothing
  about its width; no ticket here adds a guard, a delay or a spinner for it, and if the drive later
  observes a token spent in that window, `ADR-0114` §6 makes it a new `DEC`.
- **Pre-duel facts on the empty table** — starting stacks or blinds from configuration.
  `ADR-0110` §3 is `ADR-0002` applied; a future *preview stacks* idea amends a foundational rule.
- **The engine.** Nothing here opens `poker-engine`.
