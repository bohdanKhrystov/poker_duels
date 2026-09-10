# ADR-0153 — A rematch offer owes its recipient a minute, and no screen mentions it

- **Status:** Accepted
- **Date:** 2026-09-10
- **Resolves:** `DEC-165` — **does a rematch offer owe its recipient a minimum time to answer, and if
  so how long?** **Yes — sixty seconds, as a floor and never as a ceiling.** Registered 2026-09-10 by
  [`ADR-0152`](ADR-0152-the-finished-rooms-five-minutes-stand.md) §5, which measured that the window a
  recipient actually gets is `finishedMillis - (offer - finish)` and that its **floor is zero**.
- **Where the answer came from: [`docs/vision.md`](../vision.md)'s first success condition,** whose
  last beat is the press this decision is about: *"Send a link. She opens it in a browser. We play a
  full heads-up match. Someone wins. **We hit Rematch.**"* — followed by *"Everything else is
  downstream of that moment."* A `Rematch` control that answers *"That duel room is gone."* to a
  player the product invited to press it is that beat failing, and a floor of zero means it can fail
  before the player's hand reaches the mouse. The second half — that nothing is said on any screen —
  is *"Dark, quiet, fast, minimal."*
- **The number is a promise; the mechanism is not, and this ADR builds neither.** It states the
  duration and its shape. Where the floor lives, whether it is a constant or a configured field, and
  how the reaper reads it are registered below as `DEC-166` for **the architect**, because carrying
  this out amends [`ADR-0152`](ADR-0152-the-finished-rooms-five-minutes-stand.md) §§3–4 and rewrites
  its §6 test.
- **Amends** [`ADR-0152`](ADR-0152-the-finished-rooms-five-minutes-stand.md) §4 — *"A standing offer
  goes on not restamping the clock"* — which was written as the placeholder for exactly this answer:
  *"That branch is also exactly where any minimum answering window would land … so the cheap door
  stays open without being walked through."* This ADR walks through it, with a floor of sixty seconds
  rather than the restamp §4's own alternative described. §§1–2, §5 and the whole measurement stand
  byte-unchanged, and this ADR leans on them: the reaping deadline still never measures the holder's
  screen, and `ADR-0150` still opened no new interval.
- **Also amends `ADR-0152` §3's closure at two writes**, in the one way §3 provides for: *"Any such
  addition needs an ADR, and if it moves what a player experiences as a duration it needs `DEC-165`
  answered first."* `DEC-165` is answered here; the ADR that adds the write is `DEC-166`'s.
- **Touches nothing in** [`ADR-0044`](ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) — one
  intent, one room fact, an idempotent offer, no withdrawal, silence as the decline, `UNKNOWN_ROOM`
  as the frame that ends a rematch, and §7's refusal to build a countdown, which this ADR confirms;
  [`ADR-0123`](ADR-0123-a-standing-rematch-offer-follows-the-rival.md) and
  [`ADR-0138`](ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md) —
  the panel, its screens, its `Not now`, its no-effects rule;
  [`ADR-0150`](ADR-0150-back-to-the-lobby-keeps-the-room.md) — every word of it;
  [`ADR-0108`](ADR-0108-expiry-plays-the-seat-and-the-timebank-replaces-the-grace-window.md) — the
  turn allowance and the timebank, read here as evidence and left alone.
- **Moves no wire type, no `PROTOCOL_VERSION`, no schema, no stored key, no engine file, and no word
  on any screen.** `RIVAL_OFFERS`, `REMATCH_LABEL`, `ROOM_GONE` and `Not now` are as merged.
- **Registers, and does not answer:** `DEC-166` — **the architect's** — the mechanism for the floor
  (§6).

## Context

**Two durations are in tension and only one of them was ever sized.** `RoomTimeouts`' own KDoc says
what the finished room's five minutes are for, in the code's words: *"`finishedMillis`: duration a
`FINISHED` or `ABANDONED` room lingers. **Long enough for the other player to offer a rematch.**"*
That is a number sized for the **offerer** — long enough to find the button. Nobody sized anything for
the **recipient**, and until `ADR-0150` nobody had to, because a recipient who was still there was
sitting on the result screen with the control in front of them and a recipient who had pressed *Back
to the lobby* could not be reached at all.

