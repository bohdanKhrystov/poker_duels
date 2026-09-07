# ADR-0137 — A name suggestion is drawn in the browser, and the vocabulary's size is what makes a refusal rare

- **Status:** Accepted
- **Date:** 2026-09-07
- **Resolves:** `DEC-142` — **the architect's** — by what mechanism a display-name **suggestion** is
  produced: what generates the string, whether the generator consults `name_registry` before
  offering it, and whether the suggestion crosses the wire at all. Registered 2026-09-06 by
  [`ADR-0119`](ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md) §4, which fixed
  the product rules and chose no mechanism
- **Applies, and reopens none of:** `ADR-0119` §4 in full (a suggestion is the field's initial
  value, promises nothing, is refused at one press's cost, encodes nothing about the player, is
  drawn from the product's own register, and its refusal must be rare) and §5 (*nothing is written
  unless the player takes it*); [`ADR-0029`](ADR-0029-a-display-name-is-unique-and-permanent.md) §1
  (the fold and the pinned collation), §2 (trimmed, NFC, 1–32 code points), §3 (the refused
  characters), §5 (**there is no availability-check endpoint** — applied, not reopened), §6 (the
  server fabricates nothing) and §7 (a name is half of nothing);
  [`ADR-0051`](ADR-0051-a-name-is-registered-before-it-is-held.md) §1 (*a string that enters that
  table never leaves it*), §5 (curated contents are operational data, never a migration) and §9
  (**no enumeration surface**); [`ADR-0067`](ADR-0067-a-leaderboard-row-is-text-and-no-id-turns-into-a-profile.md)
  §1 (no id turns into a profile); [`ADR-0130`](ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md)
  §§1–2 and [`ADR-0134`](ADR-0134-a-rename-spends-before-it-replaces.md) §§5–6, both of which bear
  on this and are read in §5 below
