# ADR-0140 — The Clipboard API is the only copy this client attempts, because it is the only one that reports its result

- **Status:** Accepted
- **Date:** 2026-09-08

- **Resolves:** `DEC-150` — **the architect's** — by what mechanism, if any, may the client put the
  invite link on the clipboard where `navigator.clipboard` is **undefined**, and can that
  mechanism's success be read truthfully enough to be reported? Registered open 2026-09-06 by
  [`ADR-0128`](ADR-0128-the-copy-control-is-never-absent-and-a-press-that-cannot-copy-hands-over-the-selection.md)
  §7. **The answer is none**, and §2 below is the reason: not that `document.execCommand("copy")`
  is deprecated, but that where it would run there is no way to observe whether it worked.
- **Supersedes nothing, and amends nothing.** `ADR-0128` §7 delegated rather than decided —
  *"This ADR neither requires nor forbids such an attempt"* — and its conditional,
  *"If one exists whose success the client can actually read, `Link copied.` becomes reachable in
  those browsers under §4 with **no product change and no new string**"*, is answered here by
  finding its antecedent false. No clause of `ADR-0128`, `ADR-0110` or any other merged ADR is
  struck, narrowed or reworded by this one.
- **Applies:** `ADR-0128` §4 in full — *"It is never said of a selection, and never of an attempt
  whose success the client cannot read. This product does not claim an act it did not perform, and
  this rule governs every mechanism §7 may later admit"* — and `ADR-0128` §3's promise,
  *"one press leaves the link either in your clipboard or selected under your cursor, and the
  screen says which"*, which **§4 below** shows is the clause that settles the one option
  `ADR-0128` §4 leaves standing. Also
  [`ADR-0110`](ADR-0110-creating-a-duel-seats-the-host-at-the-table.md) §6 (*"A state that needs one
  more string has outgrown this decision, and the answer is a new ADR"*),
  [`ADR-0117`](ADR-0117-the-proofs-of-record-load-the-built-bundle.md) §1 (the proofs of record run
  on `http://localhost:4173`),
  [`ADR-0088`](ADR-0088-the-two-browser-proof-is-a-written-hand-check.md) §1 (no browser runner in
  `web-client/package.json`) and §5's *"the cheapest decision to undo wins"*, and
  [`ADR-0115`](ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md)
  §1 — *"No fact lives only in motion"* — carried one step across: a screen may not state a fact
  whose only witness is an effect nothing can observe.
- **Changes no file.** No `web-client/src` module, no test, no card, no wire type, no
  `PROTOCOL_VERSION`, no Kotlin, no schema, no string. The shipped `CopyLink` in
  `web-client/src/table/InvitePanel.tsx` already behaves exactly as this ADR requires; §1 ratifies
  it rather than asking for it, and **this ADR owes the planner no ticket**.
- **Registers no decision.** One adjacent product question is **named rather than registered**
  (§7), because nobody is working it and `ADR-0110` §6 already routes it.
- **Leaves open:** `DEC-126` — what serves the built bundle, on what origin. It is untouched in
  both directions here, exactly as `ADR-0128` §8 left it, but §5 names it as the one open decision
  that could dissolve this question rather than answer it.

## Context

**The product half is settled and shipped; only the mechanism was left.** `ADR-0128` fixed what the
player sees in every browser: the control is never absent (§1), one name (§2), two visible outcomes
with the no-copy path handing over the selection under `Copy it from the box above.` (§3), and
`Link copied.` said only of a copy that happened (§4). All of it is in the tree today —
`web-client/src/table/InvitePanel.tsx`'s `CopyLink` renders unconditionally, and its
`if (!navigator.clipboard)` branch calls `handOver()`, which focuses and selects the box and renders
the sentence. So this decision starts from a working product, not from a gap.

**What is genuinely in tension.** `document.execCommand("copy")` is implemented in every shipping
browser, runs in an insecure context, and on the author's own laptop over LAN would very probably
put the link on the clipboard on one press. That is what the human asked for, and refusing it costs
a real player a real convenience on the exact devices `EPIC-14` was opened from. Against it stands
`ADR-0128` §4, which is not a preference but a rule: `Link copied.` may be said only of a copy that
happened.

**The fact that breaks the tie is not deprecation.** It is that the page has no read-back. The only
way a page can observe the system clipboard is `navigator.clipboard.readText()` — the same
`[SecureContext]` interface whose absence defines this whole case. So in exactly the environment
where `execCommand` would be used, its effect is unobservable from inside the page, and the boolean
it returns answers a different question: whether the command was supported and enabled for this
invocation, not whether the clipboard now holds the link. A client that reported success from it
would be reporting *the call was allowed to run*, dressed as *the link is on your clipboard*.

**And the proof surface is thinner than it looks.** Measured this run rather than assumed: under
jsdom 24.1.3 — the version `web-client/package-lock.json` pins and `vite.config.ts` selects as the
Vitest environment — `document.execCommand` is not merely unimplemented, it is **absent**
(`"execCommand" in document` is `false`), as are `queryCommandSupported` and
`queryCommandEnabled`, and `navigator.clipboard` is `undefined`. Meanwhile `ADR-0117` §1 puts the
proofs of record on `http://localhost:4173`, which is potentially trustworthy, so the Clipboard API
*is* defined in every drive and the no-clipboard branch is unreachable there — `ADR-0128`'s own
*"The absence is invisible to every proof this project runs"*, restated for the mechanism. A copy
mechanism adopted today would therefore be exercised by nothing: not by a unit test, not by a QA
drive, not by a hand-check whose steps are written down.

**There is no deadline in either direction.** Nothing is blocked — `ADR-0128` §7's *"`DEC-150`
blocks nothing"* was true and stayed true, `STORY-1404` never waited on it, the card's three
host-alone frames are already settled by §6 of that ADR, and no frame, string or ticket depends on
the answer. Nothing about this is cheaper today than in six months. The one thing that would change
the calculation is `DEC-126` answering with an `https` origin, which would make the question moot
for the offered deployment while leaving the vision's founding case — two people in one house, over
a LAN address — exactly where it is.

## Decision

### 1. `navigator.clipboard.writeText` is the only clipboard mechanism this client uses

No `document.execCommand("copy")` and no other `execCommand` form standing in for it, no
`copy`-event interception, no `window.clipboardData`, no hidden `<textarea>` or `contenteditable`
shim, and no `navigator.share` in the place of a copy. Where `navigator.clipboard` is undefined —
the only case this section is about — the press performs `ADR-0128` §3's hand-over: focus the
invite-link box, select the whole link, render `Copy it from the box above.`, and attempt nothing
else.

This is what `CopyLink` already does. **The decision is a ratification: no file changes, and no
ticket is owed.**

### 2. The reason is the missing read-back, not the deprecation

`execCommand`'s boolean reports whether the command was supported and enabled for that invocation.
It does not report that the system clipboard now holds the link, and no browser offers a signal that
does. The one in-page read-back — `navigator.clipboard.readText()` — lives on the interface that is
undefined by hypothesis, so it is unavailable precisely and only where it would be needed.

Two consequences follow, and both matter more than the deprecation notice:

- **A client cannot tell a truthful `true` from an untruthful one.** Even in a browser whose
  `execCommand("copy")` never lies, the running client has no way to know it is in that browser as
  opposed to one whose `true` means less. A rule that holds only where you cannot check that it
  holds is not a rule the client may act on.
- **The failure is unobservable at the last step, not the first.** Every refinement of the mechanism
  (§Alternatives 3) improves the evidence that a write *began* and leaves the evidence that it
  *landed* exactly where it was: absent.

`ADR-0128` §4 therefore has, at the mechanism level, the answer its wording anticipated: no
mechanism available to this client can satisfy it, so `Link copied.` stays gated on a resolved
`writeText` and nothing else.

### 3. `Link copied.` is reachable in a secure context and nowhere else

Where `navigator.clipboard` exists and `writeText` resolves, `Link copied.` renders — unchanged.
Where it does not, the host gets the selection and the sentence, and the screen never claims a copy.
`ADR-0128` §7's conditional is closed with its antecedent false, not with its consequent denied: if
a browser ever ships a mechanism whose landed write the page can read, §5 says what reopens this.

### 4. A copy the screen cannot report is not attempted — and that refusal is §3's, not §4's

The one option `ADR-0128` §4 does not decide is a **silent** `execCommand`: attempt the copy, ignore its
return, and print `Copy it from the box above.` regardless. It claims nothing, so it breaks no rule
about claiming. It is refused anyway, and the clause it breaks must be named exactly, because a
future ADR needs to know which door to knock on.

`ADR-0128` §3's promise is *"one press leaves the link either in your clipboard or selected under
your cursor, and the screen says which."* A silent copy that succeeds and then prints
`Copy it from the box above.` keeps the first half and breaks the second: the screen says the wrong
one, on the very devices where the mechanism fires, and the product's one sentence about what just
happened becomes the sentence that is wrong. The invite would then have three real states and two
sentences.

So the refusal here is not *the client may not claim what it cannot verify* (`ADR-0128` §4) but
*the screen must say which of the two things happened* (`ADR-0128` §3). Making a silent copy honest
needs a third outcome,
a third outcome needs a string, and a string is `ADR-0110` §6's *"a new ADR, not an invented
sentence"* — **the product owner's**, not this ADR's and not a coder's.

### 5. What would reopen this, stated so the answer is not permanent by inertia

Any one of three, and nothing less:

1. **A runtime signal the client can read** that distinguishes *the clipboard now holds this link*
   from *nothing happened*, on the device the client is running on. A result that is true on the
   author's laptop is evidence for a hand-check, not a signal a client may branch on.
2. **A product answer that changes `ADR-0128` §3's two outcomes** — for instance a third, hedged
   state for *copied, we think*. That is the product owner's, it costs a string, and `ADR-0110` §6
   is the route.
3. **`DEC-126` answering with an `https` origin**, which does not answer this question but dissolves
   it for whatever is served that way: `navigator.clipboard` is then defined and §3's shipped path
   is the only one. The LAN case survives it, so this ADR still governs there.

### 6. How a mechanism would have to be proven, and what does not exist today

Named concretely, because *"we would test it"* is where a mechanism like this normally slips
through:

- **Not in jsdom.** Measured this run at the pinned jsdom 24.1.3: `document.execCommand` is absent
  from the document object, so there is nothing to call and nothing to observe. A test of an
  `execCommand` mechanism could only install a fake `execCommand` and assert that the code called
  the function it was written to call — which proves the code's own text and no behaviour at all.
  The absence also has a sharp edge worth writing down: an **unguarded** call would throw a
  `TypeError` inside the click handler and turn every suite that presses the control red —
  `InvitePanel.test.tsx`, `null-view.test.tsx` and `Lobby.test.tsx` — while a **feature-detected**
  call would take the missing-API branch in every one of them and ship uncovered. Loud and wrong,
  or silent and unproven; there is no third jsdom outcome.
- **Not in the proofs of record.** `ADR-0117` §1 fixes their origin as `http://localhost:4173`,
  where the Clipboard API is defined, so a QA drive cannot reach the branch at all.
- **What would have to change**, if a future ADR wants the evidence: (i) the built bundle would have
  to be served to the drive on a **non-secure** origin — the host's LAN address rather than
  `localhost` — which moves `scripts/qa/stack.sh` and the origin `ADR-0117` §1 fixed; and (ii)
  `scripts/qa/drive.mjs` would need a **read-back verb**, which cannot be `readText` on that origin
  and therefore has to be a paste driven from the browser side (the DevTools protocol can dispatch
  editing commands with a key event) into a field the page can then read. Both are buildable inside
  `ADR-0088` §1's no-dependency rule, since `drive.mjs` is Node's built-ins and nothing else.
  Neither exists.
- **What is already pinned, and stays.** The negative this decision preserves is tested three times
  over on the no-clipboard path: `InvitePanel.test.tsx`'s *"offers the control and hands over the
  selection with no Clipboard API"* and `Lobby.test.tsx`'s *"offers the copy button and hands over
  the selection with no clipboard"* both assert `queryByText("Link copied.")` is `null` after the
  press, and `Lobby.test.tsx`'s *"states the seven strings the host-alone table renders with no
  clipboard, and no eighth"* walks every text node under the section and compares the sorted set. A
  mechanism that added a string, or claimed a copy on that path, breaks all three. **This ADR adds
  no test**, because the assertions that would be written already exist.

### 7. The adjacent product question is named, not registered

Whether the invite may ever perform a copy it cannot report, or say something hedged about one, is
the product owner's under §4 above and `ADR-0110` §6. It is **not** registered as a `DEC`: nobody is
asking for it, no ticket waits on it, and a decision nobody is working is noise in the open table.
It is written here so that whoever does ask knows it is a product question with a route, rather than
a mechanism an architect already refused.

## Consequences

**What it buys.** The question stops being re-asked, and stops being answerable inside a ticket:
`ADR-0128` §4's rule now has a mechanism-level answer rather than a standing invitation, so a coder
who reaches for `execCommand` is contradicting a merged ADR instead of filling a gap. The client
carries no deprecated API, no iOS-Safari selection shim, and no branch that every proof this project
runs is structurally unable to reach. The shipped behaviour and the merged record agree exactly,
with no work in between — this decision is the rare one that costs a document and nothing else.

**What it costs.**

- **A copy that would probably have worked is not attempted.** On Chrome or Firefox over LAN,
  `execCommand("copy")` would in all likelihood have put the link on the clipboard on one press. The
  press is refused on an argument about what the client can *know*, not on a measurement: nobody ran
  it on the author's two devices, and this ADR does not claim the mechanism fails. That is the
  strongest thing a critic can say here, and it is true.
- **`ADR-0128` §3's *"one gesture, not zero"* stops being provisional.** It was a cost with an open
  question behind it; it is now the settled behaviour for every insecure origin, and the iPhone in
  `EPIC-14`'s own capture table will never see `Link copied.` until something in §5 happens.
- **The rule shows up in the code as an absence.** Nothing in `web-client/src` says *we
  deliberately do not call `execCommand`*; a reader of `CopyLink` finds this only through
  `ADR-0128` §7 and the register. Spending a ticket on a comment was rejected as more process than
  the sentence is worth, so the cost is real and is accepted, not designed away.
- **Reversal now costs an instrument before it costs code.** §6's two changes to the QA stack — a
  non-secure origin and a read-back verb — are the price of the evidence that would justify a
  superseding ADR. That is deliberately more than a patch, and it is the reason to write §6 down
  now, while the shape of the missing proof is fresh.

**What it forecloses.** Claiming a copy from anything but a resolved `writeText`, in this client,
forever — including from a signal that is *usually* right. And it forecloses the silent-copy
compromise on `ADR-0128` §3's grounds, so the next attempt at it has to reopen a product promise
rather than argue a mechanism.

**What it does not foreclose.** The async Clipboard API path is untouched and keeps working the
moment an origin is secure. A hedged third outcome remains available to the product owner at the
price of one string. And nothing here is expensive to undo: no code was written, none was deleted,
and no dependency, frame or wire moved — **the cheapest-to-reverse option is the one this ADR takes,
and that is the reason it takes it**, because the evidence about `execCommand`'s truthfulness on the
devices that matter is thin and this repository has no instrument that could thicken it.

## Alternatives considered

**Call `execCommand("copy")` and say `Link copied.` when it returns `true`.** Its strongest case: it
is plainly what the human asked for; the call is implemented in every shipping browser and works in
an insecure context; on the author's laptop over LAN it delivers the link in one press with no
fallback gesture at all; one outcome is simpler to draw, to test and to explain than two; and in the
overwhelmingly common case the boolean and reality agree, so the sentence would almost always be
true. Rejected because *almost always true* is the thing `ADR-0128` §4 was written to forbid — the
boolean reports that the command ran, the client has no read-back to check it against (§2), and the
one case where it lies is indistinguishable from the many where it does not. A screen that is right
by luck cannot be told from one that is right by knowledge, and this product's whole register is the
difference.

**Call `execCommand("copy")` silently and never claim anything — the compromise.** Its strongest
case, and it is the option that nearly won: it over-claims nothing, so `ADR-0128` §4 is untouched;
it adds no
string, no frame, no product change and no new state; the host on Chrome-over-LAN ends up with the
link genuinely on the clipboard, which is strictly better than today; a host who follows the
instruction anyway loses nothing; and it is the only option that improves the outcome for the exact
person whose photograph opened `EPIC-14`. Rejected on `ADR-0128` §3 rather than its §4, for the
reasons §4 above sets out: the
promise is that the screen says *which* of the two things happened, and a silent success makes the
screen say the wrong one on precisely the devices where the mechanism fires — the fallback sentence
becomes a wasted instruction at best and a lie about state at worst. Rejected also because the
branch is unreachable by every proof this project runs (§6), so a permanent, deprecated,
iOS-fragile path would ship whose only possible witness is another photograph; and because it cannot
be adopted quietly — making the screen honest about it needs a third outcome, which is a string,
which is the product owner's.

**Use the `copy` event as the signal: attach a listener, call `execCommand`, claim a copy only if
the handler ran.** Its strongest case: it is strictly more information than the boolean — the
browser asked *this page* for the bytes, which is a fact about this run rather than about command
availability, and it separates Firefox's refusal-without-user-activation from a genuine write; with
`clipboardData.setData` the page even controls what is written, so the content is known exactly.
Rejected because it moves the unobservable step one place later without removing it: the handler
firing proves the write **began**, and `ADR-0128` §4's word is *happened*. It also stays unreachable
in jsdom
(no `execCommand` to fire it) and in the drives (secure origin), so the improvement cannot be
measured here — a better proxy, adopted on faith, is still adopted on faith.

**Ship the classic iOS shim — `contentEditable`, `setSelectionRange(0, length)`, then
`execCommand`.** Its strongest case: the readonly-input path is the known-fragile one on iOS Safari,
this is the documented workaround for it, and the phone is the device the whole thread started on;
without it, the option above helps the laptop and not the person holding the phone. Rejected because
it multiplies the fragile surface — focus theft, the on-screen keyboard, restoring the selection and
the `readOnly` attribute afterwards — around a call whose result is still unreadable, and because it
would ship the largest untested branch in the client, on the one browser no instrument here can
drive, to serve a claim it may still not make.

**Reach for the platform's other door: `navigator.share`.** Its strongest case: on a phone it beats a
clipboard outright — one press hands the link to any app the host chooses, which is closer to *send
her a link* than copying is. Rejected on two counts: it is `[SecureContext]`-gated like the Clipboard
API, so it is absent in exactly the same environments and answers nothing here; and it is a
different act with a different name, which runs into `ADR-0128` §2 — one act, one name, *"the rule
[`ADR-0046`](ADR-0046-the-table-says-away-timed-out-and-back.md) opens by naming"* — making it a
product decision about what the invite offers, not a fallback an architect may slip in.

**Decide nothing and wait for `DEC-126`.** Its strongest case: an `https` deployment dissolves the
question, `DEC-150` blocks nothing, no ticket waits, and the cheapest decision of all is the one not
taken — this ADR itself concedes there is no deadline. Rejected because leaving it open is not free:
the row sits in three registers, `ADR-0128` §7 reads as an open invitation to whoever next edits
`CopyLink`, and *never guess a decision* means the next coder to reach for `execCommand` inside a
ticket would be doing so with no merged answer to contradict — the exact failure this role exists to
prevent. And the LAN case outlives any deployment answer, because the vision's founding scene is two
people in one house.
