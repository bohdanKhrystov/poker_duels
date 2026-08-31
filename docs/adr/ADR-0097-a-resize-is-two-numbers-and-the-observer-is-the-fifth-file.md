# ADR-0097 — A resize is two numbers, and the audit's observer is the fifth declared file

- **Status:** Accepted
- **Date:** 2026-08-31
- **Resolves:** `DEC-097` — by what mechanism does
  [`ADR-0096`](ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md)'s **audit focus**
  run under [`ADR-0089`](ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md)
  §2's three conditions? Registered open 2026-08-31 by `ADR-0096` §1; its half **(b)** restated the
  same day, after the human's *"we have to support phone size"* turned one viewport into two.
- **Amends:** [`ADR-0090`](ADR-0090-a-skill-may-write-the-catalogue-or-run-it-never-both.md) §2 —
  the declared-file set becomes **five**, `agents/audit.md` licensed to *mention* the cycle and
  never to invoke it, on the precedent
  [`ADR-0092`](ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md) §2 set when it
  made the set four. Everything else in `ADR-0090` — §1's amendment, §3's two commands, §4's source
  rule, §5's provisional line, §6 and §7 — stands byte-unchanged, and so does `ADR-0089` §3, which
  this ADR **applies** rather than widens.
- **Applies, and does not touch:** `ADR-0096` §§1–3 and 5–6. What the audit walks, what it judges
  against, that a criterion is binary, and how `A(N)` terminates a round are the product owner's and
  are untouched here. This ADR decides only *what text changes in which files to make the focus
  legal*, which is the split `ADR-0096` §1 drew when it registered the decision.
- **One thing recorded, not decided.** The human settled orientation on 2026-08-31, after `ADR-0096`
  merged: ***"we are ok to support only one orientation for mobile form factor."*** §5 below records
  it where `ADR-0096` §4 and §Consequences left the question open, so the next reader finds an answer
  instead of re-raising it. It is a **supported-surface** call, the human's own column, and this ADR
  transcribes it.
- **Where the numbers came from.** Every geometry quoted below was measured on 2026-08-31 against
  Chrome `--headless=new` launched by `scripts/qa/stack.sh chrome-up`, one CDP process per
  measurement — the same shape `drive.mjs` runs in. Two of them contradicted what this ADR's first
  draft assumed, and the draft changed rather than the numbers.

## Context

`ADR-0096` §4 commits an audit round to **two viewports inside one duel**: the whole eight-beat walk
at `phone` **390 × 664**, and `R2`/`R3` re-answered at `laptop` **720 × 900** at the beats where a
player is asked to act. It then declines to say how, because *"how a round produces two viewports is
the architect's."*

**The obvious implementation is closed by a merged rule, and the ADR says so.** A duel is a live
match between two sockets. Relaunching a browser mid-duel re-seats the device by `pd.deviceId`
(`ADR-0018`) into a room that has moved on, and a fresh Chrome profile — mandatory per round under
`ADR-0089` §3 — deals a different board. So the two shapes cannot be two passes over the same beat.
Whatever answers this has to change the shape of a tab that stays alive, with its socket, its seat
and its hand intact.

**The only way into a live tab's viewport is CDP, and CDP's one method for it is not only a
resize.** `Emulation.setDeviceMetricsOverride` takes `width` and `height` — and also
`deviceScaleFactor`, `mobile`, and `screenOrientation`. `DEC-097` framed the tension honestly:
a player genuinely resizes a window with their hands, which argues the call is permitted; but the
extra fields make the client believe it is running on hardware it is not, which argues something
stronger than a resize.

