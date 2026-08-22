# ADR-0067 — A leaderboard row is text, and no id turns into a profile

- **Status:** Accepted
- **Date:** 2026-08-22
- **Resolves:** `DEC-057` — does a leaderboard row lead anywhere: is another player's profile visible
  to a stranger, and what is on it? **Derived from the vision; the human did not state this call.**
  Two sentences license it, and they answer different halves. The *whether* comes from the
  roadmap, which already sorts this: **v0.3 is *"Leaderboard and seasons"*** and **v0.4 is
  *"Friends, statistics, replay viewer"***. A page about another player carrying their duels played,
  their win/loss record or their duel list is *statistics about a player*, and the vision puts
  statistics one milestone after this one. Deciding that a thing the roadmap places later **waits**
  is applying the roadmap; deciding it **moves** would be reordering it, and reordering is the
  human's — so of the two directions only one was ever available here. The *what a stranger reads*
  comes from **_"A leaderboard. Ranked results over a season."_** The thing the vision names is
  **ranked results**, not ranked people — the reading
  [`ADR-0061`](ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md) §4 already took,
  in the words *"the ladder is **results, not players**"*, and which `ADR-0065` §4 cites back for the
  player who has no place. A result is discharged by a row; it does not have a person behind it
  waiting to be opened
- **Applies:** [`ADR-0029`](ADR-0029-a-display-name-is-unique-and-permanent.md) §7 in the direction
  it was not written for — §7 keeps a **name** from becoming an id, and §4 below keeps an **id** from
  becoming a person.
  [`ADR-0063`](ADR-0063-nothing-gates-a-place-and-the-farm-is-accepted-until-the-ladder-is-public.md)
  §1 put every player who finished a duel on the ladder with no opt-in — and §2 put the nameless
  ones there too — which is what makes the size of the disclosure this decision's problem rather than
  a self-selected group's.
  [`ADR-0039`](ADR-0039-v01-offers-no-account-deletion.md) is why a disclosure here has no exit door,
  and [`ADR-0012`](ADR-0012-device-bound-anonymous-profiles.md) with
  [`ADR-0036`](ADR-0036-an-account-is-offered-never-required.md) are why the subject of one may never
  have signed up for anything. None of them is superseded or amended
- **Constrains:** `STORY-0504`, which is **`dropped`** — its premise is false and there is nothing
  left to build. `STORY-0503` is confirmed rather than changed: the refusal `TASK-050313` already
  ships stops being provisional
- **No migration, no wire change, no `PROTOCOL_VERSION` step, no new endpoint, no line of production
  code, and no ticket.** `StandingRow` keeps every field it has, including `playerId`; `LadderScreen`
  keeps rendering rows as text; `docs/protocol.md` is untouched, because there is no new contract to
  write and nothing already in it becomes wrong

## Context

`EPIC-04` parked this question here in as many words — *"viewing another player's profile or history
… it needs a name per leaderboard row and owns what a row links to. Here, `/api/me` means me."* By
the time it came due, most of it had been settled by other decisions, and what was left was smaller
and sharper than the register makes it sound.

**The ladder is already a disclosure, and it has already shipped.** `GET /api/standings` is
unauthenticated ([`ADR-0065`](ADR-0065-the-ladder-hands-a-player-their-own-row.md) §4 — the page is
byte-identical for every reader), walkable to the end
([`ADR-0066`](ADR-0066-the-ladder-is-computed-per-request-and-a-walk-is-pinned.md) §4), and lists
**every** player who finished a duel this season, because `ADR-0063` §1 gates a place on nothing.
Anyone at all can already read, for every such player: a rank, a display name or `null`, a season
standing, and an opaque `playerId`. So the question was never *does this product tell a stranger
anything about another player*. It does, today, about everybody. The question is whether a row is a
**door** to more.

### The forces

1. **Everything else in this product answers only about the requester, and one of those refusals is
   structural.** `GET /api/me` and `GET /api/me/duels` are the whole of the read surface about a
   player, `PUT /api/me/name` the whole of the write surface, and every one of them resolves its
   subject from the caller's own `X-Device-Id`. `ADR-0029` §7 goes
   further and makes a refusal testable over the public API: *"`ProfileReads` and `ProfileWrites`
   expose no function that takes a name and returns a `PlayerId`, a `DeviceId`, an `AuthSession` or a
   profile"* — which is why searching the record by opponent name returns **duels** and never
   players. There is, today, no way in this product to look a person up. A row that leads somewhere
   is the first one.
