---
id: STORY-1404
title: The waiting table copies its link, says the duel starts itself, and stops labelling the host
type: story
status: done
parent: EPIC-14
module: web-client
labels: [client, design, table, lobby]
depends_on: [STORY-1403]
---

## Goal

The host waiting alone at the table can get the invite link into their clipboard **in the browser
they are actually playing in**, is told that the duel begins by itself when the rival arrives, and
is no longer labelled `You` on their own plate.

## Why it was blocked, and what the split found

**Both questions are answered and merged**, on 2026-09-06:
[`ADR-0128`](../../docs/adr/ADR-0128-the-copy-control-is-never-absent-and-a-press-that-cannot-copy-hands-over-the-selection.md)
for `DEC-140` and
[`ADR-0129`](../../docs/adr/ADR-0129-the-host-is-told-the-duel-starts-by-itself.md) for `DEC-141`.
What follows is the record of why they had to be asked; the answers are read into *Design notes* and
*Tasks* below.

`EPIC-14`'s *Scope* row 3b judged this item *"a card"* and no decision. **Splitting it says
otherwise on two of its three annotations**, and both are questions about clauses of `ADR-0110` that
merged four days ago.

### The copy button already exists — and the photograph is the proof it was suppressed

`web-client/src/table/InvitePanel.tsx` renders the bare code, the `Invite link` label, the
selectable read-only box, and `Copy the link` with its two feedback lines `Link copied.` and
`Copy it from the box above.` — rendered by `WaitingTable.tsx:27`, exactly as `ADR-0110` §5 moved
them. `CopyLink` returns `null` when `!navigator.clipboard` (`InvitePanel.tsx:32`), which §5 fixes as
behaviour — *"the control absent where `navigator.clipboard` is"* — and §8.1 draws as the fourth of
its four named host-alone variants.

`navigator.clipboard` is exposed only in a **secure context**. The origin in the box the human
photographed is transcribed in the epic itself: `http://192.168.0.142:5173` — plain `http` to a
private address, which is not a secure context in Chrome or in Safari. So the control was absent, by
the merged design, on **both** devices for the whole of that evening's play.

The human is therefore not asking for a control that was never built. They are asking for the one
the merged rule hides in the environment they played in, and the options — a fallback copy path, a
different treatment of the no-clipboard variant, or serving the stack from a secure origin and
leaving the rule alone — differ in what the product promises, not in how it is coded. That is
**`DEC-140`**, the product owner's.

### The auto-start sentence is the eleventh string on a state whose set is closed at ten

`ADR-0110` §6 enumerates every string the host-alone table renders, calls the enumeration
**exhaustive**, and names the way out: *"A state that needs one more string has outgrown this
decision, and the answer is a new ADR, not an invented sentence."* §7 then chose silence **at the
arrival**, and its *Alternatives* rejected a *Your rival has joined* announcement by name — partly
because *"the string does not exist, so inventing it here would break §6 in the same breath that
creates it."*

The human asks for a sentence that stands **before** the rival arrives, on the `Waiting for your
rival` bar, which is neither the arrival announcement §7 refused nor a string §6 allows. That is
**`DEC-141`**, the product owner's.

### Only the third annotation is decision-free, and it waits with the others anyway

`You`, crossed out on the host's own plate. `ADR-0110` §§2–3 say the host's seat **may** carry its
shipped name, so removing it contradicts nothing and adds no string. But it changes the same four
card frames the other two answers change, and drawing them for one plate now and again when the
decisions land is `ADR-0110`'s own deadline argument in miniature: *"the card cannot be drawn until
the product says what the frames contain — deciding after it is drawn costs the drawing twice."*

## Design notes

Merged, and not re-litigated by any ticket under this story whatever the decisions answer.