**`ADR-0089` §3 does not have a slot for it, and that is the first thing to get right.** §3 is a
**three-way** partition, not a two-way one: acts a player's hands reach (*click, type, navigate,
reload, clear browser storage*), reads (*anything* — DOM, `localStorage`, the database, the server's
log), and writes of **application state** (*a store dispatch, a synthesised socket frame, a seeded
row* — forbidden). Read-versus-write is therefore the wrong axis, and both wrong answers are
expensive. Call the whole call a **read**, and §3's *"read anything"* licenses `mobile: true` along
with it. Call it a **write**, and §4 is unbuildable, the second shape goes back to the product owner,
and the first audit round waits on a round-trip for a mechanism that — measured — works.

**The sharper fence already exists and is not about reads.** `ADR-0089` §4, transposed by
`ADR-0092` §2, makes a finding a looking human cannot reproduce a **harness defect**: filed against
`EPIC-12`, repaired in `scripts/qa/`, excluded from the count, never repaired in production code.
An `R2` finding says *the amount to call is not visible at 390 px*. The human checks it by dragging
a window. So the question is not *read or write* — it is **can a human's hands produce the state the
harness produced?** That question has a measurable answer, field by field.

**Measured, it does — and only for two of the fields.** A 390 px page containing a 520 px block,
carrying the app's own `<meta name="viewport" content="width=device-width, initial-scale=1.0">`:

| the call | viewport | `devicePixelRatio` | `screen` | does the 520 px block overflow? |
| --- | --- | --- | --- | --- |
| `{width: 390, height: 664, deviceScaleFactor: 0, mobile: false}` | **390 × 664** | 1 | 800 × 600 — the host's | **yes**, `scrollWidth > innerWidth` |
| `{width: 390, height: 664, deviceScaleFactor: 3, mobile: true}` | **520 × 886** | 3 | **390 × 664** — fabricated | **no** |

The second row is the whole argument. Mobile emulation applies the mobile viewport's shrink-to-fit,
which **widens the layout viewport to 520 to contain the overflowing element**, and the overflow
disappears. `R2` asks whether the decision is *"visible at once, without scrolling"*. With
`mobile: true`, that question is answered **`met` by the emulation rather than by the product** — a
false pass, silent, in the one criterion `ADR-0096` §Consequences predicts will fire hardest.

**And the flag buys nothing the rubric asks for.** Measured on the same browser, `pointer: coarse`,
`hover: none`, `navigator.maxTouchPoints` and `ontouchstart` are **false / 0 in all three states** —
no override, honest override, and `mobile: true` alike. Touch is a separate CDP domain this ADR does
not license. So the fields that corrupt the measurement do not deliver the device signal someone
might have wanted them for.

**The fallback the decision anticipated does not work either, and this is the second thing the
draft got wrong.** `DEC-097` says that if the emulation call is a write, something else must size
the window. The candidate is `Browser.setWindowBounds`, which moves the real window and cannot lie
about a device. Measured:

| requested window | window Chrome actually set | resulting viewport |
| --- | --- | --- |
| 390 × 664 | **500** × 664 | **500 × 577** |
| 720 × 900 | 720 × 900 | **720 × 813** |

Chrome clamps a window to a **500 px minimum width**, so `phone` at 390 is unreachable by that route
at all; and the window includes 87 px of chrome the viewport does not, so neither height lands.
`ADR-0096` §4's numbers are **viewport** numbers — 664 is *"the smallest `100dvh` the card's own
column is ever asked to fill"* — and window sizing cannot express them.

**A correction worth recording, because it has propagated into two merged ADRs.** `ADR-0092` §2 and
`ADR-0096` §4 both cite *"widths under ~500 px clip rather than overflow"* in headless capture. That
floor is a property of **window sizing** — the 500 in the table above — not of capture. With a
metrics override at 390 × 664, `Page.captureScreenshot` returned a PNG measured at exactly
**390 × 664**, complete to both edges. This changes no decision in either ADR — 720 is still half a
1440 × 900 laptop screen, screenshots are still never diffed — it refines one of the two reasons
given for one number, and it means the `shot` verb keeps working at the shape §4 walks the whole
duel at.

