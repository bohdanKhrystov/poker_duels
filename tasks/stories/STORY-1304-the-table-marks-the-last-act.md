---
id: STORY-1304
title: The table marks the last act, and the next deal clears it
type: story
status: done
parent: EPIC-13
module: web-client
labels: [client, design, table]
depends_on: [STORY-1303]
---

## Goal

The duel table carries one mark for the most recent act of the hand it is painting, at the seat that
made it, in that seat's own button's words — so a player facing a bet can read what the rival just
did without having watched it happen.

## Why

`EPIC-13` item 3, and the human's third sentence: *"opponent last action shoud be visible on the
screen."* Today the client appends all six act events to `state.narration` and, `ADR-0102` aside,
**nothing renders them** — the table already knows every act and reads none of them to a player.

**It is fourth because it is the second mark at the same seat.** `STORY-1303` puts the acting mark
there; this one puts a standing act mark beside it, and the two together are what `ADR-0103`'s phone
fit has to absorb. Landing them in a fixed order means each card proves the fit against everything
merged before it, and the second one is the one that finds the fence.

## Design notes

Everything below is
[`ADR-0109`](../../docs/adr/ADR-0109-the-table-marks-the-last-act-and-the-next-deal-clears-it.md),
merged, and is not re-litigated by a ticket. §§1–4 are what the tickets implement.

- **One mark, all six acts, at the seat that made it** (§1). `FOLD`, `CHECK`, `CALL`, `BET`,
  `RAISE`, `ALL_IN` — whichever seat, **exactly one mark at the table, never one per seat**; when
  the other seat acts, the mark moves. **A player's own act is marked the same way.** Blind posts,
  deals, presence changes and rejections are not acts and never create, move or clear it.
- **It says what the actor's own button said** (§2). The verb is `action-text.ts`'s `actionVerb` —
  *Fold, Check, Call, Bet, Raise to, All in* — and a figure rides with it **exactly where the bar's
  buttons carry one**: `Call`, `Bet`, `Raise to` and `All in` carry the event's own `to` total,
  formatted by `formatChips`; *Fold* and *Check* are bare. **Nothing is netted, priced or worked
  out** — the increment (*Raise by 140*) is refused by §Alternative 7 because the server never sent
  it.
- **Within a hand it is only ever replaced; the next painted deal removes it** (§3). The next act
  replaces it. **Street boundaries leave it standing** — `BettingRoundEnded`, `StreetDealt`, every
  `Snapshot`, presence frames and rejections touch it not at all. The **next hand's deal as painted**
  removes it, which is `ADR-0102` §1's step queue and not the frame's arrival, so a fold's mark
  stands through `ADR-0095` §4's award window. `DuelFinished` retires it with the table.
- **No timer, no fade, no dismiss control** (§4). The player this mark exists for is the one who was
  not looking; a mark that expires serves everyone but its audience. `ADR-0102` §4's clock schedules
  when facts *appear* and never when they are taken away.
- **Built from the act events alone** (§6), beside `ADR-0046` §4's server-action line. When the
  server folds for an absent rival the game event is a real `PlayerFolded`, so the mark reads *Fold*
  while the neighbouring line says *The server folded for your rival.*

**The delivery is why two obvious rules are wrong**, and a ticket that proposes either is rejected
on §Context rather than on taste: *clears at the street's end* erases the closing call at the instant
it is made, because the act and its `BettingRoundEnded` ride one `Events` frame applied in one tick;
*never clears* survives a deal and manufactures a false pair.

**Two costs are accepted and are not defects** (§Consequences), so no ticket repairs them:

- **The opening corner.** Heads-up, the non-button opens every postflop street, so whenever its own
  call or check closed the previous street it opens the next under its **own** mark and the rival's
  latest act stands unmarked, one act back. Bounded: only the verb is hidden, never a bet the player
  currently faces.