2. **The pull is real, and it is the reference product's own gesture.** *"The reference points are
   **Lichess** and **Chess.com**"*, and on both of them every leaderboard row is a link to the person
   standing on it. Tapping a name is the most natural thing anybody does on a ladder. A ladder where
   it does nothing is a worse ladder in exactly the way the vision's own comparison invites.
3. **What is behind the tap is not one disclosure but five, and each is separately a commitment.**
   The register lists them: display name, all-time coin balance, duels played, win/loss record, the
   duel list. The last is the heaviest by a distance — `GET /api/me/duels` serves it only to its
   owner today, and every summary in it names the *other* player of every duel
   (`opponentPlayerId`, `opponentDisplayName`), so publishing one player's history also publishes
   fragments of the history of everybody they ever played. There is no version of *"a row leads to a
   profile"* that is a single decision.
4. **This product has none of the apparatus that makes a public profile ordinary elsewhere.** No
   opt-in: `ADR-0063` §1 puts a player on the ladder for finishing one duel, and nobody is asked. No
   opt-out: there is no setting, and a place cannot be given up. No deletion:
   [`ADR-0039`](ADR-0039-v01-offers-no-account-deletion.md) — *"no delete path, no request flow, and
   no tombstone"*. And the subject may never have signed up for anything at all: `ADR-0012` makes a
   profile a device id, `ADR-0036` makes an account *"offered, never required"*, and the default
   player has no name and no credential. A site that links a leaderboard row to a person has usually
   bought that link with a sign-up, a privacy setting and a way out. Copying the link without the
   apparatus copies the wrong half.
5. **A disclosure cannot be withdrawn once it has been read.** `ADR-0036`'s own sentence —
   *"withdrawing a capability players have is worse than never offering it"* — cuts both ways here,
   and the second way is worse: taking a **page** away annoys the reader, while taking a
   **publication** back is not possible at all, because the reading already happened.
6. **The roadmap has already sorted it.** v0.3 is *"Leaderboard and seasons"*; v0.4 is *"Friends,
   statistics, replay viewer"*. `EPIC-05` reads that the same way in its own out-of-scope table —
   *"Friends, rivals, head-to-head statistics, the replay viewer | v0.4"*.
7. **The evidence is zero.** No player has read a ladder row that was not in a test. There is no
   report of anybody wanting to know who rank 1 is, because there is nobody.

### The deadline, honestly

**One option was already gone before this decision was taken, and it is worth naming so nobody
proposes it.** `STORY-0502` merged with `playerId` on every `StandingRow`, and `STORY-0503` merged a
screen that uses it as a list key. Never handing a stranger an id at all is therefore no longer free:
it is a change to a merged wire contract, and `ADR-0066` §3 makes `player.id` the paging key that a
walk cannot work without. What **is** still free — and what this decision spends — is whether that id
ever becomes an address.

Nothing else here is expensive in either direction. The screen already refuses to link
(`TASK-050313` asserts `queryAllByRole("link")` and `querySelectorAll("a")` are both empty inside the
section), so *inert* costs no code, and *leads somewhere* would cost a new endpoint, a screen and
`DEC-054`. What forced the timing is that `STORY-0504` is the epic's last unsplit story and
`EPIC-05` cannot close with it neither built nor buried.

## Decision

### 1. A leaderboard row is text. It leads nowhere

A row on the ladder is a rank, a name or `No name`, and a season standing, rendered as one line —
`4 Ada 3`, `215 No name −1`. It is not a link, not a button, and not a control of any kind. Tapping
it does nothing, because there is nothing to tap.

This confirms what `STORY-0503` already ships rather than changing it. `TASK-050313`'s assertion —
no `<a>`, no `link` role, no `button` anywhere inside the leaderboard section — was written *"until
`DEC-057` is answered"*. It is now the decision, and it stays.

### 2. What a stranger reads about another player, in full

Exactly four things, and they are the row and its wire form:

