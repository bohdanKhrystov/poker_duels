# ADR-0060 — The record is its own screen, and the first screen is the door

- **Status:** Accepted
- **Date:** 2026-08-19
- **Resolves:** `DEC-053` — how does a player reach their whole duel record, and how do they leave
  it? **Derived from the vision; the human did not state this call.** Three sentences license it.
  The roadmap's v0.2 row — *"Persistent profile, duel coin counter, **match history**"* — is what
  makes the record a commitment already made, so this decision is about placement and nothing else.
  *"Every free poker site is a casino simulator — chip bundles, **slot machines in the lobby**, gold
  everywhere… None of that is wanted here."* is the vision naming accretion on the first screen as
  the shape it refuses. And the first success condition — *"Send a link. She opens it in a browser.
  We play a full heads-up match. Someone wins. We hit Rematch."* — is what the first screen exists
  for, and everything else *"is downstream of that moment"*
- **Builds on:** [`ADR-0032`](ADR-0032-react-subscribes-to-a-store-it-does-not-own.md) (the socket
  and the store live outside the tree, so which screen is mounted cannot cost a connection, a frame
  or a seat) and [`ADR-0036`](ADR-0036-an-account-is-offered-never-required.md) (a screen reachable
  anonymously stays reachable anonymously — the door gates on nothing)
- **Constrains:** `TASK-041313`, which is unblocked by this ADR and gains what §7 names; and every
  later screen in this client — `STORY-0412`'s account screens, `STORY-0415`'s offer, `EPIC-05`'s
  leaderboard — which inherit *the first screen is the door* until `DEC-054` says otherwise
- **Raises:** **`DEC-054`, the architect's** — does the client grow URL-addressable routes and a
  working browser *Back*, and what carries them? This ADR deliberately does not answer it: §6 says
  what it costs to defer, and the record is reachable without it
- **Amends nothing.** No server change, no wire change, no `PROTOCOL_VERSION` step; nothing here is a
  socket fact

## Context

The client has no navigation of any kind. `App.tsx` renders a heading and `<Lobby />`, and `Lobby`
swaps *itself* for the duel table and then the result screen off store state. Screens exist; a screen
a **player chooses** does not. `STORY-0413` specifies the record in full and stops at the door.

- The first screen today carries up to four things: *Create a duel room*, the join-by-code form, the
  profile strip (name, coin balance, the last few duels) and the name surface.
- The record is not a list. It is a paged walk with a cursor, four outcome choices, a search box,
  four distinct states and a *Show more* control.
- **The profile strip renders `null` when its background read fails** — deliberately, because *"the
  player cannot act on it"*. Anything hung off the strip inherits that disappearance.
- Three more surfaces are already written against whatever this answers: `STORY-0412`'s account
  screens, `STORY-0415`'s offer (which *"opens `STORY-0412`'s screen"* rather than growing a second
  form) and `EPIC-05`'s leaderboard.

### The forces

**A section of the lobby is genuinely cheaper, and not only today.** It needs no navigation state, no
affordance, no word, no way back, and no test that clicks anything; it puts the record exactly where
the strip already teases it, so nobody has to be told where it is; and it makes the browser's *Back*
button a non-question, so `DEC-054` never has to be asked.

**Against it: the first screen is the one screen the vision's success condition depends on**, and
this is the decision that decides whether it stays that or becomes a menu of everything the product
owns. The vision's own picture of what it refuses is not "a casino" in the abstract — it is
*slot machines in the lobby*. A paged, filtered, searched list of every duel a player has ever played
sitting under the *Create a duel room* button is a feed, and a feed is what the strip was explicitly
not meant to be.

**The inheritance is the deciding force.** One extra section on the lobby is cheap. Four are the
whole product stacked on one screen, and by the time `EPIC-05` lands, un-stacking them is a rewrite
of every screen at once. Adding sections is reversible one at a time and irreversible in aggregate.

**What makes the other side hurt:** without URL routes, *its own screen* means a screen with no
address — nothing to link, nothing to bookmark, and a browser *Back* button that does not come back.
That cost is real, it is named in §6, and it is why `DEC-054` is raised in the same breath.

## Decision

### 1. The whole duel record is its own screen

It **replaces** the first screen while it is open. It is never rendered beside the lobby — not below
the strip, not above the create button, not in a panel. When the record is on screen, the create
button, the join form, the strip and the name surface are not.

### 2. The way in is one control on the first screen, reading *Your duels*

