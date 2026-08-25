# ADR-0081 — A mailed link is a fragment route, and the token is the segment behind the slug

- **Status:** Accepted
- **Date:** 2026-08-25
- **Resolves:** `DEC-075` — does the mailed recovery link survive a static host with no rewrite
  rule, and if it becomes a fragment route, what is the slug?
- **Amends:** [`ADR-0031`](ADR-0031-an-optional-verified-recovery-email.md) §4's mailed link — one
  sentence of it, the URL shape — and
  [`ADR-0077`](ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md) §6's
  `RecoveryLinks`, which transcribed it. **Everything else in §4 stands byte-unchanged**: the token,
  its entropy, its hash at rest, its one hour, its single use, its fragment, its refusal of a query
  string, and `baseUrl` never coming from a header
- **Builds on:** [`ADR-0076`](ADR-0076-a-screen-the-player-chose-has-an-address.md) §1 (the address
  table, which gains two rows), §4 (the fragment and why), §5 (`screen.ts`, which gains one
  function), §7 (an address is not a capability) — and §2, which required that this arrive as a
  `DEC` and not as a ticket
- **Constrains:** `TASK-041633`, which changes in two string literals and nothing else; `STORY-0417`,
  which gains two fixed addresses and keeps every other one; and `EPIC-07`, which now needs no
  rewrite rule for recovery mail to work anywhere

## Context

`ADR-0031` §4 mails `<baseUrl>/reset#token=<token>`, and `ADR-0077` §6 built both links in one
function so this could be changed in one place. `<baseUrl>/verify#token=<token>` is the same shape.

### The forces

**The part §4 argued for was the token, not the path.** Its whole paragraph is about keeping a
bearer secret out of access logs, proxy logs and `Referer` headers, and the fragment does that
perfectly. `/reset` was not chosen over anything; it is what a URL looks like when you write one.

**But the client is one document, served at `/`.** `main.tsx` boots once per tab (`ADR-0032` §2) and
`index.html` is the only thing a host serves. `GET /reset` against a static host with no rewrite
rule is a `404`, and `EPIC-07` — the epic that would own such a rule — **still has no file in
`tasks/epics/`**. Two merged artifacts already refused to depend on it: `room-link.ts` says *"the
code is a query parameter because a path segment would 404 on reload against a static host with no
rewrite rule, and `EPIC-07` has not chosen one"*, and `ADR-0076` §4 generalised that to every client
address — *"a fragment never 404s… no host, proxy or rewrite rule has an opinion about it."*

**The failure this exposes is the worst one available in this product, on four counts.** It is
**silent** — a `404` tells the player the product is broken, not that recovery is. It is
**deterministic**, not a flake: every reset mail that deployment ever sends carries the same dead
path, so asking again produces the same link. It is **invisible to every test here** — Vite's dev
server serves `index.html` for unknown paths, so it is green in development and broken only on the
host. And it is **permanent**: `ADR-0031`'s Consequences make a failed recovery a total loss of the
account, its coins and its ladder place, so a `404` converts a player who did everything right into
the opted-out case they explicitly opted out of.

**Against that, moving it is an amendment to a merged ADR, not a gap-fill.** `ADR-0077` was right to
transcribe §4 rather than reinterpret it. And `ADR-0076` did not get its fragments for free — it
recorded six costs, two of which land here: the destination now has two spellings, and every screen
story owes an address.

**A shape constraint the answer cannot break.** `TASK-041620` — *"a reset takes a token in a body,
and never in a URL"* — is already cut, with `theTokenIsNotAcceptedAsAQueryParameter` gating it, and
`ADR-0027` refused bearer secrets in URLs outright. Any answer that moves the token in front of the
`#` contradicts a shipped position.

**And a rule that says this is a decision rather than a ticket.** `ADR-0076` §2: *"The day a screen
exists that a player must be entitled to see, giving it an address is a `DEC` and not a ticket,
because the entitlement is the server's answer and an address must never become a second claim about
it."* A reset screen is the first screen in this product whose subject is a capability. This is that
day, and §7 below is the part of the answer that rule was asking for.

The tension is real: the option that *looks* safest — a real path, `/reset` — is the one that
disappears on the likeliest host; and the option that never disappears puts a second grammar into
the fragment and asks the client to parse a 256-bit secret out of its own address bar.

## Decision