**`ADR-0152` measured what the recipient actually gets, and it is not a duration at all.** The
reaping deadline is `finish + finishedMillis`, fixed when the hand ends; a standing offer does not
restamp it (`Room.offerRematch`'s `Offered` branch returns `copy(rematchOffers = offers)`,
`Room.kt:330`); so the recipient's window is `finishedMillis - (offer - finish)` — **floor zero,
ceiling five minutes** — a remainder of somebody else's clock. An offer at `finish + 4:59` puts a
`Rematch` control in front of a player with under a second to work, and the press answers
`Failure(UNKNOWN_ROOM)`. `ADR-0152` named that its first cost and declined to fix it: *"the product
now ships a `Rematch` control that can lie to a player it invited to press it, and this ADR chooses
to leave it that way."*

**Why that is worse than it was before, rather than merely newly visible.** `ADR-0044`'s *"the button
can lie"* has always been true, and `ADR-0123` §Consequences repeated it. But both of those were
about a player **sitting on the result screen**, watching for the offer, who would press within
seconds of the panel appearing — for whom a remainder is usually enough. `ADR-0150` §1 created a
different recipient on purpose: one who is reading the ladder or their account when the panel appears
in the corner. `ADR-0152` states the consequence exactly — *"The wanderer is by construction reading
something else, so the gap between the panel appearing and the player noticing it is now seconds
rather than zero — measured against a remainder nobody controls."* The product deliberately built a
recipient whose reaction time is worse, and left them the window with no floor.

**The forces, stated as forces.** A floor is not free: the room whose life it extends is held for
**both** players, and the press that extends it is made by the one who does not own that memory —
`ADR-0152` §4 wrote the property being spent, *"today a room's life is five minutes after the duel,
one sentence, the same for both players and independent of what either of them does"*, and its own
third cost had already noted that a reload restamps it. Against that: the vision's first success
condition ends on this press, and there is no version of *Lichess, not PokerStars* in which the
rematch button is a lottery on how late your rival happened to press theirs.

