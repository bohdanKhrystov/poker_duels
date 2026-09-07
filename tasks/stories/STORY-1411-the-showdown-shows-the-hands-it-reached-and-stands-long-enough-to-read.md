---
id: STORY-1411
title: The showdown shows the hands it reached, and stands long enough to read
type: story
status: ready
parent: EPIC-14
module: web-client
labels: [client, table, design, bug]
depends_on: []
---

## Goal

The card draws the showdown states the table does not have — a hand shown, a split where both are
shown, and a fold where none is — and the last beat of a hand that turned a hand face up the viewer
had not seen **stands 2,000 ms** instead of 600. Nothing is drawn that was not drawn before: the
faces already come from the snapshot, in the seat that showed them. What changes is one union field
on `RevealStep`, one predicate over one frame, one constant at the boot seam, and one map in the
function that already owns the clock.

## Why

**It is `EPIC-14` item 2a, and it is the one item the human reported from playing the product**:
*"if we went to showdown villan card shoud be show"*, after a duel on a laptop and an iPhone against
a local-network stack on 2026-09-06.

**The epic's own measurement of it is wrong, and the ADR corrected it in writing.** `EPIC-14`
recorded that no client file outside `protocol.gen.ts` mentions `HandRevealed` and inferred that
*"the hand that is shown is sent, arrives, and is drawn by nothing."* The first half is exact; the
inference is not.
[`ADR-0120`](../../docs/adr/ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md)
traced the reveal to the **snapshot** — `Addressed.kt:52,62` passes `revealedSeats(handEvents)` into
`PlayerView.of`, `PlayerView.kt` populates a revealed seat's `holeCards`, and `DuelTable.tsx:76-78`
draws the rival's seat with `cards={rival.holeCards}` — and `web-client/src/e2e/duel-secrecy.test.tsx`
has pinned it over recorded frames since it was written. **A shown hand is already on the screen.**
What it is missing is **time**: a hand called down to the river carries no `StreetDealt`, so its
ending is **one** step at `REVEAL_STEP_MS = 600`, and `advanceReveal` drains the next hand the moment
that step expires. Two cards the player has never seen, a five-card board and an award line, in six
tenths of a second, on a hand `ADR-0103` §3.2 draws at 24–40 px.

So this story builds no second drawing path. It is a **card**, a **hold**, and the tests that pin
both — which is exactly what `ADR-0120`'s *Consequences* said `STORY-1411` would shrink to.

**Both decisions it needed are merged**, so it waits on nothing and registers nothing: `DEC-133` by
`ADR-0120` (the product half) and `DEC-146` by
[`ADR-0136`](../../docs/adr/ADR-0136-a-beat-declares-its-own-length-and-zero-silences-every-beat.md)
(the mechanism).

## Design notes

Everything below is either merged or was **measured in this worktree on `develop` at `c54b4a1f`**
against a prototype that was reverted. No ticket re-litigates any of it.

### The mechanism is `ADR-0136` §§1–3, verbatim, and it is three tickets because the seams are three

`RevealStep` gains `hold: "step" | "read"`; `layOutReveal` sets it from `ADR-0120` §3's predicate
over `view.viewerSeat` and `seat.holeCards`; `DuelStoreOptions` gains `readMillis`, absent meaning
`stepMillis`; `armTick` maps the kind at the head of the queue to a number **after** a
`stepMillis === 0` gate that returns before the kind is read; `boot.ts` names `REVEAL_READ_MS = 2000`
beside `REVEAL_STEP_MS = 600` and passes it down. `advanceReveal` is **byte-unchanged**;
`layOutReveal`'s and `applyServerMessage`'s signatures do not change.

### Nothing crosses the socket, so no ticket here is `atomic:`

`PlayerView.viewerSeat` and `SeatView.holeCards` both already ship on every `Snapshot`, both already
filtered by `PlayerView.of`'s `showCards` — the one place a hole card is ever filtered.
`ADR-0136`'s *Nothing crosses the socket* bullet spells out the consequence: no declaration changes,
so `ADR-0047` §2's fingerprint cannot move, so there is no `PROTOCOL_VERSION` step and no branch
lock. `TASK-141104` is a plain three-file ticket for the ordinary reason — `tsc` refuses the
intermediate state — and not under `ADR-0068`.