**Both mailed links become fragment routes on the client's single address. `RecoveryLinks` returns
`"$baseUrl/#/reset/$token"` and `"$baseUrl/#/verify/$token"`. The slugs are `reset` and `verify`,
fixed here because the server mints them into a mail; every other recovery screen's address remains
`STORY-0417`'s. A stale or spent link is a screen that renders and a `400` on submission — never a
routing outcome.**

### 1. The two links, character for character

| | |
| --- | --- |
| Reset | `https://duels.test/#/reset/abc123` |
| Verification | `https://duels.test/#/verify/abc123` |

for `RecoveryLinks("https://duels.test")` and the token `abc123`. The `/` before the `#` is written
by `RecoveryLinks`, not by `baseUrl` — `ADR-0077` §6 requires an origin with **no trailing slash**,
and this matches `roomLink`'s already-merged `${origin}/?room=${code}`.

`ADR-0076` §1's table gains two rows:

| Screen | Address |
| --- | --- |
| Set a new password | `/#/reset` |
| Finish verifying an address | `/#/verify` |

The **address** is the slug alone. A mailed link is that address with the token appended as one more
segment; §5 says how the two come apart again.

### 2. The token stays behind the `#`, and no `?` appears anywhere

Everything after the first `#` is a fragment: never transmitted, never in an access log, never in a
proxy record, never in a `Referer`. That is `ADR-0031` §4's property, unchanged — the token has
simply moved from `#token=abc123` to the second segment of `#/reset/abc123`. **It is not a URL path
segment**; it is a path segment *of the fragment*, and the distinction is the whole decision, because
the phrase "path segment" is what both sides of `DEC-075` were arguing about.

**A recovery link contains no `?` at all**, and that rule is absolute rather than conditional. It is
already gated: `TASK-041633`'s `neitherLinkCarriesTheTokenInAQueryString` asserts it, and it survives
this ADR word for word. An absolute rule is a one-character audit; the alternative — *a `?` is fine,
but only after the first `#`* — is a rule that erodes, and §Alternatives says why that decided the
shape.

`TASK-041620` is untouched and not contradicted. Its subject is what the **server** reads: the
handler still decodes the token from a body and contains no `queryParameters` and no
`call.parameters`. The token is in the mailed URL because `ADR-0031` §4 put it there; nothing sends
it anywhere but a request body.

### 3. The slugs are `reset` and `verify`, and this ADR coins neither

`ADR-0076` §1 keeps screen vocabulary with the story that names the screen, and the `DEC-075`
register applied that rule to these two. It cuts the other way here, for a reason the register did
not have: **these addresses are minted by the server, in Kotlin, into a mail.** The client only
recognises them. A slug one module writes and another parses is a contract between two ends with no
shared artifact, and it must be fixed by whatever fixes the link.

Neither word is new. `ADR-0031` §4 wrote `reset`; `ADR-0077` §6 wrote `verify`. Both are merged. This
ADR preserves them and changes only what surrounds them.

**`STORY-0417` keeps everything `ADR-0076` §1 actually gives it** — the account screen's address, the
*forgot password* screen's, and however many screens that story turns out to have, each a slug of a
word the product already says. It loses only the two it cannot choose alone. If `STORY-0412` or
`EPIC-06` ends up calling either screen something else, **the address does not follow**: `ADR-0076`
already accepted that a destination has two spellings, and one of them is a URL that must keep
working.

### 4. `verify` and `reset` are answered the same way

The verification link's failure looks softer — a player who never verifies still has the account they
had, and `claimPending` replaces the pending row in one transaction, so a second attempt is immediate
(`ADR-0077` §5). It is not softer, for two reasons.

**The failure is deterministic, so retrying is not a workaround.** The second mail carries the same
dead path, and so does every one after it.

**A failed verification is what creates the total-loss state.** `ADR-0031` §3 is explicit: an
unverified address resets nothing and is not stored in `recovery_email` at all, and *"a player who
attaches an address and never clicks the link has not opted in; they have only intended to."* A dead
`/verify` link leaves a player carrying the opted-out risk in full while believing they do not.

A split answer would also leave this client obeying two contradictory rules about one hazard, which
is the defect `ADR-0076` §4 refused to introduce and the reason `DEC-075` was raised at all.

### 5. What the client does with a fragment that has two segments

`ADR-0076` §5's `screen.ts` gains one function and one rule; `use-screen.ts` is unchanged in kind.

