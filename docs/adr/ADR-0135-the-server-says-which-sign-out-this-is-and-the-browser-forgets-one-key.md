# ADR-0135 — The server says which sign-out this is, and the browser forgets one key

- **Status:** Accepted
- **Date:** 2026-09-07
- **Resolves:** `DEC-152` — **by what mechanism does a browser stop owning the profile it owned, at
  a sign-out and only there, and how does the confirmation learn before it acts which of the two
  sign-outs this is?** Registered 2026-09-07 by
  [`ADR-0131`](ADR-0131-signing-out-of-your-own-account-hands-the-browser-a-new-profile.md) §7,
  which fixes the product half, names three candidate shapes and chooses between none of them.
- **Serves, and does not reopen:** `ADR-0131`. Its §1 rows, §2 invariant, §3 *nothing is revoked,
  deleted, moved or destroyed*, §4 *one abandonment at one confirmed press*, §5 *the player is told
  on the confirmation that already asks* and §6 *the name ask is not asked again* are the product's,
  and every one of them is a constraint this ADR is written inside. `ADR-0049`'s *revoking is final*
  and `ADR-0050`'s *signs out everywhere but here* are settled by `ADR-0131` §3 as irrelevant to
  this path, and are not re-argued here.
- **Nothing crosses the socket.** `Hello`, `Welcome`, `ProtocolCodec`, `protocol.gen.ts` and
  `PROTOCOL_VERSION` are byte-unchanged, `ADR-0047`'s ledger gains no row, and `STORY-1410`'s
  implementing ticket is therefore **not** `atomic:` under
  [`ADR-0068`](ADR-0068-an-atomic-ticket-names-the-gate-that-forbids-splitting-it.md) §3. The whole
  mechanism is one HTTP route, one comparison, one storage key and one prop.
- **No schema, no migration, no write.** `device_binding`, `player`, `credential`, `duel` and
  `duel_result` are untouched on this path, in either case.
- **Constrains:** `STORY-1410`'s implementing ticket. Its design card and its copy wait on nothing
  and are unaffected by this ADR, which mints no string.

## Context

`ADR-0131` §7 hands the architect a question with its hands tied in an unusual way, and the shape of
the answer falls out of the knot rather than out of preference.

**The abandonment cannot be a server act, because §3 forbids every server act that would be one.**
Revocation is out (`ADR-0049` §2's `UPDATE` and its finality trigger stay off this path, §3 is not
entered, the binding left behind stays live). Deleting the binding is out. Moving it is out. Writing
to `player`, `credential`, `duel` or `duel_result` is out, and no coin moves. The one write a
sign-out already makes — `DELETE FROM auth_session` — ends the *session*, not the ownership.
Subtract everything §3 forbids and exactly one lever remains: **what the browser holds.** `ADR-0002`
licenses pulling it — a browser may forget a credential it holds — and §7 says so in as many words.

**But the knowledge cannot be the client's, because §7 says it is a server fact and the measurement
in its §Context backs it.** `DuelSocket.kt:168` answers a session-identified handshake with
`Welcome(deviceId = null)`, and `ADR-0027` §4 does not merely ignore a device id presented beside a
token, it declines to validate one — `IdentityResolver.resolve` returns `Identity.Session(playerId)`
without ever calling `PlayerDirectory`, and `Identity.Session`'s own KDoc makes that structural:
*"a session that wins is not merely unaffected by the device beside it, it never reads one."* So a
browser holding both a token and `pd.deviceId` has, today, no way to learn which player that string
names. Neither `signedIn` nor the presence of a token may stand in for the answer (§7), and they
cannot: both are true in row one and row two alike.

**Those two forces pull the mechanism across the client/server boundary, and that is the whole
design problem.** The act is the browser's; the knowledge is the server's; the two halves are
separated by a network and by time — the fact is read when a screen loads and spent when a button is
pressed. Any such split has a disagreement mode, so the question is not *whether* the two halves can
disagree but **which way they fail when they do**. `ADR-0131` §2 answers that: a browser that keeps a
profile it should have left is today's shipped defect, visible and repairable; a browser that leaves
a profile it should have kept is `EPIC-04`'s original harm, and `ADR-0125` deleted the only warning
about it. The two directions are not symmetric and the mechanism may not treat them as if they were.

