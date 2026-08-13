# ADR-0022 — The room code is the invite, and failed joins are budgeted

- **Status:** Accepted
- **Date:** 2026-08-13
- **Resolves:** `DEC-012`
- **Constrains:** `STORY-0207` (the wiring that exposes `RoomRegistry.join` to the wire), the
  first public deployment

## Context

The product decision is made and is not re-argued here: **holding a room code is the invite.**
Whoever presents it takes the second seat, like a Lichess challenge link — no host confirmation of
the guest, ever. That decision has a direct technical consequence: the code is now the *only* thing
standing between a stranger and a seat, so what the code is worth as a secret, and what stops
someone sweeping the code space, stop being open questions and become load-bearing.

What exists today: `RoomCode` is eight characters of Crockford base32 — 32 symbols, so 32⁸ = 2⁴⁰ ≈
1.1 × 10¹² codes — minted by `RandomRoomCodeSource` from `SecureRandom`, unbiased because the
alphabet size is a power of two. `RoomRegistry.create` guarantees uniqueness among live rooms via
`putIfAbsent`. A `WAITING` room lives at most `RoomTimeouts.waitingMillis` (default 10 minutes)
before the reaper removes it. Nothing anywhere rate-limits join attempts, and the registry is not
yet wired to any transport — `STORY-0207` does that — which is exactly why now is the free moment
to decide: the limiter can exist before the first public link does.

The tension: entropy versus typability (the code goes in a link and gets read aloud — that is why
the alphabet is Crockford), and structural defense versus machinery nobody needs yet.

## Decision

**The code stays at 40 bits, `RoomRegistry.join` gains a per-player budget of failed attempts, and
a code stops being an invite the instant the seat is taken.**

1. **Entropy: 8 characters is enough, and here is the arithmetic that says so.** Guessing a
   *particular* room is hopeless: 2⁻⁴⁰ per attempt. The real threat is a sweep that hits *any*
   live room. A guess only counts if it lands while a code's room is still `WAITING`, a window of
   at most 10 minutes. At a sustained R guesses per second against N concurrent waiting rooms, the
   expected hits per window are `R × 600 × N / 2⁴⁰`: even at 10 000 guesses per second against 100
   waiting rooms — far beyond anything this product will see — that is ≈ 5 × 10⁻⁴ per window. The
   limiter below is what makes such a rate unreachable in the first place; the entropy is adequate
   *given* the limiter, and the two are decided together for that reason.

2. **A failed-join budget lives inside `RoomRegistry.join`: at most 10 refused joins per player
   per rolling 60 seconds.** An attempt over budget is refused as a new `RoomRefusal`
   value, `TOO_MANY_ATTEMPTS`, leaves every room untouched, and still counts against the budget.
   Successful joins do not count — the budget meters guessing, not playing. Concretely:

   - The numbers arrive as a `JoinLimits(maxFailed: Int, windowMillis: Long)` value with a
     `DEFAULT` of `(10, 60_000)`, a constructor parameter beside `RoomTimeouts`, surfaced through
     `ServerConfig` exactly as `TASK-020613` did for the timeouts — configuration, not a literal.
   - Time comes from the already-injected `ServerClock`, so tests run on virtual time with no
     sleeps. **The engine is untouched**: this is time-dependent server behaviour and it lives
     where `ADR-0013` put the grace period — in `poker-server`, never in `poker-engine`, which
     keeps no clock and does no I/O.
   - The budget's state is in-memory in the registry, like the rooms themselves: rooms are not
     durable (`ADR-0011`), and a restart forgetting the counters is accepted for the same reason
     it may forget the rooms.
   - It lives *inside* `join`, not in the transport handler, so every present and future caller —
     the socket wiring, a later HTTP join, a test — is covered structurally. A limiter at the call
     site is a limiter someone eventually forgets.
   - What 10-per-minute protects against, concretely: one player id sweeps at most 600 codes an
     hour — 2⁴⁰ codes deep, that is noise. Device ids are trivially mintable (`ADR-0012`), so an
     attacker with D live devices sweeps `10 × D` per minute; reaching the 10 000-per-second rate
     in the arithmetic above would take 60 000 concurrently connected minted devices, which is a
     volumetric attack — the deployment front-end's problem (per-IP throttling, connection caps),
     explicitly not this layer's, because behind a proxy this server sees only headers it cannot
     trust.

