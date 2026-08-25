# ADR-0078 — The mail is the only real check on an address, so the syntax rule refuses almost nothing

- **Status:** Accepted
- **Date:** 2026-08-25
- **Resolves:** `DEC-071` — which strings does `POST /api/auth/recovery-email` accept as an address,
  and what is the player told when one is refused? **Derived from the vision; the human did not
  state this call.** Two sentences license it. *"One duel coin per win. Not chips, not currency, not
  a balance. A counter of duels won"* — that is the whole of what this address protects, and it
  fixes the proportion: nothing here is worth a corpus, a blocklist, a network call or a bill, and
  nothing here is worth costing a real player the counter. And *"The reference points are Lichess
  and Chess.com, not PokerStars. Dark, quiet, fast, minimal"* — which decides what fills the gap,
  exactly as it did in [`ADR-0048`](ADR-0048-a-password-has-one-rule-and-it-is-length.md): a form
  that refuses as little as it can get away with
- **Builds on:** [`ADR-0031`](ADR-0031-an-optional-verified-recovery-email.md) §2 (the stored form is
  the address as typed, and the fold lives in a database index), §3 (verification is required, and
  an unverified address does nothing), §5 (`400` for an address that is not one, `202` for
  everything else, and `forgot-password`'s unconditional `202`), §6.3 (the address leaves the
  database layer only into the mail port) and §7 (no transport);
  [`ADR-0048`](ADR-0048-a-password-has-one-rule-and-it-is-length.md), which is the precedent this
  was routed on and whose shape this follows — one rule, stated in one sentence, with the refusals
  it declines to make written down as decisions rather than left as omissions;
  [`ADR-0077`](ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md), under
  which an unconfigured build sends nothing
- **Constrains:** `TASK-041624` (the predicate and its fixtures), `TASK-041625` (the endpoint's
  `400`), and what `STORY-0417` may put on the screen. `TASK-041601` is untouched except by
  §Consequences' one line
- **Leaves open:** the words `STORY-0417` writes, and bounce handling once `EPIC-07` configures a
  transport — which `ADR-0031` §7 already deferred there

## Context

`ADR-0031` §5 promises `POST /api/auth/recovery-email` a **`400` for "an address that is not
syntactically an address"** and states no rule. §2 has already settled what is stored and how it is
folded, and the V8 migration has shipped both. So one thing is open: the predicate.

It is a small question with an unusually lopsided cost, and the lopsidedness is the whole decision.

**The `400` is the only thing this endpoint ever tells a player about their address.** §5 makes
every other outcome a silent `202` — including the case where the address is already verified to
somebody else, where §Consequences already concedes that *"an honest player… got `202`, and no mail
arrives; they will conclude the mail is broken."* Whatever the rule refuses is the entire feedback
channel, which argues for a rule that catches as much as it can.

**A rule that refuses a real mailbox is unrecoverable.** There is no second door: no endpoint edits
an address, no admin path exists (§Decision builds none, and `ADR-0029` built none), so a player
whose mailbox this predicate will not accept is permanently opted out — and `ADR-0031`'s
Consequences make opted-out mean *"forget the password, or forget the handle, and the account is
gone with its coins and its ladder place."* A wrong refusal here costs an account. That argues for a
rule that refuses as little as possible.

**Those two pull opposite ways, and the thing that resolves them is already built.** §3 requires
verification before an address can do anything, and §2 notes the consequence: *"a player whose
address is stored has, by construction, received mail at it."* The system therefore already contains
a check that is exact, that costs nothing extra, and that no predicate can improve on — the mail
either arrives or it does not. A syntax rule cannot learn anything the mail does not learn. It can
only report earlier, and it can only be wrong in the expensive direction.

The rest of the tension is narrower but real:

- **Correct email validation is not a thing anybody writes.** RFC 5322's `addr-spec` admits quoted
  local parts, quoted pairs, comments, folding whitespace and domain literals; RFC 6531 admits
  non-ASCII on both sides. Every regex in common use refuses some real mailbox, and here that is not
  a bounced form — it is somebody's account.
- **A network check is off the table for this path.** `TASK-041624`'s *Out of scope* forbids DNS and
  MX lookups, and `ADR-0048` already rejected the same shape for a breach corpus, on the fork it
  forces: *"fail open and the rule is theatre, fail closed and a stranger's outage stops account
  creation."*
