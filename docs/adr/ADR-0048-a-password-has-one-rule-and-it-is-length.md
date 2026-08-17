# ADR-0048 — A password has one rule, and it is length

- **Status:** Accepted
- **Date:** 2026-08-17
- **Resolves:** `DEC-043` — what may a password be (minimum, maximum, composition, breach check),
  and is v0.1 shippable with no strength rule at all? **Derived from the vision; the human did not
  state this call.** Two sentences license it. *"Not real-money gambling, and not a path to it"*,
  read beside *"One duel coin per win. Not chips, not currency, not a balance. A counter of duels
  won"* — that is the whole of what a password defends here, and it fixes the proportion of the
  rule. And *"The reference points are Lichess and Chess.com, not PokerStars. Dark, quiet, fast,
  minimal"* — which decides what fills the gap: a form that refuses as little as it can get away
  with.
- **Builds on:** [`ADR-0027`](ADR-0027-the-session-outranks-the-device-id.md) §1 and §6 (Argon2id,
  four verification slots, and the rule that two sign-in failures are indistinguishable),
  [`ADR-0031`](ADR-0031-an-optional-verified-recovery-email.md) §§1, 4 and 5 (the handle, the
  reset, and a response table that already promises `422` *"when the token was good and the new
  password fails policy"* with no policy behind it),
  [`ADR-0029`](ADR-0029-a-display-name-is-unique-and-permanent.md) §2 (NFC, and code points rather
  than UTF-16 units), [`ADR-0036`](ADR-0036-an-account-is-offered-never-required.md) (sign-up is an
  offer made after a first win, never a requirement)
- **Constrains:** `STORY-0404` (sign-up), `STORY-0405` (sign-in), `STORY-0416` (the reset), and
  what `STORY-0412` tells a player. `STORY-0403` is untouched except by §6's single line
- **Leaves open:** whether a breach corpus is ever consulted — additive whenever anyone wants it

## Context

`ADR-0027` decided how a secret is stored and `ADR-0031` decided what a player signs in with.
Neither says what a player is allowed to *choose*, and the hole is visible in a merged table:
`ADR-0031` §5 answers `422` for a new password that *"fails policy"*, and there is no policy. The
next story to touch this, `STORY-0404`, builds the endpoint where any rule is enforced, so the hole
gets filled there by whoever is typing — in a validator nobody reviews as a product decision.
`STORY-0403` saw this coming and put **no** rule in `PresentedSecret` rather than freeze a guess
into a value class.

What is genuinely in tension:

**The asset is small, and it is not nothing.** Behind the password is a counter of duels won and a
place on a ladder. There is no money, no purchase history, no chip balance — the vision forecloses
all three. But `ADR-0012` records that device ids are trivially minted and that this *"must not
still be true when the leaderboard goes public"*; real identity is the countermeasure `EPIC-05`
depends on, and a ladder position that can be taken by guessing `1234` is not one.

**Every refusal on this form costs the account.** `ADR-0036` makes sign-up an offer, made after a
player's first win, dismissible forever. The player did not come here to make an account; they came
to play a duel and they just won one. A rule that sends them back to the field a second time is a
rule with a real chance of ending with no account at all — and the account is the entire point of
`EPIC-04`, which exists to pay `ADR-0012`'s *"a lost device is a lost profile"*.

**Argon2id's work is bounded only if its input is.** `ADR-0027` §1 fixes `m = 19456, t = 2, p = 1`
and holds verification to four concurrent slots precisely because a memory-hard hash with unbounded
concurrency is a self-service denial of service. The memory is fixed; the initial pass over the
password is not. An unbounded password is an unbounded linear term inside one of only four slots.
This is the reason a maximum exists, and it is not a usability question.

**A rule applied in the wrong place locks people out.** `ADR-0027` §6 requires that a wrong password
and an unknown account be indistinguishable. `ADR-0031` guarantees that an opted-out account has no
recovery path *at all* and that sign-in cannot even say which half was wrong. So a password that
cannot be re-typed byte-for-byte is a permanently lost account, with no support inbox behind it.
That is what makes the normalisation question load-bearing rather than cosmetic, and it is why a
rule that runs at sign-in is a different kind of object from a rule that runs at sign-up.

**The one rule that would actually work costs a network call.** Length does not refuse `password1`;
a breach corpus does. Consulting one means either a third-party request on the sign-up path or tens
of megabytes in the repository, in a product with no players and a test suite that is an exit code.

### The deadline, honestly

**One part of this is permanent, and it is not the number anybody will argue about.**

**The bytes that go into Argon2 are fixed the moment the first hash is stored.** `STORY-0403` builds
the hasher and `STORY-0404` writes the first row. Change the transformation afterwards — add
normalisation, remove it, switch its form — and every stored hash verifies against a different
string than the one the player types. There is no migration for that, because the plaintext is gone
by design; the only remedy is a forced reset for every account, and for an opted-out account
`ADR-0031` says a reset is not available. So normalisation is chosen now, at its intended final
form.

**Everything else here is cheap in both directions and stays cheap.** Raising the minimum, lowering
the maximum, adding a corpus later: each applies to passwords set from that day on, invalidates no
stored hash, changes no schema, and asks nothing of any existing account. That asymmetry is the
reason this ADR is confident about NFC and deliberately unambitious about the numbers.

## Decision

### 1. One rule: 8 to 128 code points

**A password is accepted when its normalised form is at least 8 and at most 128 Unicode code
points. Nothing else about it is examined.**

- **The count is code points, not UTF-16 units, and it is taken after normalisation** — the same
  order and the same unit `ADR-0029` §2 fixed for display names, and `canonicalDisplayNameOrNull`
  already implements. `String.length` counts UTF-16 units, so four emoji measure 8 there and 4 in
  code points: counting the other way would let four characters satisfy an eight-character
  minimum, and would refuse a 128-character password at the top for a reason no player could see.
- **8 is the floor below which the rule stops meaning anything**, and it is the published floor
  (NIST SP 800-63B's minimum for a user-chosen secret) rather than a number invented here — which
  matters mainly because an invented number gets re-argued every six months. Its real job is the
  offline case: if the database leaks, Argon2id at `m = 19456` makes eight characters expensive and
  three characters free.
- **The maximum is 128, and it is not there to help anybody.** It is a bound on work: at most 128
  code points is at most 512 UTF-8 bytes, which makes the linear term inside a verification slot a
  rounding error against the 19 MiB fill. It is stated in code points anyway, so that the player-
  facing rule has one unit. 128 is twice the 64 characters SP 800-63B requires a verifier to
  permit, so nothing published is violated by it, and it is far above any password manager's
  output or any passphrase a person will type twice.

### 2. The minimum is a sign-up rule. The maximum is everywhere a secret is hashed

These are two rules with two reasons and they are enforced in two different places. Conflating them
is the mistake this section exists to prevent.

- **The minimum applies at `POST /api/auth/sign-up` and at `POST /api/auth/reset-password`, and
  never at sign-in.** Sign-in hashes whatever it is handed and compares. An account created under an
  older rule must keep working forever; a minimum enforced at sign-in would lock out every account
  below it on the day the number is raised, which is the one failure `ADR-0031` says nobody can
  recover from.
- **The maximum applies wherever a presented secret is hashed, including sign-in**, and it is
  checked **before** Argon2 runs and **before** the identifier is looked up. Over-long answers
  exactly as a wrong password does — same status, empty body, no differing header — so
  `ADR-0027` §6's parity is untouched. It may answer fast, and that is not an oracle: the only fact
  a fast refusal discloses is a property of the input the caller sent, which is the same test
  `ADR-0031` §5 applied to `verify-email`'s `409`.

### 3. No composition rule, no character rule, no dictionary, no meter — and this is a decision

**Every code point is permitted.** Spaces, emoji, control characters, right-to-left marks, the lot.
There is no required digit, no required case, no required symbol, no refused character, no
similarity check against the handle, no expiry, no history of previous passwords, and no rule that
a new password differ from the old one.

Stated as a decision with reasons, so that nobody later reads it as an omission and repairs it:

- **A password is never displayed, never compared to another player's, never indexed and never
  rendered.** The entire class of reasons `ADR-0029` §3 gave for refusing invisible characters in a
  display name — a name that renders as another name is a spoof, and it is then permanent — has no
  analogue here. Refusing a character costs a player the password their manager generated, and buys
  nothing.
- **Composition rules produce `Password1!`.** They shift a corpus rather than strengthen it, and the
  corpus outlives the day the rule is removed.
- **They are the friction the vision refuses.** *Dark, quiet, fast, minimal* is not a styling note;
  a form that argues with you about symbols is a bank's form, and this one is being shown to
  somebody who just won a duel and was not asking for it.
- **No strength meter, either.** A meter that advises while the server accepts is theatre; a meter
  that refuses is an undocumented policy living in a JavaScript bundle, on a client
  `ADR-0002` says the server may not trust.

The policy is therefore a **pure function of the presented secret**. It reads no handle, no player
row and no history — which is what lets sign-up and reset apply the identical rule with neither
knowing anything about the other.

### 4. Nothing is trimmed

A leading or trailing space is part of the password. `raw.trim()` is the first line of
`canonicalDisplayNameOrNull` and copying it here would be a silent, permanent change to somebody's
secret that every future sign-in path would have to reproduce byte for byte or the account goes
missing — the identical reasoning `TASK-040310` used to refuse trimming a handle.

### 5. The secret is NFC-normalised before it is hashed, in exactly one place

**Unicode NFC. Never NFKC. Applied identically at sign-up, sign-in and reset.**

- **Why normalise at all.** `é` typed as `U+00E9` and as `U+0065 U+0301` renders identically and
  hashes differently. A player who sets a password on macOS and signs in on Windows would be refused
  by a system that `ADR-0031` guarantees cannot tell them why, and — if they declined the recovery
  email — cannot let them back in at all. This is `ADR-0029` §2's reason with the consequence made
  worse: there, the failure is two rows; here, it is a lost account.
- **Why NFC and not NFKC.** NFKC folds strings a player can see apart — `ﬁ` to `fi`, `５` to `5`,
  `²` to `2` — which means silently accepting a password that is not the one that was set. NFC
  unifies only sequences that are canonically equivalent, i.e. exactly those a player could not
  distinguish if they tried. Unify the indistinguishable; never unify the merely similar.
- **In one place, not three.** The normalisation happens where a presented secret becomes the bytes
  Argon2 sees, so no endpoint can forget it and sign-up and sign-in cannot disagree. *Which* file
  that is belongs to the ticket; what may not exist is two call sites that could ever drift apart.
  ASCII is unchanged by NFC, so `STORY-0403`'s published-vector tests
  (`TASK-040304`, `TASK-040305`) are unaffected.

### 6. Where it is enforced, and what the endpoint answers

**In the write path at the endpoint, exactly where `ADR-0029` §3 and `ADR-0031` §1 put the display
name and handle rules.** Not in a `CHECK` — the column holds a hash, and there is nothing for the
database to check. Not in `PresentedSecret`: `TASK-040307`'s *"no `init`, no `require`, no length
rule"* stands, and answering `DEC-043` does not retract it. The value class is constructed at
sign-in too, so a minimum in its constructor would lock out every account the day the number moves,
and a throwing constructor turns a `422` into a `500` unless every caller catches it. A type that
enforced the maximum but not the minimum would be worse than one that enforces neither.

`POST /api/auth/sign-up`, extending `ADR-0031` §5's row and `STORY-0404`'s notes:

| Outcome | Answer |
| --- | --- |
| No resolvable identity | `401`, empty body, nothing written |
| Handle fails `ADR-0031` §1's fold | `400`, empty body |
| Handle is taken | `409`, empty body |
| **Password is under 8 or over 128 code points** | **`422`, empty body** |
| This player already holds a `password` credential | `409`, empty body |
| Success | per `STORY-0404`; no session is issued |

`422` rather than `400` because `ADR-0031` §5 already spends `422` on exactly this — *"the token was
good and the new password fails policy"* — on `reset-password`, and one meaning per code beats two
paths for one refusal. It is distinct from the handle's `400` and `409` because the form must know
which field to mark, and neither code is an oracle: both state a property of the caller's own input.

**The body is empty, and it can be, because there is exactly one rule.** The client already knows
it; there is no server-composed reason to render, and no endpoint anywhere in this system returns
one.

### 7. What a player is told

The rule is stated **before** the field is filled — one sentence, naming the minimum — and a refusal
marks the password field rather than explaining itself. No meter, no colour bar, no *weak/strong*
verdict, no advice about symbols. The exact words, and where the sentence sits, are `STORY-0412`'s
inside `EPIC-06`'s design language; this ADR fixes only that the whole policy is one sentence a
player reads once, and never a verdict on what they typed.

## Consequences

**What it buys.** `STORY-0404` has a rule, a response code and a place to enforce it, and
`STORY-0403` needs no change beyond §5's one line. `ADR-0031` §5's `422` now has a policy behind it.
A player can paste anything a password manager generates, and a passphrase with a space in it is a
passphrase. The one permanent choice in the area — the bytes hashed — is made before the first row
exists rather than discovered afterwards.

**What it costs.**

- **v0.1 accepts `password`, `12345678`, `qwertyui`, and a password identical to the player's own
  handle.** These are the passwords that actually get guessed, and this decision does not refuse a
  single one of them. `ADR-0031` accepts that sign-up's `409` publicly confirms a handle exists, and
  `ADR-0027` §6 keys the sign-in budget by remote address rather than by identifier — deliberately,
  so a victim cannot be locked out — so nothing slows a distributed guesser *per account*. What is
  left standing between a guesser and one of these accounts is Argon2id itself: four slots at
  ~50–100 ms cap the whole server at tens of verifications a second, so the exposure is a short list
  tried patiently, not an exhaustive search. **The honest summary is that this rule defends against
  a short password and not against a common one**, and the rule that would defend against a common
  one is deferred in §Alternatives with its reasons.
- **A taken account loses a coin counter, a ladder place, and possibly its recovery.** An attacker
  holding the password can delete the recovery email (`ADR-0031`'s endpoint costs the current
  password, which they now have) or attach their own. They cannot read the stored address back —
  `ADR-0031` §6 keeps it out of every response body — which bounds the damage but does not remove
  it.
- **This ADR guarantees nothing about strength.** Eight code points measures typing, not entropy.
  Anybody citing this rule as a security property is citing the wrong thing, and it is written here
  so that the citation can be checked.
- **Somebody's 200-character passphrase is refused**, for a reason that is about the server's
  hashing budget and not about them, in a message that cannot honestly explain itself without
  teaching them about Argon2.
- **NFC is permanent from the first stored hash.** If it turns out to be the wrong fold — an input
  method that produces something NFC does not unify — the only fix is a forced reset for every
  account, and for opted-out accounts `ADR-0031` says there is no reset. This is the single
  irreversible commitment in this ADR and it is being made on reasoning rather than on evidence.
- **The bound is on hashing work, not on bytes buffered.** A caller can still post a large request
  body; the refusal happens in the handler. Nothing here caps a request at the HTTP layer, which is
  a generic property of every endpoint in this server and is not created by this decision.

**What it forecloses.**

- **Hashing raw bytes as received**, from the first stored row. Also case-insensitive comparison,
  truncation to a fixed length, and any other transformation of the secret — all of them are the
  same permanent choice, made once, here.
- **Storing anything about a password other than its hash.** No length column, no *weak* flag, no
  set-at date, no history of previous hashes. A consequence worth naming: if a breach corpus is
  added later, existing accounts can only be checked at **sign-in**, the one moment the plaintext
  exists, and whoever adds it will have to decide what happens to a player who fails the check while
  holding a correct password. Nothing here makes that decision, and nothing here makes it
  impossible.
- **A per-account sign-in budget.** Not foreclosed by this ADR, but worth recording where somebody
  will look: `ADR-0027` §6 refused one on purpose, and the residual above is the price of that
  refusal, not of this one.

**What this does not settle.**

- **A breach corpus.** Deliberately deferred, not rejected. It stays available at any time, costs no
  migration and asks nothing of existing accounts.
- **Whether the HTTP layer caps request bodies.** Pre-existing, generic, the architect's if anybody
  wants it, and not registered as a decision here because this ADR did not create it.
- **What `STORY-0412` writes on the screen.** §7 fixes the shape and the prohibition; the words are
  `EPIC-06`'s.
- **The Argon2 parameters and what happens when they are raised.** `ADR-0027` §1 and `DEC-044`,
  untouched here. This ADR changes what is hashed, never how.

## Alternatives considered

**No rule at all — ship v0.1 with nothing.** This was the DEC's own second half and its case is
serious: the asset is a counter of duels won, `ADR-0027` already does the expensive part properly,
and a rule that cannot refuse `password1` is arguably theatre that costs a form field's worth of
friction for nothing. It is also the smallest amount of code and the fastest sign-up in the world.
Rejected on a technicality that turns out to be decisive: **the maximum has to exist anyway**, for
the work bound in §1, so "no rule" was never actually on the table — the only question was which
bounds. Once a bound is being written, a floor that refuses a one-character password costs the same
line, and a one-character password is guessable online even at tens of verifications a second.

**Twelve characters, or a passphrase minimum of fifteen.** The strongest case is arithmetic: eight
characters is a 1980s number, and against an offline attack on a leaked database, eight lowercase
letters at `m = 19456, t = 2` is within reach of somebody who cares, while twelve is where a
memory-hard hash starts to pay for itself. Rejected because the marginal protection is bought for a
counter of duels won, and paid for on the one form `ADR-0036` says the player did not ask to see —
and because raising the minimum later costs nothing at all (new passwords only, no stored hash
touched), so the cheap direction is to start at the published floor and move up on evidence rather
than down on complaint.

**Composition rules — a digit, a case, a symbol.** They are familiar, instantly implementable, and
they do refuse the very dumbest guesses: `password` fails a digit rule. Rejected because what they
actually produce is `Password1!` — the same guessable corpus with a predictable suffix — and because
a corpus shaped by a rule outlives the removal of that rule by years. And they are precisely the
friction the vision's *Lichess, not PokerStars* sentence rules out on a form that is an offer.

**A breach corpus — a HIBP k-anonymity range query at sign-up.** The strongest case in this list,
and it is genuinely strong: it is the **only** rule that refuses the passwords that are actually
guessed, which is exactly the residual named in the consequences above; the range API discloses
nothing about the password being checked; and NIST recommends it *in place of* everything in the
paragraph above. Rejected for v0.1 on four counts. It puts a third-party network call on a path that
today depends on nothing but Postgres, which forces an answer to what happens when the third party
is down — fail open and the rule is theatre, fail closed and a stranger's outage stops account
creation. It makes the sign-up test depend on a fake standing in for a network service, where this
project's checks are an exit code that runs offline. The alternative shape, a bundled corpus, is
tens of megabytes in the repository and a deployment artefact `EPIC-07` would have to carry, for a
product with no players. And it is **strictly additive later**: refusing more passwords at sign-up
invalidates no stored hash, changes no schema and asks nothing of any existing account — which is
the tiebreaker when the evidence is this thin.

**Refuse a password equal to, or containing, the handle.** One line, no corpus, no network, and it
kills the single most predictable password on a form that has just told the player which handle is
free. It nearly earned its place. Rejected because it makes the policy a function of the *account*
rather than of the secret: the reset path would have to look the handle up to apply it, and two
paths that must agree about a rule are two paths that can disagree. It is also a one-entry blocklist
standing in for the rule this ADR deliberately does not have — and it arrives free, and complete, on
the day the corpus does.

**NFKC instead of NFC.** NIST names NFKC or NFKD, and NFKC unifies more of the ways a keyboard can
produce what looks like the same password — full-width input from a CJK IME being the real case, not
a hypothetical one. Rejected because compatibility folding changes characters a player can tell
apart, so the system would silently store a different secret from the one that was set, and would
accept `５` for `5` forever after. NFC unifies exactly what renders identically, which is the line
`ADR-0029` §2 already drew in this codebase.

**No normalisation — hash the bytes exactly as received.** The simplest possible answer, what most
systems do, incapable of ever altering a secret, and irrelevant to every password typed on a Latin
keyboard. Rejected because the players it fails are failed silently and permanently: a macOS
dead-key `é` and a Windows `é` are different byte strings, sign-in cannot say which half was wrong
(`ADR-0027` §6), and an opted-out account has no reset (`ADR-0031`). It is also the one choice in
this ADR that cannot be revisited after the first row is written, which is the whole argument for
deciding it now rather than defaulting into it.

**Put the rule in `PresentedSecret`'s `init`.** One place, impossible to forget, and the type
already exists and is already about this secret. Rejected because that type is constructed on the
sign-in path too, so the minimum would refuse every account below the current number the day it is
raised; and because a throwing constructor converts a policy refusal into an exception at the
parsing boundary, which is a `500` unless every caller remembers to catch it. `TASK-040307` refused
this for the right reason and the refusal survives the answer intact.

**A strength meter (zxcvbn or similar) instead of a rule.** It measures the thing that actually
matters — guessability, not length — and it advises rather than refuses, which fits a product that
does not want to argue with its players. Rejected because a meter that advises while the server
accepts is decoration, a meter that refuses is a policy living in a client bundle that `ADR-0002`
says the server may not trust, and either way it is a dependency bought for a signal nobody acts on.
