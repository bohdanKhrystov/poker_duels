# ADR-0052 — A takedown is told to the player it happened to, and to nobody else

- **Status:** Accepted
- **Date:** 2026-08-17
- **Resolves:** `DEC-046` — is a player told that their display name was taken away, and if so by what
  and in what words? **Derived from the vision; the human did not state this call.** The licensing
  sentences are the vision's *"Poker is not a game of pure skill and we are not going to pretend
  otherwise"* — the product's stated posture is to tell a player the uncomfortable true thing rather
  than manage their impression of it — and, for the form it takes, *"The reference points are Lichess
  and Chess.com, not PokerStars. Dark, quiet, fast, minimal."* Read beside the shipped instance of
  that posture in [`ADR-0029`](ADR-0029-a-display-name-is-unique-and-permanent.md) §5, where `PUT`
  answers `200` with the whole profile rather than `204` because *"the client must be **told** the
  exact string it now owns rather than assume it got what it sent"*, and beside `STORY-0411`'s
  existing design note that the screen must state permanence **before** the send because *"a player
  is entitled to know that at the moment they can still avoid it"*
- **Amends:** [`ADR-0051`](ADR-0051-a-name-is-registered-before-it-is-held.md) §6's *"the technical
  default is therefore silence"*, which that ADR took in the reversible direction pending this
  decision, and §1's *"`retired_from` is record-keeping … nothing in production reads it"* — one bit
  derived from it now reaches the player it belongs to. Everything else in §1 stands: no name from
  the registry crosses the wire, nothing is enumerated, and no response says anything about another
  player. Its §2 `409`, its `SetNameResult`, its trigger, its function and its migration are
  untouched
- **Builds on:** [`ADR-0038`](ADR-0038-a-name-is-screened-when-set-and-can-be-taken-away.md) (a name
  can be taken away and is retired forever, *"including the player it was taken from"*, and the
  profile *"may return to unset and be asked to choose again"*),
  [`ADR-0029`](ADR-0029-a-display-name-is-unique-and-permanent.md) §5 and §6 (the two failure states
  the client renders, and the rule that the server fabricates nothing for `null`),
  [`ADR-0036`](ADR-0036-an-account-is-offered-never-required.md) (a nameless, account-less player is
  a full participant, so nothing here may gate play),
  [`ADR-0039`](ADR-0039-v01-offers-no-account-deletion.md) (why the opponent's history row loses the
  name at all)
- **Constrains:** `STORY-0410` (the profile read gains one fact; the write path, the endpoint and the
  status codes gain nothing) and `STORY-0411` (a fourth state on the name surface, and three pieces
  of copy — one new, two corrected)
- **Adds no column and no table.** `ADR-0051` §1's `name_registry(reason = 'RETIRED', retired_from)`
  already records the fact; this decision reads it
- **Raises:** `DEC-047`, the architect's — by what shape `GET /api/me` carries that fact
- **No wire change.** `PROTOCOL_VERSION` does not move: no socket message carries a display name and
  nothing here is a socket fact

## Context

`ADR-0051` shipped the mechanism and stopped exactly where the schema stops. Its §6 records the
residual precisely: the profile returns to *unset*, `GET /api/me` answers `displayName: null`, the
old name is refused with `409` like any other spent string, and **nothing is pushed**, because this
server has no asynchronous channel to a player who is not inside a duel socket. Silence was taken as
the default because it is the reversible direction, not because it was argued for.

Five forces pull on what is left, and they do not point the same way.

**The product has committed to telling players uncomfortable true things, and to being quiet about
everything.** The vision refuses to pretend about variance and prefers *"showing a player that they
lost the match but made the better decisions"* to *"hiding the maths"*; it also fixes the register as
*dark, quiet, fast, minimal*. A moderation notice is the exact point where those two pull apart. One
says speak; the other says a poker product does not open with a banner about enforcement.

**Silence here is not a neutral state, because the player's next move is a `409`.** A player whose
name has vanished has one obvious thing to try: type it back in. `ADR-0051` §2 answers `409` for a
retired name deliberately and correctly — no answer says which source refused. But `STORY-0411`
already plans to render `409` as *taken*, and *taken* means *somebody else has it*. That is a false
statement about a string the server knows nobody holds and nobody ever can. The silent state has
exactly one misreading available and every affected player reaches it.

**Whatever is said has to be true of the mistake.** `ADR-0051`'s first recorded cost is that an
operator's mistake is unfixable: the interlock is a confirmation, and a takedown against the wrong
profile is permanent. So the copy cannot accuse, and it cannot offer a remedy — which strips a
moderation notice of the two things one normally contains, and leaves the question of what is left.