**What the product already says about how long a decision takes.** Exactly one number, merged:
the **turn allowance, thirty seconds** (`RoomTimeouts.DEFAULT_TURN_MILLIS`, `ADR-0108` §1 — *"A seat
on turn has 30 seconds to act at each decision point"*), plus a three-minute timebank. It is thirty
seconds for a decision the player is **watching for** — it is their move, they are at the table, and
`ADR-0108` §Consequences requires that the clock and the bank be *"visibly distinct"* on the screen.
The rematch offer is the opposite kind of decision on all three counts: unannounced, on a screen
chosen for another reason, with no clock anywhere. That is the whole of the arithmetic below.

**The evidence is thin, and the deadline is soft — but not symmetric.** `ADR-0152` named the
instrument that would settle the number and did not build it: nothing counts how late in a room's
life offers arrive, nothing counts how long a recipient takes to press, and no player has reported
this. So the *sixty* is taste. The *floor exists at all* is not: a window whose floor is zero is
indefensible at any frequency, because it fails the player who did everything right. Nothing here is
expensive to reverse — a floor is one comparison and one number, against code that already has
`RoomReapTest` and `RoomRematchTest` standing — and nothing becomes harder by waiting, so this is
decided now because the question is open and cheap, not because it is urgent.

## Decision

### 1. A rematch offer owes its recipient sixty seconds, as a floor

A `FINISHED` room on which a rematch offer stands does not end **before sixty seconds have passed
since that offer was recorded**. The recipient's answering window becomes

> `max(finish + finishedMillis, offer + 60s) − offer`

which is never less than sixty seconds and, as today, never more than the room's own
`finishedMillis`. This is the promise; §6 hands the mechanism to the architect.

### 2. The floor only ever raises the deadline, and never lowers it

An offer at `finish + 0:10` leaves the recipient four minutes fifty, and this decision leaves it at
four minutes fifty. A floor is not a lifetime for the offer: it does not shorten a generous window,
it does not start a second clock the recipient must beat, and nothing expires *because* the sixty
seconds are up while the room's own deadline is still ahead.

### 3. It is armed once, by the offer being recorded, and no press can pull it twice

The floor is armed by the transition that records a standing offer — `Room.offerRematch`'s `Offered`
branch — and by nothing else. A **repeat press by the same seat** does not re-arm it: `Room` answers
`ALREADY_OFFERED` (`Room.kt:315`) and never reaches that branch, and `ADR-0044` §3 already answers
that press on the wire with the same `RematchOffered`. The **other seat's** press agrees the rematch,
and an agreed room is `PLAYING` and off the reaper's list. So a finished room can take exactly one
arming, and:

> **A finished room's worst case goes from `finish + 5:00` to `finish + 6:00`, once. Neither player
> can push it further.**

That bound is the reason sixty seconds is affordable, and any mechanism that does not preserve it is
not this decision.

### 4. Nothing on any screen mentions the minute

No countdown, no *"you have a minute"*, no change to `RIVAL_OFFERS`, `REMATCH_LABEL`, `ROOM_GONE` or
`Not now`, and no new string anywhere. `ADR-0044` §7 listed *a countdown* among what it deliberately
does not build, and building one now would need a deadline on the wire and a `PROTOCOL_VERSION` bump
that this decision does not need. The vision's *"Dark, quiet, fast, minimal"* is the rest of it: a
running clock in a corner panel, over whatever the player was reading, is neither quiet nor minimal.
**A floor that works removes the reason to warn.**

### 5. The `Rematch` control is not hidden, disabled, or withheld near the deadline

`RematchNotice` and `RematchControl` ship exactly as merged, effect-free (`ADR-0138` §1) and
deadline-blind. `RematchControl`'s own rule — *"A button that can only fail is not offered"* — is
**kept by §1 rather than by a new gate**: with sixty guaranteed seconds, a panel that appears is a
panel that can be pressed, and the client never has to learn a deadline to know it.

### 6. The mechanism is registered as `DEC-166`, for the architect

**`DEC-166` — the architect's — by what mechanism does a finished room's reaping deadline observe
§1's floor?** Entirely *how*, and it is the ADR `ADR-0152` §3 requires before the write set at
`lastActivityAt` may grow. It must settle: whether the floor is a second timestamp on `Room` read by
`isReapable` or a write in the `Offered` branch `ADR-0152` §4 names (`Room.kt:330`); whether the sixty
seconds is a constant or a `RoomTimeouts` field with a deployment override beside
`ROOM_FINISHED_TIMEOUT_MILLIS`; whether the floor may arm on a room already past its own deadline but
not yet swept, given the sweep's one-second granularity; and the **rewrite** of `ADR-0152` §6's
characterisation test, which pins the opposite and which §6 named as *"the thing deliberately
rewritten rather than the thing that silently goes on passing."*

**Nothing in `DEC-166` may** move the wire or `PROTOCOL_VERSION`, add a stored key, change any word on
any screen, make a room's life a function of presence (`ADR-0152` rejected that, and this ADR does not
reopen it), or break §3's bound. **It blocks** the first ticket that implements §1, and nothing else.

### 7. What does not change

`finishedMillis`, its five-minute default and `ROOM_FINISHED_TIMEOUT_MILLIS` keep their meaning and
their values — §1 is a floor beside them, not a new value for them. An offer still cannot be withdrawn
and there is still no decline (`ADR-0044` §6, §7); `Not now` still hides the panel and tells the rival
nothing (`ADR-0123`); `ADR-0152` §§1–2's measurement and its subset argument stand whole; and a
`PLAYING` room is still never reaped.

## Consequences

**What it buys.** The vision's last beat survives the recipient `ADR-0150` invented. A control the
product puts in front of a player and invites them to press can no longer fail before they have had a
chance to press it, and the failure that remains is one a player can recognise as their own. It costs
no wire, no version bump, no string, no client change and no new screen state, and it is one number in
one comparison to reverse. It also closes `ADR-0152`'s first named cost with the smallest thing that
closes it — sixty seconds rather than the five-minute restamp §4's alternative described — and it does
so while keeping §3's bound checkable arithmetic rather than a hope.

**What it costs.**