- **Supersedes and amends nothing.** Every rule above is used as it stands
- **Constrains:** `STORY-1407` and the design card that is its first ticket (`ADR-0091` §2)
- **No wire change of any kind.** No socket frame, no HTTP route, no `docs/protocol.md` edit;
  `PROTOCOL_VERSION` does not move ([`ADR-0047`](ADR-0047-a-protocol-version-is-claimed-in-a-ledger.md)
  §2's fingerprint has nothing to read), no Kotlin file changes, and no migration is added.
  **`STORY-1407`'s tickets are therefore not `atomic:`** under
  [`ADR-0068`](ADR-0068-an-atomic-ticket-names-the-gate-that-forbids-splitting-it.md) §3 and stay
  inside the ordinary `files_touched: 1..3` cap
- **Ships no word.** The vocabulary's contents are `ADR-0119`'s deliberate omission and stay one

## Context

`ADR-0119` §4 asked for a suggestion that a player can normally accept first try, and the same
paragraph — correctly — refused every instrument that could guarantee it. Four forces meet there.

**The property must come from somewhere other than a check.** `ADR-0029` §5 refuses an availability
endpoint outright, as *"a pure enumeration surface bought for a nicer form"*. `ADR-0051` §9 refuses
any surface that lists the names in use. `ADR-0119` §5 forbids writing a string the player did not
take, because `ADR-0051` §1 lets nothing out of `name_registry` and §9 builds no un-retire — so a
pre-registered suggestion burns one name per browser that reaches the screen. Nothing may ask,
nothing may hold, nothing may claim. *Refusal is rare* therefore has to be produced by the shape of
the draw, and exactly one property is left that can produce it: how large the set drawn from is,
relative to how much of it has been spent.

**A suggestion that has to arrive can fail to arrive, on the one path that must not stall.** The ask
stands between a press and the room (`ADR-0119` §1), on a screen that same ADR already books as a
cost — *"the host meets an interstitial … in a product whose positioning sentence is fast."* A
generator behind a request has a loading state, a timeout and a down state, and the only honest
answers for the down state are an empty field — which `ADR-0119` rejected outright — or a local
fallback, which is the local generator plus a request.

**Two refusals wear the same word and are not the same event.** A `409` costs one press and is
`ADR-0119` §4's accepted price. A `400` is the product putting a string in a field that its own
write path (`canonicalDisplayNameOrNull`, `ADR-0029` §§2–3) will refuse — an offer the product does
not honour, on the screen where it asks a stranger to trust it. Nobody plans for the second one, and
it is the one a mechanism can eliminate completely rather than merely make unlikely.

**The spent set only grows, and since `ADR-0130` it grows faster than the player count.** A rename is
a `set` (§1) and the string it leaves is spent forever (§2, carried out by `ADR-0134` §3's
`TAKEN → REPLACED`). So a namespace sized against *players* is sized against the wrong number; it has
to be sized against *strings ever written*, and that number has no ceiling.

One smaller force, load-bearing in practice: **this client already has a shape for randomness.**
`reconnecting.ts` takes `jitter?: () => number` — *"A number in [0, 1) per attempt. Injected so a
test hands it a value"* — defaulting to `Math.random` at the one place that needs a real one. A
generator that reached for `Math.random` in a render body would churn the field on every render and
make every test that renders the ask nondeterministic, including any recorded-frame drive
(`ADR-0100` §3) whose path ever crosses it.

### The deadline, honestly

**There is none, and that is the strongest single argument in this ADR.** Nothing here writes a row,
moves a wire, takes a `V<n>` or spends a storage key: the whole mechanism is two client modules and
one call site. The vocabulary can be rewritten next month with no migration and no consequence for
any name already taken, because an accepted suggestion is only a string a player chose.

What *is* asymmetric is direction. Adding a server endpoint later is additive and cheap; deleting a
shipped one is not, because by then something calls it and `docs/protocol.md` documents it. So the
cheapest-to-reverse choice is the one that ships no endpoint today, and this ADR takes it for that
reason rather than because the endpoint is a bad idea in the abstract.

## Decision

**A suggestion is drawn in the browser, by a pure function of a random source, from a vocabulary
that ships in the bundle. Nothing is sent, nothing is fetched, and nothing consults `name_registry`.
Refusal is kept rare by the size of the vocabulary, which a test holds at a floor, and refusal by
`400` is made impossible by a second test over every word in it.**

### 1. The string is drawn client-side, and nothing crosses the wire

Said as loudly as `DEC-142` asked for it: **client-side, and no.**

- **No socket frame.** No `ClientMessage` and no `ServerMessage` gains a field, so `ADR-0020`'s
  emitter produces the same `protocol.gen.ts` and `ADR-0047` §2's fingerprint cannot move.
- **No HTTP request.** No route is added, `docs/protocol.md` is not edited, and no shipped endpoint
  is called to produce a suggestion — including `PUT /api/me/name`, which is called only when the
  player confirms, exactly as `ADR-0119` §2 has it.
- **No Kotlin change and no migration.** `poker-server` learns nothing, `name_registry` is untouched,
  and `poker-engine` learns nothing — a duel is played by two seats, and what they are called is a
  server fact.
- **A suggestion therefore cannot fail to arrive.** The ask has no loading state, no error state and
  no window in which the field is empty because something is in flight. The state `ADR-0119`
  rejected — a blank field at the door of a first duel — has no way to occur.
- **`STORY-1407`'s tickets are not `atomic:`** (`ADR-0068` §3) and take no branch lock.

### 2. `suggestName` is a pure function of a random source

```ts
/** The floor §4's first test holds the vocabulary to. */
export const SUGGESTION_SPACE_FLOOR = 1_000_000;

export function suggestName(random: () => number, replacing?: string): string;
```

- **It takes no player fact, and that is why it can encode none.** No `PlayerProfile`, no device id,
  no session, no room code, no clock, no `Storage`. `ADR-0029` §6's refusal of a server-minted
  `Player-3F2A` and `ADR-0067`'s rule that no id turns into a profile hold **by the signature**
  rather than by anybody's care: there is nowhere to put a player fact, and putting one there is a
  parameter added in a diff a reviewer sees.
- **`random` returns a number in `[0, 1)`**, and its default is `Math.random`, supplied at the call
  site (§7) rather than inside the function — `reconnecting.ts`'s `jitter` idiom, unchanged.
- **One entry is drawn from each list**, at
  `Math.min(list.length - 1, Math.floor(random() * list.length))`. The clamp is not defensive noise:
  without it a `random` that returns `1` yields `undefined`, and `undefined` would then be joined
  into a string the player can set as a permanent name.
- **`replacing` is the string being replaced, when there is one.** A draw equal to it is drawn again,
  **at most four draws in total**, then the last is returned. Bounded, because a `random` stuck at
  one value must not spin; the bound is reachable only by a broken source, since §4's floor puts a
  genuine repeat at about one in a million.
- **The randomness is not cryptographic and does not need to be.** `RandomRoomCodeSource` draws from
  `SecureRandom` because a room code *is* an invite; a display name is half of nothing (`ADR-0029`
  §7), and the vocabulary ships in the bundle, so predicting the draw grants nothing that reading the
  bundle does not already grant. The engine's `Rng` is not used either, and could not be: it is
  deterministic by design, which is exactly what a suggestion must not be.

### 3. The vocabulary is one data module, and its arity is data

```ts
// web-client/src/profile/name-vocabulary.ts — imports nothing.
export const NAME_VOCABULARY: readonly (readonly string[])[] = [ /* … */ ];
```

- **It lives at `web-client/src/profile/name-vocabulary.ts`**, beside `name-text.ts`, and
  `name-suggestion.ts` is the only module that reads it. It is **bundled and synchronous** — not a
  JSON asset, not a dynamic import, not a fetch — which is what §1's *cannot fail to arrive* rests
  on.
- **It imports nothing at all**, so the module graph reachable from `suggestName` contains no module
  that can see a profile, a device, a socket or a screen. That closes §2's structural guarantee at
  the other end.
- **The arity is the vocabulary's, not the generator's.** `suggestName` draws one entry from each
  list, in order, and joins them. Two lists or four is a curator's edit to this file: it changes no
  code, needs no ADR, and is the direction §4's floor is expected to be met from.
- **The parts are joined with a single `U+0020`.** `ADR-0029` §3 admits exactly one interior
  separator and refuses it doubled or at either end; §4's rule that no entry contains whitespace
  makes the join self-checking rather than something the joiner has to get right.
- **This ADR ships no word.** The contents are `ADR-0119`'s *"operational data of the same class as
  `ADR-0051` §5's blocklist — curated, not architecture"*, and the ticket that writes them carries
  the human's eye on the words the way `ADR-0091` §3 carries taste on anything minted.

### 4. Three tests hold the vocabulary, and they are the whole of the guarantee

1. **The floor.** The product of the list lengths is `>= SUGGESTION_SPACE_FLOOR` (`1_000_000`). This
   is the mechanism `ADR-0119` §4's *"a name whose refusal is rare"* asks for, expressed as
   arithmetic instead of as a check: with `k` strings spent from inside the vocabulary, a fresh draw
   is refused about `k / N` of the time, so a million combinations keeps first-try refusal near one
   in a hundred even at ten thousand names ever written, and two refusals in a row at about one in
   ten thousand. `k` is bounded by *all* strings ever spent — including every rename's vacated string
   (`ADR-0130` §2) — so the estimate is conservative, since most typed names are not in the
   vocabulary at all. **Three lists of one hundred words meet the floor**; so do two of a thousand.
2. **Every entry is a string the server would accept.** Each entry is non-empty, equals its own NFC
   form, and matches `/^[^\p{Cc}\p{Cf}\s\p{Z}]+$/u` — no control character, no format character, and
   **no whitespace of any kind**. Refusing whitespace *inside* an entry is stronger than `ADR-0029`
   §3 requires, and it is what makes §3's join total: no leading, trailing or doubled space can be
   constructed from entries that contain none.
3. **The joined string fits.** The sum of the longest entry in each list, measured in **code points**
   (`[...entry].length`, never `.length`, which over-counts astral characters exactly as `ADR-0029`
   §2 warns), plus one per separator, is `<= 32`.

Together these make one statement worth reading twice: **a suggestion can be refused with `409`, and
it can never be refused with `400`.** The product never offers a string its own rules reject. Tests 2
and 3 are `O(entries)` and complete — they bound the whole Cartesian product without enumerating it,
which at a million combinations is the difference between a test and a build step.

### 5. The draw happens once per ask, and a reroll fires on `conflict` alone

- **The suggestion is drawn when the ask mounts, as the field's initial state** — a `useState`
  initialiser or equivalent, never in a render body. A value that changed on every render is not a
  field's initial value, and `ADR-0119` §4 says the suggestion *is* the initial value.
- **Nothing is stored.** No `localStorage` key, no `sessionStorage` key, no column, no response
  field. `ADR-0086`'s one-module-owns-each-key rule and
  `web-client/src/protocol/one-module-owns-each-storage-key.test.ts` are untouched. A player shown
  the ask again — `ADR-0119` §1's *answered neither control* — gets a fresh draw, which is correct
  rather than a leak, because nothing about the previous one was ever a fact.
- **A fresh suggestion replaces the field's value only on `conflict`, and only when the player has
  not edited it.** The surface keeps the last string the generator put there; if the field still
  equals that string it is replaced with `suggestName(random, thatString)`, and if it does not, it is
  left alone. That is `ADR-0119` §4's *"a string the player typed themselves is never overwritten"*,
  implemented as an equality rather than as a dirty flag somebody has to remember to clear.
- **No other outcome rerolls.** `rejected` (`400`) cannot describe a suggestion at all (§4);
  `throttled` (`429`, `ADR-0134` §5) is a statement about requests and invalidates nothing the player
  typed; the rest are not about the name. And since `ADR-0134` §5 collapses held, blocked, retired
  and replaced into one `409`, the reroll needs no branch on *why*.
- **The reroll is free against the write budget, and that is why *one press* is true.** `ADR-0134` §6
  meters the string that was **spent**, not the attempt that was made: `admit` before the write,
  `refund` on any result that is not `NameSet`, so a `409` refunds. A player unlucky twice is not one
  press closer to a `429`. Without that clause `ADR-0119` §4's *"costs one press, never a dead end"*
  would have become false at the fifth refusal.

### 6. Nothing consults `name_registry`, and no surface learns whether a name is free

- **No query, no endpoint, no filter, no cached list, no hint.** `ADR-0029` §5's refusal stands
  exactly where it was, and `ADR-0051` §9's *no enumeration surface* is untouched.
- **The suggestion is what `ADR-0119` §4 says it is**: text in a field. The only authority on
  availability is the write, and the way it says so is by succeeding.
- **Rarity is bought with the size of the vocabulary, not with knowledge.** §4's first test is where
  the property lives — a number a suite holds, rather than a claim somebody makes in a review.

### 7. Where it lands

- Two new client modules and their tests: `web-client/src/profile/name-suggestion.ts` and
  `web-client/src/profile/name-vocabulary.ts`.
- **One surface offers a suggestion today: `ADR-0119` §1's ask.** `NameSurface` keeps its empty
  initial value. No merged decision gives the account screen's change form (`ADR-0130` §6) a
  suggestion, and a mechanism does not place itself on a surface a product decision has not asked
  for.
- **The default `random` lives at the ask's own call site**, as an optional prop defaulting to
  `Math.random`. A test that renders the ask directly injects `random` and asserts the exact string;
  a drive that reaches the ask through `App` does not, and must assert §4's rules rather than a
  literal. **A recorded frame containing an un-injected draw can never be byte-identical twice**
  (`ADR-0100` §3), so any drive whose frames reach the ask injects — named here because that failure
  reads as a flake in the generator rather than as a missing parameter.
- No design token, no CSS, no new screen slug; §3's words are the card's.

### 8. What is deliberately not built

- **No suggestion endpoint**, filtered or unfiltered, authenticated or not.
- **No availability check, no *probably free* hint, no strike-through, no debounce on the field.**
- **No stored suggestion**, no history of names already tried, no set of strings to avoid beyond §2's
  single `replacing`.
- **No second vocabulary**, no locale variants, no seasonal list, no server copy.
- **No cryptographic randomness, and no use of the engine's `Rng`.**
- **No measurement of how much of the vocabulary has been spent**, and nothing that raises the floor
  automatically. Both are additive later and neither is pretended to exist.

## Consequences

**What it buys.** `STORY-1407` is unblocked and stays a client story: two modules, one call site,
plain tickets under `files_touched: 1..3`, no protocol row, no migration, no branch lock. The two
rules `ADR-0119` called load-bearing hold by construction rather than by care — *nothing is written
unless the player takes it* is true because nothing is sent at all, and *it encodes nothing about the
player* is true because `suggestName`'s signature has nowhere to put a player. *Refusal is rare*
stops being an aspiration and becomes a floor a test holds. And the failure nobody was looking for —
the product offering a name its own write path refuses with `400` — is eliminated rather than made
unlikely.

**What it costs.**

- **Somebody must author hundreds of words before `STORY-1407` can merge**, and this ADR ships none
  of them. The floor is not reachable with a handful: at three lists it is a hundred words each,
  chosen to read like this product and reviewed by the one eye that can judge that. That is the
  largest single cost of a bundled vocabulary over a server that could have used a short list and a
  filter, and it is real, unglamorous work.
- **The vocabulary is public.** It ships in the bundle, so anyone can read it and — at `ADR-0134`
  §6's rate, with `ADR-0012`'s trivially minted device ids — spend strings out of it deliberately.
  That does not break the mechanism; it degrades it, and it is `ADR-0029`'s already-accepted *"a
  second, irreversible thing to farm"* rather than a new exposure. Server-side generation would have
  bought obscurity here and nothing else.
- **Refusals will still happen, and they will read as the product's fault.** A player who accepts what
  the product offered and is told it is not available experiences a broken suggestion, not a race.
  `ADR-0119` §4 accepted that; this bounds it at roughly `k / N` and does not remove it.
- **The floor decays and nothing watches it.** `k` only grows, and grows on every rename as well as
  every first name. At some spent count the first-try rate stops being acceptable, and the answer is
  more words rather than a new mechanism — but nothing counts `k`, nothing alerts, and §8 refuses to
  pretend otherwise. The day it matters, somebody will learn it from a complaint.
- **The tests hold shape and never meaning.** §4 cannot tell a duelling word from a slur, and
  `ADR-0051` §5's blocklist ships empty, so a suggestion is exactly as safe as the list somebody
  typed. `ADR-0119` said whoever ships a vocabulary owns that it says nothing about anybody; that
  stays a human obligation with no gate behind it.
- **Every player downloads the list**, including the invited rival who by `ADR-0119` §1 never sees the
  ask. A few kilobytes, and a dynamic import would fix it at the price of reintroducing the failure
  mode §1 exists to remove — so it is not fixed.
- **A combination can read oddly.** No pair or triple is reviewed; only the words are. The price of
  turning a hundred words into a million names is that nobody has read all million.

**What it forecloses.**

- **A suggestion that is checked, promised, held or reserved.** Each was already refused by a merged
  ADR; each is now also unreachable, because no code path from `suggestName` can see the registry.
- **A suggestion that varies by player, device, locale, season or duel count**, without a signature
  change.
- **A vocabulary that arrives at runtime** — from an asset, an endpoint or a config — because §1's
  *cannot fail to arrive* is what removes the ask's loading state.
- It does **not** foreclose a server-side generator later. Nothing about the wire changes and
  `suggestName`'s call site is one line in one component, so the client-now → server-later direction
  stays fully open. The reverse — withdrawing a shipped endpoint — is the direction that is not free,
  which is the reason to start here.
- It does **not** foreclose the arity, the words, the join or the floor moving. Three of those four
  are data, and the fourth is a constant beside a test.

## Alternatives considered

**A server endpoint that draws and filters against `name_registry`.** The strongest of the six, and
it nearly justifies its price. It is the only shape in which the suggestion is *known free at the
instant it is offered*, which is as close as anything can get to `ADR-0119` §4's *accepting it
normally works the first time*; the mechanism is one lookup against a table that already exists with
a folded unique index; the vocabulary stays off the client, so nobody can read the list in order to
burn it; and the list may then be small, because the filter carries the property §4's floor otherwise
carries alone. Rejected on four counts, heaviest first. **It puts a network round trip and a database
read between a press and a room**, on the interstitial `ADR-0119` already books as a cost against a
product whose positioning sentence is *fast* — and it must then answer *what the field holds when the
endpoint is down*, where the only answers are the empty field `ADR-0119` rejected or a local
fallback, which is this decision plus an endpoint. **It still promises nothing**: the string can be
spent between the draw and the confirm, so the `409` branch and the reroll exist either way, and the
endpoint removes a probability rather than a case. **It is a partial availability surface** — it
distinguishes free from spent for whatever it returns — bought for a nicer form, which is the
sentence `ADR-0029` §5 used to refuse the general one, and `ADR-0051` §9 refuses surfaces that report
on the names in use. And **it converts `STORY-1407` from a client story into a wire story**: a route,
a `docs/protocol.md` section, a budget of its own (an endpoint doing a per-call database read is
precisely what `ADR-0022`'s shape exists for), and a public surface that cannot be withdrawn cheaply.

**A server endpoint that draws but does not filter.** Strongest case: it keeps the vocabulary
private, which is the one thing a bundled list cannot do, and it reads no registry, so `ADR-0029` §5
is untouched by construction rather than by argument. Rejected because it buys only obscurity — the
list is recoverable by polling the endpoint often enough — and pays the whole of the first count
above for it: a round trip, a down state, a route and a protocol row, for a string the browser can
produce with no network at all.

**Draw from an alphabet instead of a vocabulary** — a random token, or `RoomCode`'s Crockford base32
under another name. The case is substantial: the floor becomes arithmetic instead of curation (eight
symbols over thirty-two is `2^40`, four thousand times over), nobody authors or reviews a single
word, there is no taste surface and no slur to miss, and this repository already owns the generator
and the argument about unbiased draws over a power-of-two alphabet. Rejected because `ADR-0119` §4
requires the string be *"drawn from the product's own register — the vision's duelling vocabulary,
dark, quiet, minimal"*, and `K7QM2XVB` is not a name a person keeps. It is the same object `ADR-0029`
§6 refuses when the server mints `Player-3F2A`, minus the player id — and a suggestion nobody accepts
is an empty field with extra steps.

**Two words and a number** — `Iron Raven 47`. Strongest case: it reaches any floor from a short,
easily curated list, and it is what most products in this space do. Rejected because the digits are
the part that reads as machine-assigned, on a string that is spent forever and printed on a public
ladder for strangers to read. It also hides the cost rather than removing it: the number is what
makes a thin vocabulary work, so the vocabulary stops needing to be good, which is the opposite of
what `ADR-0119` §4 asks a suggestion to be.

**Generate in the browser, then check with the shipped `PUT` before showing it** — send the draw,
draw again on `409`, and put a name in the field that is genuinely free. Strongest case: no new
route, no new endpoint, an availability guarantee bought entirely from surfaces that already exist.
Rejected outright, and recorded because it sits one line away from the accepted design rather than
because it was close: it **writes a name the player did not take**, which is the exact act
`ADR-0119` §5 exists to prevent, and it would burn one string per browser that ever reached the
screen — crawlers, e2e tabs and the author's own devices included — into a table `ADR-0051` §9 builds
no way out of.

**A flat list of complete names, drawn as one.** Strongest case is real: the strings a curator picks
are exactly the strings players see, so no combination can read badly, which is the failure mode of
every `adjective noun` list ever shipped. Rejected on the floor — a flat list reaching a million
entries is a million curated strings, and one that does not reach it fails §4's first test. The
combination is what turns a hundred words into a million names, and its price is one press.

**No test on the vocabulary, only a convention.** Strongest case: the words are reviewed by a human
anyway, the rules are obvious, and three tests over a data file is ceremony. Rejected because the
floor is the *entire* mechanism by which `ADR-0119` §4's rarity property is achieved — a convention
would let a curator halve the list while tidying it and nothing would fail — and because a `400` on a
suggestion is a defect nobody would think to look for until a player reported that the name the
product offered them was refused.

## What this does not settle

- **The words.** Not one is shipped here. Their contents, their register, and the arity that reaches
  §4's floor are the implementing ticket's, under the human's eye, within §§3–4.
- **What the ask says about the suggestion** — whether it is labelled at all, whether a *suggest
  another* control exists for a player who simply dislikes the draw, and every word on the screen.
  `ADR-0119` §3 and the design card (`ADR-0091` §2). This ADR fixes only that a reroll is possible
  and what it costs.
- **Whether any surface other than the ask offers a suggestion.** The account screen's change form
  (`ADR-0130` §6) has none, because no merged decision gives it one; if a later product decision
  does, it calls `suggestName` and nothing here moves.
- **When the floor should be raised, and on what evidence.** Nothing counts spent names and §8 builds
  nothing that would.
- **Moderation of the vocabulary.** `DEC-017`'s remaining half is still the human's, `ADR-0038`'s
  screening is unchanged, and §4 refuses nothing on content.
