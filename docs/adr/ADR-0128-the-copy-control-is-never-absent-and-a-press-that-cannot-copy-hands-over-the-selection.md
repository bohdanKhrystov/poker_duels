# ADR-0128 — The copy control is never absent, and a press that cannot copy hands over the selection

- **Status:** Accepted
- **Date:** 2026-09-06

- **Resolves:** `DEC-140` — does the invite offer a copy control in a browser that exposes no
  Clipboard API, and if so by what promise? Registered open 2026-09-06 by the planner splitting
  [`EPIC-14`](../../tasks/epics/EPIC-14-the-name-the-showdown-and-the-fit.md) item 3b, from the
  annotation *copy btn* on the invite-link box of `edits2.png`.
- **Where the answer came from:** the human's annotation is the **report** — one word, pointing at
  a box on a screen — and it is not a specification, so what a control does where it cannot copy is
  decided here, from [`docs/vision.md`](../vision.md). Two of its sentences license it. The success
  condition: *"**Send a link.** She opens it in a browser. We play a full heads-up match"*, followed
  by *"Everything else is downstream of that moment"* — getting the link out of the host's hands is
  the one interaction the product is built around, and this is the only control that names that act.
  And *Why this exists*: *"the author wanted to play quick heads-up duels against his sister"* —
  which happens today on a home network over plain `http`, the exact environment in which the
  shipped rule hides the control. *Positioning* — *"Dark, quiet, fast, minimal"* — settles the
  shape: one control, no explanatory paragraph, no second name for one act.
- **Amends:** [`ADR-0110`](ADR-0110-creating-a-duel-seats-the-host-at-the-table.md) in two clauses
  and nowhere else. §5's *"the control absent where `navigator.clipboard` is"* is **struck** (§1
  below); §8.1's fourth named host-alone variant — *"with no clipboard API, where the copy control
  is absent and the box is the invite"* — is **struck** with the state it drew (§6 below). The rest
  of §5 stands byte-unchanged: all three invite parts render, with their shipped strings, the
  no-oracle rule and the no-duration rule. §6's ten-string enumeration stands **byte-unchanged as a
  set** and its escape clause is not spent (§5 below). §§1–4 and §7 are untouched.
- **Applies:** `ADR-0110` §5's own reasoning that each invite part is the fallback for the next;
  [`ADR-0022`](ADR-0022-the-room-code-is-the-invite.md) (nothing is said *about* the code);
  [`ADR-0072`](ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md) §6 (no duration,
  countdown or expiry); [`ADR-0024`](ADR-0024-design-follows-the-code-workflow.md) §3 and
  [`ADR-0091`](ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md) §2 (the drawing is the
  card's and the human's, and the card comes first);
  [`ADR-0103`](ADR-0103-the-table-fits-the-phone-and-the-cards-give-before-the-numbers.md) and
  [`ADR-0121`](ADR-0121-the-table-reflows-never-scales-and-the-shape-is-measured-on-the-device.md)
  (the surface this control lives on is size-budgeted and its fit is measured on the device).
- **Constrains:** [`STORY-1404`](../../tasks/stories/STORY-1404-the-waiting-table-copies-its-link-and-says-the-duel-starts-itself.md)'s
  card ticket and every ticket that transcribes it; `web-client/src/table/InvitePanel.tsx`, the one
  render site of the invite (`WaitingTable.tsx:27`); and the host-alone frames of
  `design/screens/duel-table.html` (lines 491–590). It constrains **no Kotlin, no wire type, no
  protocol version and no stored data**: the link is composed client-side by `roomLink()` from the
  code `RoomJoined` already carries.
- **Leaves open:** `DEC-141` (whether the host-alone table says the duel starts by itself —
  `STORY-1404` stays blocked on it); `DEC-126` (what serves the built bundle, untouched in both
  directions, §8 below); and `DEC-150`, registered here for the architect (§7 below), which blocks
  nothing.

## Context