- **A refresh loses the mark until the next act.** `PlayerView` carries no last-act field.
  Deliberately not repaired, registered nowhere (`STORY-1211`'s rule). **A ticket that wants to fix
  this is asking for a `PROTOCOL_VERSION` bump and is out of scope.**

**The card is the first ticket and merges before the implementing ticket is startable** (`EPIC-13`
*Design first*, `ADR-0091` §2). `ADR-0109` §5 fixes what it owes: **six states**, one per act —
*Fold* and *Check* bare, *Call*, *Bet*, *Raise to* and *All in* each drawn **with a figure**. The
mark is the same mark at either seat, so the second seat multiplies nothing. Icon versus word versus
both, where it sits, and whether a mark left standing from an earlier street is visually
distinguished are the card's to offer and the human's to accept (`ADR-0024` §3). The card places the
mark inside `ADR-0103` §1's phone fit; **if it cannot without something giving, that re-opens
`ADR-0103`'s give list rather than being quietly spent.**

The mark is absent from the host-alone table `STORY-1302` lands (`view === null`, no acts yet), and
this story's tests say so rather than leaving it to chance.

## Tasks

Split into eight tickets on 2026-09-02, one chain; `TASK-130401` is the only startable one.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-130401](../tasks/TASK-130401-the-seat-card-draws-the-last-act-in-all-six-states.md) | The seat card draws the last act, in all six of its states, at both seats | ready |
| [TASK-130402](../tasks/TASK-130402-the-two-table-cards-carry-the-mark-and-the-host-alone-frames-carry-none.md) | The two table cards carry the last act in place, and the host-alone frames carry none | backlog |
| [TASK-130403](../tasks/TASK-130403-the-reducer-remembers-the-act-just-made.md) | The reducer remembers the act just made, and the deal that opens a hand takes it off | backlog |
| [TASK-130404](../tasks/TASK-130404-the-mark-stands-until-the-next-hand-is-painted.md) | The mark stands until the next hand is painted, the duel's end retires it, and nothing else touches it | backlog |
| [TASK-130405](../tasks/TASK-130405-the-marks-words-are-the-buttons-words.md) | The mark's words are the button's words, and its figure is the event's own total | backlog |
| [TASK-130406](../tasks/TASK-130406-the-seat-plate-draws-the-last-act-it-is-handed.md) | The seat plate draws the last act it is handed, and speaks nothing | backlog |
| [TASK-130407](../tasks/TASK-130407-one-mark-at-the-seat-the-act-names.md) | One mark, at the seat the act names, and it moves when the other seat acts | backlog |
| [TASK-130408](../tasks/TASK-130408-the-screen-feeds-the-mark-and-there-is-none-before-the-first-snapshot.md) | The screen feeds the mark, and there is none before the first snapshot | backlog |

**Three things the split settled, having read `develop` rather than the story's notes.**

- **The mark is a reducer field, `lastAct`, holding the whole act event** — `serverAction`'s
  register, and `ADR-0109` §Consequences' own *"two reducer keys"*. `ADR-0102` §1's queue then gives
  *the deal as painted* for free: frames that arrive while a hand's ending is being painted are
  held and only reach the reducer once the last step has stood, so a `HandStarted` clears the mark
  at the moment a player sees the new hand and never at the moment its frame lands. The recorded
  script confirms the shape — the next hand's `HandStarted` rides its **own** `Events` frame, after
  the hand-completing `Snapshot`.
- **`view === null` shows nothing, and a refresh loses the mark**, both written into tickets rather
  than left for a coder to find. `Lobby.tsx` renders `WaitingTable` when there is no view and that
  component mounts no `SeatPlate`, so no element, class, text or attribute reaches the null view;
  the mark speaks no `aria-label` and no `title` on **any** screen, so `null-view.test.tsx`'s
  `spoken()` closure and its digit sweep do not change shape. A resume delivers a `Snapshot` with no
  `Events` in front of it, so `lastAct` stays `null` until the next act — `TASK-130404` pins that as
  a named reducer test rather than repairing it.
- **`no-derivation.test.tsx` stays green because it never sees a mark**, not because the mark is
  admitted: all seven of its tests render `<DuelTable view={…} />` and pass no act, so the act's
  `to` — which is not a `PlayerView` field — never reaches its sweeps. Two tickets pin the file at
  **7** to prove it, and widening that guard is named as a later ticket's debt if one ever renders a
  mark into it.

## Acceptance criteria

- [ ] A card under `design/` draws **all six** named act states — four of them with a figure — at
      `ADR-0103`'s phone size, with `design/check-drift.sh` exiting 0, merged before any
      implementing ticket is startable
- [ ] Each of the six act events produces the verb `actionVerb` gives it, and only `Call`, `Bet`,
      `Raise to` and `All in` carry a figure — one named test per act, and the figure is the event's
      own `to`
- [ ] A second act **replaces** the mark rather than adding one: at no point do two marks stand —
      asserted with the two acts at **different** seats, so a per-seat implementation fails
- [ ] `BettingRoundEnded`, `StreetDealt`, a `Snapshot`, a presence frame and a `Rejected` each leave
      the mark standing — one named assertion each
- [ ] The mark survives the award window and is gone when the **next hand is painted**, not when its
      frame arrives — asserted against `ADR-0102` §1's step queue
- [ ] `DuelFinished` leaves no mark for a rematch to inherit
- [ ] The mark is absent from the host-alone table (`view === null`)
- [ ] `no-derivation.test.tsx` stays green — the mark prints the event's own token and total and
      computes nothing
- [ ] The document still fits at 390 × 664 under `ADR-0103` — `scrollHeight ≤ clientHeight`, read
      and pasted as text

## Out of scope

- **An action log, or a mark per seat.** `ADR-0109` §1 and §Alternative 2 refuse both; a client
  built on one mark is not a ledger, and growing one later retracts the ADR.
- **Putting the last act on `PlayerView`.** That is the refresh cost, accepted in
  `ADR-0109` §Consequences; it is a wire change and this epic bumps the version once, for the clock.
- **A timer or a fade on the mark** (§4), and **any clearing at a street's end** (§Alternative 3).
- **The acting-seat mark** — `STORY-1303`. Two marks, two lifetimes, two cards.
- **`ADR-0046` §4's server-action line.** It coexists with this mark and is not redesigned here.
- **The engine.** Nothing here opens `poker-engine`.