```ts
export type Screen = "first" | "duels" | "leaderboard" | "verify" | "reset";
export function screenFromHash(hash: string): Screen;
export function tokenFromHash(hash: string): string | null;
export function hashForScreen(screen: Screen): string;
```

- **A fragment is `#/<slug>`, optionally followed by `/<token>`.** `screenFromHash` matches the
  **first segment** against the table and ignores everything after it. A first segment that is not in
  the table is unknown and takes `ADR-0076` §7's path: the first screen, and the address replaced
  with `/`.
- **`tokenFromHash` returns the second segment, or `null`** when there is none or it is empty. The
  token is **not** part of `Screen` and never enters the routing module's state: a screen is a
  screen, and a capability is not an address (§7).
- **`hashForScreen` never emits a token.** `hashForScreen("reset")` is `#/reset`.
- **The token is read once, at mount, into component state**; the fragment is then replaced with
  `hashForScreen(screen)` through the same replace path `ADR-0076` §5 specifies, so the module's
  cache stays honest and the screen does not move. This is `ADR-0031` §4's *"clears the hash with
  `history.replaceState`"* under a fragment route: the token leaves the address bar and the current
  history entry, and the screen stays where it is.
- **A screen that re-derives its token from the address after that replace finds nothing.** That is
  the same class of silent bug as `ADR-0076` §5's trap — no type checker catches it — and it is why
  the read is once and the token lives in state.

**Both ends assert against the literals in §1.** The server's test compares
`RecoveryLinks("https://duels.test").reset("abc123")` to `"https://duels.test/#/reset/abc123"`;
`STORY-0417`'s client test feeds that same literal string to `screenFromHash` and `tokenFromHash` and
expects `"reset"` and `"abc123"`. Neither test may rebuild the string from parts — `TASK-041633`
already records why, and the same reason holds on the client. Nothing mechanical links the two ends;
see the first cost.

### 6. A stale or already-spent link

**It is a known address with a dead input. The client renders the screen and refuses nothing.**

- **The client never inspects the token and never routes on it.** It cannot tell a live token from a
  spent one, and it must not learn: `ADR-0080` deliberately left no way to ask, and an endpoint that
  answered would be a liveness oracle for `password_reset` that `ADR-0031` spent 256 bits to avoid
  having to defend.
- **The refusal arrives from the server, on submission.** `reset-password` answers `400` for a token
  that is unknown, expired or already consumed — indistinguishable by `ADR-0031` §5 — and under
  `ADR-0080` a `422` for a password that fails policy arrives *before* the token is touched.
  `verify-email` answers `400` for the same three, and `409` for an address already verified to
  another player.
- **A missing token is not an error address.** `#/reset` with no second segment is what the address
  becomes after §5's replace and what a reload lands on. It renders the same screen with nothing in
  hand — not a `404`, not the first screen, not an error boundary. An absent capability is an empty
  input, not an unknown address.
- **The sentence each of those shows a player is not this ADR's.** `ADR-0080` already recorded that
  `STORY-0417`'s form must move from *password refused* to *link expired* without contradicting
  itself, and copy is `STORY-0412`'s under `ADR-0031`'s *What this does not settle*. The routing is
  decided here; the words already have an owner, so no decision is raised for them.

### 7. The address claims nothing, which is what `ADR-0076` §2 asked

The screen renders **identically** for a live token, a dead token and no token at all. It has no way
to distinguish them, by construction (§6), and the server is the only thing that ever does. So the
address selects a screen and grants nothing — `ADR-0076` §7's rule holds here unchanged, and
`#/reset` is not a second claim about an entitlement.

It follows that `#/reset` and `#/verify` are reachable by typing, like everything else in that space,
and that sharing one discloses nothing. **The capability is the token, and the token is an input the
screen submits — never part of what the address means.**

### 8. What changes, concretely

- **`RecoveryLinks`: two string literals.** `"$baseUrl/reset#token=$token"` becomes
  `"$baseUrl/#/reset/$token"`, and `"$baseUrl/verify#token=$token"` becomes
  `"$baseUrl/#/verify/$token"`. That is the entire server change, which is `ADR-0077` §6's
  one-function promise collected.
- **`TASK-041633` changes in two literals and its own `DEC-075` note.** Its other four tests, its
  no-`?` criterion, its no-encoder refusal and its `Host`-header sweep survive **verbatim** — §2's
  point, not a coincidence — as does Proof step 1, which mutates `reset` to a query string.