**The control the human asked for already ships, and it was hidden on both of their devices.**
`InvitePanel.tsx` renders the bare code, the `Invite link` label, a selectable read-only box, and
`Copy the link` with its two feedback lines `Link copied.` and `Copy it from the box above.`
`CopyLink` returns `null` when `!navigator.clipboard` (`InvitePanel.tsx:29-32`) — the behaviour
`ADR-0110` §5 fixed and §8.1 drew as the fourth of four host-alone variants. `navigator.clipboard`
is exposed only in a secure context; the origin transcribed in `EPIC-14`'s own capture table is
`http://192.168.0.142:5173`, a private address over plain `http`, which is potentially trustworthy
in neither Chrome nor Safari. The control was absent by the merged design, on the laptop and on the
phone, for the whole of that evening's play. **The product owes a promise here, not a button** — the
button exists.

**The invite is reachable and silent.** Nothing is broken in the strict sense: `ADR-0110` §5 built a
three-part chain on purpose, the component's own comment says *"the invite is selectable text before
it is anything else: the one interaction this product depends on cannot need a working clipboard"*,
and the box is still there with the link in it. But absence says nothing. The host is not told the
box is the invite, is not told to select it, and `ADR-0110` §6 closes the string set so no sentence
may tell them. A control is how a product says *this is the thing to do with this box*, and it is
exactly the control that disappears.

**No label can promise a copy in any browser.** Where the Clipboard API exists, `writeText` can
still be refused — which is why `Copy it from the box above.` was written in the first place. So
`Copy the link` was never a guarantee of an outcome; it is the name of an intent, and the truth
about what happened is told by the feedback line after the press. Any argument that the control must
be hidden because it *might not copy* proves too much: it would hide the control everywhere.

**The absence is invisible to every proof this project runs.** `ADR-0117` puts the proofs of record
on `http://localhost:4173`, and `localhost` is a potentially-trustworthy origin — so
`navigator.clipboard` is defined in every QA drive, every UAT round and every hand-check. The
control is present in every instrument and absent on the only device a second player has ever held.
No gate can find this, and none did; a photograph did.