**There is nothing to say *why*.** `ADR-0051` settles that there is no actor, no reason and no log:
*"There is no actor, no reason and no log, because there is one operator."* A product cannot display
a reason it did not record, and one composed at render time would be the product asserting something
about a person that nobody wrote down.

**There is no channel and no inbox.** Nothing is pushed; most players have no account
(`ADR-0036`), most have no email (`ADR-0031` makes it optional), and nothing in this product accepts
a message from a player. So the telling can only happen where the player already looks, and it can
only ever be a statement — never the start of a conversation.

### The deadline, honestly

**There is no date, but there is a window that closes cheaply.** `STORY-0410` and `STORY-0411` are
both unsplit — `STORY-0411`'s task table still says *"Not yet split"*. Today this decision costs one
criterion in each story. Once `STORY-0411` has shipped a name surface with three states and the
sentence *"That name is taken"*, changing it is a second ticket, a copy audit and a screen a player
has already read. That is a reason to decide now. It is not a reason to decide either way.

## Decision

**The player whose name was taken away is told, on the surface where a name is set, in four
sentences that give no reason and offer no remedy; and nobody else is told anything.**

### 1. The player is told, and the telling is derived from state

The name surface shows a notice when — and only when — the requesting player **holds no display
name** *and* **at least one name has been retired from them**. Both halves matter: a player who has
since chosen a new name sees nothing, and a player who never set one sees nothing.

- **The telling is derived, never delivered.** It is a function of the profile the client already
  fetches, so there is no notification, no queue, no delivery attempt, no *seen* flag, no
  acknowledgement round trip and nothing to dismiss. It cannot be missed once and then be gone
  forever, and it costs no state to remember that it was shown. This is what keeps the answer inside
  `ADR-0051`'s promise that it is *"a screen and a piece of copy rather than a column"*.
- **It stays until the player acts.** Setting a new name ends it, because the first condition stops
  holding. Nothing else ends it.
- **It appears where the player would go to fix it** — the name surface `STORY-0411` owns, alongside
  the form that sets a name. Not a modal, not an interstitial, not a toast, and **never over the
  duel table**. A player at a table is playing; the vision's *quiet* is the whole of the argument,
  and there is nothing here to act on mid-duel.
- **It is scoped to the requesting player, about themselves.** `GET /api/me` already answers only
  for the caller. No other player's takedown is readable by anybody, from any endpoint.

### 2. The words

The notice, in full:

> **Your display name was removed.**
>
> A person running Poker Duels removed it — not a bug, and not another player. That name cannot be
> used again, by you or by anyone. Choose a new one whenever you like.

These are the shipped strings. Each sentence exists because removing it produces one specific wrong
belief, and any future edit keeps all four properties:

| Sentence | What its absence would cause |
| --- | --- |
| *Your display name was removed.* | The player sees `null` and cannot tell it from *never set* |
| *A person … removed it — not a bug, and not another player.* | *The site is broken* or *I was hacked* or *someone took my name* — the three inferences a vanished name invites |
| *That name cannot be used again, by you or by anyone.* | The player retypes their own name and is answered `409` |
| *Choose a new one whenever you like.* | The player believes they are locked, or waits for something |

**What the words deliberately do not do.** They do not accuse, because a takedown may have been a
mistake and the product has no way to know which one this was. They do not apologise, because an
apology for the deserved case is dishonest and an apology for the mistake is worthless without a
remedy. They do not say *banned*, *suspended*, *violation*, *report* or *appeal* — none of those
names anything that exists here. They do not repeat the removed string: in the deserved case the
product would be redisplaying a name an operator judged unacceptable, back to the person who chose
it, indefinitely; and in the mistaken case the player knows perfectly well what it was.

### 3. No reason is given, because none is recorded

The notice says nothing about why, and the product offers no way to ask. This is not a stylistic
choice — `ADR-0051` stores no actor, no reason and no log, so there is literally nothing to render.
Giving a reason would mean an operator typing one at takedown time, a column to hold it, and that
text becoming the product's voice at the worst moment it will ever have with a player. That is a
different decision with a schema shape, and it is not this one's default.

The honest reading of the result is stated here rather than discovered: **a player is told that a
person did this to them and is given no way to find out why or to contest it.** That is the true
state of the product, and the alternative on offer was not *a better answer* but *the same state,
unspoken*.

### 4. A new name may be set immediately, and nothing is withheld