- **A rival now holds a lever over a room that is not only theirs, and the product hands it to them.**
  One press extends a finished room's life by up to sixty seconds, and it is pressed by the player who
  does not own that memory. `ADR-0152` §4's property — *"a room's life is five minutes after the duel
  … independent of what either of them does"* — was already false of a reload and is now false of an
  offer too, and this decision spends it deliberately. Bounded at one pull and one minute (§3), which
  is the whole of the mitigation.
- **This buys sixty seconds of honesty, not honesty.** A player who notices the panel after seventy
  seconds presses a dead button and gets *"That duel room is gone."* — `ADR-0044`'s *"the button can
  lie"*, alive and unfixed past the minute. Nothing tells the recipient the minute is running (§4), so
  the lie is now late rather than impossible, which is less than the question sounded like it was
  asking for and is stated here so that nobody reads §1 as having made the control truthful.
- **The room is held for a recipient who may not be there.** The floor is armed by the offer being
  *recorded*, not by it being *seen*: the server has no notion of delivery, and `ADR-0152` rejected
  presence as a liveness signal for good reasons this ADR does not reopen. So an offer into a closed
  tab holds a room sixty seconds for nobody. Small, pure waste, accepted rather than fixed.
- **It makes a rematch slightly likelier to be agreed into a room whose offerer has walked away.** An
  offer cannot be withdrawn (`ADR-0044` §7), so an acceptance at second fifty-nine seats a duel whose
  offerer may have closed the tab, and `foldAbsent` (`AbsentSeats.kt:96`) plays it out. Reachable
  today at one second remaining; sixty guaranteed seconds is sixty more seconds in which it can
  happen.
- **A number chosen from a neighbouring number, not from evidence.** `ADR-0152` named the instrument
  that would inform this — a count of how late offers arrive — and did not build it; this ADR does not
  build it either. Sixty seconds is derived from the turn allowance and bounded by what a stranger's
  press may cost. The trigger for moving it is a player saying an offer expired under them, or the
  first count that exists.
- **A finished room now has two durations where it had one.** `ADR-0152` §3 closed the write set at
  two precisely so that no coder could add a third inside a nearby ticket; this decision opens it,
  once, by an ADR, and every future reader of the reaping code has to hold both numbers rather than
  one. That is a permanent tax on a piece of code whose whole virtue was that it was four lines.

**What it forecloses.** Little, and nothing structurally. It does **not** build: a decline, an offer
withdrawal, a countdown, a deadline on the wire, a presence-driven refresh, a cap on a finished room's
absolute life, or any notice about a held room. The one thing it does foreclose is the **full
restamp** — after this, an offer buys sixty seconds and not five minutes, and any future argument for
the restamp has to argue against a stated number rather than against silence, which is the harder and
more honest argument to have.

**What it deliberately leaves open.** The **offerer** is told nothing when the minute lapses: their
screen still says *"Rematch offered — waiting for your rival"* until they press something, because the
client learns `UNKNOWN_ROOM` only by sending. That is `ADR-0044` §6's silence, unchanged, and now
arriving up to a minute later. Whether five minutes is the right lifetime for a finished room at all
is still `ROOM_FINISHED_TIMEOUT_MILLIS`'s question and nobody has asked for it to move. And nothing
here helps the recipient who genuinely left — a closed tab, another device — which `ADR-0150` already
named as still unkept.

## Alternatives considered

**Nothing changes — the floor stays zero.** The strongest case in the set, and the one the register
explicitly allowed: `ADR-0152` §5 said in as many words that *"nothing changes is a complete
answer"*, and it is the cheapest possible one — no number, no ADR for the architect, no second
duration in the reaping code, and `ADR-0152` §4's one-sentence property left intact. The evidence for
moving is genuinely thin: no counter exists, no player has hit this, and the whole failure was
invisible four days ago. Rejected because the failure it leaves is not an edge the product stumbles
into — it is a control the product **puts in front of a player and invites them to press**, that can
fail before their hand reaches the mouse, at the end of the one sequence the vision names as its first
success condition. A product may ship a control that fails when a player is slow. Shipping one that
fails when they are fast is a different thing, and no reading of *"We hit Rematch"* survives it.

