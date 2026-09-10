# ADR-0157 — A refusal may take the panel's dealing span, and never a standing offer

- **Status:** Accepted
- **Date:** 2026-09-10
- **Resolves:** `DEC-168` — **does the rematch panel go on saying *That duel room is gone.*, and
  retiring `Rematch`, for a refusal the player brought on themselves?** **The sentence stays; the
  pre-emption goes.** `RematchNotice` goes on stating *That duel room is gone.* — in the **same
  words, in one state** — but only **in place of the dealing span**, once this player has answered
  the offer with the panel's own `Rematch`. A refusal **never** takes a standing offer off the
  panel, and what retires `Rematch` is **the press**, not the refusal. The **result screen is not
  edited**. Registered 2026-09-10 by
  [`ADR-0154`](ADR-0154-the-gone-room-is-the-rematch-surfaces-news.md) §6.
- **Derived, not transcribed, and here is the sentence that licenses it.**
  [`docs/vision.md`](../vision.md)'s first success condition, whose last beat is the press this is
  about: *"Send a link. She opens it in a browser. We play a full heads-up match. Someone wins.
  **We hit Rematch.**"* — followed by *"Everything else is downstream of that moment."* It is the
  same sentence [`ADR-0153`](ADR-0153-a-rematch-offer-owes-its-recipient-a-minute.md) spent a
  minute of somebody else's room on, and this ADR is what stops one keystroke in an unrelated field
  taking that minute back. The second half — that nothing new is said, and that no sentence stands
  *beside* the offer — is *"Dark, quiet, fast, minimal."*
- **Amends [`ADR-0154`](ADR-0154-the-gone-room-is-the-rematch-surfaces-news.md) on two named
  things**, both of which that ADR registered as open in the same breath (§6): §4's *"`RematchNotice`
  and `RematchControl` ship as merged"* — **for `RematchNotice` only**, and only in the order its
  three states are entered — and §5's **second row**, where a refusal taken on the result screen was
  restated by the panel on the front door with no press. `ADR-0154` §§1–3 are untouched and this ADR
  leans on them: the held-room front door goes on stating **no** `UNKNOWN_ROOM` line, `ROOM_FULL` and
  *The server refused that.* go on being stated there, and every other front door is byte-unchanged.
- **Amends [`ADR-0138`](ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md)
  §4 on one clause, for the panel only:** *"`refusal === "UNKNOWN_ROOM"`, where the button is
  **retired** and the sentence stands in its place"* — after this the sentence stands in place of the
  **dealing span**, and the button was retired by the press. §4's **three states**, its **words**, its
  one-module rule for `rematch-text.ts`, its copied `Not now`, and its reading of `ADR-0127` §1 all
  stand; `RematchControl`'s five states are not touched by any of it.
- **Applies, and amends nothing in:**
  [`ADR-0153`](ADR-0153-a-rematch-offer-owes-its-recipient-a-minute.md) §§1–5 — the sixty-second
  floor, its arming, its silence on every screen, and §5's *"not hidden, disabled, or withheld"*,
  which this ADR is the client half of (§5's *"ship exactly as merged"* was that ADR declining to
  touch these two files, and `DEC-168` is the touch it left for here);
  [`ADR-0123`](ADR-0123-a-standing-rematch-offer-follows-the-rival.md) §§3, 5 and 7 — one live
  surface, a panel that never retires itself, a dismissal that hides the surface and never the offer;
  [`ADR-0044`](ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) §§3, 6 and 7 — an idempotent
  offer, `UNKNOWN_ROOM` as the frame that ends a rematch, and no countdown;
  [`ADR-0072`](ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md) §7, whose refusal to
  track which frame a `Failure` answered is a **premise** of this ADR and not a casualty of it;
  [`ADR-0127`](ADR-0127-a-control-stands-only-for-a-decision-the-server-has-opened.md) §1, which
  this ADR brings the panel back into line with rather than bends;
  [`ADR-0151`](ADR-0151-the-press-is-one-fact-and-the-way-back-stops-navigating.md) §§1–6;
  [`ADR-0150`](ADR-0150-back-to-the-lobby-keeps-the-room.md) §§1–6.
- **Writes no word.** `ROOM_GONE`, `RIVAL_OFFERS`, `REMATCH_LABEL`, `DEALING_LEAD`, `DEALING_TAIL`
  and `Not now` keep their values; `web-client/src/result/rematch-text.ts` is not opened, and no
  third sentence is invented.
- **Moves nothing outside the client.** No wire type, no `PROTOCOL_VERSION`, no server file, no
  engine change, no schema, no stored key, no new player-facing string, and no change to any colour
  or shape. `poker-engine` is not opened.
