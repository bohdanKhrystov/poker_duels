# ADR-0076 — A screen the player chose has an address; a screen the server gave has none

- **Status:** Accepted
- **Date:** 2026-08-25
- **Resolves:** `DEC-054` — does the web client grow URL-addressable routes and a working browser
  *Back*, and what carries them?
- **Builds on:** [`ADR-0032`](ADR-0032-react-subscribes-to-a-store-it-does-not-own.md) §2 (one boot
  per tab, outside the tree, owning the connection) and §3 (`useSyncExternalStore` from React core,
  no library); [`ADR-0060`](ADR-0060-the-record-is-its-own-screen-and-the-lobby-is-the-door.md) §4,
  whose screens know nothing about navigation, which is what makes this a small change;
  [`ADR-0026`](ADR-0026-vite-and-npm-drive-the-web-client.md), whose dependency set this does not
  touch
- **Constrains:** `STORY-0412`, which is unblocked by this and gains one address per screen it
  turns out to have; `TASK-041313`, whose door and way back stand unchanged; `STORY-0503`'s ladder,
  which gets the address it wrote itself against; and every screen story after them
- **Amends nothing.** No server change, no wire change, no `PROTOCOL_VERSION` step, no schema, no
  deployment requirement, and no new runtime dependency

## Context

The client has six screens and one address. `App.tsx` renders `<Lobby />`, and `Lobby.tsx` picks
among six branches: three come from the store — `outcome` (the result screen), `view` (the duel
table), `roomCode` (waiting for a rival) — and three from React state, `showHistory`, `showLadder`
and the default first screen. `STORY-0412` is about to add at least one more, and it is the story
this decision is due before.

### The forces

**Browser *Back* is not an affordance this product can decline.** It exists on every browser, and
on a phone it is the system gesture with no alternative. Today, pressing it on the record or the
ladder does not return to the first screen — it leaves the client. The document is torn down, the
socket with it, and the player lands on whatever preceded the app in that tab, or on nothing at all
in a tab opened straight onto an invite link. `ADR-0060` named this in advance and called it
*"the sharpest cost here… the whole reason `DEC-054` is raised."*

**`STORY-0412` is where that stops being abstract.** Its screens are the first in this client a
player *types into*: a handle, a password, a confirmation. A control that means *throw the document
away* sitting next to a half-filled sign-up form is a different proposition from the same control
next to a read-only list, and `ADR-0056` has already committed this product to *keeping what was
typed* when a sign-up is refused.

**The one address the product does promise already refused path segments, in a merged comment.**
`roomLink` is `${origin}/?room=${code}`, and `room-link.ts` says why:

> The code is a query parameter because a path segment would 404 on reload against a static host
> with no rewrite rule, and `EPIC-07` has not chosen one.

`EPIC-07` still has no file in `tasks/epics/`. Any answer whose correctness depends on a rewrite
rule depends on a document that does not exist.

**Two of this client's navigations are load-bearing page loads.** `DuelResult`'s `<a href="/">`
(`ADR-0072` §5) and the waiting screen's *Back to the lobby* (`ADR-0073` §1) rebuild
`initialState()`, and `ADR-0075` records that this is the *only* reason a hole is unreachable:
`rivalPresence`, `graceRemainingMillis` and `rivalReturned` are cleared at no duel or room
boundary, and *"the day a client-side route replaces it — `DEC-054` — every presence field crosses
into the next room."* An answer that routes those two controls ships that leak.

**Three of the six branches cannot have an address at all.** Whether this browser holds a seat is
the server's answer, not the client's: `roomCode` arrives on `RoomJoined`, `view` on the first
`Snapshot`, `outcome` on `DuelFinished`, and `RoomRegistry.resume` may decline. An address that
said *you are at a table* would be a client asserting a game fact — `CLAUDE.md`'s third
non-negotiable — and would be false the moment the room is reaped.