**Three facts about persistence decide whether a verb is even possible.** `drive.mjs` is one process
per verb: it attaches a WebSocket, sends, and closes it in a `finally`. Emulation overrides are
often session-scoped, which would make the shape evaporate the moment the verb exited. Measured, in
this Chrome, they do not:

- the override **survives the session detaching** — set in one `node` process, read back as
  390 × 664 by the next;
- it **survives `Page.navigate`**;
- it is **per-target** — a tab opened afterwards does not inherit it.

**And the live-tab claim, measured directly.** A page holding an identity value and a
`min-height: 100dvh` column, resized 390 × 664 → 720 × 900 by a separate process: the identity value
was **byte-identical** before and after (the JS context was never torn down, so a live WebSocket and
a seated player survive it), the page received exactly **one** `resize` event, and the column
re-measured **664 → 900**. A no-op resize to the size already in force fired **no** event at all.

**Half (a) is a smaller question with the same shape.** `ADR-0090` §2 declares exactly which files
may name `qa-cycle` and its default is refusal — *"a new mention is a new caller until an ADR says
otherwise"*. `ADR-0092` §2 spent that clause once, taking the set from three to four for
`agents/uat.md`. An audit observer needs the same one sentence `qa.md` and `uat.md` both carry —
that the cycle owns the stack lifecycle the agent does not — so it would be a fifth. The force the
other way is real: a condition that is amended every time it is inconvenient is not a condition. The
force toward amending is that `ADR-0096` §2 froze `ADR-0092` §3's classifier **byte-unchanged for
the `qa` and `uat` focuses**, so `uat.md` cannot host the audit's classifier without contradicting a
sentence merged the same day.

**The deadline is sharp and it is about evidence, not about cost.** Every other part of this is as
cheap next month as today. But the moment the first audit round records *"`R2` not met — the amount
to call is below the fold at 390 × 664"*, that geometry is **evidence**, and evidence produced by a
lying viewport is worse than no evidence: it reads as settled, `ADR-0089` §4 would only catch it if
somebody happened to try reproducing it by hand, and a repair ticket would be written against a
measurement of a phone that does not exist. The mechanism has to be right **before** the first
round, not before the first repair.

## Decision

### 1. A viewport resize is an **act**, not a read — the third of `ADR-0089` §3's categories

`ADR-0089` §3 stands byte-unchanged. It is applied, not widened: **changing a tab's viewport is an
act a player's hands reach**, in the same category as *click, type, navigate, reload, clear browser
storage*, and it is neither a read nor a write of application state. A player drags a window edge.
The measured basis is in §Context: across the resize the JS context survived intact, one `resize`
event was delivered, and nothing entered the store, no frame was synthesised and no row was seeded.
Nothing the client believes about the game changed; only how much of it fits.

§3's list of acts was never exhaustive — *clear browser storage* is not something hands do either,
it is what a browser menu does on their behalf — and this ADR adds the sixth member of that list
rather than a new category.

**The classification is a property of the fields, not of the method name.** The same CDP method,
with `mobile: true`, produces a viewport a player cannot produce by any action available to them,
and — measured — turns an `R2` failure into an `R2` pass. That version is outside §3, and the reason
is `ADR-0089` §4 rather than §3's own prohibition: it manufactures findings a looking human cannot
reproduce, which is the definition of a harness defect. **Claiming a device is not resizing a
window.**

### 2. `drive.mjs` gains one verb, `size`, and it sets exactly two fields

```
size <width> <height>       set this tab's viewport, in CSS pixels
```

It sends `Emulation.setDeviceMetricsOverride` with **`width`, `height`, `deviceScaleFactor: 0` and
`mobile: false`, and no other field**. `0` is CDP's *"do not override the scale factor"*; `false` is
*"do not emulate a mobile device"*; `screenOrientation` is **never** sent (§5). Those three pins are
the whole of what separates a resize from a fabricated device, and they are written as literal
values in one verb so that one reviewer can check them against one file.