- **Registers, and does not answer:** `DEC-170` — **the architect's** — where the panel's *this
  player has answered the offer* fact lives, and how the two surfaces keep one set of words once they
  no longer share one order (§6). It **blocks** the first ticket that implements §§1–3.

## Context

**One field, no provenance, and that is settled.** `Failure` writes `state.refusal` and nothing else
(`duel-state.ts:375`); `REMATCH_UNAVAILABLE` alone is swallowed. Which frame it answered is not
recorded and will not be: `ADR-0072` §7 refuses it by name — *"tracking an in-flight `OfferRematch`
to find out would be the client-side lock `ADR-0044` §3 says no client needs."* `ADR-0154` called
that refusal a premise and so does this ADR. Every option below is an option about **what a screen
does with a fact it cannot attribute**, and none of them is an option to attribute it.

**The panel's third state was written for a screen where the field could only mean one thing.**
`RematchControl`'s `UNKNOWN_ROOM` branch (`RematchControl.tsx:46`) is sound on the result screen, and
it is sound for a measurable reason rather than a hopeful one. `DuelFinished` — the frame that puts
that screen up — clears `refusal` in the same reducer step (`duel-state.ts:349`, `:360`), so a player
arrives there with the field empty. And the result screen sends exactly one kind of frame:
`DuelResult` renders a verdict, a coin line, a meta line, the rematch node and the way back, and
sends nothing itself; the way back sends nothing either (`ADR-0151` §6 pins the count). So an
`UNKNOWN_ROOM` a player reads on the result screen answered **their own rematch press**, and retiring
the control is exactly right. `ADR-0138` §4 gave the panel the same branch, and until three days ago
the panel could only stand on screens with the same property.

**The front door is not that screen, and `ADR-0151` made it reachable with a room still held.** The
front door carries a room-code field whose `Join the duel` sends `JoinRoom` (`Lobby.tsx:508`), and a
code naming no room is answered `Failure(UNKNOWN_ROOM)` — the ordinary, expected, *correct* answer to
a typo. `ADR-0151` §2 put a held finished room's holder on that screen, and §4 put the panel there
with them. So one field is now written by two acts that have nothing to do with each other, and the
panel reads it as news about the room it is offering.

**On that screen nothing clears it, so the retirement is not a moment — it is the rest of the room's
life.** `refusal` is cleared by `RoomJoined`, `YourTurn`, `Snapshot` and `DuelFinished`, and by
nothing else. A holder browsing the front door, the ladder or their account receives none of them.
The only ways out are a **successful** join or `Play duel`, both of which reset the store and abandon
the rival's offer (`ADR-0150` §4), and a `Snapshot` — which is the rematch the panel has just stopped
offering. So after one mistyped code the `Rematch` control is gone for the whole remaining life of
the room, and the second typo cannot even change the sentence.

**What that eats was bought yesterday, and it was not free.** `ADR-0153` §1 gave a rematch offer's
recipient sixty guaranteed seconds, and §5 spent that guarantee on exactly this control: *"with sixty
guaranteed seconds, a panel that appears is a panel that can be pressed, and the client never has to
learn a deadline to know it."* The price, named in that ADR's own costs, is that *"a rival now holds
a lever over a room that is not only theirs"* — one press, up to a minute, on a room the presser does
not own. The product paid that so the recipient could press. A keystroke in an unrelated field then
takes the button away for the whole minute, and for the rest of the five as well. A floor that one
typo removes is not a floor.

**And it is the client deciding a game fact.** `ADR-0138` §4 applied `ADR-0127` §1 to this surface in
as many words: *"`Rematch` is drawn only while the rival's standing offer is a decision this client
has been told is open."* `RematchOffered` opened that decision; nothing has closed it; a `Failure`
that may have answered a `JoinRoom` is not the server closing it. Retiring the control on that field
is the client asserting *this offer is dead* on evidence the server never gave — the mirror image of
`CLAUDE.md`'s non-negotiable, and the same misattribution `DEC-166` was registered about, one surface
further along.

**Both directions cost something real, and the question is which mistake to make.** Leave the offer
standing and a genuinely reaped room wears a live-looking `Rematch` until somebody presses it —
`ADR-0044`'s *"the button can lie"*, which the panel had been pre-empting. Take it down on the field
alone and the product deletes, on a typo, the one press its own vision names as the end of its first
success condition. Neither is free; one is recoverable by a press and the other is not recoverable at
all.

