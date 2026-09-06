---
id: STORY-1404
title: The waiting table copies its link, says the duel starts itself, and stops labelling the host
type: story
status: blocked
parent: EPIC-14
module: web-client
labels: [client, design, table, lobby]
depends_on: [STORY-1403]
---

## Goal

The host waiting alone at the table can get the invite link into their clipboard **in the browser
they are actually playing in**, is told that the duel begins by itself when the rival arrives, and
is no longer labelled `You` on their own plate.

## Why it is blocked, and what the split found

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
- **The four card frames are `design/screens/duel-table.html`'s** — the host-alone variants at
  roughly lines 490–590, each drawing `You` on the host's plate — and they are redrawn **once**,
  together, after both answers merge. The live-table frames in the same file keep their `You`: the
  human crossed it out on the waiting table and nowhere else.
- **The card is the first ticket** once the answers are merged (`ADR-0091` §2, `ADR-0110` §8), and it
  merges before any implementing ticket is startable.

### The tests this story moves

Two merged tests pin the exact string set of this state and **must** move with whatever the answers
add or remove, deliberately and by name:

- `web-client/src/lobby/Lobby.test.tsx:1296` — *"states the six strings the host-alone table renders
  with no clipboard, and no seventh"*. Removing `You` alone makes it five.
- `web-client/src/table/null-view.test.tsx:203` — the `BASELINE` array plus `Copy the link`, swept
  from text nodes and `aria-label`/`title`.

Also `web-client/src/table/WaitingTable.test.tsx` and `InvitePanel.test.tsx`, which assert the copy
control's presence with a clipboard and its **absence** without one — that second assertion is the
one `DEC-140` may invert, and it is not to be deleted quietly.

## Tasks

**Not written.** No ticket under this story may be split until `DEC-140` and `DEC-141` are answered
by merged ADRs: the card cannot be drawn against an unanswered question, and every ticket here
transcribes the card.

## Acceptance criteria

- [ ] `DEC-140` and `DEC-141` are answered by merged ADRs, each naming the clause of `ADR-0110` it
      amends or upholds — §§5 and 8.1 for the control, §6 for the sentence — and `docs/adr/README.md`
      lists neither as open
- [ ] The four host-alone frames in `design/screens/duel-table.html` are redrawn **once**, carrying
      whatever the answers add and carrying no `You` on the host's plate, with
      `./design/check-drift.sh` exiting 0, merged before any implementing ticket is startable
- [ ] A host waiting at the table, in a browser served from a plain-`http` LAN origin, gets the
      invite link into their clipboard by whatever `DEC-140` chose — **or** that decision upheld the
      shipped absence, in which case this story ships no control and says so in its close
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