The verb then **reads the viewport back from the page and prints the shape it achieved** —
`innerWidth × innerHeight`, measured, not the numbers it was asked for — and **exits 1 if the
read-back is not the request.** A clamp, a stale target, a call that silently did nothing: all
become a non-zero exit and a line in the round record, instead of a walk that continues at the wrong
shape. This is the property that caught `Browser.setWindowBounds`'s 500 px floor in §Context, and it
is cheap enough to be unconditional.

**Where it runs in the walk.** The override is per-target and survives both detachment and
navigation (§Context), so:

- each browser's **first** act in a round is `size 390 664`, **before** `open` — beat 1 is walked at
  `phone` like every other beat;
- a shape change mid-duel is one more `size`, on the same live tab, with no reload and no re-seat;
- `close` creates a fresh tab, which inherits nothing, so any verb sequence that crosses a `close`
  re-applies `size`;
- nothing clears the override, because `ADR-0089` §3's fresh Chrome profile per round already ends
  the tab's life with the round.

### 3. Both browsers move together, and the walk restores itself to `phone`

At a beat where §4 re-answers `R2`/`R3` at `laptop`, **both tabs are resized, both are read, and
both are returned to `phone` before the walk continues.**

Resizing only the observed seat would confound two variables: a difference between the two browsers
would then have two candidate causes — the shape, or the seat — and the finding would not be
attributable to either. Moving both keeps the shape the only thing that changed, which is what makes
`ADR-0096` §2's *"a criterion is `met` only if it is met at every shape it was answered at"* a
statement about the product. Restoring is free when nothing changes: a `size` to the shape already
in force fires no `resize` event at all.

### 4. `ADR-0090` §2's declared-file set becomes **five**, and the check is one line

The audit focus gets its own observer, **`.claude/agents/audit.md`**, which carries the one sentence
`qa.md` and `uat.md` both carry — that `qa-cycle` owns the stack lifecycle the agent does not — and
is licensed to **mention** the cycle, never to invoke it. `ADR-0092` §8 applies unchanged and is not
re-argued: no new manager, no new skill, no `Write` on the observer; `qa-manager` remains the single
filer over the single ledger, and `qa-manager.md` still names the cycle nowhere.

**The set is amended because `ADR-0092` §8's own test is met, not because a focus was added.** That
test is whether the briefs contradict at the sentence level. They do, and the contradiction was
merged the same day: `ADR-0096` §2 relocates the merged source so that an audit observation is a
**finding** when it contradicts a rubric criterion and needs no other source — while freezing
`ADR-0092` §3 **byte-unchanged for `qa` and `uat`**, where the same observation is a **question**,
capped at three. One file cannot hold both, and *"one file holding both lists, switched by a scope
word"* is the exact arrangement `ADR-0092` §8 refused. Running the audit under `uat.md` would
reproduce the failure `ADR-0096` §Context diagnoses: round 3 promoted zero questions, correctly, on
a product the human called raw.

The check `ADR-0090` §2 wrote and `ADR-0092` §2 extended becomes:

```bash
grep -rl "qa-cycle" .claude/skills .claude/agents \
  | grep -Ev '^\.claude/(agents/(qa|uat|audit)\.md|skills/qa-(cycle|cases)/SKILL\.md)$' \
  | awk 'END{exit (NR==0)?0:1}'
```

Verified 2026-08-31: it exits **0** on the tree as this ADR merges, where four files name the cycle
and `audit.md` does not yet exist; **0** when the fifth lands; and **1** the moment a sixth does.
Running it cites no round and claims no coverage, so `ADR-0089` §2c does not reach it — it is an
exit code about which files name a skill.

### 5. Portrait only — the human's call, recorded where `ADR-0096` §4 left the question

`ADR-0096` §4 wrote *"Landscape on a phone is not walked, and is not settled here"*, and
§Consequences carried it as genuinely unresolved. It is now settled, by the human, on 2026-08-31:
***"we are ok to support only one orientation for mobile form factor."*** Portrait.