**The obvious carrier for the fact is already spoken for.** `GET /api/me` is the account screen's one
read, it already resolves identity, it already carries a derived boolean of exactly this family in
`deviceRouteLive`, and `apiFetch` already sends both `X-Device-Id` (from `readFromApi`) and
`Authorization: Bearer` (from `authorizedFetch`) on it — so the server already receives both
credentials on one request and needs no new header to compare them. The trouble is one line further
on: `PUT /api/me/name` answers **the same `ProfileResponse`**, built by `ProfileWrites` rather than
`ProfileReads`, and parsed by the client with the same `profileFromBody`. A second producer that
cannot know the answer would have to invent one. `ProfileResponse`'s own KDoc closes the escape
hatch: `Application.module()` installs `Json` with `encodeDefaults = false`, so a defaulted property
is **omitted from the wire** for every caller whose answer is the default — which is why
`displayNameRemoved`, `deviceRouteLive` and `hasRecoveryEmail` all carry no default. A field that
cannot have a default, and has two producers one of which cannot answer it, does not belong on that
DTO.

**Measured, so none of this is inferred.**

- `player.device_id` **does not exist.** `V7__device_binding.sql` migrated every row into
  `device_binding`, then ran `ALTER TABLE player DROP CONSTRAINT player_device_id_unique` and
  `ALTER TABLE player DROP COLUMN device_id` (`ADR-0049` §1, §7). The question *what happens to
  `player.device_id`* has no column to happen to; ownership is a `device_binding (device_id,
  player_id)` row with `revoked_at IS NULL`.
- `PostgresPlayerDirectory` resolves a device with
  `SELECT player_id FROM device_binding WHERE device_id = ? AND revoked_at IS NULL`, so a revoked
  binding resolves to nobody.
- `DuelSocket` already mints on `Identity.Anonymous`: `deps.deviceIds.newDeviceId()` then
  `deps.directory.resolve(...)`, answering a `Welcome` that carries the new id, which
  `connection.ts` writes to `pd.deviceId`. **The path that creates a new anonymous profile is
  shipped, tested, and needs no change.**
- The browser holds exactly four keys today — `pd.sessionToken`, `pd.deviceId`, `pd.roomCode` and
  `pd.accountOfferSettled` (which `ADR-0125` §1 deletes) — and
  `one-module-owns-each-storage-key.test.ts` asserts that exactly one production file contains each
  literal, so a new writer of `pd.deviceId` reddens that test unless it is `device-id.ts`.
- `sign-out.ts` today forgets the token and the room code and states in its own KDoc that *"the
  device id is never read, written or cleared here"*, citing `ADR-0030` §8 — the clause `ADR-0131`
  supersedes for this one case.
- `ProfileResponse` is **not** generated. `profile.ts` says so — *"Contracted in `docs/protocol.md`,
  not generated"* — so nothing in the `/api/me` family touches `protocol.gen.ts` or the protocol
  version.

### The deadline

**Response shapes in the `/api/me` family are free today and are not free later.** `profileFromBody`
requires every field it names and answers `null` — *unavailable* — for a body missing one, which is
the right strictness and also the reason a shape change is a breaking change. `EPIC-07` is *not
written*, `DEC-126` has chosen no deployment, and no browser anywhere caches a build of this client,
so adding a route or a field costs one compile error and nothing else. After the first deployment it
costs a compatibility window. This is a reason to decide **now**, not a reason to decide a particular
way.

**The second deadline is a collision, not a date.** `DEC-147` — the architect's, open, registered by
`ADR-0125` §4 — asks how the client learns that its profile carries **no credential**, and the
natural home for *that* answer is a seventh field on `GET /api/me`. The two answers touch
`docs/protocol.md`'s `/api/me` section and `web-client/src/profile/profile.ts`, so `STORY-1408`'s
anonymous-state ticket and `STORY-1410`'s implementing ticket conflict textually even though they
conflict in nothing else. They are serialised, not merged.

## Decision

**The server answers one boolean, on one route, from the two credentials the caller presented on
that same request; the browser, told `true` and only then, forgets `pd.deviceId` alongside the token
it already forgets; and the profile that replaces it is minted by the handshake that already ships.**

