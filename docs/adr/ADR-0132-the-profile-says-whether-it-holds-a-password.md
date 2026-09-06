# ADR-0132 — The profile says whether it holds a password, and the client derives nothing

- **Status:** Accepted
- **Date:** 2026-09-07
- **Resolves:** `DEC-147` — by what means does the client learn that the profile it holds carries
  **no credential**? Registered 2026-09-06 by
  [`ADR-0125`](ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md) §4,
  which rules that the account screen may name a profile *anonymous* only once it has been told,
  and never from the absence of a session token.
- **Serves, and reopens nothing in:** `ADR-0125` §3 (what the block says — the words, heading,
  order and layout stay the design card's) and §4 (when it may be said). §4's three cases are the
  requirement this mechanism is measured against, not something this ADR restates in its own words.
- **Applies:** [`ADR-0027`](ADR-0027-the-session-outranks-the-device-id.md) §4 (a session outranks
  a device id, and a route acts on the resolved player) and §6 (an answer that tells a stranger
  something is an oracle, whatever its status code);
  [`ADR-0030`](ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) §1 (a claim is one
  credential `INSERT`) and §4 (`ProfileReads` is `PlayerId`-keyed and gains no device-keyed
  overload); [`ADR-0031`](ADR-0031-an-optional-verified-recovery-email.md) §6.3 and §7, whose
  `hasRecoveryEmail` is the shape this field copies deliberately;
  [`ADR-0041`](ADR-0041-a-handle-and-a-password-are-the-only-credential.md) (what a credential is
  in v0.1); [`ADR-0002`](ADR-0002-server-authoritative.md) (the client asserts no fact of its own);
  [`ADR-0060`](ADR-0060-the-record-is-its-own-screen-and-the-lobby-is-the-door.md) §4 (a screen is
  prop-driven and knows nothing about fetching or navigation).
- **Contradicts nothing in
  [`ADR-0050`](ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md) §4** — see §Context's
  fourth force. That section is titled *What changes where*: its *"no `ProfileResponse` field"* is
  an inventory of `ADR-0050`'s own diff, not a standing embargo, and its *"`deviceRouteLive` is the
  whole of what the screen reads"* is scoped to the sentence naming which **routes** are live.
  `hasRecoveryEmail` landed on 2026-08-25, eight days after `ADR-0050`, under `ADR-0031` §6.3's own
  instruction, and the account screen has read two `ProfileResponse` fields ever since. No clause of
  `ADR-0050` is amended or superseded here, and `deviceRouteLine` stays the only place
  `deviceRouteLive` becomes words.
- **HTTP, not the socket. `PROTOCOL_VERSION` does not move**, `docs/protocol-versions.md` gains no
  row, and no generated file changes — §7.
- **No migration.** `credential` has existed since `V4`; this reads it.
- **Constrains:** `STORY-1408`'s anonymous-state ticket, which this unblocks. It constrains no other
  screen and no other story.

## Context

### What ships today, measured on `develop` at `0e83537c`

**The client is never told this fact, and it has one substitute for it.** `GET /api/me` answers six
fields — `playerId`, `coinBalance`, `displayName`, `displayNameRemoved`, `deviceRouteLive`,
`hasRecoveryEmail` — and none of them is *does this profile hold a password*. The substitute is
`signedIn`, and `AccountScreen.tsx` writes its own derivation down beside the use:

> `POST /api/auth/sign-in` is the only endpoint in `docs/protocol.md` that ever returns a
> `sessionToken` … so a browser holding a live session is a browser whose player has a password, by
> construction. That derivation is why no `hasCredential` field was asked for: `signedIn` alone
> carries it.

The derivation is sound in the direction it is written — a token implies a password — and it is the
**converse** the anonymous block needs. `signedIn == false` is a false positive for anonymity in two
states a player reaches:

- **A `201` sign-up whose follow-up sign-in failed.** `sign-up.ts` signs in right after the `201`
  and answers `signed-up` **whether or not that follow-up succeeded**, deliberately: *"reporting
  anything else would tell the player their claim failed when it did not."* So the browser can hold
  a password and no token, while `SignUpForm` prints `This profile now has a password.`
- **A player who signed out in this browser.** `sign-out.ts` drops the token; the profile the device
  id then resolves to may be one that holds a password.

`TASK-120601` filed this exact converse as a defect on this screen on 2026-08-29 and repaired it by
making the browser *hold a token*, not by telling the client the fact. That repair does not reach
either state above.

**The server knows the fact and reads it three times already.** `Credentials.holdsCredential(player,
kind)` backs `POST /api/auth/sign-up`'s `409` and `DELETE /api/me/device`'s `409`, over
`SELECT EXISTS (SELECT 1 FROM credential WHERE player_id = ? AND kind = ?)`. It has simply never
crossed to a client.

### The first force: the product needs on a screen a fact four ADRs work to contain

*Who holds no password* is a target list. The posture against publishing it is written down in more
than one place, including in the code: `DeviceRoutes.kt` puts its `409` guard **after** identity
because *"checked first, an unauthenticated caller would learn from a 409 that some profile holds no
credential."* `ADR-0027` §6 burns a dummy Argon2 hash so that an unknown identifier and a wrong
secret cost the same time. `ADR-0031` §6.3 lets the client say *recovery is on* and never what the
address is.

Nothing in that posture is about the player learning a fact about **themselves**. It is about a
*stranger* learning it about **someone else**. `GET /api/me` resolves identity before it answers
anything and returns `401` otherwise, and `ProfileReads.profileOf` takes a `PlayerId` that only
`IdentityResolver` produces. So the two are reconcilable — but only if the reconciliation is written
down as a rule, because the next feature that wants this boolean will want it in a list.

### The second force: a fact told once at boot goes stale, and one transition is why

`ProfileProvider` reads `GET /api/me` **once**, in one effect, and holds the answer for the tab's
life. Of the three transitions that change what that answer would be:

- **sign-in** calls `reload`, which rebuilds the app and re-reads;
- **sign-out** calls `reload`, likewise;
- **sign-up does not.** `sign-up.ts` hands its follow-up sign-in a `noReload`, on purpose: *"forcing
  a navigation from inside this call would be a product decision `signUp` does not own."*

So sign-up is the one transition that flips the answer with nothing re-asking for it — and it is the
first of `ADR-0125` §4's two named states. **A field on its own reproduces the defect in a new
field**: the block would print *anonymous* beside `This profile now has a password.` A mechanism that
does that has not answered `DEC-147`; it has renamed the falsehood.

### The third force: `ProfileResponse` is shaped in two places, and one of them lies

`PUT /api/me/name`'s `200` carries a `ProfileResponse` built by `PostgresProfileWrites.toProfile()`,
and `ProfileProvider.reportNameWrite` **adopts that body wholesale** as the profile it holds. That
builder computes `deviceRouteLive` with the same correlated `EXISTS` the read path uses, and passes a
literal `false` for two fields. One of them is false by construction. The other is not, and the
comment above it says so: *"a player with a verified address who renames still reads false on this
particular response."*

A seventh field that took a literal there would make **setting a display name** turn a claimed
player's screen anonymous. Whatever this decision does, it has to hold on every response that carries
the DTO, not only on the one the question was asked about.

### The fourth force: `ADR-0050` §4 appears to forbid the obvious answer, and does not

It reads *"no `ProfileResponse` field"* and *"`deviceRouteLive` is the whole of what the screen
reads."* Both sentences sit under the heading **What changes where — and a correction to
`ADR-0049`**: they inventory `ADR-0050`'s diff, and they scope the *route sentences* to one field.
The record settles it. `ADR-0031` §6.3 had already instructed that *"`ProfileResponse` gains
`hasRecoveryEmail: Boolean` and nothing more"*; that field landed on 2026-08-25, eight days after
`ADR-0050`; and `AccountScreen` has read two `ProfileResponse` fields ever since, through
`recoveryLine`. Reading §4 as an embargo would make a merged, shipped field a violation. It is not
one.

### The deadline, honestly

`ProfileResponse` is contracted by hand in `docs/protocol.md` and declared by hand in `profile.ts`
(*"Contracted in `docs/protocol.md`, not generated"*), it has exactly one consumer, and
`profileFromBody` refuses a body missing any field. A **required** seventh field is therefore free
today, because server and client land in one PR. It stops being free the first time anything else
reads `GET /api/me` — then the only option left is a defaulted field, and a defaulted field is
precisely what §4 of this decision refuses. That is a reason to decide now, not a reason to decide a
particular way.

## Decision

### 1. `ProfileResponse` gains a seventh field: `hasPassword: Boolean`

`true` exactly when a `credential` row exists for this player with `kind = CredentialKind.PASSWORD`;
`false` otherwise. **No default value**, for the `encodeDefaults` reason `displayNameRemoved`,
`deviceRouteLive` and `hasRecoveryEmail` each already state on their KDoc. Declared last.

**It is named for the kind, not for the concept.** `ADR-0027` §1 keeps `credential` deliberately
open — *"a password, a passphrase against a handle, or a third-party subject"* — so a
`hasCredential` would one day answer `true` for a player who holds no password, on a screen whose
only stated way out is the password form. `hasPassword` is narrow, true, and freezes nothing about
`DEC-027` (*may one player hold several credentials?*). If a second kind ever ships it gets its own
answer; this field does not widen to cover it.

**It is positive, and the product's word stays off the wire.** *Anonymous* is a state a screen names
(`ADR-0125` §6); the wire says what the row says. A negative field would also invert badly under any
future default.

### 2. It is computed on every response that carries a `ProfileResponse`, and is never a literal

- A fourth correlated `EXISTS` in `PostgresProfileReads.PROFILE_OF_SQL`, correlated to `p.id` and
  never to a second player parameter — the rule the three existing correlations already state.
- **The same `EXISTS` in both statements in `PostgresProfileWrites`**, so `PUT /api/me/name`'s `200`
  carries the true value and `reportNameWrite`'s adoption can never turn a claimed player anonymous.
  This field is explicitly **not** given `hasRecoveryEmail`'s literal treatment there.
- **The kind is bound as a parameter from `CredentialKind.PASSWORD`, never spelled as a SQL
  literal.** A renamed constant would otherwise leave a statement that compiles, runs, and answers
  `false` for every player in the product. Binding a kind does not weaken the *"never a second
  `?`"* rule in `PROFILE_OF_SQL`'s comment: that rule is about the **player id**, and a bound kind
  cannot make the boolean describe a different player than the row it rides on.
- **No new port and no new collaborator.** `ProfileRoutes` does not gain `Credentials`, and
  `ProfileReads` keeps its two functions. One statement, one round trip, one place the answer comes
  from — a second read is a second answer that can disagree with the first.

### 3. It is told only to the player it is about

- **`ProfileResponse` is the only type that carries it.** Not `DuelSummaryResponse`, not
  `StandingRow`, not `SelfStandingResponse`, and not any future row describing another player.
- **No route answers it for a player the caller did not resolve to.** No `playerId` parameter, no
  path segment, no admin variant.
- **Nothing selects, filters, sorts or counts on it.** A ladder narrowed to *anonymous only* would be
  an enumeration surface even though every row it returns is public.
- **It reaches no log line, no `ServerMessage`, and no `poker-engine` type.** No credential concept
  exists in the engine or crosses into it (`ADR-0027` §6's closing line), and nothing here changes
  that.

The two endpoints that carry it — `GET /api/me` and `PUT /api/me/name` — already resolve identity
before anything else and answer `401` with an empty body otherwise (`ADR-0027` §4), and
`ProfileReads` is `PlayerId`-keyed with no device-keyed overload (`ADR-0030` §4). The field inherits
those guarantees and adds no surface of its own.

### 4. The client reads it, and a missing field is *not told* rather than *false*

`PlayerProfile` gains `readonly hasPassword: boolean`. `profileFromBody` requires it to be a
`boolean` and answers `null` otherwise, exactly as it does for the other three — so a body without
the field makes the read `unavailable`, **never `false`**. That is the single line of code that keeps
`ADR-0125` §4's *not told at all* case reachable, and it is why the field carries no default on
either side of the wire.

`AccountScreen` renders `ADR-0125` §3's block when, and only when, the profile is in hand and the
field says so — `profile.kind === "profile" && !profile.profile.hasPassword` — and consults
`signedIn` for it never. Negating a boolean the server sent *is* the told fact; inferring from an
absent token is what §4 forbids, and after this decision the client has no absence left to infer
from.

### 5. Nothing that already ships is re-keyed

`showPasswordRoute` stays `signedIn && …` and `PASSWORD_ROUTE_LIVE` is unchanged — `ADR-0125` §4 says
so in as many words. `showSignUp`, `showSignInDoor`, `showAttach` and `SignOutControl` keep the
predicates they have. This decision adds one block's condition and moves no other, which is what
makes it cheap to reverse: deleting the block and the field leaves the screen exactly as it is today.

The written derivation quoted in §Context stays in the file for what it actually supports — the
password-route sentence — and stops being the source of the anonymity fact.

### 6. A profile the server has changed is re-read, never edited by the client

**The rule:** where the server confirms an outcome that changes the caller's own profile, the
provider re-reads `GET /api/me` and adopts the answer. The client never edits a field of the profile
it holds, and never composes a status code with a held profile to reach a fact.

**Today that is exactly one outcome that does not already re-read:** `signUp`'s `signed-up`. Sign-in
and sign-out both call `reload`, which rebuilds the read; sign-up passes `noReload` on purpose. The
re-read runs **after** `signUp` resolves — so the follow-up sign-in has already written its token or
failed to — never beside it. This ADR enumerates no other trigger, because no other exists.

**Where the wiring goes:** with the composition that already reads both the provider and the account
calls (`Lobby.tsx` today), so `AccountScreen` and `SignUpForm` stay prop-driven and hook-free
(`ADR-0060` §4) and remain renderable in a test alone.

Cost: one extra `GET /api/me`, at most once in a profile's lifetime.

### 7. This is HTTP, and `PROTOCOL_VERSION` does not move

`ProfileResponse` is a plain-HTTP DTO. It is contracted by hand in `docs/protocol.md` and declared by
hand in `web-client/src/profile/profile.ts`; it is **not** emitted from a serial descriptor, so
`ADR-0020`'s generator does not touch it and `protocol.gen.ts` does not change. `ADR-0071`'s
discriminator rule governs sealed wire hierarchies and has no subject here.

Therefore: **`PROTOCOL_VERSION` is unchanged**, `docs/protocol-versions.md` gains no row,
`ADR-0047`'s one-bumping-branch lock does not engage, and `ADR-0070`'s blast-radius probe is not
owed. **`STORY-1408`'s ticket is not `atomic:` on their account** — the same conclusion `ADR-0031` §7
recorded for `hasRecoveryEmail`: *"No `PROTOCOL_VERSION` change. These are plain-HTTP DTOs."*

The ticket is still one diff, for a different reason: the server field, the document row, the client
type, the parser and the screen must land together, or `profileFromBody` answers `null` and every
profile read in the product becomes `unavailable`.

### 8. Where it lands

- **Server:** `ProfileDtos.kt` (the field and its KDoc), `PostgresProfileReads.kt` (`PROFILE_OF_SQL`
  and the row read), `PostgresProfileWrites.kt` (both statements and `toProfile()`),
  `ProfileDtoFixtures.kt`, `ProfileReadsDoubles.kt`, and the route and database tests that build a
  `ProfileResponse`.
- **Document:** one row in `docs/protocol.md`'s *Profile endpoint* field table — required by
  `HttpEndpointDocumentationTest`, which reflects `ProfileResponse::memberProperties` against the
  documented field names and fails when the two disagree.
- **Client:** `profile.ts` (the interface and the parser), `profile-fixture.ts` (`aProfile` and
  `meBody`), `AccountScreen.tsx`, the provider's re-read and its wiring, and `account-server.ts`'s
  stub bodies.
- **No migration, no new endpoint, no new port, no engine change.**

## Consequences

**What it buys.** `ADR-0125` §3's block ships, and it ships true in both states where today's
derivation lies. `STORY-1408`'s anonymous-state ticket is unblocked; the story's other four parts
were never blocked. The account screen goes from two told facts to three and keeps its standing rule
— it states nothing it was not told — with one fewer exception to it. `DEC-090` (should *Attach a
recovery address* show on a profile with no password?) gains a fact to be answered against instead of
needing a mechanism of its own; it stays open, and stays the product owner's.

**What it costs.**

- **One more correlated `EXISTS` on the product's hottest authenticated read**, plus two more in the
  name-write statements. The same shape as the three already there, on the same reasoning, and paid
  on every `GET /api/me` whether or not any screen is going to use it.
- **Six or so files must move in one diff.** If the server ships the field and the client's parser
  does not require it, nothing breaks; if the client requires it and the server does not send it,
  every profile read becomes `unavailable` and the strip, the name surface and both route sentences
  vanish at once. That failure is loud rather than silent, which is the only reason this is
  acceptable.
- **One extra round trip per successful sign-up**, on a path that already makes two.
- **A fact the client did not have before.** Every per-caller fact is a fact some future feature can
  leak into a list. §3 is the rule against that, and **nothing enforces §3 structurally today**: no
  test asserts that `hasPassword` appears on no other DTO, and this ADR does not build one. The
  enumeration guarantee here is a reviewed rule, not a gate. Named so it cannot evaporate.

**What it forecloses.**

- **Deriving anonymity from `signedIn`, anywhere, ever.** The converse-of-`signedIn` mistake now has
  a named alternative, so a future recurrence is a defect rather than an oversight.
- **A one-boolean-for-every-kind field.** `hasCredential` is refused by name in §1; a second
  credential kind gets its own answer and its own decision.
- **Answering the fact per-player-by-request.** No `GET /api/players/{id}`, no admin listing and no
  ladder filter may carry it (§3).
- **A defaulted `ProfileResponse` field for this fact**, on either side — the default is what
  collapses *not told* into *told anonymous*.

**Residuals, named rather than left to be found.**

- **A defect found here and deliberately not fixed:** `PostgresProfileWrites.toProfile()` passes a
  literal `false` for `hasRecoveryEmail`, and `ProfileProvider.reportNameWrite` adopts that body
  wholesale. So a player with a verified recovery address who sets a display name reads
  `RECOVERY_OFF` — *"With no verified address, a forgotten password cannot be replaced and this
  account is lost."* — until the tab next boots. It is reachable (`NameSurface` calls
  `useReportNameWrite`), it is outside `DEC-147`, and it is **a ticket for the planner, not a
  `DEC`**: there is nothing in tension, the mechanism is §2's own, and both statements are already
  being edited by this decision's ticket. §2 is what keeps `hasPassword` off the same rake.
- **`signedIn && hasPassword == false` would put two sentences in contradiction, and is unreachable
  today.** Nothing in the product deletes a credential (`ADR-0039`; a reset changes the secret and
  keeps the row). The route that ever deletes one owns this, in its own ADR. Not a `DEC`, for the
  same reason as above.
- **The block's truth is only as fresh as the last read.** A second browser signing up for the same
  profile does not change this tab's held answer. That is true of every field on the profile today,
  it costs a page reload to correct, and no polling is introduced here.

## Alternatives considered

**1. Keep deriving from `signedIn`, and repair the two false states another way.** Its strongest case
is that it is free: no wire, no schema, no round trip, no new fact anywhere — and the same repair has
already worked once on this exact screen, when `TASK-120601` fixed the falsehood by making the
browser hold a token rather than by telling the client anything. Rejected because the repair does not
reach either state. A `201` whose follow-up sign-in failed *cannot* be made to hold a token — that is
what the state is — and `sign-up.ts` answers `signed-up` regardless, by a decision its own KDoc
defends. And `ADR-0125` §4 forbids the shape outright, independently of whether a particular hole can
be plugged: an absence is not a fact the client was told. Rejecting this is the whole content of
`DEC-147`.

**2. A dedicated endpoint — `GET /api/me/credential`, answering the one boolean.** Its strongest case
is real: `ProfileResponse` stays at six fields, the answer can be fetched at the moment the screen
needs it so it is never stale, and only the screen that wants the fact pays for it while the front
door's read stays untouched. Rejected on surface and on cost. It is a second authenticated route
answering a question about credentials — a second `401` path, a second thing that must be kept from
becoming an oracle, and a second place to get identity resolution right — for one boolean whose row
the caller is already fetching. The staleness it avoids is avoided by §6 with one re-read on one
outcome. If a fact here ever needs to be *pollable* rather than *held*, this is the shape to come
back to.

**3. Widen `deviceRouteLive` into a list of live routes — `routes: ["device", "password"]`.** This is
the truest model of the screen's actual subject: `ADR-0037` requires the account screen to state
*which routes sign in to this account*, and one field would replace two facts that are plainly the
same kind of fact, with no later question about how a third composes with them. Rejected on
reversibility and on shape. It re-shapes a shipped field that has a live consumer sentence pinned by
`ADR-0050` §3 and a single decision point (`deviceRouteLine`) built around its being a boolean; it
would force the anonymity block to key on a list's **emptiness** — an absence again, one layer up,
which is the failure mode `ADR-0125` §4 exists to close; and it is far more expensive to undo than a
boolean. If the routes ever want to be a list, this boolean is one of the two things it is built
from.

**4. Carry the fact on the socket, in `Welcome`.** Its strongest case: the client already holds
`Welcome` at boot without a second request, the socket is the channel on which the server is
authoritative by construction (`ADR-0002`), and a fact pushed at connect can be re-pushed when it
changes — which solves §Context's staleness force outright rather than with a re-read. Rejected on
layer and on price. The account screen is HTTP-driven and reachable with no socket at all, so the
fact would arrive on a channel the screen does not depend on. And it would move `PROTOCOL_VERSION` —
a ledger row under `ADR-0047`, a full blast-radius probe under `ADR-0070`, an `atomic:` ticket under
`ADR-0068` — for a fact that has nothing to do with a duel, while starting the drift away from
`ADR-0027` §6's closing rule that no credential concept crosses into the engine's wire.

**5. `POST /api/auth/sign-up` answers its `201` with the updated `ProfileResponse`, adopted exactly
as the name write's body is.** A genuinely good option: it removes the stale window with **no** extra
round trip, the adoption path already exists in `ProfileProvider`, and the sign-up handler already
holds `reads` and has already resolved the player. Rejected on blast radius and on posture. Every
sign-up response body is empty today, documented as such in `docs/protocol.md` and asserted by
`HttpEndpointDocumentationTest`; the auth routes are kept deliberately body-less; and it would make
the DTO's shaping sites three instead of two, each of which §2 then has to keep honest. A re-read is
a few lines of wiring in one place, and is reversed by deleting them. If the extra round trip ever
shows up in a measurement, this is the first thing to reach for.

**6. Ship the field with a default — `Boolean = false` on the DTO, tolerated as `false` by the
parser.** Its strongest case is compatibility: the change becomes non-breaking for any reader, an
older client keeps working, and it is what most wire formats do by habit. Rejected because it
destroys the one distinction this decision exists to preserve. A defaulted `false` makes *not told*
indistinguishable from *told anonymous* — `ADR-0125` §4's prohibition wearing a default value — and
it would print the block to a player whose server never answered. It is also why the three existing
booleans carry no default, each saying so on its own KDoc.

## What this does not settle

- **What the block says.** The words, the heading, the order and the layout are `ADR-0125` §3's and
  the design card's. Nothing here adds a string.
- **`DEC-090`** — whether *Attach a recovery address* appears on a profile with no password. It now
  has a fact to be answered against; it stays open, and stays the product owner's.
- **`DEC-027`** — whether one player may hold several credentials. §1's name is chosen so that this
  field answers it in neither direction.
- **Whether a second credential kind ever ships**, and what it would be called on the wire.
  `ADR-0041` holds, and the credential's shape is the human's.