Three things follow, and none of them is this ADR's judgment:

- **No rotation handling and no reflow decision.** No criterion asks what happens at 664 × 390, no
  ticket owes a landscape layout, and the *"reflows, refuses, or asks them to rotate back"* trichotomy
  `ADR-0096` §4 posed does not need an answer.
- **No second mobile shape.** An audit round walks exactly the two shapes `ADR-0096` §4 names. The
  table there is complete.
- **`screenOrientation` is never sent by `size`** (§2). It is the third field the verb pins by
  omission, alongside `deviceScaleFactor` and `mobile`, and portrait-only is why.

**What would reopen it, stated so the next reader can check rather than re-argue:** the day the
product does something *different* when rotated — a second layout, a lock, a prompt — it has
acquired a behaviour, and behaviour on a surface is `docs/vision.md`'s, by `ADR-0096` §4's own test.
Today it does nothing different, and *nothing different* is what one supported orientation means.

### 6. The three conditions, checked one at a time

- **§2a, no dependency — holds.** `Emulation.setDeviceMetricsOverride` goes over the WebSocket
  `drive.mjs` already holds, exactly as `Page.captureScreenshot` does under `ADR-0092` §2. Node
  built-ins only; no module's dependency set changes; no browser-automation package enters this
  repository; Chrome stays a machine-local binary this repository does not vendor, install, pin or
  ship. The verb spawns no process, so `kill`, `pkill`, `killall` and `rm` are not approached. **The
  day a viewport needs a library, §2a is failing and the question returns as a new `DEC`.**
- **§2b, no gate — untouched.** `build.yml` keeps its two jobs. `/qa-cycle audit <scope>` is started
  by the human's own message and is the first act of its turn, exactly as `ADR-0089` §2b (amended by
  `ADR-0090` §1) requires of the other two focuses. §4's grep is a command in an ADR; nothing here
  wires it into a `verify:` block.
- **§2c, no coverage claim — holds, with the corollary this focus needs said out loud.** An audit
  round's product remains a dated round record: one walk, one machine, one commit. And the corollary:
  **a shape walked is not a surface supported.** Walking 390 × 664 in a round is not evidence that
  the product supports phones. That claim is `ADR-0096` §4's, resting on the human's *"we have to
  support phone size"*, and no round may be cited as having established it — the same distinction
  `ADR-0093` §2 draws for readiness.

### 7. Reversing this is one verb, one file and five lines

Delete the `size` verb from `scripts/qa/drive.mjs`, `.claude/agents/audit.md`, the `audit` focus
from `qa-cycle`'s `SKILL.md`, the fifth entry from §4's check, and this ADR. Nothing imports the
verb, no build file names it, no CI job invokes it, no test asserts its output, and no module
depends on it — `ADR-0089` §6's own grade of reversibility, preserved because §6 above kept §2b and
§2c intact so that nothing could become load-bearing.

**That cheapness is why this direction is the one to take while the yield is unevidenced**, and it
is the second reason to prefer it over sending §4's second shape back to the product owner: this
answer is five lines to undo, and that one changes a merged round contract and costs an agent
round-trip before the first audit round can start.

## Consequences

**A resize is a real event, and the frame recorder cannot tell it from a product transition.** The
`record`/`frames` verbs arm a `MutationObserver` on `#root`, and they are the evidence `R1` is
answered with. A responsive re-render on `resize` pushes frames into `window.__pdFrames` that no
player action caused, so at exactly the beats `ADR-0096` §4 doubles, `R1`'s cleanest evidence gets
noisier. The round record must mark where each `size` was issued and the frame list at those beats
must be read with that known. This is a real cost of doing two shapes in one tab, and the
alternative — two tabs — is the one `ADR-0018` closed.