- **`ADR-0073` §§1–3's two promises stay verbatim**, wherever the frames put them: `Back to the
  lobby` calling `forgetRoom()`, and `The room stays open. That link still works for your rival, and
  it brings you back.` `ADR-0105` §2 leans on the second being **said**, not merely being true.
- **`ADR-0110` §3: no game fact before the `Snapshot`.** Nothing this story adds may be a number, a
  stack, a blind, a card, a pot or a bar, and nothing prints a duration, countdown or expiry
  (`ADR-0072` §6) — including any sentence about how long the room lives.
- **`ADR-0022`'s no-oracle rule**: nothing is said *about* the code.
- **All three invite parts stay** (`ADR-0110` §5): each is the fallback for the next, and the box is
  the whole invite where a copy path fails. Whatever `DEC-140` answers, no answer removes the box.
- **`ADR-0110` §7 stands unless an ADR moves it**: the arrival itself is silent. `DEC-141` asks about
  a sentence that stands while waiting, which is why it is a new question and not §7 reopened.
- **The card frames are `design/screens/duel-table.html`'s** — the host-alone variants at lines 491,
  520, 548 and 576, each drawing `You` on the host's plate — and they are redrawn **once**,
  together, now that both answers have merged. `ADR-0128` §6 retires the fourth (`Host alone — no
  clipboard API`) with the state it drew, so **three** frames survive, not four. The live-table
  frames in the same file keep their `You`: the human crossed it out on the waiting table and
  nowhere else.
- **The card is the first ticket** once the answers are merged (`ADR-0091` §2, `ADR-0110` §8), and it
  merges before any implementing ticket is startable.

### The tests this story moves

Two merged tests pin the exact string set of this state and **must** move with whatever the answers
add or remove, deliberately and by name:

- `web-client/src/lobby/Lobby.test.tsx:1296` — *"states the six strings the host-alone table renders
  with no clipboard, and no seventh"*. Removing `You` alone makes it five. **At `1332` on `develop`
  today**: `TASK-140302` added three tests above it, and `ADR-0128` and `ADR-0129` both cite the
  older number.
- `web-client/src/table/null-view.test.tsx:203` — the `BASELINE` array plus `Copy the link`, swept
  from text nodes and `aria-label`/`title`. **The array is at `191`–`198` on `develop` today**, its
  `"You"` at `195`, for the same reason.
- A third, found at split time and not named above: `Lobby.test.tsx:424`, *"still shows the whole
  waiting screen to a recovering browser told only that it holds a room"*, which enumerates the same
  screen a second way and asserts `getByText("You")`.

Also `web-client/src/table/InvitePanel.test.tsx` and `Lobby.test.tsx`, which assert the copy
control's **absence** without a clipboard — the assertion `ADR-0128` §1 inverts, in both files, and
neither is deleted quietly. Corrected at split time against the tree: `WaitingTable.test.tsx` was
listed here as a third such file and is not one; it installs a clipboard and asserts only presence,
so `TASK-140402` does not open it. `null-view.test.tsx` is not one either — its only exhaustive text
sweep installs a clipboard in both arms — which is why the copy-control ticket is three files and
not four.

## Tasks

Split into three on 2026-09-07, both answers merged. The order is forced, and it is the whole shape
of this story: **the card first, carrying all three annotations at once**, then the two client
tickets, serialised because they edit the same test files.

| Task | What it settles |
| --- | --- |
| [TASK-140401](../tasks/TASK-140401-the-card-redraws-the-waiting-table-three-frames-no-you-and-the-sentence.md) | The card. One file, three changes, drawn **once**: the `Host alone — no clipboard API` frame is retired (`ADR-0128` §6), the three survivors lose `You` from the host's plate, the hand-over frame shows the link selected, and the sentence's **exact glyphs are fixed here** — `ADR-0129` §4 pins no literal, so this is where it is decided and every later ticket transcribes it |
| [TASK-140402](../tasks/TASK-140402-the-copy-control-is-never-absent-and-a-press-that-cannot-copy-hands-over-the-selection.md) | `CopyLink` loses its `!navigator.clipboard` early return and its `\| null` return type; a press that cannot copy — no API, or a refused write, one outcome under `ADR-0128` §3 — focuses the invite box, selects the whole link and renders `Copy it from the box above.` Three files: `InvitePanel.tsx`, `InvitePanel.test.tsx`, `Lobby.test.tsx` |
| [TASK-140403](../tasks/TASK-140403-the-host-plate-goes-bare-and-the-table-says-the-duel-starts-itself.md) | `You` leaves the host's plate and the sentence lands, in one `atomic: 4` ticket, because a merged gate refuses every intermediate: `WaitingTable.tsx` plus the three merged test files that enumerate this state's strings |

**Which ticket owns which string-set edit, and in what order.** The two merged enumerations —
`Lobby.test.tsx:1332` and `null-view.test.tsx:203` — are the trap in this story, because adding a
string without moving them fails and moving them without the card invents a literal.

1. `TASK-140401` merges. Nothing in `web-client/` has moved; the enumerations still pass.
2. `TASK-140402` adds `"Copy the link"` to `Lobby.test.tsx`'s array — **six strings become seven**,
   and the test is renamed accordingly. `null-view.test.tsx` is **not** touched: its `BASELINE`
   already appends `"Copy the link"` in the expression, and both of its arms install a clipboard.
3. `TASK-140403` replaces `"You"` with the card's sentence in **both** arrays — one member out, one
   in, so `Lobby.test.tsx`'s title stays at seven — and rewrites `Lobby.test.tsx:424`, which names
   `"You"` in its own enumeration of the whole waiting screen.

**The fallback path is the one no gate has ever run**, and this story is where it gets one.
`navigator.clipboard` is undefined on the plain-`http` LAN origin the human plays on and defined on
`http://localhost:4173`, where `ADR-0117` puts every proof of record — so the shipped absence was
invisible to the whole instrument set and was found by a photograph. The fixture that reaches it is
not a mock: **jsdom implements no Clipboard API at all**, so a test that simply does not call
`withClipboard` is that browser, and both files' existing
`afterEach(() => Reflect.deleteProperty(navigator, "clipboard"))` is the guard against leakage from
a test that did. Each no-API test opens with `expect(navigator.clipboard).toBeUndefined()` so a
future jsdom that grows a stub cannot let it pass while testing the wrong browser. And every
hand-over assertion first breaks the box's own precondition — the input carries `autoFocus` with
`onFocus` → `select()`, so at rest it is **already** focused with the link **already** selected, and
an assertion made without `setSelectionRange(0, 0)` and a focus move would pass on a press that did
nothing.

