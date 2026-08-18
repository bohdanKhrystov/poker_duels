# ADR-0057 — A cursor names the filter it was drawn under

- **Status:** Accepted
- **Date:** 2026-08-18
- **Resolves:** `DEC-050`
- **Constrains:** `STORY-0409`'s re-plan, which builds this; `STORY-0413`'s history screen, which
  must drop its cursor when the filter changes; every future axis added to `DuelFilter`, which §8
  binds
- **Extends:** `TASK-040801`'s cursor contract — opaque, not unforgeable, validity decided by one
  canonical re-encode check. All three properties survive unchanged, and §7 says why the third
  stays deliberately weak

## Context

Read from the code rather than assumed:

- `DuelCursor(finishedAt, duelId)` encodes `"$finishedAt|$duelId"` as unpadded base64url.
  `duelCursorOrNull` refuses anything the server would not itself have emitted, and its **whole**
  validity rule is one line: `DuelCursor(finishedAt, duelId).takeIf { it.encoded() == raw }`.
- `RECENT_DUELS_SQL` orders by `finished_at DESC, id DESC`, and `recentDuelsPage` mints `nextCursor`
  from the last row actually served.
- `STORY-0409` adds `DuelFilter(outcome: DuelOutcomeLabel?, opponent: String?)` and
  `duelFilterOrNull`, which answers `DuelFilter.NONE` when neither parameter is present.

### The forces

**A keyset cursor is a position in an ordering of a set, and the set here has three parts.** The
ordering is a constant in the SQL. The player comes from the credential the server resolved, so a
cursor handed to somebody else names a position in *their* history and reads none of the issuer's
rows — `STORY-0408` recorded that, and it is why the cursor needs no player component. **The filter
is the only part of the set definition the client supplies**, which is precisely why it, and only
it, has to be bound into the cursor.

**The failure is silent and looks like an answer.** `?opponent=Halvard&after=<a cursor from an
unfiltered walk>` returns rows: the duels against Halvard older than a position the player reached
in a different list. Nothing errors, the page is well formed, and the player reads *"these are my
duels against Halvard"* about a list that begins in the middle. A malformed cursor is loud; this is
the same class of defect as the probe row `recentDuelsPage` guards against, whose comment names the
cost exactly — *"silently, and forever"*.

**The server keeps no paging state, on purpose.** There is no table of issued cursors and no
session-scoped walk; the cursor *is* the state. So the only way the server can know which filter
minted a cursor is for the cursor to carry evidence of it.

**Two spellings of one filter must not mint two cursors.** That is the property the re-encode check
already guarantees for the tuple, and it now has to hold for the filter half as well, or a client
that re-sends its own filter in a different but equivalent spelling is refused for no reason.

**Whatever goes in must survive a third axis.** `STORY-0409` promises that widening the filter set
later is additive. A fingerprint whose input silently grows when an axis is added is a cursor that
stops validating the day somebody adds one.

**There is no key in this server to sign anything with.** `MessageDigest` appears exactly once, in
`Argon2Hasher`. `TASK-040801` ruled an HMAC out of scope — *"a key, its rotation and its config to
defend nothing"* — and nothing in this decision changes that calculation.

### The deadline, honestly

Nothing negotiates the cursor's encoding. `RecentDuelsResponse` is reachable from neither socket
message root, so `PROTOCOL_VERSION` does not cover it: there is no version to bump and no handshake
in which a client could learn that the cursors it is holding have become invalid. The only
mitigation for changing the encoding is to change it **while nobody is holding one**, and today
nobody is — `STORY-0413` has not been built and no client has ever sent an `after`. The binding
deadline is therefore *before the history screen ships*, not *before `STORY-0409` closes*. It costs
nothing today and costs every in-flight page walk on the day it is deployed after that.

## Decision

### 1. The encoded cursor gains a third component, and it is a fingerprint of the filter

The payload becomes three parts, always:

```
"$finishedAt|$duelId|$fingerprint"
```

`DuelCursor` stays `(finishedAt, duelId)` — the fingerprint is **not** a field of the type. A cursor
is a position; the filter is context supplied at both ends of the encoding:

```kotlin
public data class DuelCursor(val finishedAt: Instant, val duelId: UUID) {
    public fun encoded(filter: DuelFilter): String   // was encoded()
}

public fun duelCursorOrNull(raw: String, filter: DuelFilter): DuelCursor?
```