- **Something has to be bounded, so "no rule at all" is not on the table.** The address is written
  into `recovery_email_address_unique`, a btree index on `lower(address COLLATE "und-x-icu")`, and
  Postgres bounds a btree entry to a fraction of a page. An unbounded address is a `500` at
  `verify-email` on a request that has passed every check — a refusal that arrives as a crash.
- **One clause has a consequence outside the software.** A CR or an LF inside an address is what
  ends a line in SMTP. The transport is `EPIC-07`'s and does not exist yet, so the string this
  predicate accepts will be handed to code nobody has written, and the failure would land in a
  stranger's mailbox rather than in this repository.
- **`ADR-0048` decided the neighbouring question five weeks ago and departing from its shape needs a
  reason.** It refused composition rules outright, wrote the refusals down as decisions, and fixed
  one rule in one sentence. The forces here are the same forces with the asymmetry sharpened.

### The deadline, honestly

**Almost nothing here is permanent, and the one property that makes that true has to be built now.**

Tightening this rule later costs nothing *provided it runs only where an address enters the system
and never where one is looked up*. Under that placement, a stored address that a future stricter
rule would refuse keeps working forever: `forgot-password` folds and compares against an index and
never consults the predicate. Put the rule on the lookup path instead and the day the rule moves is
the day a set of real players silently lose recovery — which is `ADR-0048` §2's reasoning
(*"a minimum enforced at sign-in would lock out every account below it on the day the number is
raised"*) one endpoint pair over. So the placement is the deadline, not the predicate.

**Loosening later is equally cheap**, which is what makes an unambitious rule the right bet on thin
evidence: this product has no players, and every stored row survives a rule that becomes more
permissive by definition.

What is genuinely fixed by answering at all: `TASK-041601` parked a catalog assertion on
`recovery_email.address`'s pinned collation *"until `DEC-071` merges; if the answer admits non-ASCII,
this becomes a ticket."* It does. The collation V8 already ships is the right one, so nothing is
forced — but the parked follow-up now has its condition met.

## Decision

### 1. The rule: an `@` with something on either side, no control character, at most 254 code points

**A string is an address when all four hold. Nothing else about it is examined.**

1. It contains at least one `@` (`U+0040`).
2. Its first code point is not `@`.
3. Its last code point is not `@`.
4. It contains no ASCII control character — no code point in `U+0000`–`U+001F`, and not `U+007F`.
5. It is at most **254 code points** long.

There is no separate minimum: clauses 1–3 make three code points the shortest thing that can pass,
and `a@b` is that string.

- **Clauses 1–3 are the whole of the syntax check**, and they refuse exactly one class of input: a
  string with no domain, no local part, or no `@` at all. That is the handle typed into the address
  field, the empty submitted form, and the bare domain — the refusals a player can act on. Every
  other judgement about the string is left to the mail.
- **The count is code points, not UTF-16 units**, the same unit `ADR-0029` §2 and `ADR-0048` §1
  fixed. `String.length` over-counts astral characters, and one unit across the three string rules
  in this codebase is worth more than matching any external definition exactly.
- **254 is RFC 5321 §4.5.3.1.3's path limit** (256 octets, less the two angle brackets) rather than a
  number invented here — which matters mainly because an invented number gets re-argued. Its real
  job is the btree entry above. Stated in code points it is *more* permissive than the octet limit
  for a non-ASCII address, which is the direction this decision leans everywhere else.
- **Clause 4 is not a syntax rule and should not be read as one.** No `addr-spec` contains an ASCII
  control character in any position — RFC 5322 excludes them from `atext`, `qtext` and `ctext`
  alike — so it denies no mailbox that exists. It is here because a line terminator inside an
  address is the one thing this predicate could pass to `EPIC-07`'s unwritten transport that would
  harm somebody who is not a player of this game. It is the only clause chosen for a reason outside
  this repository, and the reason travels with it so that a later reader does not tidy it away as an
  arbitrary exclusion.

### 2. The rule runs where an address enters, and nowhere else

**`POST /api/auth/recovery-email` is the only endpoint that applies it.** It is also the only
endpoint through which an address enters the system, so every address anywhere downstream —
`email_verification`, `recovery_email`, the `RecoveryMailer` port — either passed this predicate or
was read back from a row that did.

- **`POST /api/auth/forgot-password` applies no syntax rule and keeps its unconditional `202`.** §5
  makes that endpoint answer `202` in every case and enumerates them; a string that is not an
  address is simply an address that matches no row, which the existing fold-and-compare already
  handles by returning nothing. Adding a `400` there would buy no information the query does not
  already produce, and would spend the one property §5 built on purpose.
- **`POST /api/auth/verify-email` applies no syntax rule either.** Its input is a token, and the
  address it acts on came out of `email_verification`.
- **A stored address is never re-judged.** No sweep, no migration and no read path re-applies this
  predicate. This is the property §The deadline named: it is what makes a future tightening free,
  and it is the reason the rule may not drift onto a lookup path.

### 3. What it does not check, and every line of this is a decision

Written as decisions so that nobody later reads them as omissions and repairs them:

- **Deliverability, in any form.** No DNS lookup, no MX record, no SMTP probe, no third-party
  validation service. §3's verification mail is this system's deliverability check, it is exact
  where a lookup is a guess, and it is already built.
- **Whether the domain exists or is spelled correctly.** `bob@gmail.con` is accepted. This is the
  typo that actually costs a player their recovery, and this rule does not catch it — the mail
  does, by not arriving.
- **Disposable-address lists, role addresses, or the mailbox's reputation.** `admin@`, `postmaster@`
  and a ten-minute mailbox are all accepted. What is being protected is a counter of duels won; it
  does not justify telling a player their mailbox is the wrong kind, and a blocklist is the first
  thing *Lichess, not PokerStars* reads against.
- **Plus-addressing, and the tag is part of the address.** `bob+duels@example.com` is accepted and
  is **not** stripped to `bob@example.com`. Stripping would change what is delivered to, which §2
  forbids, and would make two of one player's own tags collide in the unique index.
- **Unicode.** A non-ASCII local part or domain is accepted as typed. No punycode conversion, no
  transliteration. §2's index collation is pinned and already handles the fold.
- **A dot in the domain.** `bob@localhost` and a TLD-only domain are accepted.
- **Quoting, comments, folding whitespace or domain literals.** None is parsed, so none is refused:
  `"john smith"@example.com` and `bob@[192.0.2.1]` both pass, because refusing them would require
  the parser this ADR declines to write.
- **A space, or any whitespace that is not a control character.** RFC 5321 permits a space inside a
  quoted local part, so refusing one would deny a mailbox. The cost of this particular line is named
  in §Consequences, and it is the sharpest one here.

The predicate is therefore a **pure function of the string**. It reads no configuration, opens no
socket, consults no clock and knows nothing about the player — which is what lets it be a `private`
function's worth of code with a table of fixtures either side of it.

### 4. Nothing is canonicalised

**`emailAddressOrNull` returns the input string unchanged, or `null`.** No trim, no `lowercase`, no
`Normalizer`, no rewriting of any kind, confirming `TASK-041624`'s scope rather than reinterpreting
it.

This departs from `ADR-0048` §5, which NFC-normalises a password, and from `ADR-0029` §2, which
folds a display name — and the departure is the point. Those two strings are *compared*: one against
a hash of itself, the other against other names. An address is a **delivery target**, and the bytes
have to reach a mail system that will apply its own rules to them. A normalisation that tidies the
index can only make delivery less likely, and §2 already fixed the stored form as *"the address as
the player typed it… because that is what must be delivered to."* The one comparison that does exist
is `recovery_email_address_unique`'s `lower(… COLLATE "und-x-icu")`, which is the database's and was
settled in §2.

The rule also stays out of `EmailAddress` itself: `TASK-041603`'s *"no `init`, no `require`, no
regex"* stands, exactly as `ADR-0048` §6 left `PresentedSecret`'s refusal intact. The type is
constructed from stored rows too, and a throwing constructor would turn a refusal into a `500`.

### 5. What the player is told

**`400`, empty body.** No endpoint in this system composes a reason and this one does not start.

The client marks the address field and says **one sentence, which states that the string is not an
address and states nothing else**. Specifically, it may not:

- mention deliverability, a mailbox, a domain or whether mail could be sent — the server does not
  know, and §3's mail is the only thing that ever will;
- say anything about whether any address is known to this system, which is the oracle §5 exists to
  refuse;
- offer a correction, a suggested domain or a "did you mean".

The `400` for a body that fails to decode and the `400` for a refused address are **the same
response**, deliberately: both are properties of the caller's own input, neither discloses anything,
and one meaning per code beats a distinguishing body nobody can use.

**One line about the `202`, because this decision makes it load-bearing.** A rule that refuses almost
nothing throws the whole weight of what a player learns onto the silent path, and §3 is explicit that
until verification *"the account is exactly an account with no email"* — `hasRecoveryEmail` stays
`false`. **The client may not report a `202` as recovery being on.** That is the only constraint this
ADR puts on the success path; the exact words, on both paths, are `STORY-0417`'s under `EPIC-06`'s
design language, the same division `ADR-0048` §7 made.

### 6. The fixtures

Both tables are non-empty and non-trivial on purpose: a function returning the input always passes
the first, and one returning `null` always passes the second, so neither alone distinguishes a rule
from a constant. `TASK-041624` asserts from these.

**Accepted**, each returning the input **unchanged**:

| String | Why it is in the table |
| --- | --- |
| `a@b` | The shortest string that passes. **The naive `.+@.+\..+` refuses it and a mail system does not** |
| `bob@localhost` | No dot in the domain. Also refused by the naive regex |
| `Bob@Example.com` | Case survives — §2 stores what was typed, and the fold is the index's |
| `bob+duels@example.com` | Plus-addressing, tag intact |
| `"john smith"@example.com` | A quoted local part containing a space — why §3 refuses no space |
| `рома@пример.рф` | Non-ASCII on both sides, unconverted |
| 254 code points, e.g. `"a".repeat(246) + "@ex.test"` | The ceiling, inclusive |

**Refused**, each returning `null`:

| String | Why it is in the table |
| --- | --- |
| `bob` | No `@` — the handle typed into the address field. The one refusal a player can act on |
| `""` | No `@`. The submitted-empty form, and the likeliest way a refusal actually arrives |
| `@example.com` | Begins with `@`; nothing to deliver to |
| `bob@` | Ends with `@`; no domain |
| `bob\u0000@example.com` | An ASCII control character, written as a Kotlin escape. **The naive regex accepts it** — `.` matches `U+0000` — and no `addr-spec` contains one in any position |
| `bob@example.com\r\nBcc: someone@else.test` | The clause-4 case that matters: a line terminator handed to a transport. A plain space is deliberately **not** in this table — `"john smith"@example.com` above is why |
| 255 code points | One past the ceiling. **The naive regex accepts it** |

The two entries marked in bold in each table are the ones that make the suite worth running:
without them, `TASK-041624`'s Proof step 4 shows the tests cannot distinguish this answer from the
first regex anyone would write.

## Consequences

**What it buys.** `TASK-041624` has a rule and both its fixture tables, and `TASK-041625` has the
one refusal its `400` needed; both leave `blocked` as far as this decision goes. `ADR-0031` §5's
`400` now has a predicate behind it, as §5's `422` got one from `ADR-0048`. No mailbox that exists
is refused by this server, which is the property the asymmetry demanded. And the answer is reversible
in both directions for as long as §2's placement holds, which is the honest position for a product
with no players.

**What it costs.**

- **The endpoint's only feedback now fires almost never, and that is the cost being chosen.** In
  practice only a bare handle and an empty field will ever produce a `400`. Every typo, every dead
  domain, every paste artefact is answered `202` and silence — and §Consequences of `ADR-0031`
  already records what that silence feels like: *"they will conclude the mail is broken."* This
  decision deliberately makes that the common case rather than the exception.
- **`Bob Smith <bob@example.com>` is accepted, and it is the commonest paste artefact there is.**
  So is a trailing space, and so is a trailing newline that some clipboards add. Each is stored as
  typed (§2 forbids trimming), each is undeliverable, and each produces a `202`. Refusing angle
  brackets would catch the first — §Alternatives says why it was refused anyway — but there is no
  version of this decision that catches all three without becoming the validator it declines to be.
- **A player can believe recovery is on when it is not.** The product does tell them —
  `hasRecoveryEmail` stays `false` and no mail arrives — but only if they look. §5 above is the
  whole mitigation and it is a constraint on a story that has not been split.
- **`STORY-0417` inherits work this decision pushed onto it.** Because the server refuses to be the
  check, everything a player learns about whether their address works comes from the mail arriving,
  so the client's copy has to be honest about a pending state rather than congratulatory about a
  `202`. That is real design work created here and paid for there.
- **The set of addresses in `recovery_email` is not the set the current rule accepts, permanently.**
  §2's entry-only placement is what makes tightening free, and its price is that the predicate is
  never an invariant over the table. Anyone who reads it as one — a validation sweep, a migration
  that re-checks rows — will be wrong.
- **Two rows can be one mailbox.** `bob+a@x.com` and `bob+b@x.com` are different index keys, and so
  are two Unicode normalisation forms of one address, so the unique index cannot see that two
  accounts share an inbox. Each reset mail names its own handle (§6.2) so no reset is ambiguous, and
  `ADR-0063` already accepts multi-accounting *"until the ladder is public"* — but this decision
  widens the door rather than narrowing it.
- **A permissive predicate hands `EPIC-07` a higher bounce rate**, which is deliverability and
  sender reputation — consequences outside the software, at a provider that does not exist yet. It
  is not answered here because `ADR-0031` §7 already put the transport and everything about it in
  `EPIC-07`; it is written down so that whoever picks a provider knows this decision moved the
  number, and so that a provider's bounce threshold is recognised as a reason to revisit this ADR
  rather than to quietly patch the regex.
- **`TASK-041601`'s parked catalog assertion becomes a ticket.** Its condition was *"if the answer
  admits non-ASCII"*, and it does. The collation V8 ships is already correct, so nothing is broken;
  the work is a gate that nobody has written, and it is the planner's to cut.

**What it forecloses.**

- **A validator on this path, for as long as this ADR stands.** No regex library, no `addr-spec`
  parser, no corpus, no blocklist, no resolver call, no third-party service. Any of them is a new
  ADR that supersedes this one, and this is the intended effect rather than a side effect.
- **A syntax `400` on `forgot-password`.** §5's unconditional `202` is now load-bearing in two ADRs;
  adding a refusal there later is a behaviour change to a documented contract, not an addition.
- **Reading the address as canonical.** Nothing downstream may assume the stored string is folded,
  trimmed, normalised or lowercase. §2 already said so; §4 makes it a property of the one function
  that admits addresses, so no later caller can acquire the assumption honestly.
- It does **not** foreclose tightening. A stricter clause applies to addresses attached from that
  day on, invalidates no row, changes no schema and asks nothing of any existing account — which is
  `ADR-0048`'s tiebreaker, and the reason this ADR is deliberately unambitious.

**What this does not settle.**

- **The words on the screen, on either path**, and any affordance the form offers — a client-side
  mirror of this rule included, which `TASK-041624`'s *Out of scope* already assigned to
  `STORY-0417` with the server's answer authoritative either way.
- **Bounce handling, retries and sender reputation.** `EPIC-07`'s, deferred by `ADR-0031` §7 and
  narrowed by `ADR-0077` (nothing above the port is retried). This ADR adds no new deferral; it
  raises the stakes of one that already exists.
- **`DEC-073`, `DEC-074` and `DEC-075` are untouched.** The budget on this endpoint, the `422`'s
  effect on a reset token, and the shape of the mailed link are all the architect's and all still
  open. In particular this rule runs *after* identity and *before* the password check in
  `TASK-041625`'s fixed order, and says nothing about where a budget sits in it.
- **Nothing here is the human's, and this was checked rather than assumed.** No money: no clause
  requires a paid service, and the one alternative that would is refused in §Alternatives on exactly
  that ground. No vision change: the human chose *optional email, recovery only* in `DEC-027` and
  this applies that choice rather than revisiting it, the same test `DEC-043` passed. No risk
  outside the software introduced: nothing is sent under any answer here (`ADR-0031` §7,
  `ADR-0077`), and the one clause with an outside consequence — the control characters — is
  resolved on the safe side.

## Alternatives considered

**A correct RFC 5322 `addr-spec` parser.** The strongest case is that it is the actual definition:
the grammar is published, a correct parser refuses precisely what a mail system refuses, and it
removes every judgement call this ADR had to make. Rejected on three counts. The grammar admits
comments, folding whitespace, quoted pairs and domain literals, so nearly all of the parser's code
runs against strings no provider has ever issued and no player will ever type — work spent entirely
on inputs that do not occur. Every implementation of it in the wild is subtly wrong in some corner,
and here a wrong refusal is not a bounced form but somebody's account. And even a perfect one proves
only that a string is well-formed, which is not the question anybody has: the question is whether
mail reaches it, and §3 already answers that exactly.

**The conventional regex** — something in the family of `^[^@\s]+@[^@\s]+\.[a-z]{2,}$`. Its case is
serious and it is what almost every product ships: it catches the display-name paste, the trailing
space, the missing TLD and the bare domain, which are precisely the failures §Consequences admits
this decision lets through, and its false-refusal set is famously small. Rejected because *small* is
not *empty*, and the members of that set here lose an account permanently rather than retype a form:
`a@b`, `bob@localhost`, `рома@пример.рф`, `"john smith"@example.com` and every new TLD-only domain
fail one clause or another. What it buys is one round trip of feedback about a check the mail runs
anyway; what it costs is measured in accounts. On this asymmetry that trade is not close.

**A DNS or MX lookup on the address's domain.** The strongest case in this list. It is the only
option that distinguishes `gmail.com` from `gmail.con` — which is the typo that actually costs a
player their recovery, and the one this ADR openly does not catch. Rejected because `TASK-041624`'s
*Out of scope* forbids it; because it puts a network call inside a pure function on a path that today
depends on nothing but Postgres, forcing the fork `ADR-0048` already rejected for a breach corpus —
*fail open and the rule is theatre, fail closed and a stranger's outage stops account setup*; because
this project's checks are an exit code that runs offline, and this one would need a resolver stood up
in a fake; and because an MX record does not imply a mailbox, so it still would not prove what the
mail proves.

**A disposable-address blocklist.** Its case is real and specific: a player who attaches a
ten-minute mailbox has a recovery path that expires without telling them, and finds out months
later when it is the only thing standing between them and a lost account. Rejected because a corpus
with an update cadence is a dependency in a repository whose checks run offline; because every such
list refuses mailboxes that real people use as their main address; because the asset is a counter of
duels won, which does not justify a server telling a player their mailbox is the wrong kind; and
because it would be the first blocklist in this product, which is the shape *Lichess, not
PokerStars* rules out. **If it ever arrives as a paid feed, the money half is the human's, not this
role's.**

**A commercial address-validation API.** Its case is that it is the only thing that answers *does
this mailbox exist* without sending anything, which is exactly the gap this ADR leaves. Rejected on
the network-call grounds above, and **refused outright on a second ground that is not this role's to
overrule: it is money, in a product whose vision forecloses money in every direction.** Recorded here
so that its absence reads as a decision rather than an oversight, and so that anybody who wants it
knows it goes to the human.

**Refuse a string containing `<`, `>`, `,` or `;`.** This nearly earned its place. It catches
`Bob Smith <bob@example.com>`, the commonest paste artefact and the one real failure named in
§Consequences, and it denies no mailbox anybody plausibly has. Rejected because it is a
four-character blocklist standing in for the validator this ADR deliberately does not have —
`ADR-0048` refused *"a password equal to, or containing, the handle"* in the same words and for the
same reason — and because it is the first step of a slope that ends in a display-name parser: the
next entry is a space, and the one after that is a comma-separated list. The friendlier form belongs
to the client, where `TASK-041624` already put it.

**Apply the same predicate at `forgot-password`.** Its case is that it is the same field on the same
subject, that a player who mistypes there gets silence too, and that one rule in one place is easier
to reason about than a rule that applies at one endpoint and not its neighbour. Rejected because §5
makes that endpoint `202` in every case, enumerated, and a rule there buys nothing the fold-and-
compare does not already deliver — but mainly because a rule on a *lookup* path is what locks people
out the day the rule moves, which is `ADR-0048` §2's reasoning and the single property that keeps
this decision reversible.

**No rule at all — accept whatever the field holds.** Its case is that it is the most permissive
answer on the table, cannot possibly deny a mailbox, and is the least code in the world. Rejected on
two counts and the second is decisive. `ADR-0031` §5 already promises a `400`, and an endpoint that
never emits its documented refusal is a lie in a merged table. And the string would be unbounded and
could hold a line terminator — an unbounded key in a shipped unique btree index, and a header
injection handed to a transport `EPIC-07` has not written. Once a bound is being written anyway,
refusing a string with no `@` costs the same line and catches the one refusal a player can act on.
This is `ADR-0048`'s own reasoning about its maximum, arriving at the same place.

**NFC-normalise the address, as `ADR-0048` §5 does the password.** Consistency across the three
string rules in this codebase is worth something, and it would make `recovery_email`'s unique index
blind to normalisation variants of one mailbox — closing half of the *two rows, one mailbox* cost
above. Rejected because an address is a different kind of string from the other two: a password is
compared against a hash of itself and a name against other names, but an address is a delivery
target whose bytes must survive to a mail system, and §2 fixed the stored form as *"the address as
the player typed it… because that is what must be delivered to."* A fold that helps the index can
only hurt the delivery, and delivery is the entire purpose the address is stored for.