**The environment cannot carry the promise yet.** `DEC-126` — what serves the built bundle, on what
origin — is open, and [`docs/architecture.md`](../architecture.md#deployment-later) says the
deployment is *"Not decided yet; not needed before v0.2"*. Answering `DEC-140` with *serve it over
TLS* would put a shipped promise inside an unmade decision. And even after that decision is made,
the founding use case in the vision's own second paragraph — two people in one house, one of them
holding a phone — runs against a LAN address today and will keep being able to.

**Against all of that: the surface is size-budgeted and the register is quiet.** `ADR-0103` and
`ADR-0121` govern the fit at 390 × 664, `ADR-0110` §8 already argued the invite's room rather than
measured it, and *Lichess, not casino* is an argument against furniture — especially a control that
sometimes cannot do what its name says.

### The deadline

`STORY-1404` cannot be split until this is answered, and its first ticket is the card. `ADR-0110`
§8.1 tells the card to draw four host-alone variants, one of them being *no clipboard API, control
absent*. Drawn as written, that frame is drawn once for this answer and again when it is reversed —
`ADR-0110`'s own deadline argument, in the same place, a second time. Nothing else is urgent: no
wire moves, no schema moves, and every string involved already ships.

## Decision

### 1. The copy control is never absent

`CopyLink` renders in every browser. There is no browser, origin or permission state in which the
invite offers the box alone. `ADR-0110` §5's clause *"the control absent where `navigator.clipboard`
is"* is struck; the rest of that section stands.

### 2. It carries one name, in every browser: `Copy the link`

The shipped string, byte-identical. One act has one name on this surface, whatever the browser can
do — the rule `ADR-0046` opens by naming and `ADR-0110` §2 applied to the rival's seat.

### 3. A press that cannot copy hands over the selection, and says the shipped sentence

Pressing `Copy the link` has exactly two visible outcomes.

- **The link was copied.** `Link copied.`, as shipped. Nothing changes on this path.
- **The link was not copied** — because the browser exposes no Clipboard API, or because the write
  was refused. The invite-link box takes focus with **the whole link selected**, and
  `Copy it from the box above.` renders. Both no-copy paths are one outcome, told in one sentence,
  because one sentence cannot mean two things.

The host's next act is their own browser's copy — a keystroke, or a long-press and *Copy* on text
that is already selected. The product's promise is therefore: **one press leaves the link either in
your clipboard or selected under your cursor, and the screen says which.** That promise is keepable
in every browser, which is why it is the one made.

### 4. `Link copied.` is said only when a copy actually happened

It is never said of a selection, and never of an attempt whose success the client cannot read. This
product does not claim an act it did not perform, and this rule governs every mechanism §7 may
later admit.

### 5. No string is added, and `ADR-0110` §6's enumeration stands

The state renders the same ten strings §6 enumerates. What changes is which of them are *reachable*:
`Copy the link` and its two feedback lines now render in every browser instead of only in a secure
context. §6's rule — *"a state that needs one more string has outgrown this decision, and the answer
is a new ADR"* — is untouched and unspent by this ADR.

### 6. The card owes three host-alone variants, not four

`ADR-0110` §8.1's fourth variant is struck: with the control always present, the no-clipboard state
at rest is byte-identical to the at-rest frame, and its pressed form is the third frame. The card
draws **at rest**; **after `Link copied.`**; and **after `Copy it from the box above.`**, whose
caption names both ways that frame is reached and which now shows the link selected in the box.
`design/screens/duel-table.html`'s `Host alone — no clipboard API` frame (line 576) is retired with
the state it drew. Whether and how a selection is drawn, and where the control sits relative to the
box, are taste, and taste is the human's (`ADR-0024` §3).

### 7. Whether a browser without the Clipboard API can be made to copy anyway is `DEC-150`

Registered open, **the architect's**: *by what mechanism, if any, may the client copy to the
clipboard where `navigator.clipboard` is undefined, and can that mechanism's success be read
truthfully enough to satisfy §4?* `document.execCommand("copy")` and its relatives work in an
insecure context in some browsers and not others, and their reported success is not always true.

This ADR neither requires nor forbids such an attempt. If one exists whose success the client can
actually read, `Link copied.` becomes reachable in those browsers under §4 with **no product change
and no new string**; an attempt that fails, or whose result cannot be read, falls back to §3
unchanged. **`DEC-150` blocks nothing** — §§1–6 are complete and implementable exactly as written
without it, and no ticket of `STORY-1404` waits on it.

### 8. Nothing here says how the bundle should be served

`DEC-126` is untouched in both directions. TLS is not a prerequisite for the invite, and the invite
is not an argument for TLS. If the offered deployment turns out to be `https`, this decision costs
one render branch that rarely fires; if it does not, the invite still works. That asymmetry is the
whole reason the control moves and the environment does not.

## Consequences

**What it buys.** The control the human asked for exists in the browser they asked for it in, and
the one interaction the vision's success condition is made of no longer depends on the host guessing
that a box is a link. One press does the best available thing everywhere. A whole class of defect —
*a control is invisible on the player's device and present in every instrument* — is removed rather
than left to be photographed again, which matters because localhost is a secure context and no gate
this project runs can see it. It costs no new string, no wire type, no schema and no version step,
and the card loses a frame rather than gaining one.

**What it costs.**

- **A button that sometimes does not copy.** A host on a phone over LAN presses `Copy the link` and
  gets a selection and a sentence. The line reads as a failure notice, because it is one. That is
  honest, and it is less than what was asked for; §4 is the decision to prefer saying *not copied*
  over claiming a copy.
- **The fallback is one gesture, not zero.** The browser's own copy is still the host's to perform.
  Nothing here makes a phone over plain `http` as good as a phone over `https`.
- **`Copy it from the box above.` acquires a second meaning and a spatial claim.** It now answers a
  refusal *and* a missing API, and it says *above*. If a card ever puts the control above the box,
  the sentence is false and fixing it is a new ADR's work, `ADR-0110` §6 standing.
- **Two shipped assertions invert, and one of them is the point.** `InvitePanel.test.tsx` and
  `WaitingTable.test.tsx` assert the control's **absence** without a clipboard; that becomes its
  presence, plus the selection on press. `Lobby.test.tsx:1296`'s six strings become seven, before
  `STORY-1404` removes `You`. Both move deliberately, by name, and neither is deleted.
- **Selection is checkable in the DOM; a clipboard is not.** No automated test this project runs can
  prove a phone copied anything, and the proofs of record cannot even reach the no-clipboard state.
  What is provable is that the control renders and that a press selects the box; the rest belongs to
  a hand-check that names its origin.
- **One more control on a budgeted surface, in the frame with the least room.** It was already drawn
  in three of the four variants, so the budget does not change — but it is now unconditional, and
  `ADR-0121`'s measurement on the device is where that is settled, not here.

**What it forecloses.** The invite can never again have a state with no control in it; hiding it
would be a new ADR. `Link copied.` can never become a reassurance — §4 binds every mechanism
`DEC-150` may bring. And `ADR-0110` §8.1's four-variant instruction cannot be followed as written:
the fourth frame is gone, not deferred.

## Alternatives considered

**Uphold the shipped absence, and name the environment as the thing that should change.** Its
strongest case: `ADR-0110` §5's chain is intact and deliberate — the box *is* the invite where no
clipboard works, and the component's own comment says the one interaction this product depends on
cannot need a working clipboard; the API returns by itself the moment the origin is secure;
`DEC-126` is due before anything is offered to real users; and *quiet, minimal* argues against a
control that cannot always do what its name says. Rejected because the promise would then be made
by a decision nobody has taken — `docs/architecture.md` says the deployment is *"Not decided yet"* —
while the use case the vision opens with, two people on a home network, runs on plain `http` today
and will keep being able to after any answer. And absence is silent: the host is never told the box
is the invite, and §6 forbids the sentence that would tell them. *Your origin is not secure* is not
something a product can say to a player.

**Keep the control hidden and add one explanatory line beside the box**, through `ADR-0110` §6's own
route. Its strongest case: it is the smallest possible change, it ships no control that can fail, it
tells the player exactly what the box is, and the string set grows by one under the rule written to
allow precisely that. Rejected because it answers *copy btn* with a paragraph; because it makes the
host with no clipboard do strictly more work than the host with one — read, select by hand, copy —
when a single press can do the selecting; and because it spends §6's escape hatch on a sentence the
product can avoid needing at all.

**Render the control always and make it genuinely copy through `document.execCommand("copy")`,
saying `Link copied.` everywhere.** Its strongest case: it is plainly what the human asked for; the
call is implemented in every shipping browser, works in an insecure context, and on the author's own
laptop over LAN would put the link in the clipboard on one press; and one outcome is simpler to
draw, to test and to explain than two. Rejected **as a promise, not as a mechanism**: this ADR would
be fixing behaviour the client cannot guarantee — the call is deprecated, its boolean return is not
reliably truthful, and copying from a `readonly` input through it on iOS Safari is a known-fragile
path — so the product would print `Link copied.` in cases where it does not know a copy happened,
which is the one thing §4 forbids. An ADR that describes behaviour the code cannot produce is worse
than no ADR. The mechanism is not refused; it is `DEC-150`'s, and §4 is the rule it must meet.

**Give the control a second name where the API is missing — `Select the link`.** Its strongest case:
a button that says *Copy* and does not copy is a false promise, and this product's register is
honesty about what it can and cannot do — the vision refuses to pretend even about variance.
Rejected because no label can honestly promise the outcome in *any* browser: a write can be refused
where the API exists, which is exactly why a second feedback line was written. The truth belongs
after the press, where it already lives. Two names for one act on one surface is the failure
`ADR-0046` opens by naming and `ADR-0110` §2 refused for the rival's seat, and the second name is a
string §6 does not have.

**Select the link automatically when the panel renders, and ship no control at all.** Its strongest
case: zero presses, no new state, and it is nearly free — the box already carries `autoFocus` with
`onFocus` → `select()`, so on a desktop browser the link is *already* selected when the screen
paints. Rejected because it is invisible: a selection nobody asked for explains nothing about what
to do with it and is lost to the next tap. And mobile Safari does not honour `autoFocus` on load,
so the one device the human actually held is the one device it does nothing on. A thing that happens
silently on the machine where it was least needed is a coincidence, not a mechanism.
