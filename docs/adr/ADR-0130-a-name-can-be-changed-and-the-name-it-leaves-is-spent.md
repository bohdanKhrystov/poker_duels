# ADR-0130 — A name can be changed, and the name it leaves behind is spent

- **Status:** Accepted
- **Date:** 2026-09-07
- **Resolves:** `DEC-131` — may a display name be **changed after it is set**, and what happens to
  the one given up: released for anyone to take, held by its former owner, or retired?
- **The answer has two halves and only one of them is chosen here.** ***That* a name may be
  changed is the human's, recorded verbatim** — 2026-09-06, `EPIC-14`'s source message: *"player
  name can be changes at any time in accounts settings"*. It reverses the human's own earlier
  answer to `DEC-017` (*"unique and permanent … chosen once … and never changed"*), and this ADR
  does not choose it, argue it or soften it; §1 records it. ***What becomes of the string* is
  derived from the vision** — *"**A leaderboard.** Ranked results over a season."* — read as
  [`ADR-0067`](ADR-0067-a-leaderboard-row-is-text-and-no-id-turns-into-a-profile.md) §1 already
  read it: **a row is text and leads nowhere**, so the display name is the whole of the identity a
  leaderboard row has. A string that can be handed to a second player is a row that can be handed
  to a second player, retroactively, because history joins the live column. §2 follows from that
  and from nothing else
- **Supersedes:** [`ADR-0029`](ADR-0029-a-display-name-is-unique-and-permanent.md) §4's clause
  ***"`name → a different name` raises"***, the word *permanent* in its title, and its
  *What it forecloses* bullet *"Renaming, in every form"* — which named this ADR's own price of
  admission (*"an answer for what happens to the names it releases back into the namespace"*), and
  §2 is that answer. **Also** §5's `403 Forbidden` row for *"this player already has a different
  name"*: holding a name is no longer a reason a name write is refused.
  [`ADR-0051`](ADR-0051-a-name-is-registered-before-it-is-held.md) §3's
  ***"`name → a different name` has no exception at all"*** goes in the same clause.
  [`ADR-0119`](ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md) §3's obligation
  **2** — *say the choice is permanent* — is replaced by §5 here, exactly as `ADR-0119` said it
  would be. Everything else in all three stands
- **Leans on, and does not touch:** `ADR-0051` §1's *"a string that enters that table never leaves
  it"* and its monotonicity trigger, §2's spend-then-hand order and its rollback, §4's takedown,
  §5's blocklist; `ADR-0029` §1's fold and pinned collation, §2's canonicalisation, §3's character
  rules, §5's refusal of an availability endpoint, §6's *the server fabricates nothing*, §7's *a
  name is never an authentication factor*; [`ADR-0038`](ADR-0038-a-name-is-screened-when-set-and-can-be-taken-away.md)'s
  screening and its *retired forever, including the player it was taken from*;
  [`ADR-0039`](ADR-0039-v01-offers-no-account-deletion.md)'s refusal to denormalise a name into
  `duel_result`, which is why §3 needs no code; [`ADR-0052`](ADR-0052-a-takedown-is-told-to-the-player-it-happened-to.md)
  §§1–5 whole
- **Answers** [`ADR-0125`](ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md)'s
  open seam — *"whether the surface is a set once form or a change whenever you like one is
  `DEC-131`'s to say, and a form cannot be placed before it is known which of the two it is"* — in
  §6: it is a *change whenever* form, and it lives on the account screen
- **Constrains:** `STORY-1409`, which it unblocks on the product side, and `STORY-1407`'s copy
- **Registers:** **`DEC-151`**, the architect's — the mechanism. This ADR designs none

## Context

`ADR-0029` did not make a display name permanent as a convention. It made it permanent as
machinery: a case-insensitive unique index under a pinned ICU collation where *"the index is the
reservation"*, and a PL/pgSQL trigger whose stated reason was that *"every other invariant this
codebase enforces in Kotlin can be repaired by an `UPDATE`, and this one is defined by the
impossibility of that `UPDATE`"*. `ADR-0051` then built `name_registry`, a table no string ever
leaves, and §9 refused to build an un-retire, a release or any way to give a name back.

The human has asked for a rename. That is settled, and the question that is left is the one
`ADR-0029` itself said would have to be answered: **what happens to the string a player walks away
from.** Four things are in tension.

