# ADR-0058 — Where a name would be, the client prints *No name*

- **Status:** Accepted
- **Date:** 2026-08-18
- **Resolves:** `DEC-051` — what does the client print where a display name would be, for a player
  who has none: on their own profile strip, and as an opponent on a duel line? **Derived from the
  vision; the human did not state this call.** The licensing sentences are the roadmap's v0.1 row —
  *"Two browsers, one room link, one complete duel, rematch. **No accounts.**"* — which makes a
  player with no name the product's ordinary shipped state rather than an unfinished one, and the
  positioning sentence *"The reference points are **Lichess** and **Chess.com**, not PokerStars.
  Dark, quiet, fast, minimal."*, which fixes the register as a plain statement rather than a mascot
  name, a prompt or a mark
- **Builds on:** [`ADR-0029`](ADR-0029-a-display-name-is-unique-and-permanent.md) §6 (the server
  fabricates nothing, and what a client renders for `null` is the client's — on the one condition
  that it never asks the server for a placeholder),
  [`ADR-0052`](ADR-0052-a-takedown-is-told-to-the-player-it-happened-to.md) §5 (a removed name
  renders *exactly* as a never-set one on everybody else's screen: no badge, no tooltip, no distinct
  styling) and its §1 (the two states are distinguishable to the player themselves, on the name
  surface and nowhere else), [`ADR-0036`](ADR-0036-an-account-is-offered-never-required.md) (a
  nameless, account-less player is a full participant, so the treatment may not read as a defect)
- **Constrains:** `TASK-041114` (the string), `TASK-041115` and `TASK-041116` (its two callers), and
  every later surface that prints a display name — `STORY-0412`'s account screens, `STORY-0413`'s
  history screen, `EPIC-05`'s leaderboard rows
- **Amends nothing.** `ADR-0029` §6 delegated this and is applied, not changed
- **No server change and no wire change.** `ProfileResponse.displayName` and
  `DuelSummaryResponse.opponentDisplayName` stay `null`, `PROTOCOL_VERSION` does not move, and
  nothing here is a socket fact

## Context

`ADR-0029` §6 left the treatment for a `null` display name to the client in as many words, and
forbade exactly one thing: the client may not ask the server for a placeholder. `ADR-0052` §5 then
made that treatment carry weight it was not designed for — every duel line belonging to a player
whose name was **taken away** renders it, on strangers' screens, unmarked and unexplained, and it
must be byte-identical to the line for a player who simply never chose one. `TASK-041020` already
asserts the two are identical on the wire. What is left is the string, and four forces pull on it.

**The nameless player is the product's default, not its edge case.** v0.1 ships with no accounts at
all, `ADR-0036` keeps an account optional forever, and `ADR-0029` makes choosing a name a deliberate,
permanent act most players will not have performed. So this string is not a rare fallback seen by a
few; it is one of the most-printed strings in the product, and it is printed about ordinary players
in good standing.

**It is also the string a takedown hides behind.** Whatever is chosen is what a stranger reads where
a removed name used to be. A word that implies a *choice* to be unnamed is false for that player; a
word that implies a *consequence* publishes the private fact `ADR-0052` §5 exists to keep private.
The safe shape is a statement about the record — this profile holds no name — which is equally true
of both, and of the takedown that was a mistake.

**It must not read as an error either.** The most common state in the product cannot render as
missing data or as a form that was not filled in. A blank, a dash or a spinner-shaped gap teaches a
new player that the product is broken at the exact moment it is working normally.

**Three later surfaces inherit it, and two of them are third-person.** `STORY-0413`'s history screen
and `EPIC-05`'s leaderboard rows print names about people the reader is not and may never have
played. A treatment that only makes sense on the player's own strip — *You* — or only in relation to
the reader — *Your rival* — does not survive its own inheritance, and a leaderboard row that says
*Your rival* is nonsense. The two surfaces the question names are the two that most flatter a
warm, second-person answer, and they are not the surfaces the answer has to live on.