Beneath the profile strip and **outside** it, one control the player activates to open the record.
Its word is `HISTORY_HEADING` — the string `TASK-041305` already authors as the record's own
heading, `"Your duels"` — so the destination has exactly **one** spelling in the product and a
future rename moves one literal. It is a `<button>`, not an `<a>`: while the client has no URLs
there is no address for an anchor to carry, and an anchor without one is a lie to the keyboard and
to a screen reader.

### 3. The door does not depend on the read

The control is on the first screen whatever the profile read answered — including `unavailable`,
where the strip renders nothing, and `no-profile`, where the record is honestly empty
(`STORY-0413` already settled that a browser holding no profile reads as *"No duels yet."*, not as a
failure). **The way to a screen may not vanish because a background read failed**, and per
`ADR-0036` it gates on nothing else either: no account, no name, no coin.

It is placed beneath the strip and **not inside it**, because `ProfileStrip` is prop-driven
presentation with no state — and because a strip that disappears would take the door with it.

### 4. The way out is one control reading *Back*, and it restores the first screen

One control on the record screen. Activating it returns the player to the first screen exactly as it
was — same store, same room code box, same strip.

- The word is **`Back`**: one word, and it names the direction rather than a place. *Lobby* is a
  word this product says to a player nowhere on screen, and the vision's only use of it is the
  sentence about slot machines; there is no reason to teach it now.
- It is rendered by whatever renders the swap, **not by `HistoryScreen`**, so the record screen knows
  nothing about what is outside it — the same layering that lets `ProfileStrip` be pure. That is also
  what makes the affordance assertable without a transport, which §7's test depends on.

### 5. The door is offered only where a player is not in a duel, and a duel outranks the record

The control appears on the first screen only — the branch with the create button — and never while
waiting in a room, never at the duel table, never on the result screen. If the store ever moves into
a duel while the record is open, **the duel takes the screen.**

A player cannot act from the record, and the server does not rescue a connected player who wanders
off: `RoomTimeouts` reaps a `WAITING` room after ten minutes and a finished one after five, but
*"a `PLAYING` room is never reaped for idleness"* and `ADR-0013`'s grace window only runs for a
**dropped** connection. So a player who opens the record mid-hand does not fold — they leave their
rival staring at a table that nothing will ever end. With the door where this section puts it that is
not reachable today, and the rule is still written down, because it costs one condition and it is the
one failure on this screen that harms the other player.

### 6. Nothing is remembered, and nothing is addressable

A reload lands on the first screen. The record's position, filter and search term do not survive it,
and no URL names the record. This is the state of the world under `DEC-054` and is recorded as a
consequence rather than left to be discovered — `ADR-0057` already forecloses a shareable
page-position link for its own reasons; this forecloses a link to the screen at all, for now.

### 7. What `TASK-041313` gains, exactly

The half already written — the module-scope binding of `readDuelPage` to `window.fetch` and
`localStorage` in `main.tsx` — does not move. Added:

| | |
| --- | --- |
| Scope | The door (§2, §3), the way back (§4), and the rule that the record is never beside the lobby (§1, §5) |
| Test | `leaves the first screen for the record, and comes back to it` |
| `verify:` | One line greping that test name out of the verbose reporter |

The test renders `App`, activates *Your duels*, asserts the first screen is gone — *Create a duel
room* is no longer in the document — activates *Back*, and asserts it is there again. It asserts the
**swap**, through controls that exist whatever the history read answers, so it needs no transport and
no provider the test file does not already have.

`files_touched` stays `3`, and **`TASK-041314`'s 472 does not move**: its arithmetic already budgets
`TASK-041313` at *"+3 (two written at the split, one named by `DEC-053`'s ADR)"*, and this ADR names
exactly one.

The ticket's criterion *"exactly one of `App.tsx` and `Lobby.tsx` differs, not both"* is satisfied by
the file that already knows whether a duel is in progress: §5 makes that knowledge the door's
precondition, and `Lobby` is where it lives. This ADR fixes the behaviour, not the file — any
implementation that honours §1 through §6 is the decision.

## Consequences

**What it costs, plainly:**

- **The first screen becomes the only door, and it will get crowded.** Every screen after this one is
  either reachable from there or not reachable at all. `STORY-0412`'s account screens and
  `EPIC-05`'s leaderboard inherit that, and the create button — the control the vision's first
  success condition runs through — ends up sharing its screen with a growing column of links. This
  ADR does not build a navigation bar, and the answer to that crowding is `DEC-054`'s, not a later
  section on the lobby.
