# ADR-0086 — The offer's answer is `pd.accountOfferSettled`, owned beside the predicate it feeds

- **Status:** Accepted
- **Date:** 2026-08-28
- **Resolves:** `DEC-080` — which storage key holds
  [`ADR-0085`](ADR-0085-not-again-is-this-browser-and-an-answer-spends-the-offer.md)'s browser-local
  answer, which module owns it, and therefore the third entry
  `web-client/src/protocol/one-module-owns-each-storage-key.test.ts` gains
- **Applies** `ADR-0085` §1 (the bit is written and read through the injected `Storage` and never
  sent) and §2 (an answer spends the offer, and nothing ever clears it). This ADR reopens neither.
  It is downstream of a **product** decision and adds no product surface: no screen, no string, no
  new case in `ADR-0085` §3's table
- **Constrains:** `STORY-0415`'s persistence ticket, which this ADR is written to be the whole input
  for, and one clause of its wiring ticket (§6)
- **No wire change, no schema change, no new endpoint, no migration.** `PROTOCOL_VERSION` does not
  move

## Context

`ADR-0085` §1 settled that the offer's answer is a browser-local bit and then stopped: *"The key's
name, the module that owns it, and the third entry it adds to
`one-module-owns-each-storage-key.test.ts` are `DEC-080`'s."* Four forces decide the rest, and three
of them were measured in this worktree rather than reasoned about.

**The gate that will enforce the answer matches substrings, so the obvious key name is a trap.**
`one-module-owns-each-storage-key.test.ts` (merged by `TASK-041230`) walks `web-client/src`
recursively, skips `*.test.ts` and `*.test.tsx`, and collects the **file names** whose text
`includes` the key literal. `includes` is a substring test. Measured, with two throwaway production
files: a file holding `"pd.accountOfferSettled"` and a file holding `"pd.accountOffer"` are **both**
returned by a scan for `"pd.accountOffer"`, and the row asserting a single owner goes red naming a
file that writes a different key. The natural short name for this fact is therefore the one name it
cannot have if any longer key ever starts with it. Three further measurements, same session: a scan
for a literal only the test file itself holds returns `[]`, so the `.test.ts` exclusion does the work
and the row may carry the literal verbatim; a second production file holding the key **in another
directory** (`src/result/` while the owner sat in `src/protocol/`) reddens the row, so the scan is
whole-`src` and the owner's directory is free; and the row goes green naming the owner, so presence
is real rather than a vacuous empty match.

**Three keys exist and the gate carries two.** `device-id.ts` owns `pd.deviceId`, `session-token.ts`
owns `pd.sessionToken`, and `room-memory.ts` owns `pd.roomCode` — and only the first two have a row.
The third key has been unguarded since it was added, which is the evidence for the deadline below:
*add the row in the next ticket* is a thing this repository has already failed to do once.

**Precedent points at `protocol/`; cohesion points at `result/`.** All three existing keys live in
`web-client/src/protocol/`, and `protocol/index.ts` re-exports two of them — but every one of those
three values crosses the wire or names a wire identity (`X-Device-Id`, `Authorization`, the room a
client rejoins), and `ADR-0085` §1's whole holding is that this bit crosses nothing. Meanwhile the
offer's other three files are already `result/account-offer.ts`, `result/account-offer-text.ts` and
`result/AccountOffer.tsx`, and every read and write of the new bit happens on the result screen.

**The predicate module cannot own it.** `TASK-041502` shipped `result/account-offer.ts` taking
`settled` as an input, with five `verify:` lines pinning that file at zero occurrences of
`localStorage`, `fetch`, `coinBalance`, `finalStacks` and `length` — including inside comments. A key
that lived there would break its own ticket's gates. The owner is a sibling, not that file.

### The deadline

The gate row is not free later. A key that exists in `develop` for even one merged PR without its row
is a key a second writer can be added to unnoticed, and `pd.roomCode` shows that the follow-up
ticket which adds the row afterwards does not get written. The persistence ticket must create the
module and add the row in the same diff. Nothing else here expires: the key, the module and the
stored value are all one file and one test row to change.

## Decision

### 1. The key is `pd.accountOfferSettled`