Underneath all four: **the mechanism that keeps `ADR-0052` §5 true is one function with one `if`**
(`nameOrNone`, `TASK-041114`). Two treatments means two components deciding separately, and drift
between them is not a cosmetic bug here — it is the takedown leaking.

### The deadline, honestly

**Free today, a copy audit in a month.** The string exists nowhere yet: `name-text.ts` is not
written, and the criterion in `TASK-041107` forbids it from guessing. `TASK-041114` is the first
place it lands, `TASK-041115` and `TASK-041116` the first two readers. Once `STORY-0413` and
`EPIC-05` have shipped rows carrying it, changing it is four surfaces of copy and a test in each.
It never becomes irreversible — it is a client string, not a wire fact, which is precisely why
`ADR-0029` §6 could delegate it — so this is a reason to decide now, not a reason to decide either
way.

## Decision

**Where a display name would be printed and the player holds none, the client prints `No name` —
the same two words, on every surface, about every player, whatever the reason there is no name.**

### 1. The string

Exactly `No name`. Sentence case, one `U+0020` between the words, and nothing else: no full stop, no
brackets, no quotation marks, no dash, no ellipsis, no leading or trailing space. It occupies a name
slot as a label, so it takes no terminal punctuation even where the copy around it does.

It is not a name, and it is never treated as one: it is never sent to the server, never compared
against a name, never used as a key, and never derived from `playerId`. `ADR-0029` §6's single
prohibition stands untouched — nothing asks the server for a placeholder.

### 2. One treatment, every surface, both persons

The same string is printed by the profile strip for the player's own missing name, by a duel line
for a nameless opponent, and — by inheritance — by `STORY-0413`'s history rows, `EPIC-05`'s
leaderboard rows, and `STORY-0412`'s account screens if they ever print a display name.

**It does not vary by surface, by who is looking, or by whether the viewer is the player
themselves.** A player looking at their own strip reads `No name`, not *You*. This is stated
explicitly because the alternative is attractive on exactly the two surfaces the question named and
wrong on the two it did not.

One function answers it — `nameOrNone(displayName)` — and it is the only place in the client that
branches on a `null` display name. That is not a style preference: it is what makes §3 a property of
the code rather than an agreement between two components.

### 3. It says nothing about why, and there is no second treatment

A player who never set a name and a player whose name was removed produce the **same two words**, on
every surface, for every viewer. No mark, no badge, no tooltip, no `title` attribute, no ordering
difference, and no styling that reads `displayNameRemoved`. There is no second function, no second
string and no parameter for the removed case.

The one place the two states differ is `ADR-0052` §1's notice on the name surface, shown to the
player it happened to. That notice is a sentence about the reader's own profile, not a name slot,
and this decision does not touch it.

### 4. Rendered, never hidden

`No name` is printed wherever a name would have been. The slot is not collapsed, the row is not
shortened, and the name is not simply omitted — `ADR-0029` §6 requires `null` to be rendered, and a
line that silently loses its opponent is a line whose reader cannot tell it from a bug.

### 5. Style is `EPIC-06`'s, on one condition

Nothing here authors a colour, a weight or a type ramp. `EPIC-06` may style the slot — dimmed,
lighter, whatever its language says — on the one condition that the styling is a function of *there
is no name* and never of *why there is no name*. Any styling that reads `displayNameRemoved` is
forbidden by `ADR-0052` §5, not by this ADR.

### 6. The live duel and its result screen are not covered

No display name crosses the socket, so the duel table and `DuelResult` have no name slot to fill.
Their *You* and *Your rival* label **seats**, not people, and they stay exactly as they are. They are
not a precedent for a name substitute, and this decision does not rename them.

## Consequences