**The current shape is tab state, not command state, and one class of error has no catch.** §2's
read-back assert catches a resize that *failed*. Nothing catches a resize that was *forgotten*: a
round that re-checks at `laptop` and does not restore silently walks the remaining beats at 720 and
answers `R2` at the wrong shape, and every number it records will be internally consistent. The
mitigation is that `size` prints its achieved viewport on every invocation, so the transcript shows
the shape the walk was in — a discipline a reader can audit, not a gate that prevents it. §2b
forbids making it a gate.

**The field discipline is a convention in one file, one edit away from being broken, and it fails
in the dangerous direction.** Adding `mobile: true` is a two-word change that would flip an `R2`
`not met` to `met` — a **false pass**, measured, silent, in the criterion most likely to matter. No
CI job can catch it, because §2b forbids one. What stands between the harness and that edit is: this
section, the literal values in one verb, a reviewer reading one file, and the fact that the verb is
the only place in `scripts/qa/` naming the `Emulation.` domain. That is priced honestly rather than
called sufficient.

**This forecloses device emulation for this harness, permanently — and that forecloses a class of
criterion.** A future criterion about tap-target size, hover-only affordances, `pointer: coarse`, a
device pixel ratio above 1, or anything that reads `navigator` for hardware **cannot be answered by
this walk**, because the fields that would supply it are the fields §2 forbids. Measured, the
`mobile` flag would not have supplied most of them anyway — touch is a separate CDP domain — so the
foreclosure is wider than the flag: it is *this harness measures layout at a width, and nothing
else*. The day the rubric wants such a criterion, §2a and §3 return as a new `DEC` rather than being
quietly widened by a ticket.

**A fifth declared file is a condition spent, and the precedent chain is now two links long.**
`ADR-0090` §2's set exists to make *"who may invoke the cycle"* mechanical, with refusal as the
default; the value of that default falls a little each time it is amended. This is the second
amendment, and it cites the first. A sixth will cite two, and the argument will be easier to make
than this one was. The counterweight is that both amendments are *mention-only* licences to
**observer** agents that structurally cannot start a cycle — but that is a property of the two files
so far, not a rule, and this ADR does not pretend to have written one.

**What it buys.** `ADR-0096` §4 becomes buildable exactly as merged: the second shape does not
return to the product owner, the first audit round is unblocked without a round-trip, and the
planner can write both a `Files` table and a beat that is walked at two shapes. More narrowly, an
`R2` finding now quotes a geometry a human can reproduce by dragging a window — which is what makes
`ADR-0089` §4's reproducibility test something that can actually be run on a fitting complaint, and
`R2` is the criterion `ADR-0096` predicts will fire hardest.

**One number in two merged ADRs now has a refined reason, and nothing else changes.** `ADR-0092` §2
and `ADR-0096` §4 both attribute a ~500 px clip to headless *capture*; measured, it belongs to
window *sizing*, and capture at an overridden 390 × 664 is faithful. Neither ADR's decision moves —
this is recorded so that the next reader sizing something does not choose `Browser.setWindowBounds`
on the strength of a reason that does not apply to it.

## Alternatives considered

**1. `Browser.setWindowBounds` — size the real window, and lie about nothing.** The strongest case
for it is the strongest case available in this whole decision: it is *literally* the act a player
performs, there is no `mobile` field to add by accident, no future edit can turn it into a device
claim, and it needs no argument about which fields are honest. If it worked, §2's entire field
discipline — and the paragraph in §Consequences pricing it — would be unnecessary. **Rejected on
measurement, not on preference.** Chrome clamps a window to a 500 px minimum width, so a request for
390 came back as 500 and produced a 500 × 577 viewport; and the window carries 87 px of chrome the
viewport does not, so 720 × 900 produced 720 × 813. `ADR-0096` §4's numbers are viewport numbers by
construction — 664 is the smallest `100dvh` the column is asked to fill — and this mechanism cannot
express either of them. A mechanism that cannot reach the phone shape at all cannot walk a round
whose every beat is at the phone shape.

