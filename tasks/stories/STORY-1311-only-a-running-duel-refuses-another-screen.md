---
id: STORY-1311
title: Only a running duel refuses another screen, and the refusal restores the address
type: story
status: blocked
parent: EPIC-13
module: web-client
labels: [client, routing, lobby]
depends_on: [STORY-1310, STORY-1302]
---

## Goal

A player holding a `WAITING`, `FINISHED` or `ABANDONED` room can ask for `duels`, `leaderboard`,
`account` or a mailed screen and get it, with the address naming it; a player in a **running** duel
asks and nothing moves, silently, with `/` restored.

## Why

`EPIC-13` item 8. The reported symptom did not reproduce; **what reproduced is the inverse** — one
effect in `Lobby.tsx` calls `leave()` whenever `outcome`, `view` or `roomCode` is non-null and the
screen is not `first`, so `location.hash = '#/leaderboard'` reads back empty within 2 s and those
three screens are unreachable while any room is held.

**Two merged promises already contradict that.** `ADR-0073` §3 tells a waiting host on screen that
they may walk away — and today refuses them a glance at the ladder inside the product. `ADR-0086`
§6's accept is `<a href="/#/account">`, and that page load rejoins the remembered `FINISHED` room,
re-seats the tree, and the effect erases `#/account` so the account screen never shows.

[`ADR-0112`](../../docs/adr/ADR-0112-only-a-running-duel-refuses-another-screen.md) resolves both:
the shipped behaviour on a **playing** browser is confirmed as intent, and the two states a player
actually sits in between hands of nothing — waiting and finished — stop being a confinement.

## Blocked on `DEC-123` — the architect's

**This story is not splittable into tickets until `ADR-0114` merges.** `ADR-0112` §7 registers it and
owns:

- how a chosen screen renders over a held **non-running** room — the branch order `ADR-0076` §3 fixed
  and `ADR-0112` qualifies is re-fixed there;
- where the room-state read lives — the store's `roomCode`, `view` and `outcome` already form the
  ladder, **but that observation is offered, not a design**;
- how promptly the running-duel refusal restores `/` — the measured 2 s in which the address lied is
  a mechanism artifact, **not a licence**;
- how a mailed `verify`/`reset` token refused mid-duel survives **unspent**.

**Nothing in `DEC-123` may move the wire or `PROTOCOL_VERSION` on `ADR-0112`'s account** (§7). A
mechanism that does is answering a different question.

## Design notes

Everything below is `ADR-0112`, merged, and the mechanism must serve it.

- **The address names the screen the player chose, and never the room** (§1). While a room is held,
  the address is `/` until a screen is chosen; the table, the wait and the result have **no address
  and get none**, and **no `Screen` member is added for any of them** — `ADR-0076` §§1–2 reaffirmed.
  `?room=CODE` stays an instruction consumed at boot, not a route.
- **While the held room's duel is running, an ask moves nothing** (§2). *Running* means `PLAYING`,
  **grace window included** — `ADR-0105` §2's definition borrowed whole. **The ask is refused and the
  address is restored to `/`, and these are one act, not two.** The refusal is **silent**: no notice,
  no dialog, **no new string**.
- **While the held room holds no running duel, the ask is honored** (§3). `WAITING`, `FINISHED` or
  `ABANDONED`: the chosen screen shows and the address names it — `duels`, `leaderboard`, `account`
  and the door-gated `sign-in` behind it. **Nothing about the room moves**: the seat is kept, the
  tab's memory is kept, the socket stays open so the rival never sees an absence, and frames keep
  applying, so `/` is always the room's screen **as it now stands**. The way back is each chosen
  screen's own *Back*, or the browser's. **No new control exists and none is drawn.**
- **A frame that seats a running duel overrules any chosen screen** (§4), `ADR-0076` §3 as written: a
  waiting host mid-scroll is pulled to the table and the fragment is replaced with `/`. A standing
  rematch **offer** seats nothing and overrules nothing.
- **The account is reachable by the same rule** (§5): mid-duel no, from a `WAITING` or `FINISHED`
  room yes. `ADR-0086` §6's accept lands on the account screen it names. **A mailed link refused
  mid-duel must not spend its token** — the same mail must work after the duel.
- **Phrased in room states, never in screens** (§3), so `STORY-1302`'s answer — wherever it puts the
  waiting host — changes nothing here. This story nonetheless **depends on `STORY-1302`** because
  both edit `Lobby.tsx`'s held-room branch and the run is sequential.
- **`STORY-1310`'s record is this story's input.** Any path that reproduced a defect is repaired
  here or in its own ticket; any path that did not is cited by name so the epic's Definition of done
  closes on written evidence.
- **No new surface, so no card.** `ADR-0112` names it: *"no new string, no new control"*, and the
  chosen screens stay ignorant of navigation and are not redesigned (`ADR-0060` §4). **If a ticket
  finds it wants one, that is a stop — `ADR-0105` §4's discipline: a screen that needs a new sentence
  for this has outgrown this decision and owes a new ADR.**
- **Costs that are accepted and are not defects** (§Consequences), so no ticket repairs them:
  mid-duel confinement is total and silent; the look-away hides the room's own surfaces (a waiting
  host on the ladder does not see their invite link, a finished player does not see a rematch offer
  arrive); a waiting host can be yanked to the table by §4 and no card draws the transition; a mailed
  link clicked mid-duel visibly does nothing.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *blocked — split after `ADR-0114` merges, then run `/plan-story STORY-1311`* | — |

## Acceptance criteria

- [ ] Holding a `WAITING` room, asking for `duels`, `leaderboard` and `account` each shows that
      screen and leaves the address naming it — one named test per screen
- [ ] The same from a `FINISHED` room, and from an `ABANDONED` room
- [ ] Holding a `PLAYING` room, the same three asks each leave the table on screen and restore `/` —
      silently, with no new string rendered
- [ ] The grace window counts as running: an ask during it is refused
- [ ] A look-away leaves the seat, the tab's room memory and the socket untouched, and a frame
      arriving while the player is on another screen still applies
- [ ] A frame that seats a running duel pulls the player to the table and replaces the fragment with
      `/`; a standing rematch **offer** does not
- [ ] `ADR-0086` §6's accept from the result screen lands on the account screen — the path
      `STORY-1310` observed
- [ ] A mailed `verify`/`reset` link opened mid-duel leaves its token **unspent** and the same link
      works once the duel ends
- [ ] No `Screen` member is added for the table, the wait or the result
- [ ] `PROTOCOL_VERSION` is unchanged and no wire type moves
- [ ] The epic's refresh Definition-of-done row is satisfied by `STORY-1310`'s written record, cited
      here by path

## Out of scope

- **Honouring a navigation mid-duel.** `ADR-0112` §Alternatives refuses it while no clock bounds a
  duel; it becomes arguable the day `STORY-1308` lands, and it is a small ADR then — **not this
  story's to take.**
- **An address for the table** (`ADR-0076` §2 forecloses it permanently), and a `Screen` member for
  the wait or the result.
- **Leaving the room on a navigation.** `ADR-0112` §Alternatives refuses the destructive glance.
- **A notice explaining the refusal**, and any new control for the way back.
- **Redesigning the chosen screens.** `ADR-0060` §4.
- **The engine and the server.** Nothing here opens either.