Everything below is that sentence applied.

### 1. Nothing is written, and there is no `player.device_id` to write to

The abandonment writes **nothing**. No `INSERT`, no `UPDATE`, no `DELETE` against `device_binding`,
`player`, `credential`, `duel` or `duel_result`, in either case, at any point on this path. The one
write a sign-out makes stays the shipped `DELETE FROM auth_session` of `ADR-0030` §3, which
`ADR-0131` leaves untouched.

The `device_binding` row the browser walks away from keeps `revoked_at IS NULL` and keeps naming the
account, exactly as `ADR-0131` §3 requires and exactly as its last bullet already told the reader it
would. `ADR-0049` §2's statement never runs, its finality trigger never fires, §3 is never entered,
and `ADR-0037`'s revoke remains the only thing that kills the route.

### 2. The browser stops owning a profile by forgetting one key, and only three keys move

`web-client/src/protocol/device-id.ts` — the one module that may contain the literal `"pd.deviceId"`
— gains `forgetDeviceId(storage: Storage): void`, beside the `readDeviceId` and `writeDeviceId` it
already owns.

`signOut` calls it **only** in the abandoning case, beside the `forgetSessionToken` and
`forgetRoomCode` it already calls, **before** `reload()`, and **regardless of what
`POST /api/auth/sign-out` answered** — a rejected `fetch` and a non-`204` run the same local half a
`204` runs. That is not a new rule; it is the rule the token already follows, and the two halves must
follow one rule or a browser can end up signed out but still owning, or owning nothing but still
holding a token.

**Exactly three keys move, and no others.** `pd.sessionToken` and `pd.roomCode` as today;
`pd.deviceId` in the abandoning case alone. Everything else the browser holds survives a sign-out
untouched — today that is `pd.accountOfferSettled` while `ADR-0125` §1 has yet to delete it, and
tomorrow it is whatever key `ADR-0119` §5's *skipped* bit lands under, which `ADR-0131` §6 rules is
**not** spent by a new profile. A key added later is kept unless an ADR says otherwise; this
mechanism enumerates what it forgets and never sweeps.

`ADR-0030` §8's write-once rule governs every other moment in the life of the browser, exactly as
`ADR-0131` says it still does. `forgetDeviceId` is reachable from `signOut` and from nowhere else,
and `ADR-0027` §5's stale-client harm stays forbidden: no reload, no failed handshake, no refused
session, no `Welcome` carrying a null `deviceId` and no error path may reach it.

### 3. The new profile comes from the handshake that already ships, and is created there

`signOut` already ends in `reload()`. After the reload the browser holds no token and, in the
abandoning case, no device id, so `openConnection` sends
`Hello { deviceId: null, sessionToken: null }` and `IdentityResolver` answers `Identity.Anonymous` —
`ADR-0027`'s third path. `DuelSocket` then does what it already does: mints via `DeviceIdSource`,
resolves a profile through `PlayerDirectory`, and answers a `Welcome` carrying the new id, which
`connection.ts` writes to `pd.deviceId` under the same write-once rule.

**No server code changes for this.** The empty profile is created at that handshake and not at the
press, which has one consequence worth stating because it is better than the estimate `ADR-0131`
made: a player who signs out and closes the tab creates **no rows at all**. The accumulation
`ADR-0131` §Consequences names is one `player` row and one `device_binding` row per sign-out **that
reconnects**, not per press.

### 4. The case is one boolean, on one new route

**`GET /api/me/device`**, installed beside the `DELETE /api/me/device` that already lives there.

- **Authentication:** `Authorization: Bearer <token>`, and `X-Device-Id` alongside it when the
  browser holds one. Identity is resolved through `IdentityResolver` exactly as
  `DELETE /api/me/device` resolves it, and **a session is required**: `Identity.Device`,
  `Identity.UnknownDevice`, `Identity.Refused` and `Identity.Anonymous` all answer
  `401 Unauthorized` with an empty body, on `ADR-0049` §5's reasoning — a caller with no session has
  no sign-out to make, so there is no question to answer for them.