3. **A code's life as an invite ends when the seat is taken.** `Room.join` already refuses every
   non-`WAITING` state — `PLAYING` refuses `ROOM_FULL`, `FINISHED` and `ABANDONED` refuse
   `UNKNOWN_ROOM`, indistinguishable from a room that never existed — so this is already
   structurally true; it is recorded here as the *decision*, so nobody later "fixes" a finished
   room to be joinable by its code. After the duel ends the room lingers `finishedMillis` (default
   5 minutes) for a rematch and is then reaped, at which point the code returns to the space and
   may in principle be minted again. A stale link meeting a re-minted code would seat a stranger —
   at 2⁻⁴⁰ per new room, that is accepted, and `create`'s `putIfAbsent` already makes a collision
   between *live* rooms impossible.

4. **Not decided here.** Host confirmation is foreclosed by the human's decision, recorded above.
   Per-IP limiting, connection caps and flood defense belong to the deployment front-end. What a
   client *says* when it receives `TOO_MANY_ATTEMPTS` is `EPIC-03`'s rendering, not this ADR's.

## Consequences

**What it buys.** The Lichess-link experience survives contact with the open internet: joining
stays one click with zero friction for the invited, while a sweep of the code space is capped at a
rate the entropy laughs at. The defense is testable on virtual time, configurable without a code
change, and in place before any transport exposes `join`.

**What it costs.**

- `RoomRegistry` gains its first time-dependent state beyond timestamps — a counter map that must
  be pruned as windows expire, and one more refusal for `STORY-0207` to carry to the wire and
  `EPIC-03` to render.
- A legitimate player who fumbles a code more than ten times in a minute is locked out for up to a
  minute. The budget is deliberately generous against human typo rates; if it ever bites a real
  player, it is one config value.
- The per-player key inherits `ADR-0012`'s weakness: minted devices dilute the budget. Accepted,
  with the dilution quantified above — exploiting it requires DDoS-scale device minting, which is
  the front-end's fight and already flagged in `ADR-0012` as a gate on the public leaderboard.

**What it forecloses.** Nothing structural. Host confirmation could still be added later as an
opt-in room setting without undoing any of this — but that is a product question, and today's
answer is no.

## Alternatives considered

**Longer codes — 12 characters, 60 bits — and no limiter.** Its strongest case: sweeps become
hopeless with zero runtime machinery, no counters, no new refusal. Rejected: it trades away a
permanent product property — a code short enough to type and unambiguous enough to read aloud,
the entire reason the alphabet is Crockford base32 — to buy what a page of limiter code buys
without touching the product.

**Entropy alone — do nothing.** Its strongest case: 2⁴⁰ already makes a targeted guess hopeless,
and it is the code we have. Rejected: the sweep arithmetic is only comfortable when a rate ceiling
exists; with joins at line rate, an attacker's expected hits grow linearly with rate and time, and
"months of flooding might seat one stranger" is a strange risk to keep when the fix is this small.

**Rate limiting in the transport handler, per connection or per IP.** Its strongest case: it
meters the resource actually being spent and could see network identity. Rejected: this server
runs behind whatever fronts it and sees only forwarded headers it cannot trust, so per-IP is the
front-end's job; and a call-site limiter covers only the call sites someone remembered.

**A global budget rather than per-player.** Its strongest case: immune to device minting.
Rejected: one attacker exhausting a global budget locks every legitimate guest out of every room —
it converts a guessing defense into a one-loop denial of service, a strictly worse failure mode.

**Proof-of-work or CAPTCHA on join.** Its strongest case: raises the cost of every guess
regardless of identity. Rejected: it puts friction on the product's single success condition —
*"send a link, she opens it in a browser"* — out of all proportion to a 2⁻⁴⁰-per-guess threat.
