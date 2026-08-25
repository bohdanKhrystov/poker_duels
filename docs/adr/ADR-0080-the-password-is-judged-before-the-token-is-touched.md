# ADR-0080 — The password is judged before the token is touched, so a refusal costs no link

- **Status:** Accepted — amends [`ADR-0031`](ADR-0031-an-optional-verified-recovery-email.md) §5's
  reading of the `reset-password` `422`; §4's `DELETE … RETURNING` and its *"no read-then-write
  window"* are byte-unchanged and are the clause this decision protects
- **Date:** 2026-08-25
- **Resolves:** `DEC-074` — does a good reset token survive a `422`, and by what mechanism?
- **Amends:** `ADR-0031` §5, in one clause. Its status table still answers **`422`** on
  `POST /api/auth/reset-password`; what is corrected is the gloss *"when the token was good and the
  new password fails policy"*, which describes an order the endpoint does not run in. The corrected
  reading is in §2 below. No other line of `ADR-0031` moves, and it is not superseded — `ADR-0062`
  §5's convention exactly, one clause at a time
- **Builds on:** `ADR-0031` §4 (one statement, one transaction, one use) and §5 (the status table
  and the fifteen-minute suppression),
  [`ADR-0048`](ADR-0048-a-password-has-one-rule-and-it-is-length.md) §2 (the maximum runs *"before
  Argon2 runs and before the identifier is looked up"*), §3 (the policy is *"a pure function of the
  presented secret"*), §6 (enforced in the write path at the endpoint, one meaning per code) and §7
  (the rule is published before the field is filled),
  [`ADR-0074`](ADR-0074-sign-in-is-ten-wrong-passwords-a-minute-reserved-before-the-hash.md) §4 and
  [`ADR-0078`](ADR-0078-the-mail-is-the-only-real-check-on-an-address.md), which apply the same
  not-an-oracle test to a refusal that precedes a lookup
- **Constrains:** `TASK-041629` (which gains the check and loses one of its named tests),
  `TASK-041617` (which transcribes the corrected sentence rather than §5's), and `STORY-0417`'s
  form, which may never render a `422` as *your link is still good*. `TASK-041620` is **unchanged**
  and ships as planned — see §7
- **Leaves open:** whether a breach corpus is ever consulted, which `ADR-0048` already left open and
  which this ADR attaches one condition to (§Consequences)

## Context

`ADR-0031` says two things about `POST /api/auth/reset-password` that cannot both be true of the
same endpoint.

§4 makes single use a property of one statement:

> ```sql
> DELETE FROM password_reset
>  WHERE token_hash = ? AND expires_at > now()
> RETURNING player_id
> ```
>
> No `used_at` column, **no read-then-write window**, no way for two concurrent submissions of the
> same token to both succeed.

That statement is inside *"the transaction that writes the new password"*. So under §4 there is no
moment at which the server knows the token was good and has not yet spent it: **knowing is
spending**, and the same transaction that learns the answer also sets the password.

§5's table then promises:

> `422` when the token was good and the new password fails policy

which presupposes the opposite order — a moment where the token is known good, the password is then
judged, and the caller is told to try again. If the endpoint has reached that moment under §4, the
new password is already written and the sessions are already deleted; answering `422` at that point
would be a lie about what the server just did.

This is a conflict between two sections of a merged ADR, not an acknowledged gap: `ADR-0031`'s
*What this does not settle* does not mention it. It surfaced while `STORY-0416` was being split,
and the planner resolved it inside a ticket before pulling the resolution back out — which is the
correct instinct and the reason this file exists rather than a paragraph in `TASK-041629`.

What is genuinely in tension:

- **A refusal that spends the link is not a refusal, it is a small disaster.** `ADR-0031`'s
  Consequences make no recovery a *total, permanent* loss of the account, and
  [`ADR-0077`](ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md) prices a
  lost mail at *"fifteen minutes of silence they cannot distinguish from anything else"*. A short
  password is the single most ordinary mistake at a password field. Pricing it at a mail round trip
  is a real cost paid by exactly the player who has already lost their password once.
- **A refusal that does not spend the link may be a probe.** §4's window is closed on purpose. Any
  shape that reads `password_reset` to decide *which* refusal to send makes token validity
  observable without consuming it, which is the property the 256 bits were chosen to make
  unnecessary to defend: `ADR-0031`'s Alternatives rejected a six-digit code precisely because it
  *"has to be defended by a per-account attempt counter"* and concluded *"a 256-bit token in a
  fragment needs no counter at all."* That conclusion holds only while liveness and consumption are
  the same event.
- **`422` is already spent, and spent on a meaning.** `ADR-0048` §6 chose `422` for sign-up's
  password refusal **because** `ADR-0031` §5 had already spent it here — *"one meaning per code
  beats two paths for one refusal."* Whatever this decides, the same rule must not answer two
  different codes at two endpoints.
- **This endpoint has no budget and is not getting one.** `ADR-0031` §5 budgets `recovery-email` and
  `forgot-password` by name;
  [`ADR-0079`](ADR-0079-five-to-attach-ten-to-forget-and-the-attach-budget-is-the-only-mail-cap.md)
  fixed those four numbers two decisions ago and reaches neither this endpoint nor any account. So
  whatever work a refusal does here, an unauthenticated caller may do it as often as they like.

### The deadline, honestly

Nothing here is a migration and nothing is permanent in the data. The deadline is a sentence, not a
schema: **`STORY-0417` builds the form that renders this `422`, and `TASK-041617` writes the
sentence a client author will read.** A form that has told players *your link is still good — pick
a longer password* cannot be quietly retracted, because the retraction is the case where the player
loses the account. Deciding after either lands means correcting copy that has already been
believed; deciding now costs one `if`.

## Decision

### 1. The order is: decode, judge the password, then spend the token

**`POST /api/auth/reset-password` evaluates exactly three steps, in this order:**

1. **Decode the body.** Any failure ⇒ `400`, empty body. (`TASK-041620`, unchanged.)
2. **`passwordIsLongEnough(newPassword)` and `passwordIsWithinTheWorkBound(newPassword)`.** Either
   `false` ⇒ **`422`, empty body, and nothing else runs**: no connection is taken, no statement is
   executed, and `password_reset` is neither read nor written.
3. **`passwordResets.consume(token, newPassword)`** — `ADR-0031` §4's single `DELETE … RETURNING`
   inside the transaction that writes the password, unchanged in every respect. `true` ⇒ `204`;
   `false` ⇒ `400`.

**A `422` therefore never touches the token, and a token is looked at only when the password has
already been accepted.** The two predicates are one conjunction and their relative order is
unobservable — one status, an empty body, no message (`ADR-0048` §6) — so no ticket has to choose
between them and no test may pin which fired.

This is `ADR-0048` §2's placement rule applied at this endpoint rather than a new rule: the maximum
is *"checked **before** Argon2 runs and **before** the identifier is looked up"*, and at
`reset-password` the token is the identifier. The minimum joins it because `ADR-0048` §2 makes reset
one of the two endpoints the minimum applies at, and splitting the pair across the lookup would give
one published rule two positions, two timings and one status.

### 2. §4 stands as written; §5's precondition is what gives way

**§4 is untouched.** The statement, the transaction, the absence of a `used_at` column and the
sentence *"no read-then-write window"* are all byte-unchanged, and this decision exists to keep
them: no shape here reads `password_reset` outside that `DELETE`.

**§5's status table is untouched.** The endpoint still answers `422`.

**§5's parenthetical is corrected.** The row's reading becomes:

> `422` when the new password fails policy — **whether or not the token is good, and without the
> token being looked at**. `400` for a bad token, which is answered only once the password has
> passed.

Recorded the way `docs/adr/README.md` and `ADR-0062` §5 require an amendment to be recorded rather
than a supersession: this ADR carries an `**Amends:**` header, `ADR-0031`'s status line carries the
correction, and the index says so in both rows. `ADR-0031`'s decision is intact — the token is still
256 bits, still one hour, still single use, still spent by one statement, and the reset still ends
every session.

### 3. A `422` states a fact about the caller's own input, and three merged ADRs already accept that shape

The worry that `DEC-074` recorded against this order is that it *"answers `422` before the token is
known, which tells a caller their token may be fine."* Checked in the direction it actually runs,
the disclosure goes the other way and is smaller than the alternatives':

- **A `422` is byte-identical for a live token, an expired token, a spent token and a string the
  caller invented.** The branch is selected entirely by the caller's own password, so the status
  carries **no** information about `password_reset` at all. Every order that keeps the `422` and
  reaches the token first makes `400`-versus-`422` a report on that token's liveness; this one
  removes the report rather than the status.
- **The policy is a pure function of the presented secret.** `ADR-0048` §3: it *"reads no handle, no
  player row and no history."* So the `422` is computable by the caller offline, before they send
  anything, and the endpoint has told them nothing they did not already have.
- **The rule is published.** `ADR-0048` §7 states it before the field is filled, and `docs/protocol.md`
  already documents sign-up's identical `422` as *"the password is under 8 or over 128 code
  points"*. There is no secret here to leak, and after this decision the two endpoints' `422` mean
  the same sentence — which is what `ADR-0048` §6's *one meaning per code* asked for.
- **This is the test three merged ADRs apply, not a new one.** `ADR-0048` §2: an over-long password
  *"may answer fast, and that is not an oracle: the only fact a fast refusal discloses is a property
  of the input the caller sent."* `ADR-0074` §4, on a refusal that precedes the lookup and the hash:
  *"That is not an oracle by the test `ADR-0048` §2 and `ADR-0055` §3 both applied."* `ADR-0078`
  puts the address syntax predicate in front of the attach budget on the same ground.

**The endpoint verifies no secret; it sets one.** There is no stored password to test a candidate
against, so *guessing* has no meaning at this endpoint under any order.

### 4. What a refusal leaves behind, and what the next request sees

- **A `422` leaves the row exactly as it was**: same `token_hash`, same `player_id`, same
  `issued_at`, same `expires_at`. Nothing is written anywhere, no counter is incremented, and there
  is no state in which "this token has been refused once" is representable — the same reason §4
  refuses a `used_at` column.
- **The next request carrying that token and an acceptable password answers `204`** while
  `expires_at > now()`, and `400` after. The token's one hour runs from issue and is not extended,
  restarted or shortened by a refusal.
- **`ADR-0031` §5's fifteen-minute suppression still sees a live token**, because `issued_at` is
  untouched. So a player who presses *email me a link* again after a `422` gets §5's complete
  no-op — nothing sent, and *crucially the outstanding token is not invalidated*, so the link they
  are about to use survives their frustration. Under any order that spends the token on a refusal
  this is the opposite: the row is gone, the suppression no longer applies, and the player is one
  mail round trip and `ADR-0079`'s shared-address budget away from trying again.
- **A `204` is the only outcome that deletes the row**, and `TASK-041621` still owns the two
  concurrent submissions.

### 5. The endpoint stays unbudgeted, and this order makes its cheapest request cheaper

`reset-password` gets no `AttemptBudget` here, and this decision does not move the case for one:

- **The `422` branch does strictly less work than the `400` it replaces** — one `codePointCount`
  over a string the server has already materialised, then a response. No connection, no statement,
  no Argon2. The per-request cost of the cheapest refusal goes **down**.
- **Probing a token costs exactly what it cost before**: one `DELETE … RETURNING` round trip, and
  only for a caller who also sends an acceptable password. A live token found this way is spent by
  the finding, which is §4's property and the reason `ADR-0031` needed no counter.
- **There is no oracle to budget.** A caller can learn one thing they already knew — whether their
  own string is 8 to 128 code points — and nothing about any account.

The condition that would reopen this is named rather than left to be discovered: **if a policy rule
ever becomes expensive or secret**, this order puts it in front of an unauthenticated, unbudgeted
endpoint, and the cheapest repair is a budget on this endpoint rather than a reordering. See
§Consequences.

### 6. Reaching the losing branch in a test, deterministically

The refused branch is a pure function of the request body, so `TASK-041629`'s tests need no clock
control, no second connection, no latch and no fixture ordering:

- **`422` is reached by the password alone** — 7 code points at one bound, 129 at the other, and
  four astral characters for the unit. Nothing about the token, the row or the clock participates.
- **Survival is asserted with one extra request**: the same token, an 8-code-point password, `204`.
  Nothing mutates between the two, so there is nothing to race.
- **No test can assert the row's absence after a `422`**, because no request produces that state.
  The assertion is the second request's `204`.
- **The one clock hazard is in the fixture, not in the branch.** `ADR-0062` §5 puts `expires_at` on
  an injected `java.time.Clock` while §4's `DELETE` compares it against SQL `now()`, so a fixture
  that pins a `Clock` to a fixed instant far from the database's own clock mints tokens that are
  already expired — which would turn the survival assertion's `204` into a `400` for a reason that
  has nothing to do with this decision. Whatever instant a fixture chooses, `expires_at` must land
  in the future of the database's clock.

### 7. Where it lands

- **`TASK-041620` stands unchanged and needs no re-cut.** It ships decode ⇒ `consume` ⇒
  `204`/`400`, with no policy and no `422`; its four tests, its eight acceptance criteria and its
  *Out of scope* are all still correct, because the step this decision adds goes in **front** of
  `consume`, where nothing that ticket writes has to move. One constraint it must satisfy to stay
  green afterwards, and it is a fixture constraint rather than a behaviour one: **every request in
  `ResetPasswordRouteTest` must carry a `newPassword` of 8 to 128 code points, including the two
  that expect `400`** — a shorter one will answer `422` the day the check lands in front of the
  lookup, and `TASK-041629`'s requirement that this file pass *unchanged* would be unsatisfiable.
- **`TASK-041629` gains the check between decode and `consume`**, and two of its named tests are
  re-specified by this answer. `aRefusedPasswordLeavesTheTokenAsTheDecisionSays` resolves to the
  surviving branch: a second request with the same token and an acceptable password answers `204`.
  `aBadTokenStillAnswersFourHundredNotFourHundredAndTwentyTwo` asserts the order this decision
  reverses and cannot be written — a fabricated token with a 7-code-point password now answers
  `422`, because the token is never looked at. What that test was defending is worth a test in the
  other direction, and it is the sharper property: **the `422` for a fabricated token and the `422`
  for a live one are indistinguishable**, which is what keeps the status from reporting on the row.
  The re-cut is the planner's; this ADR edits no ticket.
- **`PasswordResets.consume(token, secret): Boolean` is unchanged** and needs no third outcome.
  `TASK-041614` already assumes this split — *"the password policy … runs at the endpoint … a
  distinction this port cannot express and must not try to"* — and this decision is what makes that
  sentence true rather than merely convenient.
- **`TASK-041617` transcribes §2's corrected sentence**, not §5's parenthetical, when it writes the
  `reset-password` response table into `docs/protocol.md`. The status is still `422`, so this is a
  wording correction and not the route-versus-table mismatch that ticket's *Out of scope* sends
  back as a ticket. Written well, the row reads as sign-up's already does: *the password is under 8
  or over 128 code points* — plus *this is answered whether or not the token is good*.
- **`STORY-0417`'s form may not render a `422` as the link being alive**, in any wording. It means
  the password was refused and nothing else. What the form *says* is `STORY-0412`'s and
  `EPIC-06`'s, under `ADR-0031`'s *What this does not settle* and `ADR-0048` §7 — this ADR fixes
  only what the status may not be read to mean.
- **Nothing else moves.** No migration, no column, no index, no sweep, no config value, no
  `PROTOCOL_VERSION`, no `ServerMessage`, no new type. `poker-engine` learns nothing.

## Consequences

**What it buys.** `TASK-041629` unblocks with an order, a status, a token fate and a test that
cannot race. `ADR-0031` §4's *"no read-then-write window"* survives intact — the sentence with the
security reason behind it is the one that stands. A player who types a short password loses nothing:
they are told, they type a longer one, and the same link works. Sign-up and reset answer `422` for
the same reason and mean the same sentence, which is what `ADR-0048` §6 asked for and did not quite
get. And the whole decision is one `if` in one handler: reversing it is moving three lines below
`consume` and rewriting one test, which is why it is the right call on evidence this thin.

**What it costs.**

- **A `422` no longer tells anybody the link is alive, and that information does not exist anywhere
  else.** A player refused for a short password, who then takes ten minutes to choose a longer one,
  can be answered `400` on the second submission — two refusals for one attempt, the second of them
  the one that costs them the recovery path. Under §5's literal reading the first refusal would have
  proved the link was good. That proof is gone, deliberately, and `STORY-0417` has to build a form
  that can move from *your password was refused* to *your link has expired* without having
  contradicted itself.
- **An unauthenticated stranger holding no token can now make this endpoint answer `422`.**
  `TASK-041629`'s planner wrote a test to prevent exactly that, and this ADR overrules it on the
  ground that the rule is published and the answer is a pure function of the caller's own input.
  **That ground is a condition, not a permanent fact**: the day the policy consults anything the
  caller does not already have — a breach corpus, which `ADR-0048` explicitly leaves open and calls
  *"additive whenever anyone wants it"*, or any rule that reads a row — this order hands an
  unbudgeted, unauthenticated caller an unlimited oracle over it, and turns a network call into
  something a stranger can trigger without a token. Whoever adds that rule must either budget this
  endpoint or move the rule behind the lookup, and this paragraph is where they will find that out.
- **A caller who has both a dead link and a short password now needs two round trips to learn it**,
  in a fixed order they cannot choose: the password first, the token second. That is one more
  refusal than the old reading implied, aimed at the least-equipped caller in the system.
- **`ADR-0031` §5 now reads wrong on its own**, and is corrected only in its status line and here.
  Somebody will read that table without following the link and build the other endpoint in their
  head. That is the standing price of amendment-by-status-line — the same price `ADR-0062` §5 paid
  three times in one change — and it is cheaper than retiring a 300-line decision, and every other
  clause it settles, to correct a subordinate one.
- **One planned test is deleted rather than adjusted**, and a ticket that was written to be
  complete now needs a planner pass before it can be started.

**What it forecloses.**

- **`422` as a liveness probe, for the client and for anybody else.** No response from this endpoint
  distinguishes a live token from a dead one except by spending it, and that is now a property
  rather than an accident.
- **Any policy rule that is expensive, stateful or secret**, without either a budget on this
  endpoint or a new ADR moving the check behind the lookup (above).
- **It does not foreclose** consume-then-roll-back, a pre-check, or a `400`-for-everything endpoint.
  Each is one handler's worth of change away, which is the whole argument for deciding it this way
  today.

**What this does not settle.**

- **What the player is told**, on a `422` or on a `400`. `ADR-0031`'s *What this does not settle*
  and `ADR-0048` §7 already assign the words to `STORY-0412` inside `EPIC-06`'s design language;
  this ADR adds one constraint and no vocabulary — a `422` may not be reported as the link being
  alive. **Not a `DEC`**: the question is already assigned, and no wording it can produce changes
  what the server does.
- **Whether `POST /api/auth/reset-password` is ever budgeted.** Not raised as a `DEC` because
  nothing here changes its exposure — the cheapest refusal got cheaper, the token probe cost the
  same, and `ADR-0031` §5 named the two endpoints that get budgets. §5 above names the one condition
  under which it becomes a question.
- **`TASK-041621`'s concurrency** — two submissions of one link — is untouched. Both still reach
  `consume`, and §4's statement decides them.

## Alternatives considered

**Spend the token first, and a `422` means the player needs a new link.** Its strongest case is that
it is the only shape under which `ADR-0031` §5's sentence is literally true: the endpoint answers
`422` *because* the token was good, so the status is token-authenticated, the policy is never
disclosed to a stranger, and the client can say *your link is spent* with certainty. Rejected on the
price of the most ordinary mistake at a password field. A short password would cost a mail round
trip: back to the mailbox, a fresh `forgot-password` against `ADR-0079`'s ten-per-minute
shared-address budget, `ADR-0077`'s *no retry — a lost mail is fifteen minutes of silence*, and
`ADR-0031`'s *a misconfigured deployment has silently broken recovery, undetectable from outside* —
all of it spent by the one player in this product whose failure mode is total and permanent. It also
cannot be built without breaking §4's weld: the `DELETE` and the password write are one transaction,
so answering `422` after the delete means either committing half of that transaction on purpose or
splitting it in two, and a crash between the halves loses the link *and* leaves the password
unchanged. The one thing it gets right, and this decision gives up, is recorded above as a cost.

**Consume inside a transaction that the policy failure rolls back.** The strongest case is genuinely
the strongest of the four: it is the only option that satisfies §4 and §5 *simultaneously and
literally*. The token is known good because it was spent; the row survives because the transaction
never commits; and no read-then-write window opens, because the row is deleted under lock for the
whole of it. Rejected on three counts. `consume(token, secret): Boolean` has no third value, so the
port would grow a result type whose only purpose is to carry a status code `ADR-0048` §6 puts *"in
the write path at the endpoint"* and `TASK-041614` says the port *"cannot express and must not try
to"*. The refusal path becomes a transaction whose **normal** outcome is a rollback, which reads as
a fault in every log and metric that will ever look at it, and which no middleware may retry. And it
holds a row lock across a check that needs no database at all — a shape that invites the next
expensive thing inside the same transaction. For all that, it buys exactly one bit over this
decision: that a `422` proves the link is alive.

**A non-consuming `SELECT` on `password_reset`, then the policy, then the consume.** Its strongest
case is that it answers the register's question in the most literal and most helpful way available:
`400` for a dead link, `422` for a live link with a bad password, every refusal token-authenticated,
and the client told the maximum it could usefully know. Rejected because it makes **token validity
observable without consuming it**. That is not merely §4's window reopened by name; it is the
property that lets somebody holding a candidate or stolen link *confirm* it and then wait, on an
endpoint with no budget, where `ADR-0031` chose 256 bits precisely so that no attempt counter would
be needed. Single use would survive — the `DELETE` still decides the race — but the reason the
`DELETE` was the only statement would not.

**Judge the policy first, but answer `400` when the token is also bad.** Its case is that it keeps
`TASK-041629`'s written test true and keeps the password rule undisclosed to a caller with no token,
at the cost of one extra query. Rejected because it is the previous option wearing a different hat:
deciding *which* refusal to send requires knowing whether the token is live without spending it, and
there is no way to obtain that fact that is not the pre-check above. Recording it separately is
worth the space, because it is the shape a reader will reach for when they see the test being
deleted, and the test cannot be satisfied without the defect.

**`ADR-0031` §5's `422` is wrong; a refused password answers `400` like everything else.** Its
strongest case is real: one refusal, one status, no new behaviour, and a caller learns strictly less
— which is the shape §5 itself chose for `verify-email`, where *"a token that is unknown, expired or
already consumed"* collapse into one `400` on the express ground that the three must be
indistinguishable. Rejected because `ADR-0048` §6 spent `422` on this exact meaning at sign-up
*because* §5 had already spent it here, so collapsing it would make one published rule answer two
different codes at two endpoints — the opposite of *one meaning per code*. And `verify-email`'s
collapse hides three facts about **server state** from a caller who should learn none of them; here
the two facts are one about server state and one about the caller's own input, which is precisely
the pair a client must be able to tell apart in order to know whether to fix the password or ask for
a new link.