**What no automated gate here can prove**, said plainly rather than left implied (`ADR-0128`
*Consequences*): that a phone on a LAN origin actually copied anything. What is proved is that the
control renders with no API, that a press selects the box and says so, and that the whole screen
does the same through `Lobby.tsx`. The device reading is the human's, on the same pass as the card's
visual verdict, and it is not ticketed because no dispatched agent can hold the phone.

## Acceptance criteria

- [x] `DEC-140` and `DEC-141` are answered by merged ADRs, each naming the clause of `ADR-0110` it
      amends or upholds — §§5 and 8.1 for the control, §6 for the sentence — and `docs/adr/README.md`
      lists neither as open
- [ ] The host-alone frames in `design/screens/duel-table.html` are redrawn **once** into **three**
      variants, carrying the sentence and carrying no `You` on the host's plate, with
      `./design/check-drift.sh` exiting 0, merged before any implementing ticket is startable
- [ ] A host waiting at the table, in a browser that exposes no Clipboard API, is offered
      `Copy the link` and a press leaves the whole link selected in the box under the shipped
      `Copy it from the box above.` — `ADR-0128` §3's promise, driven by a test with the API absent
- [ ] The host's own plate renders no `You`
- [ ] The set of strings the host-alone state renders equals `ADR-0110` §6's enumeration as the
      answering ADRs amend it, asserted by the two string-set tests above, each moved deliberately
      with no assertion weakened
- [ ] `Back to the lobby` and `The room stays open. That link still works for your rival, and it
      brings you back.` still render, byte-identical
- [ ] `cd web-client && NO_COLOR=1 npm run --silent check` exits 0

## Out of scope

- **The rival's arrival.** `ADR-0110` §7's silence is not this story's to break; `DEC-141` asks only
  about what stands on the screen **while the host waits**.
- **The room's lifetime and the second room.** Ten minutes is `RoomTimeouts.DEFAULT_WAITING_MILLIS`
  and returning to a room already held is `DEC-139`'s and `STORY-1416`'s. Nothing here may print a
  duration.
- **The bare code, the link box and the `Invite link` label.** They move nowhere and lose nothing.
- **The front door** — `STORY-1402` and `STORY-1403`.
- **The phone fit** — `DEC-136`, `STORY-1405`. The waiting table's own fit at 390 × 664 is the card's
  to prove under `ADR-0103`, exactly as `ADR-0110` §8 already requires.
- **Serving the dev stack over TLS.** If `DEC-140` answers that the shipped absence is right and the
  environment is what should change, what serves the bundle is `DEC-126`'s, which `EPIC-14` already
  places out of scope.