`PUT /api/me/name` works the instant the takedown commits (`ADR-0051` §6), and this decision adds no
hold, no cooling-off period, no rate limit and no confirmation. The player is invited to choose again
in the notice's last sentence, exactly as `ADR-0038` describes — *"may return to unset and be asked
to choose again"*.

Nothing else is withheld either: the profile keeps its coins, its history and its leaderboard place,
and a nameless player remains a full participant (`ADR-0029` §6, `ADR-0036`). A takedown removes a
name and nothing else, and the product says so by behaving that way.

### 5. Nobody else is told, and the state is invisible from outside

Every history row on every other player's screen that named this profile now renders `null`, and it
renders **exactly as any nameless opponent does** — `STORY-0411`'s treatment for a player who never
set a name, unchanged, unmarked, unexplained. No badge, no *name removed*, no tooltip, no distinct
styling.

- **`DuelSummaryResponse` gains nothing**, and this is a criterion rather than an omission: the fact
  that a takedown happened is readable by the player it happened to and by nobody else.
- The other player's record silently degrading is a cost `ADR-0051` already recorded and `ADR-0039`
  already caused by forbidding the denormalised name. This decision declines to convert it into a
  feature. Marking those rows would publish a private consequence onto strangers' screens,
  permanently, and it could not be done honestly without the reason §3 does not have.
- **To the player themselves the two `null` states are distinguishable; to everyone else they are
  not.** That asymmetry is the decision, and it is the half that protects the person who lost the
  name.

### 6. What this needs: one bit, no column

**No column is added, and the reversible default was not overturned for lack of somewhere to put
the fact.** `ADR-0051` §1 already stores it: a player has had a name taken away exactly when a
`name_registry` row exists with `reason = 'RETIRED'` and `retired_from` equal to their id. What
changes is that a production read path consults it for the first time, which is why this ADR amends
§1's *"nothing in production reads it"* rather than pretending it did not say that.

What crosses the wire is **one boolean about the caller, and no string**. The registry is still not
enumerated, no response carries a name from it, and nothing about another player is exposed.