**The case for addresses got thinner, not thicker, while this sat open.**
[`ADR-0067`](ADR-0067-a-leaderboard-row-is-text-and-no-id-turns-into-a-profile.md) made a
leaderboard row text that leads nowhere, and `EPIC-05` recorded the consequence in its own words:
*"`DEC-054` loses its sharpest argument… an address-less client just got more comfortable."*
Nothing in this product needs to *express a link* any more. What is left is Back, the reload, and
the crowding `ADR-0060` predicted.

**A word that is not this question.** `STORY-0412` is titled *"…and which routes are live"*, and
that phrase means **sign-in routes** — the device binding and the credential (`ADR-0037`: *"an
account has two sign-in routes forever"*). It is not about URLs, this ADR does not touch it, and
no acceptance criterion of that story changes because a screen gained an address. The collision of
words is why it is written down here.

## Decision

**The client gets addresses. An address is a URL *fragment* naming a screen the player chose; it is
carried by the History API through two small owned files and no new dependency; and the store
outranks it — a screen the server put the player in has no address and never gets one.**

### 1. The address space is the screens a player navigates to

| Screen | Address |
| --- | --- |
| The first screen | `/` |
| The duel record | `/#/duels` |
| The leaderboard | `/#/leaderboard` |
| `STORY-0412`'s screens | one slug each — however many screens that story turns out to be |

**A slug is the lowercase ASCII form of a word the product already says to a player** — `Your
duels` → `duels`, `Leaderboard` → `leaderboard`. This ADR coins no player-facing vocabulary and
takes none away: the words stay the product owner's, and the slug is a mechanical form of one that
already shipped. It is a **literal in `screen.ts`**, not derived from `HISTORY_HEADING` or
`LADDER_HEADING` at runtime — a URL that changed when `EPIC-06` restyled a heading would break
every link that ever worked.

The set is **open**: a new screen is one entry in one table and one branch. How many account
screens `STORY-0412` has, and what they are called, is that story's to decide and not this ADR's.

### 2. What has no address, and why it never gets one

The waiting screen, the duel table and the result screen. All three are chosen by frames, not by
the player, and §1's rule is the reason: the address names what the player asked for, and nobody
asks to be dealt a hand. A reload while seated lands on `/`, and boot rejoins from `?room=` or from
the code the tab remembers (`ADR-0072`) — **the frames put the player back at the table, and the
address never claims to.**

The day a screen exists that a player must be *entitled* to see, giving it an address is a `DEC`
and not a ticket, because the entitlement is the server's answer and an address must never become a
second claim about it.

### 3. The store outranks the address, always

`Lobby.tsx` keeps its branch order exactly as it stands: `outcome`, then `view`, then `roomCode`,
then the chosen screen. A player on `#/duels` whom a frame seats is shown the duel — `ADR-0060` §5,
unchanged — and the client **replaces** the fragment with `/` so the address does not lie about
where they are. When the two disagree there is one authority, and it is the store.

### 4. The fragment, not a path segment

- **A fragment never 404s.** It is not part of the request, so no host, proxy or rewrite rule has
  an opinion about it. This is the same call `room-link.ts` already made for the one address this
  product depends on, and making the opposite call for the other screens would leave one client
  obeying two contradictory rules about the same hazard.
- **A fragment is never sent to the server**, which is right: no address in this client is a
  request. The server learns what a player is looking at from frames, or not at all.
- **`#/duels`, not `#duels`.** A bare fragment is a fragment *identifier*: the browser looks for
  `id="duels"` and scrolls to it. The leading `/` makes that collision impossible, because no
  element id starts with one.
- **`?room=` is not a route and does not become one.** It is not a screen a player chose; it is an
  instruction consumed once at boot, before the tree exists (`ADR-0032` §2). The two spaces compose
  without either parsing the other: `/?room=ABCD#/duels` is a valid address, and `roomLink` is
  untouched.

### 5. What carries it: two files, no dependency

**`web-client/src/routing/screen.ts` — framework-free and pure**, in the `room-link.ts` and
`duel-state.ts` tradition. It takes strings and never touches `window`, so its tests need no DOM:

```ts
export type Screen = "first" | "duels" | "leaderboard";
export function screenFromHash(hash: string): Screen;
export function hashForScreen(screen: Screen): string;
```

**`web-client/src/routing/use-screen.ts` — the one React-aware file**, `useSyncExternalStore` over
`hashchange`: exactly the primitive `ADR-0032` §3 chose for the store, so the client gains no new
concept and no new library. It exposes the current screen and the two navigations of §6.

**One trap, named because it is silent:** `history.pushState` and `history.replaceState` fire
*neither* `popstate` nor `hashchange`. So a **push is an assignment to `location.hash`**, which
does fire `hashchange` and does add a history entry; and a **replace is `history.replaceState`
followed by the module notifying its own subscribers**, because nothing else will. A version that
pushes through `pushState` and subscribes to `hashchange` renders a stale screen and looks like a
React bug.

`Lobby.tsx` loses `showHistory` and `showLadder` and reads the hook instead. `HistoryScreen` and
`LadderScreen` do not change at all — `ADR-0060` §4 kept them ignorant of navigation, and this is
the decision that collects on it.

### 6. What *Back* does, at every boundary

| Boundary | What happens |
| --- | --- |
| First screen → a chosen screen | Opening one **pushes** an entry. Browser *Back* returns to the first screen **in the same document**: no reload, no second socket, the store untouched. This is the whole fix. |
| A chosen screen, via `ADR-0060` §4's in-page *Back* control | The control stays, and it **replaces** the current entry with `/`. Replacing rather than pushing keeps a lobby↔record ping-pong from growing the stack, and stops the browser's *Back* from retracing into a screen the player has just deliberately left. |
| The first screen | *Back* leaves the client. `/` is the entry point, and there Back means what it means everywhere. Unchanged. |
| A screen reached by typing or following its address | *Back* leaves the client, because there is no entry to return to. No client can synthesise a history entry it never had; this is correct, not a defect. |
| Waiting, the table, the result | Unchanged: they pushed nothing, so *Back* leaves the page exactly as it does today. **No `beforeunload`, no confirmation** — `ADR-0073` §4 refused a confirmation on a comparable path, and whether one is ever offered is the product owner's, not raised here and not needed by this. |
| `DuelResult`'s `<a href="/">` and the waiting screen's *Back to the lobby* | **They stay real page loads.** They are store boundaries, not screen boundaries. `ADR-0075` records that three presence fields are cleared at no boundary and that the hole is unreachable *only* because those two rebuild `initialState()`. Routing them client-side would ship that leak in the same change that fixed *Back*. |

### 7. A reload of an address the player is not entitled to see

**There is no such address**, and this section is the rule that keeps it that way.

- **An address selects a screen. It grants nothing, implies nothing, and preserves nothing.** A
  screen reloaded at its address renders from what the server answered on *that* boot — the same
  read, the same `Welcome`, the same token or none — and never from the address.
- **An address is not a capability.** `#/duels` opened in another browser shows *that* browser's
  record: `readDuelPage` sends `X-Device-Id` read from the storage it was handed, so the answer is
  a function of what the browser holds and never of the address. Sharing one discloses nothing,
  which is why it is safe to have one.
- **Nothing in the space is gated anyway.** `ADR-0036` keeps every screen reachable anonymously,
  `ADR-0060` §3 keeps the door open when the profile read fails, and a browser holding no profile
  reads the record as *"No duels yet."* rather than as a failure.
- **An unknown fragment renders the first screen** and is replaced with `/`. There is no *not
  found* screen and no error: a fragment is not a request, so there is nothing to refuse. That is
  also what a renamed slug and an old bookmark get.

### 8. What does not change

`main.tsx`'s boot — still module scope, still once, still reading `window.location.search` before
the tree exists; the fragment is read *inside* the tree and boot never sees it. The wire, the
server, `PROTOCOL_VERSION`, the schema: nothing. The Vite dev proxy: nothing, and **no deployment
requirement is created** — a fragment reaches no host, which is §4's point. `ADR-0057`'s refusal of
a shareable page-position link stands: an address names the record, never a position in it.

## Consequences

**What it costs.**

- **The address bar is now player-facing text this product owns forever.** `#/duels` can be read,
  pasted and misread, and it is deliberately *not* tied to `HISTORY_HEADING` — so the destination
  now has **two** spellings, which is exactly the thing `ADR-0060` §2 spent a paragraph avoiding
  when it made the door's word a single constant. That divergence is the price of links that keep
  working when `EPIC-06` restyles the copy, and it is a real regression in the tidiness of that
  ADR's rule.
- **The screen survives a reload; the state inside it does not.** `#/duels` lands on the first
  page, under no filter, with an empty search box, because `ADR-0057` forecloses a cursor in a
  link. A player who bookmarks the record after paging into 2023 gets the top of the list back, and
  the address quietly promises more than it delivers.
- **Two navigation authorities, held apart by prose and a branch order.** The store owns three
  branches and the fragment owns the rest; §3 is the rule and nothing mechanical enforces it. The
  next screen that arrives with a plausible-looking address for a server-owned state will be caught
  by review or not at all.
- **A second hand-rolled `useSyncExternalStore` source.** `ADR-0032` accepted *"roughly thirty
  owned lines of store whose notify-and-cache contract we must keep correct ourselves"* rather than
  buy a library; this takes the same trade a second time, and now there are two subscription
  surfaces to keep correct instead of one — including §5's trap, which no type checker catches.
- **The in-page *Back* and the browser's *Back* now coexist and are not the same operation.** One
  replaces, one traverses. A player who uses the control and then presses the browser's sees the
  first screen again and nothing appears to happen. `ADR-0073` §5 refused the word *Back* for the
  waiting screen partly because it *"collides head-on with the browser's own Back"*; this does not
  remove that collision, it defines it and leaves it standing.
- **Every screen story from here owes an address.** One table entry, one branch, one test — small,
  but it is a new obligation, and a screen shipped without one is now a defect rather than the
  norm. `STORY-0412` pays it first.

**What it buys.** *Back* stops leaving the client from the screens a player chose, which is the
harm `ADR-0060` named. A reload keeps the player where they were, which matters most on the one
screen they type into. The record and the leaderboard can be linked and bookmarked. `STORY-0412`
can be split. And the crowding `ADR-0060` predicted gains a second door — an address — without
building the navigation bar that ADR refused.

**What it forecloses.** An address for anything the server decides — the table, the waiting screen,
the result — permanently, and **by rule rather than by omission**, which is the part worth having.
Also server-side rendering and any host-level knowledge of what a player is looking at, neither of
which `ADR-0026` ever had.

**On reversibility, which is why it went this way.** Fragment routing is the option that is undone
by deleting two files and restoring two `useState` flags: no host to reconfigure, no rewrite rule
to un-deploy, no dependency to remove, no build change, nothing on the server. Moving *up* to path
segments later is also open, at the price of keeping fragment reading forever as a redirect for
links already shared. On evidence this thin — nobody has asked to bookmark anything, and `ADR-0067`
just removed the strongest reason to — the cheapest thing to unwind is the right thing to build.

**On timing.** Free today, expensive later, in two distinct ways. `STORY-0412` is about to add
screens, and the pattern the first five share is the one every later screen copies — the argument
`ADR-0032` made about the first screen, holding again. And the sharper one: §6's last row is
load-bearing *only because nobody has written it down*. The longer this stays open, the likelier a
ticket "fixes" `DuelResult`'s anchor into a client-side route and ships `ADR-0075`'s four-field
leak with no ADR anywhere near it.

## Alternatives considered

**Stay a single-address app, and defer again.** The strongest case, and it is strong: it costs
nothing, it is what `ADR-0060`, `ADR-0067` and `EPIC-05` each concluded in turn, and the evidence
for addresses has *weakened* since the question was raised — a leaderboard row is text now, so
nothing in this product needs to express a link; no vision sentence asks for a bookmark; the one
link the product promises already works; and `ADR-0060` guaranteed the conversion would stay cheap
by keeping every screen ignorant of navigation, so waiting costs nothing structural. Rejected
because the thing being deferred is not a feature but a **control that already exists on every
player's device and currently does harm**: deferring is not neutral, it is choosing that the
most-pressed navigation control in the world keeps meaning *leave the product*, on a screen set
that is about to grow by half. And the deferral stops being free in the one place it matters —
each new screen is another `useState` flag in `Lobby.tsx` and another screen shipped with the wrong
*Back*, which is the aggregate `ADR-0060` warned about in a different register.

**React Router.** The strongest case: it is what the ecosystem reaches for; it is maintained by
people who have thought about history edge cases far longer than we have; Back, deep links,
redirects, nested layouts and the trap in §5 all come free and correct; and it has the largest
corpus of any option here, so future work against it is the cheapest kind of work. Rejected on the
reasoning `ADR-0032` used to refuse a store library, which transfers almost word for word:
`ADR-0026` fixed the client's dependency set, and a runtime routing library is a real addition — a
version to track, a supply chain to trust — bought to replace two small files whose hard part,
*which screen is showing*, the store already owns for half the branches. The fit is also
specifically bad here: a `<Routes>` element wants to own what is mounted, but three of six branches
are decided by frames arriving on a socket booted outside the tree (`ADR-0032` §2). Either the duel
branches become routes — a client asserting a game fact — or the router governs a strict minority
of the screens while `Lobby.tsx` keeps its `if` ladder anyway, which is a dependency bought for
part of a job. It is also the easiest option to adopt later, precisely because §5's seam is one
hook — which, on thin evidence, is an argument for not spending it now.

**The History API with path segments — `/duels`, `/leaderboard` — behind the same two owned
files.** The strongest case: it is the address a reader expects and the only one that looks like a
real URL; `pushState` is no harder than a hash and has none of §5's trap; and Vite's dev server
already serves `index.html` for unknown paths, so it costs nothing in development and demos
perfectly. Rejected on a sentence already merged in this repository: a path segment 404s on reload
against a static host with no rewrite rule, and `EPIC-07` — the epic that would own that rule —
**has no file in `tasks/epics/`**. An answer whose correctness depends on a document that does not
exist is a deferral in an ADR's clothes, and its failure mode is the worst available: green in
development, every deep link broken the day it deploys, discovered by a player. It also
contradicts the choice already shipped for the one address this product actually promises. It
becomes the right answer the moment a host with a rewrite rule is chosen, and §1's table is the
only thing that has to change.

**A query parameter beside `?room=` — `/?screen=duels`.** The strongest case: one address space
instead of two; no fragment in the address bar; no 404, since a query never routes; and
`roomCodeFromSearch` already proves the parsing, so it is the smallest diff of any option that
gives addresses at all. Rejected because a query **is** sent to the server on every load, so what
each player is looking at would appear in the access logs and proxies of every host this app is
ever served from — a fact this product publishes nowhere today and has not decided to publish. And
it contaminates the one link that matters: the invite a host copies out of the waiting screen would
carry whatever screen they had open, so `/?room=ABCD&screen=duels` is what a wandering host sends
their rival. A fragment is invisible to the network and drops out of `roomLink` by construction.

**Keep the screens address-less and make browser *Back* work anyway, by pushing a history entry
that changes no URL.** The strongest case: it fixes the one thing that does real harm, costs no
address bar, coins no slugs, publishes nothing, and is fewer lines than §5 — a pure win against the
narrowest reading of the question. Rejected because an entry whose URL does not change is invisible
to a reload and to a share: the record still cannot be linked and a refresh still lands on the
first screen, so `STORY-0412`'s typed-into screen keeps the behaviour that made this urgent. It
also stores navigation somewhere the player cannot see and a test can only observe through
`popstate`, which is the least legible half of the History API. It buys the smallest part of the
problem at nearly the full price.