- **No credential guard, and none is needed.** `DELETE` refuses `409` for a player who holds no
  password, so that revoking cannot strand a profile. `GET` needs no equivalent: a session is issued
  by `POST /api/auth/sign-in` and by nothing else, so **a session-identified caller holds a password
  credential by construction**. `ADR-0131` §2's invariant — nothing is ever abandoned that cannot be
  signed back into — is therefore a property of this route's identity rule rather than a rule anyone
  has to remember.
- **Body:** `200 OK` and a new DTO in `ProfileDtos.kt` with exactly one field and no default:

  ```kotlin
  @Serializable
  public data class DeviceStandingResponse(val signOutHandsANewProfile: Boolean)
  ```

- **The predicate.** `signOutHandsANewProfile` is `true` **exactly when the `X-Device-Id` presented
  on this same request does not resolve, through a live `device_binding`, to a player other than the
  caller.** It is `false` only when it does.

  The comparison is `IdentityResolver`'s, which already holds the `PlayerDirectory` and is already
  *"the one place the session-versus-device precedence rule is written"*. It gains one function:

  ```kotlin
  public suspend fun namesPlayer(deviceId: DeviceId, playerId: PlayerId): Boolean
  ```

  implemented as `players.findOrNull(deviceId)?.id == playerId`, reusing the shipped
  `revoked_at IS NULL` lookup. **`resolve` and all five `Identity` cases are byte-unchanged**, so
  `ADR-0027` §4's precedence is untouched, the socket gains no query, and `Identity.Session` keeps
  the guarantee its KDoc makes.

- **It compares two credentials the caller already holds, and can therefore tell them nothing.** A
  caller holding a device id can already read the player it names — `GET /api/me` with `X-Device-Id`
  and no `Authorization` answers that profile, `playerId` included — and a caller holding a token can
  already read its own. This route computes a comparison the same caller could already make in two
  requests; it is not an enumeration surface, and it must be computed **only** from credentials
  presented on that one request, never from a stored association, or it would become one.
- **A client cannot weaponise it.** `true` requires that no *other* player stands behind the
  presented device id, so a caller presenting a device id that is not theirs can only move their own
  answer from `true` to `false` — from abandoning to keeping. There is no input that makes another
  player's browser abandon anything, and the only act the answer authorises is the caller's own
  browser deleting the caller's own storage.

### 5. The five states, and what the predicate makes unreachable

| The caller | The presented `X-Device-Id` resolves, live, to | `signOutHandsANewProfile` | `ADR-0131` §1 |
| --- | --- | --- | --- |
| a session | **the caller** | `true` | row one — a new, empty profile |
| a session | **another player** | `false` | row two — back to the profile it owns |
| a session | nothing, because none was presented | `true` | row three — a first-time visitor |
| a session | nothing, because the binding is revoked | `true` | row three in substance: the browser owns no profile |
| no session | — | `401`, no body | there is no sign-out to make |

The predicate is written as a negative on purpose. **`true` is unreachable while any live binding
names another player**, so the one outcome `ADR-0131` §2 forbids — a browser abandoning an anonymous,
credential-less profile it owns — is not merely avoided by the copy or by the client's care; it
cannot be expressed. That is the property worth having, and it is why the two framings that read more
naturally (*"the device names me"*, *"I am signed in as this device's player"*) were not taken: both
answer `false` for the revoked and no-device rows, and both would then tell a browser that it returns
to a profile it does not have.

The revoked row deserves its own sentence, because it is the only place this ADR changes an outcome
`ADR-0131` did not discuss. A browser whose binding was revoked and which then signs out **already**
gets a fresh profile today, silently, because `Identity.UnknownDevice` sends the dead id to
`PlayerDirectory.resolve` and `ADR-0049` §3 permits a revoked device to bind again to a profile that
does not yet exist — so the shipped `SIGN_OUT_WARNING` is already false for that browser. Answering
`true` there makes the sentence true, and forgetting the dead string means the next handshake mints
cleanly instead of binding a new profile to a revoked id. Nothing is taken away: a revoked id can
never name the old profile again.

### 6. Everything unknown is a keep

**A client that has not been told reads `false`.** An absent field, a `401`, an *unavailable* read, a
rejected `fetch`, a read that has not returned yet, and a client too old to ask all mean the same
thing: the browser keeps `pd.deviceId` and the confirmation states the returning sentence.