**The name is the only identity a leaderboard row has.** `ADR-0067` §1 decided a row is text and
leads nowhere — no link, no profile, no id a reader can follow. The consequence nobody had to face
while names were permanent is that if the text can move between people, the row moves with it, and
there is nothing else on the row to say otherwise.

**History is a live join, and that is load-bearing rather than incidental.** `ADR-0039` forbade
copying a display name into `duel_result`; `PostgresProfileReads` joins `p.display_name AS
opponent_display_name` and `PostgresStandingsReads` joins `p.display_name` per row. So whatever a
player is called *now* is what every finished duel and every ladder row says they were called. This
is also what makes `ADR-0038`'s takedown work at all: `retire_display_name` nulls one column and
the name is gone from every screen in the product. A rename inherits that behaviour whether or not
anyone decides it should, which is precisely why it has to be decided rather than inherited.

**Permanence was bought for a reason the human has not withdrawn.** `DEC-017`'s words were
*"no impersonation on a leaderboard, and no rename to escape a reputation"*. The 2026-09-06 message
asks for a name that can be changed. It does not ask that the abandoned name become available to
somebody else, and nothing in it requires that. The smallest change that gives the human what they
asked for is therefore available, and choosing anything larger would be this ADR's invention, not
the human's request.

**Every rename spends something.** Under `ADR-0051` a string is claimed by an `INSERT` into a table
with monotone rows. Whatever answer is chosen, a rename turns one profile from a thing that can
spend one string in its lifetime into a thing that can spend many, and `ADR-0029` already recorded
squatting as *"a second, irreversible thing to farm"*. That cost lands under two of the three
answers and is not a reason to pick between them, but it is a reason the mechanism cannot be left
unwritten.

### The deadline, honestly

Two clocks, and neither is the schema's.

The first is **the product is currently telling players the opposite.** `name-text.ts` ships
`PERMANENCE_LINE` — *"A name is chosen once. You cannot change it later, and it can be taken
away."* — and `ADR-0119` §3 put it on the ask at the player's first press. Every day that sentence
ships while item 1c is planned, the product makes a promise it intends to break, to the players who
arrived earliest. That is not a schema migration; it is a lie with a date on it.

The second is **the direction of the mistake is asymmetric, and only one way round is fixable.**
Retiring a string today leaves a later ADR free to release it, because nothing has happened yet.
Releasing a string today and letting a second player take it cannot be undone by anybody: the
second player's rows are printed, the first player's rows now name them, and no decision reaches
back. With no players yet and no evidence about how often anyone renames, that asymmetry is most of
the argument for §2 and is stated as such rather than dressed up as a principle.

## Decision

**A display name may be changed, as often as the player likes. The name it leaves behind is spent
forever — nobody may take it, including the player who gave it up — and the new name is the
player's name everywhere, including on every row already printed.**

### 1. A name may be changed, at any time, and every change is a name being set

Recorded from the human, not chosen here. A player who holds a display name may replace it, with no
quota, no cooling-off, no confirmation step and no *changes remaining* counter — *"at any time"* is
the human's phrase and this ADR adds nothing to it.

- **A change is a `set`, under exactly the rules a first name is set under.** `ADR-0029` §2's
  trimming, NFC and 1–32 code points; §3's refusal of `Cc`, `Cf` and doubled spaces; `ADR-0029`
  §1's case-insensitive fold against everything already spent; `ADR-0038`'s blocklist. The
  twentieth name is screened exactly like the first, with no carve-out and no grandfathering.
- **A change replaces a name; it never leaves the player nameless.** There is no *clear my name*
  control and no way to return to `null` by choice. The one transition that makes a named player
  nameless is still `ADR-0051` §3's single exception — an operator's takedown — and its scope is
  unchanged.
- **Setting the name you already hold succeeds and changes nothing**, which is `ADR-0029` §5's
  idempotent retry, unchanged.
- **No refusal in this product says a name cannot be changed.** The product-visible refusals are
  *not available* and *cannot be used*; `refusalSentence`'s `permanent` sentence has nothing left to
  describe and leaves with `PERMANENCE_LINE` (§5).

### 2. The name given up is retired: not released, not held

The string the player walks away from is spent. It joins the same set a taken-down name joins, and
`ADR-0038`'s sentence covers it word for word: **retired forever, including the player it was taken
from.**

- **Nobody else may take it, ever.** This is the whole of the derivation: the display name is a
  leaderboard row's only identity (`ADR-0067` §1), and history is a live join (`ADR-0039`), so a
  reissued string would print a second player onto the first player's finished duels — on the
  opponent's own history screen, retroactively. That is impersonation granted by the product rather
  than achieved against it, and it is the harm the human named when they asked for permanence.