| Field | What it is |
| --- | --- |
| `rank` | The competition rank of `ADR-0064` §1, for this season |
| `displayName` | The name the player chose, or `null` — printed by `nameOrNone` as `No name` (`ADR-0058`) |
| `coins` | The season standing of `ADR-0061` §4, signed, negative when it is (`ADR-0014`) |
| `playerId` | An opaque identifier that names a row — see §4 |

That is the whole of what one player may learn about another from this product without playing them.
It is not a new disclosure: every one of the four already ships. What is new is that it is now the
**boundary**, deliberately, rather than the set of fields the first story happened to need.

### 3. What a stranger does not read

Named field by field, because a list of what is permitted is only half a rule, and because a story
asserting absence needs something to enumerate:

- **The all-time coin balance.** `player.coin_balance` is the profile strip's number
  (`ADR-0065` §2) and it belongs to the player it counts. The ladder's number is a month's window
  and is the only coin figure a stranger sees.
- **Duels played, wins, losses, draws** — a count of duels, a win rate, a record, or anything from
  which one can be derived, including the ladder total (`ADR-0065` §7 refused that for the reader's
  own line, and this is the same refusal aimed outward).
- **The duel list.** `GET /api/me/duels` keeps serving exactly one player: the one who asked. It
  gains no player parameter, no `opponentId` filter, and no second route — and the reason is force 3,
  not tidiness.
- **Streaks, form, movement, last seen, whether they are online, whether they are in a duel now.**
  None of these exists; none is created here.
- **Anything about their account:** whether they hold a credential, a handle, an email, a device
  binding, a session. And `displayNameRemoved`, whose contract already says it *"never says anything
  about another player"* (`ADR-0053`).
- **Anything from inside a duel**, which needs no restating but gets one: no hole cards, no folded or
  mucked cards, anywhere, ever.

### 4. No id turns into a profile

`playerId` stays on the wire, and it is **a name for a row, never an address**.

- **No route takes a player id as its subject.** The server serves five paths — `/api/me`,
  `/api/me/name`, `/api/me/duels`, `/api/auth/sign-up`, `/api/standings` — and not one of them
  answers *what about player X*. This decision adds none and permits none.
- **A client may use the id for what an id is for**: a stable list key, and correlating the same
  player across two responses it already holds — the duel summary's `opponentPlayerId` and the
  ladder row are the same string on purpose (`ADR-0021`). Correlating what you were already told is
  not a lookup.
- **`ADR-0029` §7, from the other side.** §7 keeps a **name** from resolving to an identity, which is
  why history search returns duels. This keeps an **identity** from resolving to a person. The two
  together are one sentence with no gap in the middle: **no path in this product turns anything into
  a lookup of a player.** A leaderboard hands out names and ids together, which is the one place that
  rule could have been walked around, and this is the decision not to.

### 5. `STORY-0504` is dropped

Its premise — that a row leads somewhere — is false, so there is nothing left in it to build. The
file stays and records the decision that killed it, as `STORY-0505`'s does. `EPIC-05` is four live
stories and its critical path is unchanged: `0501 → 0502 → 0503 → 0506`.

Nothing in `STORY-0504` moves to another story. Every design note it carried was a constraint on a
thing that is not being built, and its one testable assertion that survives — *a row leads nowhere* —
already ships in `TASK-050313` and is now §1.

### 6. What reopening this requires, so that it is reopened rather than filled in

This is the cheapest sentence here to reverse and it is deliberately written to be reversed **as a
decision**, not as a ticket. Anyone who wants a row to lead somewhere:

- Raises a new `DEC` and names **the exact field or fields**, one at a time. *A player page* is not a
  proposal; *a player page showing duels played* is.
- Answers force 4 — what a subject is asked, what they can turn off, and what happens when they want
  it gone — or records in writing that the answer is *nothing*, *nothing* and *nothing*, the way
  `ADR-0063` §5 recorded its own accepted risk with the event that expires it.
- **Takes it to the human if the field is not already public.** Publishing a fact about a player that
  the player did not choose to publish, on a product with no opt-out and no deletion, is where a
  product decision starts having consequences outside the software. The decision recorded here needed
  no such escalation because it publishes nothing new; the opposite decision may.

The direction of the asymmetry is the whole reason this is the answer: adding a page later is
additive and costs a ticket, while un-publishing a record is not a thing that can be done.

## Consequences

**What it buys.**