- **`TASK-041632` is untouched.** `baseUrl` is still an absolute origin with no trailing slash.
- **`TASK-041620` is untouched**, per §2.
- **`poker-engine` learns nothing, no wire type moves, no `PROTOCOL_VERSION` step, no schema
  change, no new dependency, and no deployment requirement is created** — a fragment reaches no host,
  which is the point.
- **`STORY-0417` gains** the two addresses of §1, the parsing rule and the read-once/replace rule of
  §5, and the stale-link behaviour of §6. It keeps its own slugs for its own screens.

## Consequences

**What it costs.**

- **The agreement between the two ends is two literals in two modules, held together by prose.** The
  fragment never crosses the wire, so nothing can carry it: `protocol.gen.ts` is emitted from
  `ClientMessage` and `ServerMessage` only, and a fragment is neither. If the client's slug and the
  server's ever diverge, a mailed link renders the **first screen** (`ADR-0076` §7) and the player is
  silently in the lobby having spent one of their four recovery mails an hour. This is `ADR-0076`'s
  *"two navigation authorities, held apart by prose and a branch order"* cost paid a second time, in a
  worse place: this one loses accounts.
- **The fragment now has two grammars — a screen, and a screen with a secret behind it.**
  `screenFromHash` moves from an equality to a first-segment match, so `#/duels/anything` now renders
  the record rather than the first screen. That is a real widening of `ADR-0076` §7's *unknown
  fragment* rule, and it is chosen deliberately: the alternative discards a token by calling a known
  screen unknown.