**What it buys.** One string, in one function, means `ADR-0052` §5's invisibility is structural: two
surfaces cannot drift apart because there is only one decision. `TASK-041114` ships as written —
a single string from a `nameOrNone`-style function — and `TASK-041115` and `TASK-041116` need no
re-split. Three later surfaces inherit an answer instead of each inventing one inside a ticket.
And the answer is true in every case it covers, including the takedown that was a mistake: it states
what the record holds and claims nothing about the person.

**What it costs.**

- **A list of nameless rivals cannot be scanned.** Five duels against five different nameless
  players are five identical lines, and a player cannot tell whether they played one rival five
  times or five rivals once. `ADR-0052` §5 requires this and `TASK-041105` drops the opponent id at
  the parse, so the client could not distinguish them even if it wanted to — but it is a real loss
  of meaning in the history list, and it lands hardest on exactly the default player this product
  ships for.
- **On the strip it echoes the neighbouring empty state.** `ProfileStrip` already prints *"No
  profile yet."* when there is no profile; it will now print *"No name"* one line above the coin
  count when there is one. The two never appear together, but they are two lines of the same shape,
  and a glance can read a nameless profile as an absent one. `EPIC-06` can separate them
  typographically; the words themselves stay close.
- **The string is registrable as a display name.** `ADR-0029` §3 refuses invisible characters and
  odd whitespace, not ordinary words, and `ADR-0051` §5 ships the blocklist empty — so a player may
  set their display name to `No name` and appear on every list as though they had none. The only
  remedy is after the fact and manual: a takedown. This ADR adds no write-path rule and seeds no
  blocklist, and the hole is recorded rather than discovered.
- **A player's own strip states a small deficiency about them and points nowhere.** The offer to set
  a name lives on the name surface (`TASK-041108`), not on the strip, which is deliberate — the
  strip does not nag. The cost is that the one place a player looks at themselves says what they
  lack and offers nothing there.
- **Two words enter the product's vocabulary permanently.** Every later surface that prints a name
  inherits them, and any future rewording is four surfaces of copy plus a test in each. Cheap today;
  not free after `EPIC-05`.
- **The warmer answer is spent.** *You* on your own strip and *Your rival* on a duel line is
  friendlier, matches copy already shipped in `DuelResult`, and reads better on both surfaces the
  question actually named. It is being given up for two surfaces that do not exist yet, on the
  judgement that a leaderboard of *Your rival* rows is a worse failure than a flat strip. If that
  judgement is wrong, this is where it went wrong.

**What it forecloses.**

- **A per-surface or second-person treatment**, without a new ADR that overturns §2. This is the
  main thing given up, and it is given up knowingly.
- **A mark or a styled blank as the treatment itself.** Styling stays additive on top of the string;
  it may not replace it.
- **Telling anybody apart in a name slot.** Already `ADR-0052` §5's rule; this makes it a property
  of one function, so a future feature that wants to mark a row has to change code that two surfaces
  share rather than add a branch in one of them.

It does **not** foreclose changing the words: one string, one file, one function, one test literal
per caller. Nor does it foreclose `EPIC-06` styling the slot, `EPIC-05` deciding a nameless player is
not listed on a leaderboard at all, or a later rule that refuses `No name` as a display name.

## Alternatives considered

**`Anonymous`.** The strongest case: it is Lichess's own word for exactly this state, so it arrives
pre-understood by the audience the vision names; it reads as a *person* rather than as a gap, which
is kinder on a duel line than a description of missing data; and it is a single word that scans
cleanly in a table. Rejected on three counts. It asserts a **choice** to be unnamed, which is false
for the player whose name was taken away — the one player §5 is protecting — and misleading for a
player who has simply not chosen yet. It is the exact string `ADR-0029` §6 names as forbidden for
the server to mint (*"no `Anonymous`, no `Player-3F2A`"*), so printing it on the client makes a
shipped rule look overturned to anybody who reads either document without reading both. And it is
the most plausible-looking *name* of any candidate, which makes it both the most impersonable and
the easiest to misread as one recurring opponent across a history list.