**Hide the control when it cannot work — do not show an offer with no time left in it.** Its case is
the product's own principle, already merged in `RematchControl`'s KDoc: *"A button that can only fail
is not offered."* It changes no duration, mints no number, takes nothing from anyone's room, and is
the most conservative reading of *don't lie to the player*. Rejected on two counts. It cannot be built
where it is being asked for: the client does not know the room's deadline — `ADR-0044` §7, *"the wire
carries no deadline"* — so hiding means putting one on the wire and bumping `PROTOCOL_VERSION`,
against a floor that needs neither. And it is the worse product: it converts *sometimes the press
fails* into *sometimes the challenge is never delivered*, so a rival who pressed `Rematch` gets
silence from a player who was never told they were challenged. §5 gets the KDoc's principle for free
instead.

**Tell the recipient how long they have — a countdown on the panel.** The most honest option
available, and its case is real: it needs no change to any duration, it treats the player as an adult,
and nobody presses a dead button when the panel reads `0:04`. Rejected three ways. `ADR-0044` §7
already declined a countdown by name, and reversing that costs a wire field and a version bump.
It contradicts *"Dark, quiet, fast, minimal"* by putting a running clock over whatever the player was
reading, on a panel `ADR-0138` §1 deliberately built with **no effect of any kind** — a countdown is a
`setInterval`, which is exactly the property that ADR made structural. And it would turn a room's five
minutes into a number on a screen: `ADR-0152` measured that no string in `web-client/src` states the
lifetime today, and a stated number is a promise the product would then have to keep. Available later,
unchanged, if the floor turns out not to be enough.

**A full restamp — the offer buys the room another five minutes.** `ADR-0152` called this *"the
strongest of the set by a distance"*, and it is: one field, no second number at all, no new concept in
the reaping code, and it guarantees the thing the product wants with the simplest sentence anyone
could write. Rejected because five minutes is a lifetime sized for something else — the KDoc says
`finishedMillis` is *"long enough for the other player to offer a rematch"*, which is a search for a
button, not an answer to a yes/no — and paying it again for one press buys a promise nobody asked for
at five times the lever. It also changes what `finishedMillis` **means**, from *five minutes after the
duel* to *five minutes after the last offer*, and a changed meaning is harder to reverse than a
number. §3's bound is what this ADR keeps that the restamp gives up.

**Make the floor thirty seconds — the turn allowance itself.** Its case is consistency, which is worth
real money here: the product would have exactly one number for *how long a player has to decide*, used
twice, and a reader of either would recognise the other. It is also the smaller lever and the cheaper
promise. Rejected because the two decisions are not alike in the way that matters. `ADR-0108` §1's
thirty seconds is for a decision the player is **watching for** — it is their move, they are at the
table, and `ADR-0108` requires the clock be visible while it runs. `ADR-0150` built a recipient who is
none of those things. Thirty seconds is a fair floor for the player who never left the result screen
and a poor one for exactly the player this decision exists for. It stays one constant away if sixty
proves generous.

**Raise `DEFAULT_FINISHED_MILLIS` instead — ten minutes, and the problem gets smaller.** Its case: no
new concept, no second duration, one constant, and it helps the offerer as much as the recipient.
Rejected because it does not answer the question. The floor is zero at ten minutes exactly as it is at
five; an offer at `finish + 9:59` is the same failure one number later. It buys nothing this decision
is about and charges every finished room on the server double for it. `ADR-0150` and `ADR-0152`
rejected it on measurement; this makes three, and it is the same rejection.

**Wait for the instrument — count how late offers arrive, then pick a number.** Its case is the honest
one, and `ADR-0152` made it: this ADR admits the sixty is taste, and a count would settle it —
`ADR-0152` even named the product owner as that instrument's real customer. Rejected because the thing
being weighed is not close. No frequency of late offers makes a floor of **zero** defensible, so the
count could only move the sixty, which is one constant against tests that already stand. Building the
counter first would block an answer that blocks a ticket, on a measurement whose result changes one
number. Named here so the next reader knows the count still does not exist, and that it is the thing
to build before arguing with the sixty.