- **The player who gave it up may not take it back either.** A rename is one-way. Their old name
  answers the same refusal every spent string answers, which is exactly what `ADR-0051` §6 already
  says happens to a player whose name was taken away — one rule for both ways a name can leave a
  player, and no new vocabulary for either.
- **`ADR-0051` §1 is not amended.** *A string that enters that table never leaves it* is what this
  section rests on, and its monotonicity trigger already permits only `TAKEN → RETIRED`.
- **No name is enumerated anywhere.** No screen lists a player's former names, no response carries
  one, and `ADR-0029` §5's refusal of an availability endpoint is applied, not reopened.

### 3. A player has one name, and history shows the name as it is

**The name a player holds now is the name on every row that names them** — their ladder row, their
opponents' duel history, the profile strip's last-duel line. A rename is retroactive by
construction, and **no row is rewritten, because no row holds a name** (`ADR-0039`).

- **The old name appears nowhere in the product after the change.** Not on a row, not in a
  *formerly known as*, not in a tooltip, not in an `aria-label`, not in a footnote on a season.
- **Nothing marks a row whose name changed.** `ADR-0052` §5's asymmetry is extended one step: to
  everyone else a name is simply the name, drawn exactly as any other name is drawn.
- **A rename is invisible at the table.** Measured on `develop` at `e7061e7b`: no `ServerMessage`
  carries a display name, and `DuelTable.tsx:58` and `DuelResult.tsx:108` call the opponent *Your
  rival*, a fixed string. A duel in progress is untouched, and a name that changes mid-duel changes
  nothing either player is looking at.
- **The whole blast radius is three surfaces** — `LadderScreen`, `HistoryScreen` and
  `ProfileStrip`, the only non-test callers of `nameOrNone`.

### 4. Nobody is told, and there is no notice

- **The rival is not told, then or later.** No message, no line on a result screen, no badge. The
  vision's *"Dark, quiet, fast, minimal"* is the whole argument, and there is nothing here for
  another player to act on.
- **The player who changed it is told by the write succeeding**, carrying the canonical string back
  (`ADR-0029` §5). That is the only confirmation the product owes.
- **A player who changed their own name is never shown `ADR-0052` §1's removal notice.** Whatever
  the mechanism writes to `name_registry`, `displayNameRemoved` stays false for a player who
  renamed themselves; that notice is for a takedown and for nothing else, and its four sentences
  are unchanged.

### 5. What the form says before the send

`ADR-0119` §3's obligation **2** — *say the choice is permanent* — is now false and is replaced,
not deleted. `PERMANENCE_LINE` leaves the product.

Two obligations, on every surface that writes a name:

1. **It says the name can be changed later.**
2. **It says the name being given up is gone for good — the player cannot take it back, and nobody
   else can take it either.** This is the non-obvious, irreversible half, and the reason it must be
   said *before* the send is `STORY-0411`'s, quoted by `ADR-0052`: *a player is entitled to know
   that at the moment they can still avoid it.*

The words, the heading, the control labels and the layout are the design card's (`ADR-0091` §2),
within those two. `ADR-0119` §3's obligations 1 and 3 stand unchanged.

**The change form offers no suggestion.** `ADR-0119` §4's suggestion exists to get a player past a
blank field at their first press; a player changing a name already knows what they want, and a
suggested replacement would be the product proposing that somebody stop being who they are.

### 6. One form, on the account screen

*"in accounts settings"* is the human's placement, and it is taken.

- **One surface does both acts** — it sets a first name and changes an existing one — because with
  §1 in force those are the same act.
- **`NameSurface` moves to the account screen and leaves the front door.** The door to it is the
  `Account` control `ADR-0125` §2 made the standing one. The front door keeps `ProfileStrip`, which
  already prints the player's name, so the move also ends the duplication `Lobby.tsx:433-436`
  currently renders.
- **`ADR-0052` §1's notice travels with the surface**, unchanged in its words and in its trigger:
  it still appears where a name is set, and that place is now the account screen.
- **`ADR-0119` §1's ask is untouched** and stays a screen in place of the front door at the
  player's own first press. A name can therefore be written from two places, and both obey §5.

### 7. What is deliberately not built

- **No release, no un-retire, no reclaim, no grace period** in which an abandoned name is held for
  its former owner.