- **A recovery link opened in a tab that is already seated is destroyed.** `ADR-0076` §3 has the store
  outrank the address; a player who follows a reset link in a tab holding a live duel sees the table,
  the fragment is replaced with `/`, and the token goes with it. Fifteen minutes and a second request
  is the whole recovery (`ADR-0031` §5's suppression window), and §3 is not being carved out for this.
- **The token is in the address bar until §5's replace runs.** `ADR-0031` §4's exposures — the mailbox
  and the browser's own history of the navigation — are unchanged, and this adds the shoulder of
  whoever is standing behind the player for the width of one render. One hour and one use remains the
  entire mitigation.
- **`/reset` and `/verify` become dead addresses that some hosts will happily serve.** The day
  `EPIC-07` picks a host with a rewrite rule, `<baseUrl>/reset` resolves to the client and shows the
  first screen with no token, so a reader who remembers `ADR-0031` §4 will type it and conclude
  recovery is broken. The two spellings `ADR-0076` accepted for the record are now two spellings that
  differ in whether they work.
- **Two player-facing words are fixed in a URL before `STORY-0412` has named the screens** (§3). The
  slug rule's source is weaker here than `ADR-0076` §1 intends: `duels` came from *Your duels*, and
  these two come from two ADRs.

**What it buys.** Recovery works on the cheapest host anyone could choose — an object store serving
one file at `/` — which is the deployment `EPIC-07` is most likely to pick and the exact one under
which `ADR-0031` §4 fails silently, permanently and invisibly to every test in this repository. One
rule about one hazard across the whole client, so `room-link.ts`, `ADR-0076` and the mail stop
disagreeing. `STORY-0417` can be split without knowing a host, and `EPIC-07` inherits no requirement
it would otherwise have discovered from a player who could not get back in.

**What it forecloses.**

- **A rewrite rule as a precondition of recovery mail working**, permanently. No deployment can be
  wrong about it, because there is nothing to configure.
- **A second parameter in a mailed link.** The second fragment segment is the token and nothing else;
  a link carrying two things needs an ADR. That is `ADR-0031` §6's *recovery only* enforced by shape
  one more time.
- It does **not** foreclose path segments later, and it does not foreclose a router: `screenFromHash`
  is still the only thing that reads an address.

**On reversibility, which is why it went this way.** The mailed address space is the one place in
this product where changing a slug is nearly free: a reset link lives **one hour** and a verification
link **24 hours** (`ADR-0031` §3, §4), so every address this decision mints has expired a day after
it is changed. Moving back to `/reset` the day a host with a rewrite rule exists costs two literals
on the server, two on the client, and a day of accepting both forms — against a bookmarked `#/duels`,
which `ADR-0076` must honour forever. The evidence about hosts is thin and will stay thin until
`EPIC-07` exists; the form that never `404`s is both the cheapest to unwind and the only one whose
failure cannot be.

**On timing.** Free today, and not later, in two ways. No sender is configured, so **no link is in
any mailbox** and nothing in flight is stranded; the moment `EPIC-07` configures one there is a
24-hour window of live links behind every change. And `STORY-0417` has not been split — a client
built against `ADR-0031` §4 would carry the path assumption into `screen.ts`, into `main.tsx`'s boot
and into a dev server that serves `index.html` for `/reset` regardless, so the defect would ship
green and surface on the host.

## Alternatives considered

**Keep `<baseUrl>/reset#token=…` and require `EPIC-07` to configure a rewrite rule.** The strongest
case is strong: it is the address every reader expects; it is what `ADR-0031` §4 decided and
`ADR-0077` §6 transcribed, so it needs no amendment to a merged ADR and no change at all; the rule it
depends on is one line on every host that has ever served a single-page app; and the token was
*already* safe, because the fragment is the only part §4 argued for. Rejected on the sentence that
already decided `room-link.ts` and `ADR-0076` §4: the rule lives in a document that does not exist,
so the correctness of the one URL in this product whose failure is unrecoverable is currently owned
by nobody. Add the four properties from the Context — silent, deterministic, invisible to every test
here, permanent — and this is the only option whose defect cannot be found by anything but a player.

**`<baseUrl>/#/reset?token=…` — a query section inside the fragment.** The strongest case, and this
was the close call: it keeps `token=` a **named** parameter rather than a position, so both a reader
of the URL and a reader of the code can see what the tail is; it is the shape every hash router in
the ecosystem emits, so if this client ever adopts one — `ADR-0076` left that door open and called it
*"the easiest option to adopt later"* — the mailed links already work; `URLSearchParams` parses it
with a standard API instead of a hand-rolled split; and it extends to a second parameter without a
positional convention. Rejected because it makes the rule about where this particular secret may
appear **conditional**. Today that rule is absolute and one character wide — *a recovery link
contains no `?`* — and it is gated by a test that already exists. Under a query section it becomes
*a `?` is fine, but only after the first `#`*, and the edit that breaks it is the deletion of two
characters from a string that still looks entirely ordinary, inside a function no test outside its
own file exercises. A path-shaped fragment keeps one grammar in the fragment and one absolute rule
about the token, and costs a parameter name nobody reads.

**`<baseUrl>/?screen=reset#token=…` — the screen in the query, the token in the fragment.** The
strongest case is that it changes the least: `#token=…` survives from `ADR-0031` §4 byte for byte, a
query never routes so it never `404`s, and `main.tsx` already reads `window.location.search` at boot
(`ADR-0032` §2), so the screen would be known before the tree exists — arguably where a mailed screen
*should* be decided. Rejected twice over. `ADR-0076` §4 refused `?screen=` for ordinary screens
because a query is sent to every host and proxy, and *this player is resetting their password* is a
stronger thing to publish in an access log than *this player is looking at the leaderboard*; the
token is safe either way, but the fact is not. And it puts a second key into the one query space this
product promises — `roomLink` is `${origin}/?room=${code}` — which is how a wandering host ends up
mailing their rival `/?room=ABCD&screen=reset`.

**Answer `reset` now and leave `verify` a path segment until `STORY-0417` reaches it.** The strongest
case: the verification link's failure genuinely is less bad in the moment — the account survives, and
`claimPending` replaces the pending row in one transaction so a second attempt is immediate rather
than fifteen minutes away — and deciding one link is strictly less than deciding two. Rejected
because the failure is deterministic, so the immediate second attempt mails the same dead link; and
because §4's point stands: a failed verification is what *creates* the total-loss state, since
`ADR-0031` §3 makes an unverified address recover nothing while the player believes they have opted
in. The split would also leave one client obeying two rules about one hazard, which is the thing
`DEC-075` exists to stop.

**Leave both slugs to `STORY-0417`, as the register suggested.** The strongest case is the standing
rule and the register's own: `ADR-0076` §1 keeps screen vocabulary with the story that names the
screen, and an architect coining `reset` is an architect choosing a word a player reads. Rejected on
the distinction in §3 — these two addresses are *minted by the server into a mail*, so a story that
picks the slug later has to reach back into `RecoveryLinks` to keep the two ends equal, and the
window in which they disagree is one where every mailed link lands on the lobby. Neither word is
being coined anyway: `ADR-0031` §4 and `ADR-0077` §6 wrote them and both are merged. The rule keeps
everything it was written for — `STORY-0417` still names its own screens — and gives up only the two
addresses a single story cannot own.