- **The product keeps one rule instead of two.** *No path turns anything into a lookup of a player*
  is now true across the whole read surface and is testable at the route table: five paths, none with
  a subject that is not the caller. `ADR-0029` §7 gains a matching half rather than an exception.
- **`EPIC-05` ships without a privacy surface.** The epic that puts every player's name and standing
  on an unauthenticated page adds nothing further about anybody, so `EPIC-07`'s re-affirmation of
  `ADR-0063` §5 has exactly one disclosure to weigh and not six.
- **`STORY-0504` is buried rather than left open**, which is worth more than it sounds: an open story
  attached to an unanswered question is where a coder invents an answer.
- **No code, no migration, no wire change.** The decision is already implemented, which is the only
  circumstance in which that sentence is a virtue rather than a warning.

**What it costs.** The first one is the real one.

- **The ladder is a list of strangers and the product gives a player no way to ask who any of them
  are.** You lose a duel, open the leaderboard, see the name that beat you sitting at rank 3 — and
  the gesture every ladder on the internet has trained you to make does nothing. The vision's own
  vocabulary includes *rival*; a rival you cannot look at is a row. This is a genuine loss against
  the product the vision names as its reference point, and it is taken knowingly rather than
  discovered later.
- **`DEC-054` loses its sharpest argument.** `EPIC-05` wrote that this epic *"strengthens the case
  for answering `DEC-054`: a leaderboard row that leads to another player (`DEC-057`) is a link, and
  a client with no addresses cannot express one."* That case is gone. The client can stay
  address-less through another milestone with one fewer reason to fix it, and this decision is what
  made the deferral comfortable. `DEC-054` is not answered here and its other reasons stand
  untouched, but it just got easier to keep putting off, which is a cost of this decision and not a
  benefit of it.
- **The question is deferred, not solved, and it gets harder rather than easier.** By v0.4 there will
  be players with histories and a *Friends* feature that has to say what a friend can see. The
  proposal will arrive as a ticket — *just add a player page* — and §6 is the only thing standing
  between that ticket and six unexamined disclosures. §6 is prose, and prose is a weaker guard than
  a schema.
- **The asymmetry is deliberate and uncomfortable.** A player is published on a ladder they never
  opted into and cannot look up anybody else, including the person who just beat them, while the
  server holds all of it and the operator can read any of it. That is a choice against the player's
  curiosity and in favour of every other player's exposure, and it is only defensible while the
  ladder itself stays as small a disclosure as it is.
- **v0.3 ships with no social surface whatsoever.** With rows inert, the first thing in this product
  that connects one player to another outside a duel is v0.4's *Friends*. A ladder is now the one
  place two players coexist, and they cannot see each other there.

**What it forecloses.**

- **Nothing structurally** — and that is the point. Every option remains open at the price of a
  decision, which is the correct price.