- **No list of a player's previous names**, to them or to anybody.
- **No quota, no counter, no cooling-off, no confirmation press.**
- **No notification of a rename**, to the rival or to anyone.
- **No operator rename.** `ADR-0038`'s takedown is still the only path by which a name is removed
  from a player who did not choose to remove it, and it is unchanged.

## Consequences

**What it buys.** `STORY-1409` is unblocked on the product side and `ADR-0125`'s deferred placement
question is closed, so the account screen can be drawn as one screen rather than two halves waiting
on each other. `ADR-0029`'s sharpest recorded cost — *"A typo is forever… a player named `Bobb` who
meant `Bob`, with no recourse, ever"* — stops being true. A name that slips past the blocklist can
now be abandoned by the player who chose it, without an operator. And the two ways a name leaves a
player converge on one rule, so the product has one sentence to say about a spent string instead of
two.

**What it costs.**

- **A rename is one-way, and this is the sharp edge.** The most common outcome of §2 will not be an
  impersonation defeated; it will be a player who changed their mind and cannot change it back. The
  string they left is refused to them like any other, and there is no appeal, because there is no
  operator path that gives a name back and this ADR builds none.
- **The namespace now burns faster than one string per profile.** `ADR-0029` recorded squatting as
  *"a second, irreversible thing to farm"* at the price of one profile per name; a rename makes it
  one `PUT` per name from a single profile. There is no brake in the product today. That is the
  reason `DEC-151` is due **before** `STORY-1409` ships, not after it.
- **A rival cannot find you again.** Beat `Ace` in March, look in April, read `Bob`, and nothing in
  the product says they are the same person. `ADR-0067` already made a row lead nowhere, so
  recognition was never a supported feature — but the string was the last thing standing in for it,
  and §3 makes it unreliable too.
- **Other people's records change under them, silently and often.** `ADR-0051` §6 recorded this
  retroactivity as a consequence of a takedown, which is rare and operator-driven. §3 hands the
  same power to every player, over their opponents' history screens, at will.
- **The product gives up *strongest identity*.** A name still identifies exactly one player at a
  time and never two at once. It no longer identifies one player over time, and the human's
  `DEC-017` reasoning is honoured only in the half §2 preserves.
- **Shipped, tested strings are deleted.** `PERMANENCE_LINE` and `refusalSentence`'s `permanent`
  sentence are golden strings with tests around them, and `ADR-0029` §5's `403` loses its cause.
  That is real work in `STORY-1409`, not a line removed.
- **Two merged migrations were written around permanence.** `ADR-0029` §4's trigger and `ADR-0051`
  §3's replacement of it both encode *`name → a different name` raises* as their central clause;
  §1 makes that clause wrong and the repair is a migration `DEC-151` owns.

**What it forecloses.**

- **Ever giving a retired string to a second player** — for as long as this ADR stands. Reversing
  that direction is possible and cheap *today*, because nothing has been reissued; it stops being
  cheap the moment one has, and a future ADR that wants it must first answer what a reissued name
  does to the rows the previous holder already printed.
- **Freezing a name onto history rows.** Reopening that must answer what a takedown does to the
  frozen copies, or `ADR-0038`'s one moderation lever stops working — see *Alternatives*.
- **A names history, a *formerly known as*, or any surface that says two strings were one player.**
  §§3 and 4 refuse all of them; each is additive and none is foreclosed permanently.

**What this does not settle.**

- **The mechanism, in full — registered as `DEC-151`, the architect's.** What the permanence trigger
  becomes; what statements a change runs and in what transaction; whether the vacated registry row
  moves `TAKEN → RETIRED` and whether `retired_from` is populated for a rename — under the
  constraint in §4 that `ADR-0052` §6's derived bit must read **false** for a player who renamed
  themselves; what `PUT /api/me/name` answers now that `403 AlreadyNamed` has no cause, and what
  `docs/protocol.md` says instead; and **whether the write needs a budget**, which `ADR-0029` §5
  already named as additive and `ADR-0022`-shaped, keyed by `PlayerId`. It may not change what
  §§1–7 say a player sees.
- **The words**, the heading and the layout of the form and of §5's two sentences — the design
  card's, within `ADR-0091` §2.
- **`DEC-017`'s remaining half — what a name may contain — is still the human's.** Nothing here
  refuses a name the write path does not already refuse, and §1 screens a replacement exactly as it
  screens a first name.