That is the failure direction §Context demands. Wrong-and-safe is a browser that stays inside its own
account after pressing *Sign out* — the shipped defect `ADR-0131` exists to fix, visible to the
player, repairable by pressing again. Wrong-and-unsafe is a browser that abandons a profile with no
password. This mechanism has no state that produces the second.

One trap, named because it produces exactly the wrong answer quietly: **this read must not go through
`readFromApi`.** That helper returns `no-profile` *without making a request* when the browser holds no
device id — which is precisely the browser whose answer is `true`. The client module for this route
sends `X-Device-Id` when one is held, omits the header when one is not, and lets the server answer.

The fact is read when the account screen reads its profile, not at the press. The window between them
cannot change the answer in the dangerous direction: nothing another device can do turns a `false`
into a `true`, because only this browser's own sign-in or sign-out changes which player its session
names, and both reload the page.

### 7. What the confirmation is given, and what this ADR refuses to write

`SignOutControl` gains the boolean as a prop and renders the sentence that applies. It still returns
`null` unless `props.signedIn`: **`signedIn` decides whether a sign-out exists at all, and never
which of the two it is** — which is what `ADR-0131` §7 forbids inferring from it.

`signOut(...)` gains the same boolean as a **required** field of its request object, not an optional
one with a safe default. `false` is the safe value, but an omitted argument is a feature that
silently does not exist, and this is a feature whose absence is invisible from the outside.

`main.tsx` wires the value from the same account read that already feeds the screen, and passes
`false` whenever that read has not answered.

**This ADR mints no string.** The words on the confirmation are the card's, inside `ADR-0131` §5's
two obligations and three refusals, and `SIGN_OUT_WARNING`'s second sentence moves in the same diff
as the behaviour because `ADR-0131` §Consequences says it must.

### 8. What does not move

Stated plainly because `STORY-1410`'s ticket shape depends on it: **nothing crosses the socket.**
`Hello` and `Welcome` are unchanged, `ProtocolCodec` is unchanged, `protocol.gen.ts` is not
regenerated, `PROTOCOL_VERSION` does not move, `ADR-0047`'s ledger gains no row, and not one of the
artifacts `ADR-0068` §5 computes for a protocol bump is touched. The implementing ticket is a plain
ticket under the
`files_touched: 1..3` cap, split as the planner sees fit; it is **not** `atomic:`, and a ticket that
claims to be would be claiming a gate that does not apply to it.

Nor does any schema move: no migration, no column, no index, no constraint.

## Consequences

**What it buys.** `ADR-0131`'s behaviour becomes implementable in one plain ticket chain with no wire
break, no migration, and no irreversible act anywhere in it. The confirmation can state which of the
two sign-outs it is about to perform, from a fact the server computed, without inferring anything
from `signedIn` or from the presence of a token. The one harm `ADR-0131` §2 forbids becomes
unrepresentable rather than merely avoided. And a browser whose device binding was revoked stops
being lied to by a sentence that was already false for it.

**What it costs.**

- **The abandonment is not durable, and this ADR does not pretend otherwise.** Because §3 forbids
  every server write, the only thing that changes is what one browser remembers. A player who backed
  up `localStorage` before pressing, or who reads the value out of the console first, keeps the
  device route into the account — which `ADR-0131` §3's last bullet already stated as a property of
  the decision rather than of the mechanism. Nothing in the product tells a player this, no screen
  offers it, and **this ADR adds no undo, no grace window and no "sign back in as you were" path**:
  `ADR-0131`'s named cost — *a forgotten password now loses the account in one press that doing
  nothing used to undo* — is exactly as sharp after this mechanism as before it, and nothing here
  makes it look recoverable.
- **The account screen makes a second request.** One more round trip, one more thing that can be
  unavailable, one more `401` path to get right. A field on `GET /api/me` would have cost none of
  that; §Alternatives 1 says why it was not available.
- **A new route is new public surface.** `GET /api/me/device` needs its `docs/protocol.md` row, its
  identity tests and its own client module, and it exists to carry one bit.
- **A fact read on one screen is spent at a later press.** The window is analysed in §6 and fails
  safe, but it is a window, and a future affordance offering sign-out from somewhere the account read
  does not reach would silently take the keeping branch. That is the safe branch, and it is still the
  wrong one.