- **A head-to-head or rival view inside `EPIC-05`**, including the cheap version that discloses
  nothing new (filtering the reader's *own* duel list by opponent id, which the reader already has).
  That is a small ticket and a defensible one, and it still does not happen here: it is v0.4's row on
  the roadmap, and it is a different question from the one that was asked.
- **Any v0.3 feature that needs to identify a person from the ladder** — reporting a player,
  challenging one, following one, messaging one. Each was already out of scope somewhere; §4 is now
  the single reason rather than five separate ones.

## Alternatives considered

**1. A row links to a player page carrying the row's facts plus their duel list.** The maximal,
Lichess-shaped answer, and the strongest one on the merits of the moment: it is what the reference
product the vision names actually does, it answers the question every ladder reader has, it makes
*rival* mean something, and the data all exists — the page is one query and the endpoint is a
morning's work. It also happens to be what a coder would have built if this question had been left
open long enough. Rejected on forces 3, 4 and 6, in that order. It is five disclosures in one, not
one; the heaviest of them republishes fragments of every opponent's history alongside the subject's;
it is *statistics*, which the roadmap places in v0.4, so taking it now would be reordering the
roadmap rather than applying it; and it lands on a product with no opt-in, no opt-out and no
deletion, where the subject is very often somebody who has never signed up for anything and whose
whole account is a device id. Lichess links a row to a person **because** it has an account, a
privacy setting and a closure path behind it. Copying the link and not the apparatus is copying the
wrong half.

**2. A row links to a minimal page carrying only the three facts the row already carries.** The
sharpest alternative, and the one that took the longest to reject. It discloses **nothing new** —
literally the same fields, rearranged — so every privacy objection above evaporates; it settles the
*shape* now while it is free, so a later field is one line rather than an architecture; and it gives
the tap somewhere to land. Rejected because the disclosure was never the expensive part: **the
address is.** A route that answers *what about player X* is the thing that every later field gets
added to, and it gets added to it inside a ticket, because by then the hard question looks answered.
The page also buys the player nothing on the day it ships — a screen that repeats the line you just
tapped is a worse version of that line — so it would arrive with no argument for existing except the
one it forecloses. And it needs `DEC-054` or another address-less in-client swap, which means paying
`ADR-0060`'s cost again for a screen with no content.

**3. A row links to your head-to-head with that player — the duels the two of you have played.**
Genuinely attractive, and the only option here with a real claim to zero new disclosure: those duels
are already in the reader's own `GET /api/me/duels`, already carry that opponent's id and name, and
that endpoint's `opponent` parameter already filters them by name. It is a filter on a list the
reader owns, not a window into somebody else's. Rejected on scope rather than on privacy: *rivals
and head-to-head statistics* is v0.4's roadmap row, `EPIC-05` puts it out of scope in those words,
and it is not an
answer to the question that was asked — *what does a row lead to* was asked about the **subject** of
the row, and answering it with a page about the **reader** would settle a different question quietly.
Recorded because it is the cheapest thing on this list and the obvious place to start if a row is
ever given somewhere to lead.

**4. Take `playerId` off the wire, so no id is handed to a stranger at all.** The maximal-privacy
answer and the natural extension of `ADR-0029` §7: an identifier you never publish can never become
an address, and it would make §4 structural instead of a promise. **Not available, and it is worth
knowing why.** `ADR-0066` §3 makes `player.id` the paging key — *"forced rather than chosen"*, the
only key that is identity, unique and immutable — so a walk cannot be expressed without it; it is on
a merged wire contract that `STORY-0502` shipped; and `ADR-0021` already hands the same string to
every opponent a player faces. It would also buy less than it appears to: the id names a row and
opens nothing, which is §4. This is the option the deadline took away, recorded so that it is not
re-proposed as though it were free.

**5. Escalate to the human.** The register itself warns that exposing one player's record to another
sits closer to the human's line than anything else in this epic, and forces 4 and 5 are exactly the
shape of *risk with consequences outside the software*: a published record about a person who did not
opt in, on a product with no deletion. If the answer had been any of alternatives 1 to 3, this would
have been the right move. It is not the right move for **this** answer, because this answer
**publishes nothing** — it declines to publish, and declining needs no license from anybody. An
escalation here would have spent the human's attention to be told to keep doing what the code already
does. What is escalated instead is the *direction*: §6 requires the human's call before any field
about a player that the player did not choose to publish is added, which puts the question in front
of them at the moment it is a real question and not before.

## What this does not settle

- **`DEC-054`** — whether the client grows URL-addressable routes and a working browser *Back*. The
  architect's, untouched, and deliberately not pre-empted. This decision removes one argument for it
  (see *Consequences*) and answers no part of it.
- **`DEC-060`** — whether a finished season is ever reachable from a screen. The product owner's, and
  untouched. A past season's ladder would be rows under this same rule, whatever the answer.
- **`DEC-008`** — whether the full `MatchLog` is persisted, and therefore whether a duel can ever be
  replayed. Untouched. A replay is a different disclosure with a different subject — a hand, not a
  person — and `EPIC-08` will have to ask its own version of this question.
- **Whether the ladder itself should be public at all.** Already accepted in writing by `ADR-0063`
  §5, with an expiry event rather than a date: the first time the ladder is served on a public
  address. That re-affirmation is `EPIC-07`'s and this decision neither strengthens nor weakens it —
  it only guarantees there is one disclosure to weigh there and not six.
- **What a friend may see, in v0.4.** *Friends* is on the roadmap and will need this question asked
  again with a different subject, since a friend is not a stranger. Nothing here decides it, and §6
  applies to it.
- **Whether a player may ever be reported, blocked or muted.** No surface in this product needs it
  today because no player can reach another outside a duel. That stops being true the first time one
  can, and it is a decision nobody has raised yet.