`filter` is non-nullable, because after `duelFilterOrNull` the route always holds one: an unfiltered
request holds `DuelFilter.NONE`, which fingerprints like any other filter. There is no second shape
and no branch.

**No new refusal path is added.** `duelCursorOrNull` splits on `|` requiring exactly **three**
parts, parses the first two exactly as it does today, and ends on the same single line — now
`it.encoded(filter) == raw`. The third part is never parsed. A wrong fingerprint, a non-canonical
instant, an upper-cased id and mangled base64 all fail in one place, in one way, and the property
`TASK-040801` established holds verbatim: *a cursor is valid exactly when it is the string this
server would itself have issued* — for this position, under **this** filter.

The separator stays safe by construction: an `Instant`, a `UUID` and unpadded base64url all cannot
contain `|`.

### 2. A filter's canonical form is the parsed value, and its text names only the axes that narrow

The fingerprint is computed over the **parsed** `DuelFilter`, never over the raw query string. That
is what makes it well defined: every axis parser on the way in already canonicalises — an outcome
becomes an enum entry or is refused (`?outcome=won` is a `400`, not a fold), and an opponent term is
NFC-normalised before it is stored in the filter. So `?opponent=Hal&outcome=WON` and
`?outcome=WON&opponent=Hal` mint one cursor, and an NFD spelling of a name and its NFC spelling mint
one cursor. **No separate canonicalisation step exists, and none is needed.**

The canonical text is one line per axis that actually narrows the read, in the fixed order below,
each line length-delimited so no value can fake a boundary:

```
"<axis>:<utf8ByteLength>:<value>\n"
```

- The axis order is `outcome`, then `opponent`.
- An axis whose value is `null` contributes **nothing at all** — not an empty segment, not a
  placeholder. §8 depends on this.
- `DuelFilter.NONE` therefore renders to the empty string.

`DuelFilter(WON, "Halvard")` renders `"outcome:3:WON\nopponent:7:Halvard\n"`. The rendering is
injective — the axis names are literals containing no `:`, and the byte count consumes the value
exactly — so two distinct filters can never share a canonical text, whatever a search term contains.

### 3. The fingerprint is the first 8 bytes of the SHA-256, as unpadded base64url

```kotlin
Base64.getUrlEncoder().withoutPadding()
    .encodeToString(MessageDigest.getInstance("SHA-256").digest(canonicalText).copyOf(8))
```

Eleven characters, fixed width, in the same alphabet as the enclosing encoding.
`DuelFilter.NONE` fingerprints to `47DEQpj8HBQ` — a constant, visible in every unfiltered cursor,
which is a useful thing for a reviewer to be able to eyeball.

**Sixty-four bits, and the reason is not collision resistance in the cryptographic sense.** §7 makes
this a consistency check rather than a security control, so the only thing the width must do is make
an *accidental* collision impossible between the handful of filters one player exercises. It does,
by a margin no one will ever test.

**Base64url rather than hex, for a reason that is measurable.** The `DuelCursorTest` fixture from
`TASK-040801` is deliberately 64 payload bytes — not a multiple of three — so
`theEncodedFormCarriesNoPadding` actually fails against a padded encoder. Twelve added bytes
(`|` plus eleven) makes 76, still ≡ 1 (mod 3), and the test stays load-bearing. Seventeen added
bytes (`|` plus sixteen hex characters) makes 81, a multiple of three, which pads to nothing and
turns that assertion vacuous. The ticket that lands this must re-verify the arithmetic either way,
but this rendering is the one that keeps it true.

Neither `canonicalText` nor `fingerprint` is `public`: they live in `DuelFilter.kt`, `internal` to
`poker-server`. Nothing outside this module may depend on the hash's shape. It travels on the wire
only inside an opaque string, and a client that parses it out has left the contract.

### 4. `DuelCursor` never learns what an axis is, and the route parses the filter first

`DuelCursor.kt` knows only that a `DuelFilter` has a fingerprint. Adding an axis therefore touches
`DuelFilter.kt` and nothing else.

`respondWithDuels`'s order becomes **identity, limit, filter, cursor**. The swap is forced — the
cursor cannot be decoded before the filter it is decoded under exists — and it costs nothing the
existing reasoning cares about: the only ordering that is a security property is identity first,
which is unchanged, and a request that is bad in two ways answers `400` either way, so no existing
test observes the difference.