Namespaced `pd.` like the other three, and named for the **fact** (`settled`) rather than the
feature, so it matches the term `offerAccount` already takes. It is deliberately *not*
`pd.accountOffer`: a key that is a prefix of another key makes the gate's substring scan return two
files for one row, measured above.

### 2. `web-client/src/result/account-offer-settled.ts` owns it, and nothing else names the literal

The module is the only place in `web-client/src` where the string `"pd.accountOfferSettled"` appears
outside a test file, and it exports:

```ts
export const ACCOUNT_OFFER_SETTLED_STORAGE_KEY = "pd.accountOfferSettled";
export function readOfferSettled(storage: Storage): boolean;
export function markOfferSettled(storage: Storage): void;
```

Both take the `Storage` as a parameter — `main.tsx` injects the real `localStorage`, exactly as it
does for the other three keys, and no module in this client reaches for the global itself.

It sits in `result/` and not in `protocol/` because every caller is on the result screen and because
`protocol/` is where values that go on the wire live. Placing a never-sent bit there would make the
directory a reader consults to answer *"what does this client send?"* hold the one value that is
never sent.

The key literal stays **one string literal on one line**, which is what keeps it visible to the scan.

### 3. The stored value is the sentinel `"1"`, and the read fails toward *not settled*

`markOfferSettled` writes `"1"`. `readOfferSettled` returns `true` **iff** the stored string, trimmed,
is exactly `"1"`; absent, blank, and every other value read as `false`, and the player is offered
again after their next win.

The value carries no information and nothing may branch on it. Which control the player pressed is
not recorded, because `ADR-0085` §2 makes the two answers identical in effect and its §Consequences
forecloses any record of who was offered an account.

The failure direction is not invented here: `ADR-0085` §Consequences already chose it, *"takes the
side that risks asking twice over the side that risks never telling them"*. An unrecognised value is
a browser whose answer cannot be read, and this rule asks again rather than silently suppressing a
warning `ADR-0036` §Alternatives says a player otherwise *"learns by losing them"*.

### 4. The module exports no way to clear the bit

There is no `forget…`, no `clearOfferSettled`, no argument that unsets. `ADR-0085` §2's *"nothing in
the product ever clears the bit"* is therefore mechanical rather than a promise: an un-dismiss has to
add an export, which is a diff a reviewer sees.

`signOut` is **not** changed. It clears the session token and the room code and will not touch this
key. That leaves `ADR-0085` §Consequences' signed-out-account-holder case exactly where that ADR left
it — named, not solved — rather than quietly deciding it here.

### 5. The gate's third row, exactly

`web-client/src/protocol/one-module-owns-each-storage-key.test.ts` goes from two tests to three:

```ts
it("only the account-offer-settled module writes the offer-settled key", () => {
  expect(productionSourcesContaining("pd.accountOfferSettled")).toEqual([
    "account-offer-settled.ts",
  ]);
});
```

Three properties make it worth having, and a row missing any of them is green and proves nothing:

- **Presence.** The expected array is non-empty and names the owner, so a row that matches nothing
  is red. A row asserting `toEqual([])` would pass against a key that does not exist.
- **Self-exclusion.** The literal may be written verbatim, because the walk skips `*.test.ts` —
  measured, not assumed. The row does not match its own file.
- **Discrimination.** Three keys now resolve to three **different** file names, so the scan is doing
  work rather than returning everything.

### 6. The one clause this fixes for the wiring ticket

`ADR-0085` §2 makes **both** controls answers, and the merged `AccountOffer` has a callback for only
one of them: `onDismiss`, plus an `<a href>` accept control with no handler, because `ADR-0076` §3
forces the accept path to be a real page load. So `markOfferSettled` runs from a click handler on
that anchor, before the browser navigates — the precedent is already merged in the same file:
`DuelResult`'s `onLeave` prop, whose KDoc reads *"The link stays an `<a href="/">`, so the handler
runs and navigation stays the browser's. Storage operations are synchronous, so a handler that
forgets has finished before the page leaves."*

The alternative — marking it settled when the account screen loads — is wrong and is refused here, because
the lobby's own account control reaches that screen too, and it would settle an offer that was never
made.

### 7. What the persistence ticket is