**The shape of that read is the architect's, and is registered as `DEC-047`** — whether it is a field
on `ProfileResponse` (as `ADR-0049` §5's `deviceRouteLive` is), whether the profile query joins or
subqueries `name_registry`, and whether `ADR-0051` §1's structural refusals need a formal amendment
or are satisfied by one bit about the requesting player, to the requesting player. This ADR fixes
that the fact is available on the profile read and fixes nothing about how.

### 7. What `STORY-0410` and `STORY-0411` gain, concretely

**`STORY-0410` gains one thing, on the read path:**

- `PostgresProfileReads.profileOf` answers whether a name has been retired from the requesting
  player, in the shape `DEC-047` fixes.
- Two criteria, from two distinct fixtures: a player whose name was retired reads *true*; a player
  who never set one reads *false*. One fixture cannot tell a copy from a constant.
- One negative criterion: a duel summary line for an opponent whose name was retired is
  byte-identical to one for an opponent who never set a name.
- It gains **nothing** on the write path. `SetNameResult` keeps three cases, `PUT /api/me/name` keeps
  `200`/`400`/`403`/`409`, `retire_display_name` takes no third argument, the operator types no
  reason, and no `docs/operations.md` step is added.

**`STORY-0411` gains a fourth state and three pieces of copy:**

- **The fourth state.** The name surface today has two: *has a name* and *has none, offer to set
  one*. It gains *has none, and one was removed* — §2's notice above the same form.
- **The `409` sentence is corrected.** It may not name a holder, because for the one player most
  likely to trigger it there is none. It reads **"That name is not available. Try another."** —
  never *taken*.
- **The permanence line is corrected**, because otherwise the screen contradicts the notice a player
  may later read on it. Where `STORY-0411` requires the screen to state permanence before the send,
  it states permanence *to the player*: **"A name is chosen once. You cannot change it later, and it
  can be taken away."** `ADR-0038` turned permanence into *permanence to the player* and no screen
  has yet said so; this is the smallest copy that is true for everybody, not only for the player who
  has already lost one.
- Its `403` sentence, its canonical-string round trip, its null treatment and its no-derivation rule
  are untouched.

### 8. What is deliberately not built

- **No notification, no inbox, no email, no push, no badge, no unread count, no dismissal.**
- **No reason, no appeal, no contact route, no report form.**
- **No mark on anybody else's screen**, and no change to `DuelSummaryResponse`.
- **No hold, no cooling-off, no restriction** on the next name a player chooses.
- **No new endpoint**, no admin surface, and no enumeration of the registry.
- **No re-telling.** One state, one notice, shown while it is true and gone when it is not.

## Consequences

**What it buys.** The one state a player could not form a true belief about becomes one they can:
their name is gone, a person did it, the old one is finished, and they may choose again. The product
stops being about to tell a player that their own retired name belongs to somebody else, which is
the concrete defect `STORY-0411` was on course to ship. `ADR-0038`'s *retired forever, including the
player it was taken from* becomes a sentence the player has actually read rather than a rule they
discover by being refused. And the answer costs no notification machinery at all: it is derived from
state the schema already holds, so there is nothing to deliver, nothing to acknowledge and nothing
to keep in sync.

**What it costs.**

- **The player is told something final that they can do nothing about.** No reason, no appeal, no
  address to write to. Silence at least left the possibility that they had misremembered; this
  replaces it with a flat, unarguable statement in the product's own voice. For the player who
  deserved it this is fine. For the player who did not, the product now confirms the mistake to
  their face and declines to fix it, which is worse than not knowing, and it is the cost this
  decision most deliberately accepts.
- **The notice raises the one question the product refuses to answer.** *Why?* is the first thing
  every reader will think, and §3 guarantees there is no second screen. A player who wants to ask
  has nowhere to go, because nothing here accepts a message. That gap is now visible where it was
  previously invisible.
- **A player who never opens the name surface is still never told.** There is no push, so silence's
  failure mode survives for anybody who does not look. This narrows the gap; it does not close it,
  and an answer that closes it needs a channel that does not exist.
- **`GET /api/me` pays for a rare state on every call.** The profile read gains a fact almost every
  player will never be in, on the hottest profile query, forever. Small; paid by everybody.
- **`ADR-0051` §1's *"nothing in production reads `retired_from`"* is spent.** One bit, to its own
  player — but the sentence is no longer true, and the next person who wants to put something from
  `name_registry` on a response has one fewer structural reason not to.
- **The copy is a commitment.** *"That name cannot be used again, by you or by anyone"* is now
  printed on a screen. Any future un-retire, expiry or name recycling makes the product a liar to
  people who already read it, on top of needing the ADR `ADR-0051` already requires.
- **`STORY-0411` gains a state and loses a simpler sentence.** Three states become four, and *"That
  name is taken"* — the clearer, friendlier, more informative sentence — is replaced by a vaguer one
  for every player, to avoid lying to a few. That trade is deliberate and it is a real loss of copy
  quality on the common path.
- **The word *removed* is the product's, forever.** A takedown now has a public vocabulary, and the
  next feature that removes anything from a player inherits it or contradicts it.

**What it forecloses.**

- **A quiet takedown.** An operator can no longer take a name away without its subject being told.
  There is no discreet mode, no *remove and say nothing* — which is a capability the operator had
  under silence and might have wanted for a name that was removed precisely because attention to it
  was the harm. That option is closed; the mirror of `ADR-0051` §5's refusal to express *blocked but
  still displayed*.
- **Telling anyone else, cheaply.** Marking history rows is now a decision that would have to
  overturn §5 rather than a gap somebody fills.
- It does **not** foreclose a reason, an appeal, a contact route or a delivered notification. All
  four stay additive: each needs something that does not exist yet — a column, a surface, an inbox,
  a channel — and none is made harder by this.

## Alternatives considered

**Silence — `ADR-0051` §6's default, kept.** Its case is genuinely strong and it is the one this ADR
had to beat. It costs nothing, it ships today, it is the most reversible option available since
telling later is purely additive; the state is already visible to anybody who looks, so nothing is
concealed; it spares the product from pronouncing a final unappealable verdict on a person; it
leaves `ADR-0051` §1 intact and `GET /api/me` unchanged; and it avoids re-surfacing, on the player's
own screen, an incident they may prefer not to be reminded of. Rejected because silence is only free
when the silent state has no wrong reading, and this one has exactly one: the player retypes their
name and is answered `409`, which `STORY-0411` renders as *taken*. The product would be telling a
player that a string nobody holds and nobody ever can belongs to somebody else. A product cannot
hold *we are not going to pretend otherwise* and also do that.

**Silence, with the refusal copy fixed** — ship nothing new, but say *"That name is not available"*
instead of *"taken"*. The strongest of the rejected options, and it very nearly won: it removes the
lie for the price of two words, needs no server change, no read of `name_registry`, no fourth client
state and no copy about moderation anywhere in the product, and it remains perfectly reversible.
Rejected because it leaves the player holding a state they cannot explain: their name is gone, the
product behaves as though they never had one and invites them to choose their *first* name, and the
single name they try comes back refused with no reason. That is a product that knows something about
the player, keeps talking to them about it, and declines to say it — which is a worse posture than
saying nothing, and it is *hiding the maths* in the vision's own words. It also leaves the *I was
hacked* and *the site is broken* readings uncorrected, and a player who believes either of those
leaves.

**Tell, and give the reason.** Its case is the strongest on the merits: a removal without a reason
is indistinguishable from an arbitrary one, and arbitrariness is corrosive in exactly the product
whose positioning is quiet competence. It is what every mature moderation system does, and *why* is
the only question the player actually has. Rejected because there is no reason to show:
`ADR-0051` records no actor, no reason and no log, so a reason would have to be typed by the
operator at takedown time, stored in a column, and printed as the product's voice. It also makes the
mistaken takedown strictly worse — the operator who removed the wrong name would have written down a
reason that is false about that player, and the product would publish it to them. If a reason is
ever wanted it arrives with a place to put it, an operator obligation to fill it, and its own ADR.

**Tell at sign-in, or as a one-time notification with a seen flag.** Its case is the one thing this
decision does not deliver: guaranteed delivery. The player sees it once, deliberately, rather than
only if they visit the name surface — which is the direct answer to *possibly never if they do not
look*. Rejected on three counts. Most players never sign in at all (`ADR-0036` — an account is
offered, never required; `ADR-0012`'s device profile has no sign-in), so it would reach exactly the
players who need it least. *Seen once* needs a column and an acknowledgement round trip, which is
precisely the state `ADR-0051` was careful not to add and which `DEC-046` was framed to avoid. And a
dismissible notice is gone after one glance while the state it describes is permanent — derived
telling costs nothing to remember and cannot be missed twice.

**Tell everybody — mark the history rows that lost the name.** Its case is the honest treatment of
`ADR-0051`'s second recorded cost: a takedown silently rewrites other people's records, and somebody
who beat a named rival is entitled not to have their own history quietly degrade under them.
Rejected because it converts a private consequence into a permanent public mark on strangers'
screens — a scarlet letter rendered by the product, about a player, to people who had no part in it
and no way to judge it. It cannot be written honestly without the reason §3 does not have, and the
alternative wording (*this player's name was removed*) is an accusation the product cannot support
in the mistaken case. The opponent's row renders as any nameless opponent's does, and the degraded
record stays a cost `ADR-0051` recorded rather than a feature this ADR builds.

**Prevent the player setting a new name — a hold, a cooling-off period, or an operator-lifted
block.** Its case is real and is about the deserved takedown: an operator who removes a name for
cause has done nothing to stop the same player choosing an equally bad one five seconds later, and
`ADR-0051` §5 ships the blocklist empty, so the screen refuses nothing. A hold would at least make
the second attempt deliberate. Rejected because it is punishment machinery with nobody to operate
it: there is one operator, no role system (`ADR-0038` refused to grow one), no appeal, and no timer
anyone could shorten for the player it was applied to by mistake. `ADR-0038` says the profile is
*asked to choose again*, and being asked to choose again is not a wait. The operator's remedy for a
second unacceptable name is identical to the remedy for the first, and it costs one function call.

**Echo the removed name in the notice** — *"Your display name «Bobb» was removed."* Its case is
clarity: a player with any doubt about which name is meant has none, and it is warmer than the
impersonal version. Rejected because in the deserved case the product would be redisplaying, on the
player's own screen and for as long as they remain nameless, the exact string an operator decided
was unacceptable — and in the mistaken case the player did not need to be told what their own name
was. It also puts a registry string on the wire, where §6 keeps the answer to one boolean, and it
raises a question nobody has asked about what a player with two retired names sees.

## What this does not settle

- **A statement of reasons, if an obligation to give one ever arrives.** If a legal or regulatory
  requirement to explain a content removal applies to this product one day, this ADR is not the
  obstacle — the *record* is, since `ADR-0051` stores no reason and no actor. Meeting it would be a
  column, an operator obligation and a schema change, and it is the human's to require, not this
  ADR's to anticipate.
- **A contact route.** Nothing in this product accepts a message from a player, and this decision
  does not create one. Whether it ever should is a surface the vision does not mention.
- **The exact wire shape of the fact** — `DEC-047`, the architect's.
- **Anything about `STORY-0412`'s account screens.** The notice lives on the name surface; whether
  the account screen ever repeats it is not decided here, and today the answer is that it does not.
- **What a nameless player looks like at all.** `ADR-0029` §6 left that to the client and
  `STORY-0411` still owns it. This ADR adds a notice above the form; it does not choose the
  treatment the notice sits above.