- **`GET /api/me/device` is a second place identity is resolved without the shared helper.** It joins
  `DELETE /api/me/device` in needing the `Identity` case rather than just the player, so
  `resolvedPlayerOrNull` now covers three route files rather than four, and a sixth `Identity` case
  must be handled in one more exhaustive `when`.
- **`IdentityResolver` grows a second question.** It stays the only place a device id is turned into
  a player, which is the point, but a class whose whole KDoc is about precedence now also answers a
  comparison, and a careless later reader could mistake `namesPlayer` for a second resolution path.
- **Two tickets are serialised that would otherwise be parallel.** `DEC-147`'s answer and this one
  both edit `docs/protocol.md`'s `/api/me` section and the client's profile module.

**What it forecloses.** A sign-out that abandons a profile **without the server having said so** —
after this, the client's forgetting is licensed by a server answer and by nothing else, and no later
change may reintroduce *clear it and see*. A `PROTOCOL_VERSION` bump for this feature: the socket is
untouched, so any later shape that needs the wire is a new decision and not an extension of this one.
And a `ProfileResponse` that carries request-scoped facts — the line this ADR draws is that
`ProfileResponse` describes the **player**, and a fact about the pair of credentials on one request
gets its own surface.

**What it does not foreclose.** Folding `GET /api/me/device` into a single account-facts read once
`DEC-147` has chosen its own shape — a tidying, cheap in both directions, and deliberately left for
after both have landed. Widening `ADR-0131` §1 to every sign-out if the human answers its open
sentence: that is **one expression on the server** — the predicate becomes unconditionally `true` for
a session-identified caller — with no client change and no wire change at all, which is the main
reason the boolean names the outcome rather than the underlying comparison. And `ADR-0037`'s revoke,
untouched and still the only thing that kills the route.

### The reversal trigger

Reverse this if a player is ever handed a new profile in `ADR-0131` §1's **second** row — a browser
that owned an anonymous, credential-less profile and signed into somebody else's account. §5 makes
that unreachable through the predicate, so a single sighting means the fact is being derived
somewhere other than this route, and the repair is to find that derivation rather than to change the
predicate.

## Alternatives considered

**1. A seventh field on `GET /api/me`, beside `deviceRouteLive`.** The strongest case by a distance,
and it was the intended answer until the DTO was read. No new route, no second round trip, no new
client module; the account screen already sends both credentials on that request, so the server
already has everything it needs; `deviceRouteLive` is the same family of derived boolean and sits
right there; and `DEC-147`'s answer will probably live in the same place, so one screen would read
one body. Rejected on a fact rather than a preference: **`PUT /api/me/name` answers the same
`ProfileResponse`**, built by `ProfileWrites.setDisplayName` from a producer that never sees the
request's credentials, and parsed by the client with the same `profileFromBody`.
`encodeDefaults = false` — stated three times in `ProfileDtos.kt`'s own KDoc — forbids giving the
field a default, because a defaulted property is omitted from the wire for exactly the callers whose
answer is the default. So one of the two producers would have to write a constant `false` that one
route then overwrote: two producers, one truth, and a name-change response that quietly disagrees
with the profile response about which sign-out this is. Threading the presented device id into
`ProfileReads.profileOf` instead — a second bound `?` in `PROFILE_OF_SQL` — was the other way to save
it, and it makes a player-read port request-aware against that statement's own written rule that its
booleans are *"correlated to `p.id`, never to a second `?`"*.

**2. Two client reads, and the client compares the two `playerId`s.** The cheapest answer available,
and it needs no server change whatsoever: `GET /api/me` with the token gives the session's player,
`GET /api/me` with `X-Device-Id` and **no** `Authorization` gives the device's player, and they are
equal exactly in row one. Every part of it ships today. Rejected because its failure mode is silent
and points the wrong way. The probe must be sent *without* the bearer header, in a client whose
`authorizedFetch` exists to attach that header and whose `main.tsx` already carries a comment about
how easy the mistake is in the other direction. A future refactor that routes the probe through
`apiFetch` makes it return the **session's** profile, the two ids then match for every caller, and
every browser takes the abandoning branch — including row two, which is precisely the profile
`ADR-0131` §2 forbids abandoning. A mechanism whose one plausible regression is the one forbidden
outcome is not worth the round trip it saves. It also puts the rule in the client, which §7 says is
the server's.