**Two treatments: *You* on your own strip, *Your rival* on a duel line.** The strongest case, and the
one that lost most narrowly: it is warm, it is the vision's own duelling vocabulary (*rival* is in
the list of words this product uses), it matches copy already shipped in `DuelResult.tsx`, and on
the two surfaces `DEC-051` actually named it reads better than any third-person label. Rejected
because `STORY-0413` and `EPIC-05` inherit whatever is chosen and both are third-person: *Your
rival* on a leaderboard row is meaningless, and a history list where every nameless opponent is
*Your rival* puts a relationship word in a name slot beside real names. It also costs two branches
where one keeps two surfaces from drifting, and drift is how `ADR-0052` §5 breaks. Finally it
re-splits `TASK-041114`, which is written against a single string. If a warmer treatment is ever
wanted, it can be added for the strip alone by a later ADR — the cost of starting flat is lower than
the cost of starting split.

**A mark: `—`, or a dimmed blank.** The strongest case is the vision's own *dark, quiet, minimal*:
a dash adds no word to the product's vocabulary, cannot be misread as a name, cannot be registered
by anybody, and is what a well-made table does with a cell it has no value for. Rejected because it
reads as **missing data, not as an ordinary player**: a reader cannot tell it from a row that failed
to load, a screen reader announces nothing useful where a person should be, and `ADR-0029` §6 wants
`null` rendered rather than hidden — a bare dash is the closest thing to hiding that still occupies
space. For the product's *default* player, that is the wrong first impression to ship.

**`Unnamed`.** A genuinely close second: one word, no space, factual, no punctuation, scans in a
narrow column better than two words, and free of the *choice* implication that sank `Anonymous`.
Rejected narrowly, on two small things. It describes the **player** (*this person is unnamed*) where
`No name` describes the **record** (*this profile holds no name*), and describing people is the
thing this string must not do. And the component that renders it already speaks in the other
register: `ProfileStrip` ships *"No profile yet."* and *"No duels yet."*, so *No name* is the voice
the screen already has.

**A spelling that cannot be registered as a name** — for instance `No name` written with `U+00A0`
between the words, which `ADR-0029` §3's whitespace rule refuses on the write path while rendering
identically. Its case is real: it closes the impersonation hole recorded above at zero cost and
without a new rule. Rejected because it is precisely the spoofing technique §3 exists to stop,
turned on the product's own readers: a string that renders identically to a string it is not. It
also cannot be typed into a test literal without becoming a trap for the next person to edit it,
and buying a rare-case defence with an invisible character is a bad trade for a product whose stated
posture is saying the true thing plainly.

**A prompt: *No name yet*, or *Set a name*.** Its case is that on the player's own strip the string
is otherwise dead information, and `STORY-0411` is building the surface a prompt would point at, so
the click is one screen away. Rejected because the same string is printed about **other people** on
duel lines, where a prompt is meaningless and *yet* is a promise nobody made; because *yet* is false
for the player whose name was removed, who may never choose another; and because `ADR-0036` makes a
nameless player a full participant, so their own profile is not a to-do list with an item
outstanding. The offer to set a name belongs on the name surface, where the form is.

## What this does not settle

- **Whether a nameless player appears on a leaderboard at all.** `EPIC-05`'s. This ADR says what is
  printed *if* a name is printed; it does not decide who is listed.
- **Whether `No name` — or any word — is ever refused as a display name.** Closing the impersonation
  hole needs either a blocklist entry (an operator obligation, `ADR-0051` §5 ships it empty) or a
  write-path rule (`ADR-0029` §3, the rule that ADR expects to move). Both are additive and neither
  is decided here.
- **Colour, weight, type, and whether the slot is dimmed** — `EPIC-06` owns the language.
- **Whether a name is ever a link, and to what** — `EPIC-05`.
- **The removal notice's words** — `ADR-0052` §2, shipped verbatim and untouched.
- **What `STORY-0412`'s account screens print.** They have no screen yet; if one prints a display
  name, §2 says it prints this.