`recentDuelsPage` takes the filter too. `nextCursor` is minted under the filter that produced the
page, which is what makes the next request valid.

### 5. A mismatch is a flat `400`, indistinguishable from a malformed cursor

Same status, same empty body, same "nothing is read" as every other refusal on this endpoint. Three
reasons, and the last is decisive:

1. **The client's remedy is identical.** In both cases the cursor is unusable and the only recovery
   is to request the newest page of the current filter — `after` omitted.
2. **A retryable-looking status would be a lie.** No state this client can reach makes the same
   cursor succeed, which is exactly the reasoning `ADR-0029` §5 used to answer `403` rather than
   `409` for a name already held.
3. **This endpoint's entire refusal vocabulary is one status and no body**, for the limit, for the
   cursor and for both filter axes. One exception makes the rule *"it depends"*, permanently, in a
   document and a test. Widening `400` into something more informative later is additive; narrowing
   is not.

**The intended client behaviour is that this never fires.** A client that changes a filter starts a
new page walk — it drops `nextCursor` and sends the filter alone. `STORY-0413` builds it that way:
the cursor is cleared whenever the filter changes, and a `400` on a request carrying `after` is
treated as *restart the walk from the newest page*, retried once without `after`, and never shown to
the player as an error. The `400` is the backstop that turns a client bug into a visible failure
instead of a truncated history. It is not the workflow.

### 6. What is deliberately outside the fingerprint

- **`limit`.** A page size changes neither the set nor its ordering, so changing it mid-walk is safe
  and occasionally useful. The fingerprint covers what defines the *set*, not what slices it.
- **The player.** Part of the set, but supplied by the server from the credential and never by the
  client, so replay cannot vary it. Binding it would defend nothing and would make a cursor
  unusable across a legitimate re-authentication.
- **The ordering.** A constant in one SQL string. If it ever becomes a query parameter, that
  parameter joins §2's canonical text as a new axis, under §8's rules.

### 7. The fingerprint is a consistency check, and never an authorisation check

Nothing stops a client editing a cursor. It is unkeyed, so anybody who knows the scheme can mint a
cursor the server would have issued, and this ADR does not change that — `TASK-040801` chose
*opaque, not unforgeable* deliberately, and the reason still holds: the read is keyed off the player
the **server** resolved, so any cursor a client forges names a position inside its own history. The
worst a forger achieves is skipping their own rows, which they can already do by walking normally.

So the fingerprint defends against **confusion, not against an adversary**, and must never be cited
as a security control. The condition under which that has to be revisited is precise, and is written
here so a future reader recognises it: **the day a cursor names a set the requester does not own** —
a public profile's duel list, a leaderboard page, anything where forging a position leaks somebody
else's rows — this mechanism is insufficient and the answer is a keyed MAC, with the key's storage
and rotation decided at the same time. That is a new ADR, not an edit to this one.

### 8. A third axis appends, and never anything else

Because an absent axis contributes nothing to the canonical text (§2), adding a third axis leaves
the canonical text of every filter that does not use it **byte-identical**, so cursors already in
flight keep validating. That is what makes widening additive in fact and not merely in intention.
Three rules keep it true, and they bind every future change to `DuelFilter`:

1. **A new axis is appended to the order in §2, never inserted**, and an existing axis is never
   renamed. Either would silently invalidate every cursor for a filter using the axes that moved.
2. **An axis parser must map every spelling it accepts to exactly one value.** Widening
   `duelOutcomeOrNull` to accept `won` is safe precisely because it would yield the same
   `DuelOutcomeLabel.WON`, and so the same fingerprint. Widening an axis so that it yields a *new
   shape* — a set of outcomes rather than one — changes the rendering and invalidates cursors for
   that axis; that is permitted, and it must be stated in the ticket that does it.
3. **One golden vector is pinned in a test**: a hard-coded `DuelFilter` and the exact encoded cursor
   string it produces, written as a literal. Rules 1 and 2 are otherwise enforced by nothing — a
   rendering change regenerates every computed fixture and no test fails. The golden test cannot say
   whether a change was intended, but it guarantees that a change is *noticed*.

### 9. Nothing in `STORY-0409`'s eleven tickets changes

Checked ticket by ticket, and the answer is that the split holds:

- **`TASK-040909` is confirmed, not contradicted.** Its instruction to *write no test that asserts
  anything about that combination, in either direction* is exactly right under this decision: the
  ticket that answers it asserts the refusal, and a test asserting acceptance would have to be
  undone. Its filter-after-cursor ordering is correct for the endpoint it ships, and §4's swap
  belongs to the ticket that gives `duelCursorOrNull` a filter to be parsed before.
- **`TASK-040906` and `TASK-040910` stand verbatim** — one filter for a whole walk, and no `after`
  beside a filter.
- **`TASK-040911` writes a sentence a later ticket deletes, deliberately.** The document must
  describe the code that exists; holding it back would serialise the story behind this ADR, which is
  what the split existed to avoid. **One ordering caveat:** if the tickets below ever land before
  `TASK-040911`, its gap sentence must be replaced with §5's rule rather than written.

The re-plan builds four things, in this order:

1. `DuelFilter.kt` gains `canonicalText` and `fingerprint`, with the golden vector of §8.3 and a
   test that two filters differing in one axis fingerprint differently.
2. `DuelCursor.kt`'s two functions take a `DuelFilter`; the payload becomes three parts;
   `DuelCursorTest`'s fixtures are re-cut and its padding fixture re-verified against §3.
3. `ProfileRoutes.kt` parses the filter before the cursor and passes it to both `duelCursorOrNull`
   and `recentDuelsPage`; `ProfileRouteTest` covers the four combinations — the same filter pages, a
   cursor from filter A under filter B is `400`, an unfiltered cursor under a filter is `400`, and a
   filtered cursor with no filter is `400`.
4. `docs/protocol.md` swaps the gap sentence for §5's rule, and `HttpEndpointDocumentationTest`
   follows.

## Consequences

**What it buys.** The one acceptance criterion `STORY-0409` left unticked closes, and it closes
inside the function that already owns cursor validity rather than as a second check somewhere a
caller can forget. A history page can no longer be a truncated list presented as a complete one.
`nextCursor` becomes self-describing enough to be checked without the server remembering anything,
so the endpoint stays one query and no state. And the rules in §8 make the third filter axis —
which `STORY-0409` explicitly anticipates — a change that costs old cursors nothing.

**What it costs.**

- **Every cursor in flight on the day this deploys is refused.** A client mid-walk gets one `400`
  and restarts from the newest page. That is free today only because nothing holds a cursor; the
  encoding is covered by no version negotiation, so this is not a cost that can be paid gracefully
  later, only one that can be paid *early*.
- **A mismatch and a corrupt cursor are indistinguishable, to a client and to a developer reading a
  network tab.** §5 argues the remedy is the same either way, and that is true of the client and
  false of the person debugging it, who has no body, no error code and no server log to tell *"you
  changed the filter"* from *"you mangled the string"*. This is a real and accepted debugging cost,
  and the first thing to revisit if the history screen turns out to trip over it.
- **`encoded` and `duelCursorOrNull` gain a required parameter**, so every call site and every
  fixture in `DuelCursorTest` changes — including the padding fixture, whose "not a multiple of
  three" property must be re-established rather than assumed (§3), or an assertion that catches a
  real bug today silently stops catching it.
- **`DuelFilter`'s rendering becomes a compatibility surface**, guarded by §8 and one golden test
  and nothing else. Renaming an axis is now a wire-affecting change in a file that looks purely
  internal, and the golden test tells the ticket's author that something changed without telling
  them whether it was meant.
- **The cursor grows by twelve payload bytes**, about sixteen characters encoded. Irrelevant against
  a fifty-row page, and stated so nobody has to re-derive it.
- **Two SHA-256 computations per page served** — one to validate the incoming cursor, one to mint
  the outgoing one. Microseconds beside a database round trip, and named here so it is not
  rediscovered as a mystery.
- **Somebody will read the hash as a signature.** §7 exists solely to stop that, and prose is the
  only defence available; nothing in the type system distinguishes an unkeyed digest from a MAC.

**What it forecloses.** Resuming a walk under a *widened* filter is no longer expressible, even
though that position is still a real row in the wider set — dropping `outcome=WON` to see everything
from here restarts at the top instead. That is deliberate, because the client has not seen the newer
rows the widened set adds, and it means "keep my place, show me more" can never be an operation on
this endpoint without a new cursor kind. And a cursor is now meaningful only to a request that
reproduces its filter exactly, so a bookmarked or shared page-position link is not something this
endpoint can ever offer.