**3. The sign-out response carries a new device id** — `ADR-0131` §7's second named shape. It is a
genuinely good shape: one round trip instead of two, no separate fact to go stale between a screen
load and a press, the server decides and the client obeys, and the browser is never in a state
holding no device id at all. Rejected because minting a device id means creating one — `resolve`
writes a `player` row and a `device_binding` row — and §3 forbids writes on this path. It would also
move profile creation from the handshake, where `ADR-0012` puts it and where the mint-and-resolve is
tested, to an authentication endpoint; it would create a row for every browser that signs out and
never comes back, which the shape in §3 above avoids; and it would give `POST /api/auth/sign-out` a
body where `ADR-0030` §3 fixes a uniform `204` answered whether or not a session existed. Three
shipped rules bent to save one round trip.

**4. The server does something the client only obeys** — §7's third shape, in its strongest form: the
sign-out request marks the binding as no longer this browser's, and the client is merely told. It is
the direction this repository normally prefers, it makes the abandonment a fact of the database
rather than a fact about what a client remembers, and it would close the *restore your storage and
you are back in* gap §Consequences names. Rejected because every expression of it is a write, and §3
forbids writes; the only write already available is `ADR-0049`'s revoke, which `ADR-0131`
§Alternatives 3 rejected in the product's own terms and which §7 puts out of reach. A new column or a
new state on `device_binding` meaning *left, but not revoked* is a schema change in service of a
distinction no shipped read makes, and it would need its own ADR and its own answer to what
`ADR-0049` §3's finality means for it.

**5. `Identity.Session` carries the bit, so the comparison is structural.** Strongest case: the
answer would arrive with identity, in one call, in the class that already owns every device lookup,
and no route could forget to ask. Rejected because `IdentityResolver.resolve` runs on **every socket
handshake**, so every connection in the product would pay a `device_binding` lookup for a fact one
HTTP route needs once per account screen; and because it would delete the guarantee
`Identity.Session`'s KDoc makes — *"a session that wins is not merely unaffected by the device beside
it, it never reads one"* — which is `ADR-0027` §4 made unarguable rather than merely documented. A
separate function on the same class buys the single-owner property at none of that price.

**6. The browser forgets `pd.deviceId` on every sign-out, and nobody has to be told anything.** No
route, no field, no round trip, no staleness, no `401` path, no client module — and it is the
plainest reading of *"the browser is handed a new profile"*. Rejected outright: it is `ADR-0131` §1
row two abandoning an anonymous profile with no credential and no warning, which §2 forbids in as
many words and which `EPIC-14` filed `DEC-132` against in the first place. This is the shape that
makes `DEC-152` a decision rather than a chore.

**7. A window in which the sign-out can be undone**, by keeping the old device id under a second key
for a while. It would pay off `ADR-0131`'s sharpest named cost — the forgotten password that one
press now loses. Rejected because `ADR-0131` §4 licenses one abandonment at one confirmed press and
nothing else; because an undo affordance would have to be described on a screen, and §5 refuses to
turn the confirmation into a warning about loss; and because a second copy of a bearer credential
living in storage under a key nothing owns is a worse thing than the risk it hedges. It is also
exactly the shape §Consequences forbids: it would make the loss look recoverable while resting on
storage the product never promises to keep.

## What this does not settle

- **The words on the confirmation**, and which sentence carries which of `ADR-0131` §5's two
  obligations — the card's, and `STORY-1410`'s copy waits on nothing.
- **`DEC-147`** — how the client learns its profile carries no credential. Adjacent, unanswered here,
  and the reason `STORY-1408` and `STORY-1410` are serialised.
- **Whether `ADR-0131` §1's second row survives**, which is the human's one sentence and `ADR-0131`'s
  own open question. If it is deleted, this mechanism narrows to one server expression.
- **Whether `GET /api/me/device` and `GET /api/me` should later be one read.** A tidying, cheap in
  both directions, deliberately deferred until `DEC-147` has chosen its shape.
- **What collects the empty profiles.** `ADR-0039` ships no deletion and `ADR-0131` names the
  accumulation as a cost; §3 here only makes the count smaller than that ADR estimated.