**The deadline.** The story that implements `ADR-0150` and `ADR-0151` is unwritten, and
`RematchNotice`'s gate is among the lines it edits (`ADR-0151` §4). Decided now, this is a branch
order in a file that ticket was opening anyway; decided later, it is a coder choosing whether a
mistyped code costs a player their rematch, inside a ticket where nobody will look for it. Nothing
about it hardens with time — no schema, no wire, no stored key, no name — which is why it is settled
cheaply and now rather than urgently.

## Decision

### 1. A refusal never takes a standing offer off the panel

While a rival's offer stands and this player has **not** answered it, `RematchNotice` states
*Your rival offers a rematch* and draws `Rematch`, **whatever `state.refusal` holds**. `Not now`
stands beside it exactly as it does today. No refusal of any kind — `UNKNOWN_ROOM` included — removes
that button, greys it, or puts a sentence in its place.

### 2. The panel goes on saying *That duel room is gone.*, in place of the dealing span

Once this player has answered the offer with the panel's own `Rematch`, the panel shows the dealing
sentence (`ADR-0138` §4). If `state.refusal` is `UNKNOWN_ROOM` at that moment or becomes so
afterwards, the panel states *That duel room is gone.* in the dealing sentence's place: the same one
constant, the same single node, the same `Not now` below it. The panel's three states, its words and
its dress are otherwise as merged.

Because the refusal may already be set when the press happens, the answer can be **immediate** — the
player presses once and reads the truth without waiting for a round trip.

### 3. What retires `Rematch` is the press, not the refusal

The button leaves the panel when this player answers the offer, and that is the only thing that takes
it. `RematchControl`'s rule — *"A button that can only fail is not offered"* — is kept the way
`ADR-0153` §5 keeps it, by the sixty-second floor rather than by a gate: a panel that appears is a
panel that can be pressed. What this ADR removes is the panel's attempt to keep that rule a second
time, on a field that cannot tell it whether the room is dead.

### 4. The result screen is not edited

`RematchControl` ships exactly as merged: the same five states in the same order, the
`UNKNOWN_ROOM` branch still first (`RematchControl.tsx:46`), the same words, the same retirement, the
same `mySeat === null` return. Nothing in §§1–3 reaches it.

**The reason is the measurement in Context, not symmetry.** On the result screen `DuelFinished`
empties `refusal` in the frame that puts the screen up, and the only frame that screen can send is
`OfferRematch`. So an `UNKNOWN_ROOM` a player reads there **did** answer their own press, the room
**is** gone, and retiring the control states a fact the server gave. The two surfaces go on sharing
`rematch-text.ts`'s constants — one fact stated twice, as `ADR-0138` §4 requires. What they stop
sharing is the **order** in which the three states are entered, and that is a property of the two
screens rather than of the two components.

### 5. What a player reads, by the way they arrived

| The way in | What is on screen |
| --- | --- |
| The panel's `Rematch`, pressed on the front door, refused | *That duel room is gone.* in the panel, alone, with `Not now` below it — **unchanged from `ADR-0154` §5's first row** |
| A code typed into the front door's field naming no room, with a rival's offer standing | the panel keeps *Your rival offers a rematch* and `Rematch`; the front door states nothing (`ADR-0154` §1). **This is the row this ADR exists for** |
| Refused on the result screen while answering a rival's offer, then the way back | the panel states the offer and draws `Rematch` again; one press restates *That duel room is gone.* with no round trip. **Amends `ADR-0154` §5's second row**, which had the panel restate it with no press |
| Refused on the result screen for the player's **own** offer, then the way back | `RematchControl` already said it there; no panel stands, because no rival's offer does — unchanged |
| A refusal that lands after the way back, for a press made before it, with no rival's offer standing | **nothing is stated** — `ADR-0154`'s own named cost, unchanged and not reopened |
| The room genuinely reaped, a rival's offer standing, the player has not pressed | the panel offers `Rematch`; pressing it answers *That duel room is gone.* — named as a cost below |

### 6. Registers, and does not answer: `DEC-170`

**`DEC-170` — the architect's — where does the panel's *this player has answered the offer* fact
live, and how do the two rematch surfaces keep one set of words once they no longer share one
order?** Entirely *how*, and it must not move what §§1–5 say a player sees. It must settle: whether
`RematchNotice`'s existing `accepted` `useState` (`RematchNotice.tsx:42`) is the fact §2 means, given
that `ADR-0138` §3's render-phase clear resets it on `!theirs` (`RematchNotice.tsx:61`) and that the
offer outlives the room's death; whether the divergence from `RematchControl` is a reordered
conditional in each file or a pure helper beside `rematch-stand.ts`; and **what a test must prove**,
which is a negative `ADR-0138` §8's proofs do not cover — that the panel keeps the offer and its
button under `state.refusal === "UNKNOWN_ROOM"` before a press, with a control that fails if the
merged order is restored.