Two files created, one edited: `result/account-offer-settled.ts`,
`result/account-offer-settled.test.ts`, and the third row appended to the gate. Its tests must
include the two that are not tautologies — a value the module did not write (`"0"`, or a blank) read
back as **not settled**, and `markOfferSettled` proved idempotent, since `ADR-0085` §2 permits either
control to run it and does not say which ran first. Reading back only what the module itself just
wrote cannot tell a real read from a constant.

## Consequences

**What it buys.** The persistence ticket is now a `## Files` table and a `## Tests` table with
nothing left to invent, and it lands with its own gate row instead of promising one. `settled` gets a
source that matches the term's name. The offer's four files sit together, so a reader who opens
`result/` sees the whole feature, and `protocol/` keeps meaning *what this client sends*. And the
whole answer is one file and one test row — the cheapest thing in this story to reverse, which is why
it was decided rather than deferred on thin evidence.

**What it costs.**

- **A row that outlives its meaning, with nothing able to remove it.** §4 gives the product no way to
  clear the key, so a browser carries `pd.accountOfferSettled` forever — including long after the
  player signed up from the lobby and the bit can never be read again, because `signedIn` short-
  circuits the predicate first. It is dead storage by design, and the design is what makes it dead.
- **A fourth key in a namespace that is the only separation there is.** `pd.` is a prefix
  convention, not a mechanism; the four keys share one origin's bucket with anything else ever served
  from it. Worse, the namespace now carries a **shape rule** — no key may contain another as a
  substring — that nothing enforces. The gate catches the symptom (two files in one row) and not the
  cause (a name chosen as a prefix), and only if someone remembers to write the row at all, which
  `pd.roomCode` shows is not automatic.
- **Clearing site data re-offers the account.** `ADR-0085` accepted this for a different reason —
  the profile and its coins go too, a larger loss than the prompt. This ADR makes it mechanical:
  there is exactly one copy of the bit, in the bucket a privacy clear empties, and no second place
  to reconstruct it from.
- **A sentinel nobody else in this client uses.** The other three modules store an opaque value and
  guard only for blank. This one compares against a magic `"1"`, so a developer who hand-writes
  `true` in devtools sees the offer return and reasonably concludes the flag is broken. That surprise
  is the price of §3's failure direction.
- **One more literal that has to stay a single line forever.** A future reformat, a constant
  extracted for tidiness, or a key assembled from parts silently empties the row's match set — and
  the row would then be red, which is the good case; the bad case is the same edit made to the
  *owner's* literal while a second writer holds the whole string, which reads as a clean ownership
  transfer.
- **An unguarded `setItem`.** A `Storage` that refuses the write loses the answer and the offer
  returns after the next win. This is the same exposure the other two key modules already carry;
  this ADR neither adds a guard nor fixes theirs, and the failure lands on §3's chosen side.
- **A modified click on the accept control settles the offer without leaving the page.** §6's handler
  runs on a middle-click or a cmd-click that opens the account screen in a new tab, so the original
  tab has spent the offer while still showing it. Correct under `ADR-0085` §2 — the player answered —
  and still a state where the screen and the storage disagree until something re-reads.

**What it forecloses.**

- **Any local record of *which* answer was given.** §3's sentinel carries no information, so the
  question `ADR-0085` §Consequences names as its sharpest cost — the player who accepted and
  abandoned — cannot be answered from this browser either. Reversing that is not a value change: the
  key has no version field, so a second value format has to tolerate `"1"` forever or ship a
  migration for a key nothing migrates.
- **Counting anything.** No shown-count, no timestamp, no last-seen. `ADR-0085` §6 forbids the
  behaviour; storing a bare bit forecloses the data even for a debugging session.
- **`protocol/` as the one place to ask what this browser stores.** After this, the answer is spread
  over two directories, and the only complete list is the gate's rows.

## Alternatives considered

**`web-client/src/protocol/account-offer-settled.ts`.** The strongest case for it is precedent and
discoverability: every storage key in this client lives in `protocol/` today, `protocol/index.ts`
already re-exports two of them as a barrel, the one-module gate itself lives in that directory, and a
maintainer asking *"what does this browser keep?"* has exactly one place to look. Rejected because
the property those three keys share is the one this key does not have — each of them is a wire fact
(`X-Device-Id`, `Authorization`, the room code a client rejoins with), and `ADR-0085` §1's holding is
that this bit is never sent. The co-location was never a written rule, and it is already incomplete:
`SESSION_TOKEN_STORAGE_KEY` is not in the barrel. The gate does not care either way — measured, a
second writer in `src/result/` reddened a row whose owner sat in `src/protocol/` — so the choice is
about what a directory means, and cohesion with the three `account-offer*` files that make up this
feature wins.

