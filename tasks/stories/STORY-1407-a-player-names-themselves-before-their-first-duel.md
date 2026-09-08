---
id: STORY-1407
title: A player names themselves before their first duel
type: story
status: backlog
parent: EPIC-14
module: web-client
labels: [client, profile, lobby, design]
depends_on: []
---

## Goal

A player who holds no display name and presses *Play duel* or *Join the duel* meets one screen
before the room: a question, a field already holding a suggestion, and two ways out — take a name,
or skip and play. Taking one sends the shipped `PUT /api/me/name`; skipping sends nothing, writes
nothing, and takes the player into the duel their press asked for. The suggestion is drawn in the
browser from a bundled vocabulary, so nothing crosses the wire to produce it and the ask has no
loading state. Every other route into a duel — the invite link, a rematch, a resume, a press by a
player who already holds a name or had one removed, a press in a browser that has already skipped —
is byte-unchanged.

## Why

**`EPIC-14` item 1a, and the human's own sentence:** *"First time you play dialog 'choose your
name' appears with sugestion(before duel)."* It is the first item of the first message after
playing the product on a laptop and an iPhone against a local-network stack on 2026-09-06.

**Both decisions under it are merged, and they leave nothing open.**
[`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md)
fixes *when* the ask stands (the player's **own** first press, never on the invite path), that it
is a **screen and not a modal**, that it **may be skipped**, and that **nothing is written unless
the player deliberately takes a name** — `ADR-0051` §1 lets no string leave `name_registry`, so an
auto-accepted suggestion would burn one name per browser that ever reached the screen.
[`ADR-0137`](../../docs/adr/ADR-0137-a-name-suggestion-is-drawn-in-the-browser.md) answers
`DEC-142`: the suggestion is a **pure function in the browser** over an import-free bundled
vocabulary, `name_registry` is never consulted, **nothing crosses the wire**, and rarity is bought
with the **size** of the vocabulary rather than with a check.

**So this is a client-and-design story end to end.** No Kotlin file is opened, no route is added,
`docs/protocol.md` is not edited, no migration is written, `PROTOCOL_VERSION` does not move, and
**no ticket here is `atomic:`** — `ADR-0137`'s own preamble says so, on `ADR-0068` §3.

## Design notes

Everything below was **measured in this worktree at `9dd8571c`** unless it names a merged
document. No ticket re-derives any of it.

### The baselines, measured

`cd web-client && npm ci` fresh, `FORCE_COLOR=0 NO_COLOR=1 npx vitest run`:

| Subject | Today |
| --- | --- |
| whole client suite | **124 files, 1183 tests** |
| `src/lobby/Lobby.test.tsx` | **104** |
| `src/App.test.tsx` | **36** |
| `src/protocol/one-module-owns-each-storage-key.test.ts` | **4** |

Every per-file count a ticket below pins is that number plus the tests its own *Tests* table names,
and nothing is computed across two tickets.

### What is already on the front door

`Lobby.tsx:387-457` is the front door, and it is the **last** branch in the file — under the six
chosen-screen branches, under the result, the table and the waiting room, and under
`if (standing === "unknown") return <></>` (`ADR-0118` §1). It holds exactly two ways into a duel:

- `Play duel` — `onClick={() => send({ type: "CreateRoom" })}` (line 406);
- the `Room code` form — `onSubmit` refuses an empty code and then
  `send({ type: "JoinRoom", code })` (lines 410-432), where `code` is `roomCodeFromField(typedCode)`.

**Nothing else in the client sends either frame from a press.** The invited rival's `JoinRoom` is
sent by `store/boot.ts` on the socket's `Welcome`, outside React (`ADR-0032`); a rematch is
`RematchControl`'s `OfferRematch`; a resume is the room the tab remembers. So `ADR-0119` §1's list
of *where the ask is never shown* holds **structurally** once the ask is reached only from those two
handlers — it is not a set of conditions anybody has to remember.

### What `ADR-0130` leaves this story to build — measured, because it changes the answer

`ADR-0130` §6 moves `NameSurface` to the account screen as the **one form that sets and changes a
name**. Three consequences, and the third is the one that would otherwise have been got wrong:

1. **This story builds a new component and reuses none of `NameSurface`.** `ADR-0137` §7:
   *"One surface offers a suggestion today: `ADR-0119` §1's ask. `NameSurface` keeps its empty
   initial value."* `ADR-0130` §6 says the same from the other side — *"`ADR-0119` §1's ask is
   untouched"* — and §5 adds *"the change form offers no suggestion"*.
2. **This story does not move `NameSurface` and does not open it.** `Lobby.tsx:433-436` keeps
   rendering it exactly as it does today; the move is `STORY-1409`'s, and a story that moved it here
   would be doing `STORY-1409`'s work with `STORY-1407`'s tests.
3. **`PERMANENCE_LINE` may not appear on the ask.** `ADR-0130` §5 **supersedes `ADR-0119` §3's
   obligation 2** and takes that string out of the product, so writing the ask against `ADR-0119` §3
   as printed would put a superseded sentence on the product's first dialog. What binds instead, *on
   every surface that writes a name*, is `ADR-0130` §5's pair: **the name can be changed later**, and
   **the name a player gives up is gone for good, for them and for everybody**. For a player taking
   their *first* name the second is forward-looking rather than vacuous — the string they leave
   behind on a later change is the one that is spent — and one sentence can carry both. `ADR-0119`
   §3's obligations **1** (what a name is for: what a rival and the ladder see) and **3** (a refusal
   reads in the words the product already uses) stand unchanged.

**`name-text.ts` is imported, never re-spelled.** `refusalSentence` and `mayTryAgain` ship there,
their strings are `ADR-0052`'s golden ones, and the ask calls both for **every** refusal kind
without a switch of its own. That is `ADR-0119` §3's obligation 3, and the answer to that ADR's own
warning that *"the ask and `NameSurface` must not drift"*. It also means the `permanent` sentence
`ADR-0130` §5 retires, and the `403` `ADR-0134` deletes, leave the ask **in `STORY-1409`'s diff and
not in one of ours** — the ask changes not at all when they go.

### The mechanism, module by module

Six new client modules, all under `web-client/src/profile/` beside `name-text.ts` and
`NameSurface.tsx` — the two surfaces that share one write path and one set of sentences are kept
where a reader sees both:

| Module | What it is |
| --- | --- |
| `name-vocabulary.ts` | `NAME_VOCABULARY: readonly (readonly string[])[]`. Imports **nothing** (`ADR-0137` §3), bundled and synchronous, arity is data |
| `name-suggestion.ts` | `SUGGESTION_SPACE_FLOOR = 1_000_000` and `suggestName(random, replacing?)` — pure, with no player fact anywhere in its signature |
| `name-ask-skipped.ts` | the one browser key, `pd.nameAskSkipped`, with `readNameAskSkipped` / `markNameAskSkipped` beside it — `account-offer-settled.ts`'s shape, which is `ADR-0086`'s, which is what `ADR-0119` §5 asks for |
| `name-ask.ts` | `askForName(input)` — the predicate the key feeds |
| `name-ask-text.ts` | the ask's own words, transcribed from the card |
| `NameAsk.tsx` | the screen |

plus `main.tsx`, which binds the key to `localStorage`, and `Lobby.tsx`, which holds the press and
releases it.

**The press is held, not repeated.** `Lobby` gains one piece of state, typed as the frame it is
holding — `useState<CreateRoom | JoinRoom | null>(null)`, so the ask can only ever release a
duel-starting frame — and one helper that either sends at once or stands the ask up. When the ask is
answered the held frame is sent and the state is cleared. `ADR-0119` §1: *"the press the player
already made goes through and they continue into the duel; they do not press twice"*, and *"the room
is created — or joined — after the answer, never before it."*

**The ask's branch sits immediately above the front door's `return`, below every other branch.**
That placement is what makes *never over the table, never over the waiting room, never over the
result, never over a chosen screen* true by construction rather than by five conditions: a frame
that seats the player wins the branch race the moment it arrives.

**The write is reported to the provider.** `NameSurface` calls `useReportNameWrite()` so the held
profile carries the accepted name on this render rather than on the next boot; the ask must do the
same, or a second press in the same tab would ask a player who now holds a name — the one thing
`ADR-0119` §1's *"already holds a display name"* row forbids. The hook is a no-op where no provider
is above, so it is safe in every test that mounts the component bare.

### The predicate, and the one row `ADR-0119` §1's table does not have

```ts
askForName({ profile: ProfileStripState | null, skipped: boolean }): boolean
```

True only when `profile.kind === "profile"`, `profile.profile.displayName === null`,
`!profile.profile.displayNameRemoved` and `!skipped`. It takes no `signedIn` — `ADR-0119` §1's
condition is *holding no name*, not *holding no account*, and a parameter that is not there cannot
be consulted.

**A press made before the profile read has landed sends at once and does not ask.**
`useProfileStrip()` answers `null` until the read settles and wherever no provider is above
(`profile-provider.tsx:72-74`), so at that moment the client does not know whether this player holds
a name. Named here and **refused rather than registered** (`ADR-0105` §6's route), because every
merged rule points one way: asserting nothing beats asserting a *no* we were never told; `ADR-0119`
§2's *"no state of this screen ever leaves a player unable to duel"* refuses a spinner on the path
into a duel; and being **not asked spends nothing** (§5), so the player meets the ask at their next
press. The alternative — holding the press until the read settles — buys one earlier ask and pays
with a stall on the one path the vision's success condition is about. `TASK-140716` pins the
behaviour with a test so it is recorded rather than accidental. A human who wants the other answer
gets it for one `DEC` and one `useEffect`.

The same answer covers `no-profile` (there is no profile to name) and `unavailable` (the read
failed), for the same reason.

### The floor is arithmetic, and it is the whole of the rarity guarantee

`ADR-0137` §4's first test: the **product of the list lengths** is `>= SUGGESTION_SPACE_FLOOR`
(`1_000_000`). That is how *"a name whose refusal is rare"* (`ADR-0119` §4) is bought without ever
asking whether a name is free — with `k` strings ever spent from inside the vocabulary a fresh draw
is refused about `k / N` of the time.

**The shape is three lists — 50 × 100 × 200 = exactly 1,000,000.** Three is the arity a three-word
name has, and it is the cheapest one that reaches the floor: two lists would need two thousand words
and four would put a fourth word on a public ladder. The split across the three is a curator's
convenience — the arity is data (`ADR-0137` §3) — chosen so the **hardest list to write is the
shortest** and so each data ticket stays inside a ticket's line budget.

| List | Length | Register | Example |
| --- | --- | --- | --- |
| 0 | 50 | a quality | `Quiet`, `Cold` |
| 1 | 100 | a substance or element | `Iron`, `Ash` |
| 2 | 200 | a figure — a hundred beasts and a hundred offices | `Raven`, `Warden` |

joined by one `U+0020` into `Quiet Iron Raven`. **Four tickets**, because prettier prints one string
per line — **measured** — so a hundred entries is a hundred lines and 350 words cannot be one `S`
ticket. Exactly at the floor is deliberate: **one deleted word puts the vocabulary under it**, which
is the property `ADR-0137` §4 says a convention could never hold.

**The count is honest, not nominal.** The product counts distinct *tuples*; two entries in one list
differing only by case are one name under `ADR-0029` §1's fold, so a per-list *no case-fold
duplicate* test is what makes the product a real count of reachable names. Distinct tuples give
distinct strings because no entry contains a space (below), so nothing else can collapse.

### Why a suggestion can be refused with `409` and never with `400`

This is the strongest claim in `ADR-0137`, and it is proved rather than restated.
`canonicalDisplayNameOrNull` (`poker-server/src/main/kotlin/duels/poker/server/http/DisplayName.kt`,
read at `9dd8571c`) has five ways to answer `null`, and each is closed by a named test over the
**entries**, in `O(entries)`, without enumerating the million:

| The server's refusal (`DisplayName.kt`) | What forecloses it |
| --- | --- |
| `codePoints < 1` after trim + NFC | `everyEntryIsANameTheServerWouldAccept` — the regex's `+` refuses an empty entry, so no join is empty |
| `codePoints > 32` | `theLongestNameFits` — the sum of each list's longest entry in **code points** (`[...entry].length`, never `.length`) plus one per separator is `<= 32`; and `noEntryIsLongerThanTenCodePoints`, which is the rule a curator can break in a single word |
| a `Cc` or `Cf` code point | the same regex: `/^[^\p{Cc}\p{Cf}\s\p{Z}]+$/u` |
| whitespace other than `U+0020` (`Character.isSpaceChar`, i.e. `Zs`/`Zl`/`Zp`) | the same regex's `\s` and `\p{Z}` |
| two consecutive `U+0020` | no entry contains **any** space, so the joiner's single separator can never double, and no join carries a leading or trailing space either — the server's `trim()` is then a no-op rather than a silent rewrite |
| — (not a refusal, but a silent rewrite) | `everyEntryEqualsItsOwnNfcForm`; and the join of NFC entries across a `U+0020` is itself NFC — **probed, not assumed**: `"a" + " " + "́x"` is unchanged by `normalize("NFC")`, because no canonical composition pairs `U+0020` with a combining mark |

Refusal by `409` stays exactly what `ADR-0119` §4 priced at one press, and it is the only refusal a
suggestion can meet. `TASK-140702` carries the mutation probes that make each of those tests bite —
a spaced entry, a decomposed entry, an eleven-code-point entry, a deleted word and a case-duplicate
— because a test that has never been seen to fail is a claim, not a gate. `TASK-140706` then asserts
the same rules once more over the **joined string**, so the claim is made at the output as well as
over the entries.

### The reroll costs nothing, and `ADR-0134` §6 is why it stays that way

A reroll is a **redraw in the browser**: no request, no frame, no route. Measured on `develop`,
`PUT /api/me/name` has no budget at all today (`ProfileRoutes.kt:93-125` — identity, body, canonical
form, three outcomes, no meter), and `ADR-0134` §6 adds one keyed by `PlayerId` that **meters the
string that was spent** — `admit` before the write, `refund` on anything that is not `NameSet` — so
a `409` refunds and a player unlucky twice is not one press closer to a `429`. **No ticket in this
story opens a Kotlin file**, so that clause is untouched here; it is `STORY-1409`'s to build, and
`ADR-0119` §4's *"costs one press, never a dead end"* depends on it landing there.

### The reroll fires on `conflict` alone, and on an equality rather than a flag

`ADR-0137` §5: the surface keeps the last string the generator put in the field; on `conflict`, if
the field still equals that string it is replaced with `suggestName(random, thatString)`, and if it
does not it is left alone. That is *"a string the player typed themselves is never overwritten"*
implemented as an equality, so there is no dirty flag anybody has to remember to clear. No other
outcome rerolls: `rejected` cannot describe a suggestion at all (the table above), and the rest are
not about the name.

### Nothing recorded, nothing drawn twice

One draw per mount, as the field's `useState` initialiser and never in a render body (`ADR-0137`
§5) — a value that changed on every render is not an initial value. **Nothing is stored**: no key,
no column, no response field, so a player shown the ask again gets a fresh draw. The one key this
story does add is the **skip**, and it is a browser fact that never travels (`ADR-0119` §5): no
request, no column, nothing on `GET /api/me`.

### No drive and no recorded frame reaches the ask — measured

`ADR-0137` §7 warns that a recorded-frame drive reaching an un-injected draw could never be
byte-identical twice. It cannot happen here: `src/e2e/drive-duel.tsx:232-235` mounts
`<DuelProvider><Lobby /></DuelProvider>` with **no `ProfileProvider`**, so `useProfileStrip()` is
`null` and the predicate is false; and no file under `src/e2e/` presses a front-door control at all
(`whole-duel.test.tsx:62` only *queries* for `Play duel`, to prove the lobby is gone). **No file
under `web-client/src/e2e/` is edited by this story and `scripted-duel.gen.json` is byte-unchanged.**

### The merged press tests stay green, and the one file that does not is known by name

Every merged test that presses `Play duel` or `Join the duel` lives in `Lobby.test.tsx` and renders
through `renderLobby()` — `DuelProvider` only, no `ProfileProvider` — so the predicate answers false
and the send is immediate, exactly as today. `renderLobbyWithProfile()` mounts a profile but presses
nothing. **Measured with a probe applied: no merged assertion in that file moves.**

**`App.test.tsx` is the exception, and it was probed rather than guessed.** Its `vi.mock("./main", …)`
is an explicit factory, not a spread of the real module, so *any* export `Lobby` imports from
`../main` that the factory does not name throws at render:

> `Error: [vitest] No "nameAskSkippedHere" export is defined on the "./main" mock.`

Measured with a one-line probe: **24 of `App.test.tsx`'s 36 tests fail**, whole suite
`24 failed | 1159 passed (1183)`, and every failure is in that file. That is why `TASK-140714`
exists and lands **before** the wiring: a mock factory may name an export before anything imports
it, so the seam and the wiring are two green tickets instead of one `atomic:` one.

### The card comes first, and what it owes

`ADR-0091` §2, and `EPIC-14`'s *Design first* names this surface first: **the product's first
dialog**. It is *composing*, not minting — the token sheet, the frame, the field and the two control
treatments all exist — so it is an ordinary dispatched ticket whose visual verdict is the human's and
may trail the merge (`ADR-0091` §3, `ADR-0024` §3).

Four frames, and the fourth is the one that would otherwise never be drawn:

1. **the suggestion offered** — the field holding a drawn name, both ways out;
2. **a name typed** — the player's own string in place of the suggestion;
3. **the name is not available** — the shipped refusal sentence, a fresh suggestion in the field,
   both ways out still there;
4. **nothing reached the server** — a refusal `mayTryAgain` answers `false` to: the form withdrawn,
   the skip still standing and still working. This is the frame that draws `ADR-0119` §2's *"no state
   of this screen ever leaves a player unable to duel"*.

**Two controls and no third.** The card draws no *suggest another* button. `ADR-0137`'s *What this
does not settle* hands that question to the card, and the answer is no: the field is editable and
pre-filled, so a player who dislikes the draw types over it; a third control on the product's first
dialog competes with the two `ADR-0119` §2 makes equally reachable; and the one case that needs a
fresh draw — `conflict` — already gets one automatically. Additive later, and named in *Out of scope*
rather than registered.

**No wordmark.** `ADR-0098` §1's lockup is card-drawn on the front door's **pre-create branch**
(`Lobby.tsx:389-401`), and the ask is a different screen — the account and sign-in screens carry no
wordmark either. The ask renders exactly one heading, an `<h2>`, the level every screen this client
renders already uses.

## Tasks

Split on **2026-09-08**. **Sixteen tickets, one linear chain** — the words come from the card, the
generator from the vocabulary, the screen from both, and the wiring from all of them, so exactly one
ticket is startable at a time. Nothing here is `atomic:`; every ticket touches at most three files
and every intermediate state is green. Four of the sixteen are word lists, which is what
`ADR-0137`'s *"somebody must author hundreds of words before `STORY-1407` can merge"* costs when a
formatter prints one string per line.

| Ticket | Est | What it is |
| --- | --- | --- |
| [`TASK-140701`](../tasks/TASK-140701-the-card-draws-the-ask-four-frames-and-two-ways-out.md) | S | The card: four frames, two controls, and the words every ticket below transcribes |
| [`TASK-140702`](../tasks/TASK-140702-the-vocabulary-module-and-the-tests-that-make-a-400-impossible.md) | S | `name-vocabulary.ts`, fifty qualities, and the six tests that make a `400` impossible |
| [`TASK-140703`](../tasks/TASK-140703-the-second-list-is-a-hundred-substances.md) | S | The second list — a hundred substances |
| [`TASK-140704`](../tasks/TASK-140704-the-third-list-takes-its-beasts.md) | S | The third list opens — a hundred beasts |
| [`TASK-140705`](../tasks/TASK-140705-the-third-list-takes-its-offices-and-the-arity-reaches-a-million.md) | S | A hundred offices, and the arity reaches exactly a million |
| [`TASK-140706`](../tasks/TASK-140706-the-floor-and-the-draw.md) | S | `SUGGESTION_SPACE_FLOOR`, `suggestName(random)`, the floor test, the clamp, and a draw the server accepts |
| [`TASK-140707`](../tasks/TASK-140707-the-redraw-is-bounded-and-only-replaces-what-it-drew.md) | S | `replacing`: a draw equal to it is drawn again, at most four draws in total |
| [`TASK-140708`](../tasks/TASK-140708-one-module-owns-the-skipped-key.md) | S | `name-ask-skipped.ts` and its row in the storage-key gate |
| [`TASK-140709`](../tasks/TASK-140709-one-predicate-decides-who-is-asked.md) | S | `askForName` — one condition, four refusals, and no `signedIn` |
| [`TASK-140710`](../tasks/TASK-140710-the-asks-words-are-transcribed-from-the-card.md) | S | `name-ask-text.ts`, golden literals gated against the merged card |
| [`TASK-140711`](../tasks/TASK-140711-the-ask-draws-itself-and-its-two-ways-out.md) | S | `NameAsk.tsx`: the suggestion in the field, the write, the skip that sends nothing |
| [`TASK-140712`](../tasks/TASK-140712-a-refusal-reads-in-the-words-the-product-already-uses.md) | S | The refusal sentence, the withdrawn form the skip survives, one write per two presses |
| [`TASK-140713`](../tasks/TASK-140713-a-conflict-redraws-only-what-the-generator-put-there.md) | S | The reroll on `conflict` alone, over an equality rather than a flag |
| [`TASK-140714`](../tasks/TASK-140714-the-browsers-answer-is-bound-at-the-boot-seam.md) | XS | `main.tsx` binds the key; `App.test.tsx`'s factory mock names it — the 24 failures, prevented |
| [`TASK-140715`](../tasks/TASK-140715-the-press-is-held-until-the-ask-is-answered.md) | S | `Lobby.tsx`: the held frame, the two handlers, the branch, the release |
| [`TASK-140716`](../tasks/TASK-140716-the-moments-the-ask-never-stands.md) | S | The refusals, pinned: a named player, a removed name, a skipped browser, a read that has not landed, and a frame that takes the screen |

## Acceptance criteria

- [ ] A player holding no name who presses `Play duel` sees the ask in place of the front door and
      **no** `CreateRoom` is sent until they answer; the same for `Join the duel`, which carries its
      code through the answer unchanged
- [ ] Taking a name sends the shipped `PUT /api/me/name` and then the held frame; skipping sends
      **no** request at all and then the held frame
- [ ] The ask never stands for a player who holds a name, whose name was removed, in a browser that
      already skipped, before the profile read has landed, or without a press — each pinned by its
      own test
- [ ] `NAME_VOCABULARY`'s list lengths multiply to `>= 1_000_000`, and deleting one word reddens that
      test
- [ ] Every entry is non-empty, equals its own NFC form, matches `/^[^\p{Cc}\p{Cf}\s\p{Z}]+$/u`, is
      at most ten code points, and no list repeats a name under a case fold — each with a mutation
      that reddens exactly the test that owns it
- [ ] `suggestName` draws one entry per list in order, clamps a `random` that returns `1`, joins with
      a single `U+0020`, and returns a string no branch of `canonicalDisplayNameOrNull` refuses
- [ ] A `conflict` replaces the field only when it still holds the string the generator put there,
      and never a string the player typed
- [ ] `PERMANENCE_LINE` appears in no file this story writes, and the ask's refusal sentences come
      from `name-text.ts` rather than from a second vocabulary
- [ ] `pd.nameAskSkipped` appears in exactly one production source file, held by
      `one-module-owns-each-storage-key.test.ts`
- [ ] No Kotlin file, no file under `web-client/src/e2e/`, no `docs/protocol.md` section and no
      migration is touched; `scripted-duel.gen.json` is byte-unchanged and `PROTOCOL_VERSION` does
      not move
- [ ] `cd web-client && npm run check` exits 0, `./design/check-drift.sh` exits 0, and
      `python3 .github/scripts/lint_tickets.py` exits 0

## Out of scope

- **Moving `NameSurface` to the account screen**, and the rename form itself. `ADR-0130` §6 —
  `STORY-1409`. This story leaves `Lobby.tsx:433-436` exactly as it found it.
- **Retiring `PERMANENCE_LINE` and the `permanent` refusal from `name-text.ts`**, and the `403`
  `ADR-0134` deletes. Both are `STORY-1409`'s, in the diff that makes them false. The ask imports the
  two helpers and needs no edit when they change.
- **The rename's budget, the `429`, and `ADR-0134` §6.** Server work, `STORY-1409`. Nothing here
  sends a request to reroll, so nothing here can spend it.
- **A *suggest another* control.** Refused in *Design notes* on `ADR-0119` §2 and `ADR-0137` §5;
  named rather than registered (`ADR-0105` §6), additive later, and one line in `NameAsk.tsx` if the
  pane asks for it.
- **Asking the invited rival.** `ADR-0119` §1 forbids it by name and its *Consequences* books the
  cost. The boot path is not opened.
- **A modal.** `ADR-0119` §1: this product has no modal, `DEC-138` owned the question and
  [`ADR-0123`](../../docs/adr/ADR-0123-a-standing-rematch-offer-follows-the-rival.md) answered it for
  the rematch panel without minting one.
- **Any availability check, hint, debounce or pre-registration.** `ADR-0029` §5, `ADR-0051` §9 and
  `ADR-0137` §8 each refuse it; the only authority on availability is the write.
- **A second vocabulary, a locale variant, a server copy, or anything that measures how much of the
  vocabulary has been spent.** `ADR-0137` §8.
- **`docs/test-plan.md` and the QA catalogue.** `EPIC-14`'s per-epic suite is the `qa-cases` skill's
  to write from the epic's *Definition of done* — one case per promise the epic made, not one per
  ticket.
- **Pushing the new card to the design pane.** The `DesignSync` push, including the
  `_ds_manifest.json` entry a new card needs, is the human's and is invoked by them.