## Alternatives considered

**Accept the reinterpretation and document it — the weaker contract `TASK-040909` ships.** Its
strongest case is genuinely strong: zero code, zero cursor growth, and the behaviour is *well
defined* rather than undefined — paging never loses a row within a consistent filter, and a client
that behaves correctly, dropping its cursor when the filter changes, never observes any difference
from the decision above. Rejected because `STORY-0409` requires refusal as an acceptance criterion,
and because the failure it leaves open is a wrong answer that looks like a right one — the class of
defect this endpoint's own comments already refuse twice, over the probe row and over the empty
`after`.

**The cursor carries the filter's literal text.** Its strongest case: self-describing, debuggable,
and it could make a mismatch *explainable* rather than merely detectable — the server would know
which filter the cursor was drawn under and could say so. Rejected on three counts, each sufficient.
It discloses: the term a player searched — a person's name — would live in a string the client
holds, logs, and pastes into a bug report. It destroys the separator's safety by construction: a
search term may contain `|` and anything else that is not blank, so the payload needs an escaping
rule, and an escaping rule is a rule that can be got wrong silently — the same reasoning that put
`POSITION` rather than `ILIKE` behind `TASK-040905`. And it is unbounded: 32 code points of name
today, plus whatever every future axis adds, on a value that travels in a query string.

**The cursor carries the filter and the server *uses* it, ignoring the query parameters.** Its
strongest case is that a mismatch becomes impossible: the walk carries its own set definition and no
client can get it wrong. Rejected because `?after=X&outcome=WON` would then silently ignore
`outcome` — the same silent reinterpretation this decision exists to refuse, pointed the other way —
and because a response whose shape depends on a parameter the client can no longer see is a worse
debugging story than §5's flat `400`. It also pays the disclosure and escaping costs above.

**Server-side cursor state: issue an opaque id, remember `(player, filter, position)`.** Its
strongest case is the strongest possible reading of `ADR-0002` — the client holds a handle and
cannot construct or edit anything, the cursor becomes genuinely unforgeable, and any future set
definition fits with no wire change at all. Rejected because it puts a table, or a map with an
eviction policy, behind a read that is currently one query and no state, and because it introduces
an expiry: a cursor that worked five minutes ago and does not now is a *new* failure mode, strictly
worse than the one being fixed and much harder to explain. Worth revisiting only under §7's trigger.

**HMAC the cursor with a server key, filter included.** Its strongest case is that it answers this
question and the forgery question in one move, and the code is barely larger. Rejected because
`TASK-040801` already priced it — *"a key, its rotation and its config to defend nothing"* — and
nothing here changes the reasoning: a forged cursor still names only the forger's own rows. There is
no signing key in this server today, and introducing one to defend against a client skipping its own
history is a whole configuration surface bought for nothing. §7 names the day this becomes the right
answer.

**Refuse `after` whenever any filter parameter is present — no paging inside a filter in v0.1.** Its
strongest case: one `if`, no fingerprint, no encoding change, and the silent reinterpretation
becomes unreachable rather than merely detected. Rejected because `TASK-040906` exists to prove that
paging inside a filter is total and disjoint across a matching insert, and a filtered history that
cannot page is useless to exactly the player filters are for — the one with two hundred duels.

**Keep `duelCursorOrNull` one-argument and compare in the route**, with `DuelCursor` gaining a
`filterFingerprint` field. Its strongest case is compatibility: every existing fixture and call site
compiles unchanged, and the check is one visible `if` in the place that already decides refusals.
Rejected because it makes it possible to decode a cursor *without* saying which filter you are
decoding it under, so the one caller who forgets restores the silent reinterpretation with no test
failing; and it splits cursor validity across two files when the entire value of the canonical
re-encode check is that validity lives in one function and admits no second opinion.

**Answer `409 Conflict` on a mismatch.** Its strongest case is precisely the cost §5 accepts: it
makes a client bug findable in a network tab without server-side logging, and Ktor makes it free.
Rejected because the client's remedy is identical either way, because a second status is a permanent
second thing to document, test and keep true, and because a `409` invites the retry that can never
succeed. Adding information to a `400` later is additive; taking a `409` back is not.
