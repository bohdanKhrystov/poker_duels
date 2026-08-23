# ADR-0071 — A discriminator is its Kotlin type name, and the name moves rather than the gate

- **Status:** Accepted
- **Date:** 2026-08-23
- **Resolves:** `DEC-066`
- **Amends** [`ADR-0028`](ADR-0028-the-wire-names-an-absent-opponent.md) §1 — the second of its two
  new `ServerMessage` subtypes is named **`ActedForAbsent`**, Kotlin type and `@SerialName`
  together. Its four fields, both its `require` blocks, its recipients and every emission point in
  §5 stand exactly as written; `SeatPresence`, `OpponentPresence` and the rest of `ADR-0028` are
  untouched. Wherever `ActedForAbsentSeat` appears in `ADR-0028`, `ADR-0044`, `ADR-0045` or
  `ADR-0046`, read `ActedForAbsent`.
- **Constrains:** [`TASK-021402`](../../tasks/tasks/TASK-021402-the-wire-names-presence-and-the-version-takes-its-step.md)
  and the rest of `STORY-0214`, `STORY-0313`, `STORY-0405`, and every `ClientMessage` or
  `ServerMessage` subtype added after this.

## Context

Two merged decisions disagree about one string, and the disagreement was found by **running** the
gates rather than reading them — the [`ADR-0070`](ADR-0070-a-blast-radius-is-complete-only-when-the-gates-are-green.md)
§2 probe, during `STORY-0214`'s split:

```
org.opentest4j.AssertionFailedError: Discriminator longer than 16 characters: [ActedForAbsentSeat]
```

`ADR-0028` §1 writes `@SerialName("ActedForAbsentSeat")` verbatim — **eighteen** characters.
`ProtocolDiscriminatorTest.everyDiscriminatorIsShortAndUnique` has failed the build on any
discriminator over **sixteen** since `TASK-020210` merged. `OpponentPresence`, the story's other
new type, is exactly sixteen and passes; the fourteen names already on the wire are all fourteen
characters or shorter, `RematchOffered` longest. Four readings of this story's blast radius — one of them the
reading that produced `ADR-0028` itself — never surfaced the collision.

One of the two gives. Which one is not obvious, and five things bear on it.

### The limit has no stated reason, and this ADR does not invent one

Looked for, not assumed. `TASK-020210`'s *Tests* table says only *"each is at most 16 characters"*.
`TASK-020718`, which later edited that same class, restates it as *"discriminators stay short"*. The
test's own KDoc explains `noDiscriminatorIsAFullyQualifiedClassName` at length — a defaulted serial
name *is* the fully-qualified class name — and says nothing whatever about length. No ADR mentions
it. `TASK-020213`'s frame limits are measured in kilobytes and nesting depth, so nothing on the wire
is sized by it.

**Sixteen is a round number somebody typed.** Nothing below turns on it being the right one.

### It is a style gate, and the wire already exceeds it one level down

What catches an accidentally-defaulted discriminator is the fully-qualified-name check, because a
defaulted name always contains a `.`. The length check catches nothing that check does not. What it
enforces, and all it enforces, is brevity — mechanically, which is the only way a naming rule is
ever enforced at all.

It enforces it on fourteen names. The same JSON frame carries the engine's discriminators nested
inside `Events`: `BettingRoundEnded` is seventeen and `UncalledBetReturned` is nineteen, both
merged, both on the wire today, both outside this gate's reach. **Sixteen is therefore not a
property of the wire.** It is a house rule on the envelope.

That cuts both ways and the other way is the stronger one: the envelope's names are the set every
client switches on — `frames.ts`'s `satisfies Record<ServerMessage["type"], true>` table and the
exported `SERVER_MESSAGE_TYPES` — while the engine's are read one level down by whatever handles a
`GameEvent`. A tighter rule on the outer vocabulary is coherent even when the inner one is longer.

### Nothing in this protocol has ever used the freedom `@SerialName` grants

All fourteen `ClientMessage`/`ServerMessage` subtypes carry a `@SerialName` whose string is their
Kotlin simple name, character for character. So does every `@Serializable` type in `poker-engine`. A
reader who sees `"type":"RoomJoined"` in a frame can grep `RoomJoined` and land on the declaration,
and that has been true of every frame this project has ever sent. The invariant is total, free, and
written down nowhere.