**`pd.accountOffer` as the key.** The better name by every ordinary standard: shorter, it names the
thing rather than a state of it, and there is only one fact about the offer so a qualifier looks like
noise. Rejected on a measurement rather than taste. The gate scans with `String.includes`, so the day
any key begins with those characters — `pd.accountOfferShownAt`, `pd.accountOfferVersion` — the row
for `pd.accountOffer` returns two files and fails as though a second module had claimed the key. The
failure is confusing precisely because it is not a second writer, and the reader's first instinct
would be to weaken the row.

**No new key at all: derive `settled` from something already stored.** Its case is real — the
cheapest key is the one not added, and this browser already stores a session token, a device id and a
room code. Rejected because none of them holds the fact. A token means *signed in*, which is already
the predicate's second term; a player who pressed *Not now* holds no token, no new device id and no
new room code, and is behaviourally identical in storage to a player who has never seen the offer.
`ADR-0085` §2 requires that both controls settle it, and one of the two leads to no credential at
all, so nothing already stored can distinguish an answered offer from an unasked one.

**Presence-only: settled iff `getItem(key) !== null`.** The simplest read available, it cannot
mis-parse anything, and it is what most codebases write. Rejected for two reasons. It fails in the
direction `ADR-0085` §Consequences explicitly rejected — any stray value, from any source, suppresses
the offer permanently and the player is never told their coins are device-bound. And it cannot be
tested without a tautology: there is no value that exists and reads `false`, so every test writes
through the module and reads back through it, which cannot tell a real read from a constant `true`.
§3's sentinel gives the test a second input.

**Store which control was pressed — `"dismissed"` or `"accepted"`.** The same single write, no extra
key, no extra code path, and it preserves the one fact anybody will plausibly want later:
`ADR-0085` §Consequences names *"a player who accepts and abandons is never asked again"* as the
sharpest cost of the whole feature, and this value is the only thing that could ever identify that
player locally. Rejected because `ADR-0085` §2 makes the two answers identical in effect, so the
distinction would be read by nothing and would sit there as an invitation to branch on it without an
ADR — and because the record it enables is exactly what `ADR-0085` forecloses, one origin at a time.
On the day the product wants it, it takes an ADR that supersedes that foreclosure, and the value
format changes with it.

**`sessionStorage` instead of `localStorage`.** Genuinely attractive against the first cost above: a
key that expires with the tab never outlives its meaning, needs no cleanup, and is the least storage
a browser can be asked to carry. Rejected because it is not free to choose — `ADR-0085` §4 fixes what
the dismissal survives, *"a reload, a new tab, closing and reopening the browser"*, and
`sessionStorage` survives the first of those three and neither of the others. Choosing it would
reverse a merged product decision under the cover of a technical one.

**A `Storage`-agnostic settings module holding every client flag.** The tidy end state, and it would
absorb the next flag for free instead of adding a fifth key. Rejected as churn bought on speculation:
it is one flag, the shape would be guessed from a single example, and the gate is written against key
literals and file names, so a module holding several keys weakens exactly the property the gate
exists to prove. Cheaper to write it when there is a second flag and the shape is evidence.

## What this does not settle

- **`pd.roomCode` has no row in the gate.** Found while measuring, out of scope to fix here, and
  named so it becomes a ticket rather than a discovery someone else makes twice.
- **Whether signing out should re-offer the account.** `ADR-0085` §Consequences left the signed-out
  account holder *named, not solved*; §4 keeps `signOut` unchanged so this ADR does not decide it by
  accident. If it is ever revisited it is the product owner's, not the architect's.
- **The wiring itself.** §6 fixes only where the accept-side write happens. Which component holds the
  `settled` state, and how the offer disappears after a dismissal without a reload, is the wiring
  ticket's.
- **Whether the offer is ever measured.** Nothing measures it, by `ADR-0085`, and §3 removes the data
  as well as the behaviour.