- **The record has no address.** It cannot be linked, bookmarked, opened in a second tab, or reloaded
  into. **The browser's own *Back* button does not bring a player back from the record**: it leaves
  the page for whatever the tab held before this client, or does nothing at all in a tab opened
  straight onto the invite link. A player who presses it out of habit does not return to the lobby.
  That is the sharpest cost here and it is the whole reason `DEC-054` is raised.
- **`STORY-0415` inherits a shape it must design around.** The offer fires after a first *win* — on
  the result screen — and *"opens `STORY-0412`'s screen"*. So whatever holds *which screen is showing*
  has to be reachable from the result screen too, not only from the first one. That story gets a
  harder wiring problem than it would have had if every screen were a section.
- **Two new strings of player-facing copy** (*Your duels* as a control, *Back*) for `EPIC-06` to
  style, and one more component that knows what a screen is.
- **It forecloses the glance.** The whole record is now somewhere a player has to go, so nothing on
  the first screen ever shows more than the strip's handful of recent duels. If the record turns out
  to be the thing people open every session, this answer costs them a click, forever, until routes
  make it a bookmark.

**What it buys:** the first screen keeps doing the one job the vision's first success condition needs
it to do; the record reads as a record rather than as a feed under a create button; `HistoryScreen`
stays a screen with no knowledge of navigation, so it can be moved behind a route later without being
rewritten; and the pattern the next three surfaces inherit is *a screen is a screen*, which is the one
that survives routes arriving.

**What it forecloses, deliberately:** the record as a section of the lobby, in this client, at any
size. Reversing this is cheap while it is one screen and expensive once three more have copied it —
which is the reason to settle it now rather than after `STORY-0412` is split.

## Alternatives considered

**A section of the lobby, beneath the strip.** The strongest case, and the one that would have been
cheapest by a wide margin: it adds no navigation state, no affordance, no word, no way back and no
test that clicks anything; it is the smallest possible version of `TASK-041313`; the strip already
lists recent duels, so a fuller list underneath is where a player would look without being told; and
it makes the browser *Back* question — the one real cost of the answer chosen — simply not exist, so
`DEC-054` need never be asked. Rejected on the vision's own words: the first screen is the path to
*"Send a link. She opens it in a browser… We hit Rematch."*, and the thing the vision refuses is
named as *slot machines in the lobby* — accretion on exactly this screen. Concretely, it would put a
paged, searched, filtered list with four states under the create button, moving and reloading itself
while somebody is trying to type a room code. And it does not stay one section: `STORY-0412`,
`STORY-0415` and `EPIC-05` all inherit it, so the end state is every screen the product has, stacked
on the one screen a new player sees first — reversible one section at a time, irreversible in
aggregate.

**Its own screen, entered by making the profile strip itself the link.** The strongest case: the
strip already shows a sample of exactly what the record holds, so *click the thing you want more of*
is the most discoverable affordance available and it costs no extra control on the screen. Rejected
because the strip renders `null` whenever its background read fails — by design, since the player
cannot act on that failure — so the only route to the record would silently disappear at the worst
moment, and because it would put navigation inside a component whose whole contract is prop-driven
presentation with no state.

**A modal or overlay over the lobby.** The strongest case: no screen state at the app level, the
lobby stays mounted underneath so *back* is just *close*, and browser *Back* stops mattering because
nothing ever navigated. Rejected because a dialog is for a decision a player answers and then leaves,
while the record is a place they stay in, scroll, page and search; because the duel table could
appear underneath one; and because a correct dialog — focus trap, Escape, restored focus, inert
background — is a component this client has not built and this ticket cannot afford.

**Its own screen with URL routes, now.** The strongest case: it is the answer we will eventually
want; it makes *Back* work, makes the record linkable, and pays for itself the moment there are three
screens. Rejected because it is not this decision's to make — a router, the History API, the dev
proxy and whatever `EPIC-07` deploys all get a say, which makes it technical — and because the record
is fully reachable without it. It is registered as `DEC-054` for the architect, and nothing decided
here has to be undone when it lands: §4's *Back* control stays useful beside a working browser
*Back*, and a screen that knows nothing about navigation is exactly what a router wants to mount.

**Its own screen, reached from a persistent navigation bar built now.** The strongest case: it is
where this ends up once four screens exist, and building it once is cheaper than adding a link to the
lobby four times. Rejected as answering a question nobody asked yet — one screen does not need a bar,
`EPIC-06` owns the visual language a bar would have to live in, and a bar rendered above a duel table
would be a way out of a hand in progress, which §5 refuses.