**Nothing in `DEC-170` may** move the wire or `PROTOCOL_VERSION`, add a stored key, register an
effect on the panel (`ADR-0138` §1), change any word on any screen, or edit `RematchControl` (§4).
**It blocks** the first ticket that implements §§1–3, and nothing else.

## Consequences

**What it buys.** The press the vision's first success condition ends on survives a keystroke in a
field next to it, and `ADR-0153` §1's sixty seconds stop being removable by a typo — which is what
that ADR bought, at the price of a lever over a room the presser does not own. The panel stops
asserting a game fact the server never stated, so `ADR-0138` §4's own reading of `ADR-0127` §1 —
*"drawn only while the rival's standing offer is a decision this client has been told is open"* —
becomes true of the surface rather than approximately true of it. It costs no wire, no version, no
schema, no storage, no server change and no word; it is a branch order in a file the implementing
story was already opening.

**What it costs.**

- **A live-looking `Rematch` now stands over a room that may be dead, and the player finds out by
  pressing.** This is `ADR-0044`'s *"the button can lie"*, restored to the panel where `ADR-0138` §4
  had been pre-empting it, and it is the strongest thing the losing alternative had. Bounded: the
  press costs one `OfferRematch` frame — an offer, not a join, so it spends none of `ADR-0022`'s ten
  failed joins a minute — and when the refusal is already set the answer is immediate. Paid
  deliberately, because a lie a press corrects is cheaper than a truth a typo fabricates.
- **A player already told on the result screen is offered the button again.** §5's third row is a
  strict regression against `ADR-0154` §5's second: they pressed, they read *That duel room is
  gone.*, they took the way back, and the panel offers them the same duel. They must press once more
  to be told again. This is paid to keep §1 one sentence long — the only rule that never needs to
  know which act a refusal answered — and it is the row `DEC-170`'s architect would revisit first if
  a real player complains.
- **The misattribution is narrowed, not removed.** A player who presses the panel's `Rematch` and
  *then* mistypes a code into the front door's field reads *That duel room is gone.* over an offer
  that may be alive. The window is the span between the press and the `Snapshot` that answers it, and
  the state it replaces — an eternal *"dealing hand 1…"* — is itself a lie if the room is dead. Real,
  narrower, and not fixed here.
- **The two rematch surfaces stop entering the same state the same way.** `ADR-0138` §4 put the words
  in one module so a compiler could hold half of `ADR-0123`'s *"only prose holds them together"*;
  after this the **order** is prose again, in two files, and a reader of `rematch-text.ts` can no
  longer assume both surfaces reach `ROOM_GONE` alike. The reason lives in this ADR and in a comment,
  not in anything a gate enforces.
- **`UNKNOWN_ROOM` now has a per-screen rule on both of the surfaces that read it.** `ADR-0154` §3
  made it the one refusal the front door treats specially; this makes it the one refusal the two
  rematch surfaces treat differently. A third surface that ever reads `state.refusal` will have to be
  decided rather than follow either precedent.
- **A reaped room goes on looking alive for longer than it did.** The client learns `UNKNOWN_ROOM`
  only by sending (`ADR-0044` §6), and the panel now waits for a press before it will say so. On a
  screen the player chose for another reason, that is a stale offer sitting in the corner until they
  act on it or the tab closes.
- **The assertion that protects this is a negative**, and negatives rot quietly: a suite that renders
  the panel with an offer and a refusal, and checks the button is *there*, passes today and would
  pass again the moment somebody restored the merged order for a reason that looked good in a diff.
  Named for `DEC-170` rather than left to be discovered.

**What it forecloses.** Very little, and nothing structurally. It does not build provenance for
`Failure`, a second refusal field, a per-request correlation id, a memory of which screen a refusal
landed on, a countdown, a decline, or any way to clear `refusal` from the front door. It does not
touch the result screen, the wire, or any string. Reversing it is restoring one branch order in one
file.