- **Whether the ladder ever shows a player's rank against a name that changed mid-season.** A
  season is a calendar month (`ADR-0061`) and §3 makes the row print the current string; nobody has
  asked for anything else, and nothing here builds a season-scoped name.
- **`DEC-132`, noted rather than decided.** If signing out hands a browser a new anonymous profile,
  a player can also *change their name* by becoming a different player, and the string they leave
  behind is retired by §2 either way. That is a reason to answer `DEC-132` with this ADR in view,
  not a reason to assume its answer.

## Alternatives considered

**Released for anyone to take.** The strongest case, and it is the one most products take: a rename
would then cost the namespace nothing, short names would circulate instead of being buried one per
whim, and the section of *Consequences* about burning strings would not exist. It is also what a
player arriving from anywhere else expects, so it is the answer that needs no explanation on a
form. Rejected because `ADR-0067` §1 made the display name the whole of a leaderboard row's
identity — the row is text and leads nowhere — so handing the string to a second player hands them
the row, and because history is a live join (`ADR-0039`) it hands it to them **retroactively**, on
the opponent's own history screen, for duels they never played. That is the impersonation the human
bought permanence to prevent, and the 2026-09-06 message asks for none of it. It is also the only
one of the three answers that cannot be walked back: a retirement can be released later, and a
reissue can never be un-issued.

**Held by its former owner — unavailable to everyone else, reclaimable by them.** This nearly won.
It defeats the impersonation objection completely, since nobody else ever gets the string; it makes
a rename reversible, so a mistyped or regretted change is a five-second fix instead of a permanent
one, which is the exact remedy `ADR-0029`'s *"a typo is forever"* was written about; and the fact it
needs is already on disk, because `ADR-0051` §1 records `retired_from` on every retirement.
Rejected on two grounds. First, it obliges the product to say what a player's old names *are* —
either a list on a screen, which is a read path over `name_registry` that `ADR-0051` §1 refuses
outright, or a set-time answer that succeeds for one player and refuses everybody else, which is a
rule that has to be printed on the form and understood before it helps anyone. Second, and
decisively: retiring keeps this option open and holding does not. `retired_from` already records who
left which string, so a later decision may let a player re-take a name retired from them, with no
data lost and nobody harmed in the meantime — whereas a promise that your old names are yours
cannot be taken back once made. Retiring is the same answer with the cheaper mistake.

**A quota — one change per season, or per month.** Its strongest case is real and it is the shape a
larger product would ship: it caps the namespace burn §2 opens, it keeps ladder names stable enough
to recognise across the season a ladder is scored over, and it is a rule players already understand
from elsewhere. Rejected because the human's words are *"at any time"*, and because a quota is a
product commitment — a number to print on the form, a count to store, a clock to explain, and a
refusal a player will read as arbitrary — bought to solve an abuse nobody has yet committed against
a product with no players. The brake that addresses the abuse that actually matters is a rate limit
on the write, which is a refusal about requests rather than a rule about names, needs no vocabulary
on any screen, and is `ADR-0029` §5's already-named additive route. If churn turns out to be a
problem in play rather than in theory, a quota is a later decision with evidence behind it.

**History shows the name as it was — the name denormalised onto the rows it played.** A strong case,
and the one a reader's instinct reaches for: a record that changes under its reader is a poor
record, the opponent's history is *their* memory of *their* duel, and a stranger's later choice
editing it is a genuine wrong. It would also repair `ADR-0051` §6's already-recorded surprise that a
takedown blanks other people's rows. Rejected because a frozen copy survives a takedown.
`retire_display_name` exists so that a name an operator judged unacceptable leaves the product; with
that name copied onto every row it ever played, a takedown would remove it from one place and leave
it on all the others — so `ADR-0038`'s single moderation lever would stop working, or would need a
scrub across every copy, on the denormalised schema `ADR-0039` deliberately did not build. It also
has to answer which of a player's several names a season row shows, a question the live join never
asks.

**Refuse the change; keep permanence, and let a player who wants a different name apply for a
takedown.** Its strongest case is that `ADR-0029`'s reasoning has not been shown wrong by anything
in the human's message: permanence is the strongest identity a leaderboard can carry, the trigger
and the registry that enforce it are merged and working, and every cost listed above disappears.
Rejected because it was never available. *That* a name may be changed is the human's stated call,
and re-arguing it here would be this ADR inventing a product the human did not ask for — the one
failure mode the product-owner role exists to prevent. What was open, and all that was open, is
what happens to the string.