### The type checker fixes `TASK-141104` at exactly three files, and the third was measured

`RevealStep` is constructed in **one** place, `layOutReveal`. But making `hold` required also breaks
one literal in a test file the grep for `RevealStep` never finds, because it never spells the type:

```
src/routing/room-standing.test.ts(58,17): error TS2741: Property 'hold' is missing in type
'{ board: never[]; street: "PREFLOP"; }' but required in type 'RevealStep'.
```

That is the whole list — `tsc --noEmit` returns nothing else. Three files, irreducible, exactly the
ordinary cap.

### Two merged assertions move, and they were counted rather than guessed

`duel-state.test.ts:1471` and `:1488` assert `state.reveal?.steps` with `toEqual` over whole step
objects, so both go red the instant a field is added. Both frames are built from a local
`sampleSeat` whose `holeCards` defaults to `[]` — the short side — so **both gain `hold: "step"` and
neither's meaning changes**. Measured with the prototype: the full client suite went
`124 files, 1166 tests` green to `1 failed | 123 passed (124)`, `2 failed | 1164 passed (1166)`,
and those two are the two. `TASK-141104` owns them and says so in its budget.

Nothing else in the repository moves. `DuelTable`'s `revealStep` prop keeps its declared
`{ board; street } | null`: `Lobby.tsx:338` passes a variable, not an object literal, so no excess
property check reaches it, and `DuelTable.test.tsx`'s literal props stay valid. `Lobby.tsx`,
`DuelTable.tsx`, `PotStrip.tsx` and `no-derivation.test.tsx` are **not opened by any ticket here**.

### The hold is proved by the `delayMillis` arguments, never by a call count

`ADR-0136` §7.4's own instruction, carried into `TASK-141105` verbatim: with a `schedule` spy at
`stepMillis: 600, readMillis: 2000`, assert the **arguments** — `[600, 600, 600, 2000]` for a
three-`StreetDealt` runout to a showdown, `[600]` for a fold's ending. **A call count is identical
either way and proves nothing.** Both sequences were produced by the prototype and are measurements,
not arithmetic.

The reason the instruction matters is structural: `ADR-0136` §4 keeps the DOM **byte-identical** at
600 ms and at 2,000 ms — no component gains a prop, no element gains an attribute or a
`data-testid` — so nothing rendered can ever see this decision. The injected `schedule` is the only
place a duration is observable in this client.

### The driver's zero is load-bearing, and how load-bearing was measured

`drive-duel.tsx:229` boots at `stepMillis: 0` under a comment saying the four recorded-frame suites
it serves may not be edited and none may gain a clock. `ADR-0136` §3 puts the gate in
`duel-store.ts`, before the kind is read, and warns that a `readMillis` reachable at `stepMillis: 0`
would stop the next hand ever reaching the screen inside the `act()` that delivered it — *"and the
symptom reads like a flake."*

Measured: with the four lines of the zero gate deleted and nothing else changed,
`npx vitest run src/e2e` goes from **7 files, 55 tests** green to **4 files failed, 24 of 55 tests
failed**. `TASK-141105` carries that as a `verify:` gate — the mutation is applied, the e2e run is
required to fail, the file is restored and the restore is checked with `cmp -s`. **No file under
`web-client/src/e2e/` is edited by any ticket in this story**, `scripted-duel.gen.json` is
byte-unchanged, and `drive-duel.tsx` is deliberately **not** given a redundant `readMillis: 0`
(`ADR-0136` §3: a redundant zero is a second statement that can drift out of agreement with the gate
that makes it redundant).

### The card comes first, and it is three tickets because one frame is ~60 lines

`ADR-0091` §2, and `ADR-0120`'s *Applies* names it: *"no card draws a showdown today, so
`STORY-1411`'s first ticket is that card."* `design/screens/duel-table-states.html` draws three
frames — a wait, a showdown the viewer **won** with the loser mucking, and a fold — and none of them
is the state this story is about. Two frames must be added and both existing endings are in arrears,
which is more than 120 changed lines in one file, so it is `TASK-141101` (the arrears),
`TASK-141102` (the hand shown) and `TASK-141103` (the split), in that order, all on one file.

The arrears were measured, and each is struck by a **merged** ADR rather than by taste:

- `<div class="bet-line">ImKate mucks</div>` — `ADR-0120` §1: *"**No line is added to say a hand was
  mucked.** The cards that stay face down say it … a *Your rival mucks* string is not licensed by
  it."* The client agrees already: `DuelTable.tsx:132`'s `BetLine` prints a committed figure and
  nothing else, in either seat, on every street.
- `<span class="meta">Two pair, aces and sevens</span>` and
  `<span class="meta">Nobody shows — your river bet of 800 goes uncalled and returns</span>` —
  `ADR-0095` §3 struck both by name (*"the card's showdown frame loses that line"*, *"The fold
  frame's second line … goes with it"*) and §1 says what stands in their place: the facts line,
  untouched, `Blinds N/N · Hand N · Hand complete`. That ADR said *"the card correction is its own
  `module: design` ticket"* and the ticket was never written; `TASK-141101` is it. The literals are
  the client's own: `PotStrip.tsx:14,119-120` builds `Blinds 75/150 · Hand 14 · Hand complete`, and
  `awardLineFor` builds `You win N`, `Your rival wins N` and `Split pot — you win N`.

**`ImKate folds` stays.** `ADR-0120` §1 strikes the muck line by name and says nothing about a fold
line, and whether `ADR-0109` §1's `Fold` mark on the plate makes it redundant is a question nobody
has asked. It is named in `TASK-141101`'s *Out of scope* so a coder does not take one deletion as a
licence for the other.

### The card must not invent a highlight, and `ADR-0126` is why the line is written down

[`ADR-0126`](../../docs/adr/ADR-0126-the-table-shows-the-cards-and-marks-none-of-them.md) merged on
2026-09-06 and answers `DEC-134` **no**: *"the five cards that made the winning hand are drawn
exactly as the two that did not"*, and a mark is *"a ring, border, glow, tint, shadow, lift, scale,
opacity, reordering, badge or connector; a class name a stylesheet can key on; a `data-*` attribute;
a `title`; or anything in an `aria-label` beyond the rank and suit"* — **dimming the cards that did
not play included**. §4 asks for *"one line on `STORY-1411`'s showdown card … so the drawing is not
re-decided at the pane."* Both new frames carry it in their margin note, and both carry a `verify:`
gate that their face-up cards differ only in rank, suit and suit colour.

`ADR-0126` §4's other half — the **client** gate, *the drawing of a card does not depend on whether
it played* — is `STORY-1412`'s and is not ticketed here.

### The beat is time, not entrance, and the card draws a still state

`ADR-0136` §6, stated as a corollary the card must not re-derive: the hold **has no CSS**. It is
`REVEAL_READ_MS` in `boot.ts` and a `delayMillis` argument to an injected `schedule`; no
`--pd-motion-*` token is minted, `design/tokens/tokens.css` is not edited, the sheet's one
`prefers-reduced-motion: reduce` block is not edited, and the store has no `matchMedia`. So a fade-in
or a scale on the revealed hand is refused twice over — it would spend part of the 2,000 ms drawing
rather than showing, and it would put a fade beside `ADR-0095` §4's *never on a fade*. **The reveal
is already on screen the instant the beat begins.** Neither design ticket adds an animation, a
transition or a `--pd-motion-*` reference, and both gate that.

### The rival's shown hand is drawn as the client draws it, not as `.pc.mini`

Measured: `design/screens/duel-table-states.html:182` inlines a `.pc.mini .pip { display: none; }`
rule that **no frame in the file uses**, and `design/components/playing-card.html:81` draws the
40 px reference as `class="pc mini"`. But `web-client/src/table/PlayingCard.tsx`'s `CardFace` has no
mini variant at all: one `SHELL` constant, and the corner index and the pip are both sized as
fractions of `--w` at every width. The client has simply never drawn a face-up card at the rival's
width, because until this story no card was ever shown there.

Both new frames therefore draw the rival's two shown cards as plain `class="pc" style="--w:40px"`,
**byte-for-byte the treatment `PlayingCard.tsx` produces**, and say so in the frame's own note. A
card that specified the mini would be specifying a client change nobody has asked for. If the human
prefers the mini at the pane, that is a repair ticket against the card *and* against `CardFace`, and
`ADR-0091` §3 lets that verdict trail the merge.

### The predicate answered for a *delivery*, and `ADR-0139` is what repaired it

`ADR-0120` §3's rule is *"a hand face up the viewer **has not been shown before**"*. The sentence
that restates it *"as the client can evaluate it"* — *"the hand-completing view carries hole cards
for the rival's seat"* — **drops the word `before`**, and `TASK-141104` merged the restatement, so
until `TASK-141108` lands **every** delivery carrying rival hole cards answers `"read"`. Nothing in
the frame can tell a first showing from a repeat: `PlayerView.of` fills a revealed seat's
`holeCards` from every `HandRevealed` in the hand's log on **every** projection of it,
`duel-state.ts` re-lays a reveal on every `COMPLETE`, and `duel-store.ts` builds its state once and
keeps it across sockets.

**It is latent rather than live, and that was measured rather than argued.** A throwaway
`poker-server` probe on `develop` at `5cce33c4` — 60 duels, since reverted — took `resumeFrames` at
every step: **0** hand-completing `Snapshot`s in **2,220** resume frames, and **0** seats ever sent
one hand's completing view twice across **604** hand endings and **115** showdowns. `act` calls
`advance` in the same call (`DuelAction.kt:59`), `advance` loops until a hand is not over, and
`GameState.isHandOver` *is* `street == COMPLETE`, so a live `DuelRunner.hand` is never over and a
resume cannot re-project one. The probe was **falsified rather than trusted**: asked what a runner
would hand back if `advance` were not called, the same run found **602**.

So the honest guard contradicts `ADR-0136` §1 in three places — *"`layOutReveal`'s signature does
not change"*, *"from the view it is already given and from nothing else"*, *"no previous view is
remembered"* — in order to fix an answer no path reaches, and only an ADR amends an ADR. That was
**`DEC-153`, the architect's**, and it is answered by
[`ADR-0139`](../../docs/adr/ADR-0139-the-read-beat-is-spent-once-and-the-store-already-remembers.md),
merged 2026-09-07: **take the guard, and amend those three sentences and no others.**
`layOutReveal` gains `held: PlayerView | null`, the `Snapshot` case passes `state.view`, and
**`DuelState` gains no field** — the memory was already there, which is what keeps the amendment
narrow and leaves all three of `ADR-0136` §1's stated reasons standing. The second answer, *write
the invariant down instead*, lost on where its pin would live: `EPIC-14` forbids `poker-server`, and
a test deferred outside the epic is a test nobody writes. `ADR-0139` §9 writes the invariant down
anyway, as a fact with no test, and registers nothing.
[`TASK-141108`](../tasks/TASK-141108-the-read-beat-is-spent-once-and-a-repeat-is-a-step.md) is
therefore **unblocked and stands exactly as written**, every measured number included.

### The per-file test counts are measured, never computed

Baselines on `develop` at `c54b4a1f`, this worktree, `npm ci` fresh: `duel-state.test.ts` **89**,
`duel-store.test.ts` **14**, `boot.test.ts` **26**, `src/e2e` **7 files, 55 tests**, whole client
suite **124 files, 1166 tests**. Every ticket gates its own file's absolute count after the change,
because a whole-suite number is wrong the moment two tickets batch and a collection error prints a
passing count.

## Tasks

Split on **2026-09-07**. One linear chain — the three design tickets share one file, and each client
ticket reads a declaration the one before it adds — so exactly one is startable at a time.

| Ticket | Est | What it is |
| --- | --- | --- |
| [`TASK-141101`](../tasks/TASK-141101-the-card-strikes-the-muck-line-and-both-second-lines.md) | XS | The card strikes what two merged ADRs already deleted: `ImKate mucks` (`ADR-0120` §1) and both banners' second lines, which become `ADR-0095` §1's untouched facts line. `ImKate folds` stays, and a gate says so |
| [`TASK-141102`](../tasks/TASK-141102-the-card-draws-the-showdown-the-viewer-lost.md) | S | A fourth frame: the rival's two cards **face up** in her own seat, `Your rival wins 4,850`, no card picked out, no motion — the state the whole story is about, and the one `duel-table-states.html` has never had |
| [`TASK-141103`](../tasks/TASK-141103-the-card-draws-the-split-where-both-hands-are-shown.md) | S | A fifth frame: `ADR-0120` §1's third row — a split, both hands face up, `Split pot — you win 2,425`, and the viewer's own share is the only share stated |
| [`TASK-141104`](../tasks/TASK-141104-a-beat-carries-its-kind-and-one-frame-decides-it.md) | S | `RevealStep.hold: "step" \| "read"`, set once in `layOutReveal` from `ADR-0120` §3's predicate. Three tests, two mutations, and the two merged `toEqual` assertions that gain `hold: "step"` |
| [`TASK-141105`](../tasks/TASK-141105-the-store-holds-the-read-beat-and-zero-silences-every-beat.md) | S | `readMillis` on `DuelStoreOptions`, absent meaning `stepMillis`; `armTick` maps the kind after a `stepMillis === 0` gate. Proved by `[600, 600, 600, 2000]` versus `[600]`, and by an e2e run that must go red when the gate is removed |
| [`TASK-141106`](../tasks/TASK-141106-boot-names-the-read-beats-length-beside-the-steps.md) | XS | `REVEAL_READ_MS = 2000` beside `REVEAL_STEP_MS`, `BootOptions.readMillis`, and two tests that read the `delayMillis` boot's own `schedule` reaches `setTimeout` with |
| [`TASK-141107`](../tasks/TASK-141107-the-showdown-card-is-gated-frame-by-frame-not-file-by-file.md) | S | `design/check-frame-cards.sh` — a frame-scoped gate, added after `TASK-141102` merged because all 48 of its gates were whole-file aggregate counts and a reviewer proved a swap of the fold and showdown-lost `oppcards` slots left every one green while the fold frame showed a folded hand face-up. Anchors on each frame's own `<h2>` and refuses when an anchor matches nothing. **done** |
| [`TASK-141108`](../tasks/TASK-141108-the-read-beat-is-spent-once-and-a-repeat-is-a-step.md) | S | **Unblocked by [`ADR-0139`](../../docs/adr/ADR-0139-the-read-beat-is-spent-once-and-the-store-already-remembers.md)**, which answers `DEC-153` for the guard and says this ticket stands exactly as written. The read beat is spent once per hand: `layOutReveal` takes the view the store already holds and answers `"step"` for a repeat of the same `handNumber`. Two files, **92 → 95**, two mutations |

**`TASK-141108` comes last, not before `TASK-141105`**, and the ordering was measured rather than
assumed. It was raised as due *before* the store starts scheduling from `hold`, on the reading that
a reconnecting player would otherwise sit through the 2,000 ms again — but there is no reachable
second delivery to sit through (see *Design notes*), so making the store's ticket wait on an
architect's decision would stall a startable chain for a symptom no path produces. It therefore
depends on `TASK-141106`, was `blocked` on `DEC-153` until `ADR-0139` merged on 2026-09-07, and
gates nothing.

**A seventh ticket was considered and refused.** `docs/test-plan.md` gains nothing here: `EPIC-14`'s
per-epic suite is the `qa-cases` skill's to write from the epic's *Definition of done* — *"one case
per promise the epic made… not one per ticket"* (`docs/test-plan.md` §Per-epic suites, rule 1) — and
the epic's showdown promise is already written there.

## Acceptance criteria

- [ ] A hand-completing view where the **rival's** seat carries `holeCards` lays out a last step of
      `hold: "read"`; the **same** view read from the rival's own seat lays out `hold: "step"`
- [ ] A hand that ended in a fold lays out `hold: "step"`, and every non-final step of a runout does
      too
- [ ] At `stepMillis: 600, readMillis: 2000`, the injected `schedule` receives the `delayMillis`
      sequence `[600, 600, 600, 2000]` for a three-`StreetDealt` runout to a showdown and `[600]`
      for a fold's ending — asserted as **arguments**, never as a call count
- [ ] At `stepMillis: 0` with `readMillis: 2000` explicitly set, `schedule` is **not called at all**
      and the queued next hand drains in the same turn
- [ ] `bootDuelClient` with no `readMillis` holds a read beat at **2,000 ms**, and holds it at what
      it was told when it was told something else
- [ ] No file under `web-client/src/e2e/` is edited, `scripted-duel.gen.json` is byte-unchanged, and
      `npx vitest run src/e2e` stays at **7 files, 55 tests** passing
- [ ] `no-derivation.test.tsx`, `Lobby.tsx`, `DuelTable.tsx` and `PotStrip.tsx` are untouched, and
      `DuelTable`'s props gain no member
- [ ] `design/screens/duel-table-states.html` holds **5** frames; the two new ones draw the rival's
      hand **face up** in her own seat, carry `Your rival wins 4,850` and `Split pot — you win 2,425`,
      and name no hand
- [ ] Neither new frame marks a card: every face-up card in them differs from every other only in
      rank, suit and suit colour — no class, no `data-*`, no `title`, no `style` beyond `--w`, and no
      `aria-label` past rank and suit
- [ ] Neither new frame adds an animation, a transition or a `--pd-motion-*` reference, and
      `design/tokens/tokens.css` is not edited
- [ ] `ImKate mucks`, `Two pair, aces and sevens` and `Nobody shows` appear nowhere in the card;
      `ImKate folds` still appears exactly once
- [ ] A hand-completing view **delivered a second time** — drained and re-applied, or queued behind
      the beat it repeats — lays out `hold: "step"`, while the **next** hand's showdown is still a
      `"read"` (`ADR-0139` §§2 and 10, which answered `DEC-153` for the guard rather than for the
      invariant)
- [ ] Every changed behaviour is proved red by a `verify:` command that reintroduces the old
      behaviour, captures the failure, restores the file and checks the restore with `cmp -s`
- [ ] `cd web-client && npm run check` exits 0, `./design/check-drift.sh` exits 0, and
      `python3 .github/scripts/lint_tickets.py` exits 0

## Out of scope

- **The all-in-and-called reveal.** `ADR-0120` §5 names it as the most likely first widening —
  *"in every live poker room and every online client, a player who is **all in and called** turns
  their hand face up before the board runs out, because no decision remains to protect"* — and
  refuses it **here** rather than on merit: it is an addition to `docs/duel-rules.md`'s written
  rules, it needs either the engine reveal `ADR-0008` forbids or a post-hand disclosure with its wire
  move, and it is not what was reported. `ADR-0105` §6's route: **named, deliberately not
  registered**, and not ticketed.
- **A voluntary show.** `ADR-0008`'s own rejected alternative — *"Showing is a `PlayerAction`"* —
  deferred there on sequencing. `ADR-0120` §4 repeats that nobody has asked for it and nothing in it
  opens the question. Not ticketed.
- **Any post-hand disclosure of the mucked hand.** `ADR-0120` §1: `ADR-0008` stands unamended, the
  wire does not move, and the loser's hand is never revealed. §5 states the trigger that would
  reverse it — the first duels played by people who are not the author — and it is not met.
- **Marking the winning five.** `ADR-0126` answered `DEC-134` **no**. Its client gate — *the drawing
  of a card does not depend on whether it played* — is `STORY-1412`'s, and only its *one line on the
  showdown card* lands here.
- **The pot travelling to the winner.** `EPIC-14` item 2c, `STORY-1413`, governed by `ADR-0115` and
  `ADR-0102`. The existing frames' `pile flying` is copied into the new frames for parallelism and
  nothing about the flight is decided, added or changed.
- **How wide the rival's hand is drawn.** `DEC-136`, answered by `ADR-0121` for the table's fit;
  `ADR-0120` §4 says outright that §3 is worth what that decision leaves it worth. The new frames
  draw 40 px because that is `ADR-0103` §3.2's ceiling and what the shipped client renders.
- **`CardFace` growing a `mini` variant.** Named in *Design notes*, deliberately not ticketed: no
  client file is opened by this story, and the pane verdict may trail the merge (`ADR-0091` §3).
- **The `.mucked` dimming on a face-down back.** `ADR-0126` §1's closing sentence keeps what a card
  **is** *"exactly as it is drawn today"*, and no new frame in this story draws a card back at all —
  at a showdown both hands the rules showed are face up. Nothing here changes it in either
  direction.
- **`ImKate folds`.** See *Design notes*; `ADR-0120` §1 strikes the muck line and not this one.
- **The engine and the server.** `EPIC-14`'s *Out of scope* forbids opening `poker-engine`, and
  `ADR-0120` took `poker-server` out of item 2a altogether. No Kotlin file is opened.
- **`docs/test-plan.md` and the QA catalogue** — see the refused seventh ticket above.
- **Naming a hand.** `ADR-0095` §3 stands unamended: not at a showdown, not in the facts line, not
  in an `aria-label`, not in a `title`. `TASK-141101` deletes the one place the card broke it and
  adds nothing in its place.