**2. Full device emulation — `mobile: true`, `deviceScaleFactor: 3`, an iPhone user agent.** Its
case is genuine and it is the industry default: it is what every browser-automation tool ships as
"iPhone 14", it exercises the mobile viewport rules a real phone applies, and an audit that claims
to walk a phone arguably owes the closest available imitation of one. **Rejected because it changes
the number the rubric measures, in the direction of a false pass.** Measured: with the app's own
`width=device-width` meta, mobile emulation's shrink-to-fit widened the layout viewport from 390 to
520 to contain overflowing content, and `R2`'s *"without scrolling"* became true because of the
emulation. It also fabricates `screen` and `devicePixelRatio`, putting any finding outside
`ADR-0089` §4's reproducibility test. And it buys nothing the rubric asks for: `pointer: coarse`,
`hover: none` and `maxTouchPoints` were unchanged by the flag.

**3. Declare the resize a `read` under §3's *"read anything"*.** The tidy version of this ADR: no
new category, no field discipline, one sentence. Its case is that a resize genuinely does not write
any application state, which is true and is why §1 says the act writes nothing. **Rejected because
the licence is too wide for the thing it licenses.** *"Read anything"* is unqualified by design — it
is what lets the harness read the database and the server log — so classifying the CDP call as a
read licenses every field on it, including the two that manufacture a false pass. The distinction
this decision turns on is *between fields of one method*, and only the act category can carry a
distinction that fine, because *what a player's hands reach* is a test each field either passes or
fails.

**4. Declare it a write, and send §4's second shape back to the product owner.** `ADR-0096` §4
explicitly leaves this open and `DEC-097` names it, so it is a legitimate landing place and would
have been the right one had the mechanism not existed: a second round for the laptop shape is
buildable with no new verb at all. **Rejected because the premise is false.** The mechanism does
exist, it is measured, and it costs one verb — so returning a *product* question to the product
owner when the *technical* obstruction has dissolved would spend a round-trip to reverse a merged
design for no reason. It would also cost something real: a second round re-deals the board
(`ADR-0018`), so the laptop shape would be answered against a different hand than the phone shape,
and `ADR-0096` §2's *"one bar, checked more than once"* would quietly become two bars over two
duels.

**5. Run the audit under `.claude/agents/uat.md`, switched by the scope word, and keep the declared
set at four.** The strongest case: it spends none of `ADR-0090` §2's condition, adds no file, and
the cycle already switches focus by a word — `/qa-cycle uat regression` proves the pattern works.
**Rejected on `ADR-0092` §8's own test.** The two briefs contradict at the sentence level, and the
contradiction was merged the same day this decision was registered: under `uat.md` an observation
with no card, token or literal behind it is a **question**, capped at three; under the audit it is a
**finding** against a rubric criterion. `ADR-0096` §2 freezes `ADR-0092` §3 byte-unchanged for the
`uat` focus, so one file would have to hold two mutually exclusive classifiers — and a scope word
deciding which classifier applies is precisely the leak §8 built two files to make structurally
impossible. The concrete evidence that the leak is not hypothetical is in `ADR-0096` §Context: round
3 promoted zero questions, correctly, on a product the human then called raw.

**6. A second observer agent that also owns the stack, so no sentence about `qa-cycle` is needed and
the set stays at four.** Its case is that it satisfies the condition literally — the grep stays
green — and removes a dependency between the agent and the skill. **Rejected because it satisfies
the letter and breaks the point.** `ADR-0090` §1 put the stack lifecycle in the skill so that one
document owns bringing the stack up and tearing it down; an observer that owned it would be a second
copy of that procedure, which is `ADR-0092` §8's *"two copies of a rule drift"* arriving a third
time. It would also be the one shape `ADR-0090` §2's check genuinely cannot catch — a file that
*runs* the cycle without naming it — bought by an agent deliberately written to evade a grep, which
is a worse thing to have in this repository than a fifth declared file.