**What it deliberately leaves open.** `ADR-0154` §1's silence is **not** reopened: a player who
mistypes a code on a held-room front door still reads nothing about the code they typed. This ADR
stops that silence also costing them their rematch; it does not answer the silence, and the press
that goes unanswered is still `ADR-0154`'s named cost. Also untouched: the **offerer** is told
nothing when the room dies under their own standing offer (`ADR-0153`'s open item, unchanged); and
`REMATCH_UNAVAILABLE` is still swallowed by the reducer and states nothing anywhere, which is
`ADR-0044` §6's *transient* reading and is not reopened here.

## Alternatives considered

**Nothing changes — the panel goes on branching as merged.** The cheapest answer in the set and the
one the register explicitly allowed: `ADR-0154` §4 shipped it this way four sections ago, it costs no
code and no risk, and the path needs a player holding a finished room with a rival's offer standing
who then mistypes a room code — a narrow state nobody has reported, in a client no player has used.
It is also the only option that keeps every row of `ADR-0154` §5 as written. Rejected on two things
that are not narrow. The retirement is not a moment but the **rest of the room's life**, because
nothing on that screen clears `refusal` short of abandoning the room the offer is in. And it silently
eats `ADR-0153` §1's sixty seconds, which the product bought the same day at the cost of a lever over
somebody else's room; an offer whose guaranteed minute a stranger's typo can delete did not have a
guaranteed minute.

**The panel drops the gone-room state entirely — the sentence and the retirement belong to the result
screen alone.** The simplest rule anyone could write: the panel reads no refusal at all, has two
states rather than three, and the misattribution becomes impossible rather than narrower. It would
also make §4's asymmetry a clean one — one surface reads the field, one does not. Rejected because it
replaces a false sentence with a worse one. A player who presses `Rematch` into a reaped room would
sit under *"dealing hand 1…"* for as long as the tab is open — a promise that a duel is being dealt,
which nothing will ever correct — and `ADR-0044` §6's frame that *"ends a rematch"* would state
nothing anywhere on that screen. The dealing span is the one state that must be interruptible.

**State *That duel room is gone.* beside the offer and keep `Rematch` live.** Its case is real: it
keeps both facts, it is one line, and it never removes the press the vision names — the player sees
the risk and decides for themselves. Rejected because the two nodes contradict each other on one
surface — a sentence saying the room is gone directly above a button offering a duel in it — and
because two things on screen for one event is precisely what `ADR-0154` refused three sections
earlier, on *"Dark, quiet, fast, minimal."* A player who reads both has to work out which one the
product means.

**Route the typed code's refusal to a field of its own, so the panel never sees it.** The obvious
repair, and the only one that gets **every** row of §5 right at once: the client dispatched the
`JoinRoom` itself, a second field in the store is cheap, and the front door and the panel would each
read a field that only their own act writes. Rejected because the reducer sees a `Failure` and
nothing else, so filling the second field means knowing which frame it answered — `ADR-0072` §7 by
another name, *"the client-side lock `ADR-0044` §3 says no client needs"*, whose objection applies to
a `JoinRoom` exactly as it does to an `OfferRematch`. Overturning it is an amendment to two merged
ADRs and a mechanism decision, which is not what `DEC-168` asked for; if the residue this ADR names
ever justifies the cost, that is the road, and it starts here.

**Withdraw the room-code field from the held-room front door.** The strongest structural answer: no
field, no typo, no collision, no branch order to remember, and a defensible product reason behind it
— a player holding a live finished room who joins another by code loses the room they hold
(`ADR-0141`) and abandons a standing offer in silence (`ADR-0150` §4), so the field is offering to
throw away a rematch without saying so. Rejected on three counts, any one of which is enough. It
takes a merged capability away from a player nobody has asked to restrict. It contradicts `ADR-0154`
§3, which ruled four sections ago that `ROOM_FULL` and *The server refused that.* go on being stated
on exactly that screen — sentences only that field can produce. And it does not answer the question:
§5's third and sixth rows stand untouched by it.

**Remember which screen the refusal landed on, and let the panel trust one that landed on the result
screen.** Its case is genuinely strong and it is the only shape that keeps `ADR-0154` §5's second row
*and* fixes the typo: the screen is a fact the client already computes for `ADR-0151` §2's cascade
(`finishedRoomScreen`), so this needs no correlation with a request and does not touch `ADR-0072` §7
at all. Rejected because it mints a new remembered fact — a second thing in the store or a second
provider boolean — to buy one row of a table, and it still reads wrong in the press-then-typo case
this ADR names as its third cost. It is also a mechanism this decision would be **forcing** rather
than choosing, and forcing a mechanism from a product ADR is how the architect's half of a question
gets decided by the wrong agent. It stays available, unchanged, and `DEC-170` is where it would be
reached for.

**Let `Not now` clear the refusal, so a panel flipped by a typo can be reset.** Its case: one line,
no branch order, and the control is already in front of the player — a dismissal is the natural
*"I did not mean that"*. Rejected because `ADR-0123` §7 fixes what a dismissal does — it hides the
surface and never touches the offer — and because the press removes the panel, so there is nothing
left to correct: a player pressing it to get their `Rematch` back would be hiding the offer they were
trying to keep.