### Shortening only the `@SerialName` would not stay in Kotlin

`typeNameOf` takes the last dotted segment of `descriptor.serialName`, so the generated TypeScript
interface **is** the serial name:

```ts
export interface RematchOffered {
  type: "RematchOffered";
```

A `@SerialName` shortened away from its type therefore renames the *client's* type as well, along
with `frames.ts`'s table key, `docs/protocol.md`'s row and the version ledger's fingerprint. The long
Kotlin name would survive in exactly one place — the server's own sources — which is not where
anybody starts when they are looking at a frame.

### The invariant cannot be gated, cheaply

A serial descriptor carries serial names and nothing else; reading a Kotlin simple name needs
reflection, which `TASK-020210` excluded by name (*"no `@OptIn`, no reflection, no new
dependency"*). So whether the two names agree is a **convention** whichever way this goes — which is
a reason to keep it simple enough to hold by eye.

### The deadline, and it is not a reason to decide a particular way

No client is deployed, and `PROTOCOL_VERSION` moves in this story regardless (`ADR-0028` §8,
`ADR-0045` §4). A wire-visible name is at its cheapest to change today and behind a compatibility
window afterwards.

## Decision

### 1. A wire discriminator is its Kotlin simple name

Every `ClientMessage` and `ServerMessage` subtype carries an explicit `@SerialName` whose string
**equals its Kotlin simple name, character for character**. A subtype whose two names differ is a
defect, not a style choice.

`@SerialName` is not a licence to diverge. It is what `TASK-020210` put there so that moving the wire
name is a *deliberate* edit and never a side effect of a refactor, and that purpose is unchanged:
the two names are held equal on purpose, and moving one means moving the other, in a commit that
also takes a version step.

**No test proves this and none is added.** A descriptor cannot see a Kotlin name and `TASK-020210`
forbade the reflection that could; the rule holds by review, and its whole value over the status quo
is that a reviewer now has something to cite.

### 2. `ActedForAbsentSeat` becomes `ActedForAbsent` — type and `@SerialName` together

Fourteen characters. `ADR-0028` §1's declaration is amended in exactly two lines and in nothing else:

```kotlin
@Serializable
@SerialName("ActedForAbsent")
public data class ActedForAbsent(
    val seat: Int,
    val handNumber: Int,
    val actionSequence: Int,
    val action: ActionType,
) : ServerMessage
```

Both `require` blocks, all four fields, both recipients and every emission point in `ADR-0028` §5
stand exactly as written. `OpponentPresence` and `SeatPresence` do not move.

**The word that goes is the one the payload already supplies.** `ActedForAbsentSeat` names the seat
twice — once in the discriminator, once in `seat`. Its sibling makes the same distinction in the
other direction: `OpponentPresence` deliberately carries no seat field *and* names no seat, because
it is recipient-relative (`ADR-0028` §1). The protocol's names already track what its payloads
carry, and this is that rule applied rather than a syllable sacrificed to a gate.

`ActedForAbsent` has the same subject the old name had, which is to say none. `ADR-0046` §4's rule
that *the subject is always the server* governs the sentence a client renders — `The server folded
for your rival.` — and is untouched here: no rendered string changes, and none of `ADR-0046`'s five
strings names the type at all. `Absent` is `SeatPresence.ABSENT`, a term this protocol defines.

### 3. The limit stands at sixteen, unratified, and this is what would move it

`ProtocolDiscriminatorTest` is not edited. `ProtocolDiscriminatorTest.kt` does **not** become a
fourteenth *Files* row on `TASK-021402`, whose `files_touched` stays at thirteen.

This ADR does not claim sixteen is right — the Context says plainly that nobody ever defended it. It
claims that **this is the wrong evidence to move it on**. A threshold that moves because a name
somebody already wrote is two characters over it is not a threshold; it is a comment. This
repository has spent three ADRs — [`ADR-0068`](ADR-0068-an-atomic-ticket-names-the-gate-that-forbids-splitting-it.md),
[`ADR-0069`](ADR-0069-the-blast-radius-is-probed-not-remembered.md),
[`ADR-0070`](ADR-0070-a-blast-radius-is-complete-only-when-the-gates-are-green.md) — learning that a
number which moves to fit whatever was found this time was never a constraint, and `ADR-0070` §4
states the general form for a coder: propagation may add a file and may **never** weaken an
assertion. Raising sixteen to eighteen weakens the assertion that failed, in order to admit the thing
that failed it.

What *would* move it: a frame whose clearest name cannot be written in sixteen characters without
losing meaning, demonstrated with the alternatives written down and rejected on what each costs a
reader — not asserted. §2 is the demonstration that this was not that case. If the limit ever moves
it moves in its own ADR, and that ADR renames nothing.

### 4. `poker-engine`'s discriminators are untouched, and the gate is not extended to them

`BettingRoundEnded` and `UncalledBetReturned` keep their names. The envelope and the game-event
vocabulary are different sets with different readers, and extending a style rule across that boundary
would rename merged, replayed event types and move `EVENT_SCHEMA_VERSION` to do it. The incoherence
the Context names is left standing, in the open, rather than settled in passing.

## Consequences

### What it buys

- **`TASK-021402` unblocks at thirteen files with no merged gate edited.** The wire step goes back to
  being propagation: `files_touched` and the *Files* table are unchanged, and
  `ProtocolDiscriminatorTest` stays in `verify:` as a gate the ticket satisfies rather than one it
  edits. `status` moves off `blocked` to **`backlog`**, not `ready` — `lint_tickets.py` refuses
  `ready` on a task whose `depends_on` is open, and `TASK-021401` is still in flight — so the ticket
  becomes startable the moment that one lands, with nothing left to decide.
- **The one invariant a reader has when debugging a frame survives, and is now written down.** Grep
  the `type` string, land on the declaration. Before this it was true fourteen times by accident.
- **The rename is free exactly once, and this is that moment.** No client is deployed and the
  version steps in this story anyway.

### What it costs

1. **Twelve settled documents now name a type that will not exist**, and are deliberately left as
   written, because a merged ADR records what was decided and a `done` ticket records what was done:
   `ADR-0028` (8 mentions in its body), `ADR-0045` (4), `ADR-0046` (2), `ADR-0044` (1),
   `docs/adr/README.md`'s answered rows for `DEC-018` and `DEC-038`, `tasks/BOARD.md`'s two
   historical paragraphs (its live rows *are* corrected), and the six settled tickets and stories
   `TASK-030303`, `TASK-030308`, `STORY-0303`, `STORY-0310`, `STORY-0312` and `STORY-0213`. Only `ADR-0028` gets an amendment line,
   because only its decision changed; a reader who lands on `ADR-0046` §4 first meets a dead symbol
   and has to follow the chain here. **This paragraph is the index** — a grep for
   `ActedForAbsentSeat` should end at it.
2. **Eight live documents are corrected in this PR**, which is real churn on a decision whose
   alternative would have cost none: `TASK-021402`, `TASK-021407`, `TASK-021408`, `TASK-021409`,
   `STORY-0214`, `STORY-0313`, `EPIC-03` and `tasks/BOARD.md`. The comment in
   `web-client/src/protocol/frames.ts` that names the old type is corrected by `TASK-021402`, in the
   same edit that adds the two table rows.
3. **§1's invariant has no gate and cannot cheaply have one.** The next `@SerialName` that drifts
   from its type passes every check in this repository, and the generated TypeScript quietly changes
   name with it. A rule this ADR states as a *defect* is enforced by review alone.
4. **A worse name than the one `ADR-0028` chose, by the lights of whoever chose it.**
   `ActedForAbsentSeat` says in the discriminator what `ActedForAbsent` leaves to a field, so a
   reader looking at a bare `type` with no payload — a log line, a metrics label — gets one word
   less. That is §3's cost and it is paid here, in the name, on purpose.
5. **Sixteen is now defended without ever having been justified**, which is the least satisfying half
   of this. The next collision re-runs the same argument with the same missing evidence, and §3's
   trigger is the only thing standing between here and that.
6. **`OpponentPresence` sits exactly on the limit**, so this corner of the vocabulary has no headroom
   left in practice. The pressure that eventually moves the limit is already in the room; what this
   ADR buys is that it moves on a written case rather than on a build failure.

### What it forecloses

- **The divergence family, permanently, unless §1 is superseded.** No `ServerMessage` may be named
  one thing in Kotlin and another on the wire — *including* the legitimate use of that freedom:
  renaming a Kotlin type for clarity while deliberately holding the wire still. After this, such a
  rename costs a version step. That is a real loss, and it is the price of the grep.
- **`ActedForAbsentSeat` as a name.** Reclaiming it now costs a raised limit *and* a version step.

## Alternatives considered

**Raise the limit to eighteen (or twenty) and keep `ActedForAbsentSeat`.** *Its strongest case:* the
number has no stated reason anywhere; it is not a property of the wire, since the same frames already
carry `UncalledBetReturned` at nineteen; and the assertion it lives in catches an
accidentally-defaulted discriminator through a different check entirely, so nothing correct depends
on the length half. `ActedForAbsentSeat` is the name four merged documents use, including the one
specifying the frame and the one specifying its rendered copy. It costs **zero** documentary churn
against this decision's eight files, and the edit is one character in one merged test. On the merits
of the name alone it is the better name, and this ADR concedes that. *Rejected* because the reversal
is asymmetric in a way the symmetry of the two edits hides: a name renames again on any version step,
while a limit cannot come back down without renaming whatever grew past it in the meantime — and
things will. And because *"the name I already wrote is two over"* is the one piece of evidence that
must never move a threshold: accepting it here makes the threshold advisory from now on, and an
advisory length rule is indistinguishable from no rule at all.

**Shorten the `@SerialName` only, keeping `ActedForAbsentSeat` in Kotlin.** *Its strongest case:* the
smallest possible edit — one string in one file — leaving every merged ADR and every backlog document
correct about the Kotlin type it names, and using `@SerialName` for exactly what it is for.
`TASK-020210`'s stated purpose is that the wire name is explicit and independent of the class, so a
divergence is not a violation of anything currently written. *Rejected* because the divergence does
not stay in Kotlin: `typeNameOf` reads the serial name, so `protocol.gen.ts`'s interface,
`frames.ts`'s key, `docs/protocol.md`'s row and the ledger fingerprint would all carry the short name
and the long one would survive only in the server's own sources. And because it would be this
protocol's **first** divergence, which is the expensive one — it converts *the discriminator is the
type name* from something a reader may rely on into something they must check, for all sixteen types
this story leaves behind and every one after them, with no gate able to tell them which.

**A name that puts the actor in the discriminator — `ServerActed` (eleven), `ServerActedFor`
(fourteen).** *Its strongest case:* short, grammatical, and it satisfies `ADR-0046` §4's strongest
rendering rule — *the subject is always the server* — at the type level and not only in the copy,
which is arguably where that rule belongs. *Rejected* because it is broader than the type's own
`require`s: the server also deals, ends duels and refuses actions, and a discriminator that could
name any of those invites the next server-originated fact to be squeezed into it. A frame's name
should be exactly as narrow as its invariants, and this frame's invariants are *an absent seat* and
*fold or check, never anything else*.

**`AbsentSeatActed` (fifteen).** *Its strongest case:* it keeps every word of the original name and
fits the gate with a character to spare. *Rejected* on meaning: it makes the absent player the
subject of an action they did not take — `ADR-0023`'s indistinguishability, which `ADR-0028` spent a
version step retracting, reintroduced in the type name. `ADR-0046` §4 rejects the identical reading
in the rendered copy, for the identical reason.

**Replace the length check with one rule over the whole wire, engine included.** *Its strongest
case:* it is the only option that ends the incoherence the Context names — one vocabulary, one rule —
and therefore the only one that would make sixteen mean something. *Rejected* as out of proportion
and out of scope: it renames merged, replayed `GameEvent` types, moves `EVENT_SCHEMA_VERSION`, and
reaches into `poker-engine` for a style rule, on a decision whose subject is two new frames. If that
incoherence is worth fixing it is worth its own ADR and its own evidence.
