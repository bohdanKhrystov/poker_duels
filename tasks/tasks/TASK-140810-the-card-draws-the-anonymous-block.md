---
schema: 2
id: TASK-140810
title: The card draws the anonymous block
type: task
status: backlog
parent: STORY-1408
module: design
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [design, account]
depends_on: [TASK-140809]
verify:
  - ./design/check-drift.sh
  - ./design/check-frame-cards.sh
  - awk 'index($0, "<!-- ANON-BLOCK: ") { n++ } END { exit (n != 3) }' design/screens/account.html
  - awk 'index($0, "<h2>No password yet</h2>") { n++ } END { exit (n != 1) }' design/screens/account.html
  - awk 'index($0, "<h1>Account</h1>") { n++ } END { exit (n != 1) }' design/screens/account.html
  - awk 'index($0, "<p class=\"scrtitle\">Account</p>") { n++ } END { exit (n != 2) }' design/screens/account.html
  - sh -c '! grep -qiF "real account" design/screens/account.html'
  - sh -c '! grep -qiF "upgrade" design/screens/account.html'
  - sh -c '! grep -qiF "promote" design/screens/account.html'
  - sh -c '! grep -qiF "you should" design/screens/account.html'
  - sh -c '! grep -qiE "anonymous account" design/screens/account.html'
  - sh -c 'grep -qi "anonymous" design/screens/account.html'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/screens/account.html`'s **No password yet** frame draws the block `ADR-0125` §3 requires, in
the words the code will then copy — and the card is where those words are decided (`ADR-0091` §2).

## Files

| File | Action |
| --- | --- |
| `design/screens/account.html` | modify |

Read, and do not edit:
[`ADR-0125`](../../docs/adr/ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md)
§3, §4 and §6;
[`ADR-0132`](../../docs/adr/ADR-0132-the-profile-says-whether-it-holds-a-password.md) §4 — the
condition the block renders under;
`docs/vision.md` — *Positioning* (*"The reference points are Lichess and Chess.com, not PokerStars.
Dark, quiet, fast, minimal."*) and *What it is* (*"One duel coin per win. Not chips, not currency,
not a balance. A counter of duels won."*), the two sentences `ADR-0125` derives the register from;
`web-client/src/account/account-text.ts` — the voice of the sentences already on this screen.

## What the block must say — three things, and no fourth

`ADR-0125` §3, verbatim in substance:

1. **What the profile is** — *anonymous*: it has no password, and this browser is the only thing that
   signs in to it. It sits **beside** the route sentences the frame already prints and replaces
   neither.
2. **What that costs** — the profile lives in this browser: the duel coins and the duels go with it,
   and nothing reaches them from another browser.
3. **The way out, and what it keeps** — the password form already on this screen, and the fact that
   giving the profile a password keeps **every duel coin and every duel**, because nothing moves
   (`ADR-0030` §1). This sentence is `EPIC-14` item 1e's *"sentence on a screen"*, and this is the
   screen.

## Three refusals, and they are the reason this ticket is `sonnet`

- **It is not a tier.** No badge, no pill, no status label, nothing that reads as a class of account
  that a *real* account is the upgrade from. There is one kind of profile in this product; a password
  is a **route into it**, not a rank. The word *real* is not used about an account, and *promote* is
  not owed a place — `Give this profile a password` already says the act plainly. The human's own
  phrase *"Annonymus Account"* is the **state**, not a label to print as a title: do not draw
  *Anonymous Account* as a heading, a badge or a caption.
- **It states, it does not urge.** No deadline, no urgency, no *you should*, no consequence for
  leaving the screen as it was found. `ADR-0036`'s *never required* is the standing rule.
- **The heading stays `Account`** — the lobby door's label and the address's name. Nothing asked for
  it to change, and the frame's own `scrtitle` stays as it is.

And one more, from §6: **the block says nothing about names.** After `ADR-0119` a player may be
**named and anonymous**, or **nameless and claimed**; *anonymous* here is a fact about credentials
only. No sentence in the block may refer to a display name, its absence, or `No name`.

## Scope

- Add the block to the **first frame only** (`<h2>No password yet</h2>`). The *Signed in* and
  *Recovery address* frames are untouched — a profile that holds a password is never told any of
  this (`ADR-0125` §4).
- Place it as `<p class="line">` entries in the existing frame's flow, above the `panel` that carries
  the sign-up form, so the state is named before the way out is offered. Use the card's existing
  classes; **add no new CSS, no new token and no new colour** — `check-drift.sh` gates the token
  names, and a badge would need a style, which is exactly what refusal 1 forbids.
- **Mark each sentence for the code ticket.** Immediately after the block, emit exactly three
  one-line HTML comments, each of the form `<!-- ANON-BLOCK: «the sentence, verbatim» -->`, one per
  sentence, each entirely on **one line** and containing the sentence exactly as the frame prints it.
  `TASK-140811` gates `account-text.ts` against these three lines with an `awk` that reads both files,
  so a sentence that differs by one character there fails there.
- Extend the frame's `<p class="note">` to say which of `ADR-0125`'s obligations each line
  discharges, and that the block renders only when the client has been **told** — never from the
  absence of a session token (`ADR-0125` §4, `ADR-0132` §4).
- Update the page `lede` so it mentions the state the screen now names. One clause; the lede's
  existing sentences stand.

## Out of scope

- `design/screens/duel-end.html`. It already ends at `Rematch` and draws no offer — `ADR-0125`
  §Context measured that and `ADR-0091` §5 recorded it. Nothing there changes.
- Any client file. `TASK-140811` copies the words; `TASK-140812` renders them.
- The `Signed in` frame's `Your password signs in to this account.` — `ADR-0132` §5 leaves it exactly
  as it is.
- `DEC-090` (should *Attach a recovery address* show on a profile with no password?). It stays open
  and the recovery panel still composes onto this state, as the frame's note already says.

## Tests

A design card has no unit test; its gates are the two scripts and the `awk` counts in `verify:`. The
counts are what stop a rewrite: three marker comments and exactly one *No password yet* heading, so a
block drawn twice, or drawn in the wrong frame, fails.

## Acceptance criteria

- [ ] `./design/check-drift.sh` exits 0
- [ ] `./design/check-frame-cards.sh` exits 0
- [ ] `design/screens/account.html` contains exactly three `<!-- ANON-BLOCK: ` lines
- [ ] It contains exactly one `<h2>No password yet</h2>` and one `<h1>Account</h1>`, and still two
      `<p class="scrtitle">Account</p>`
- [ ] `grep -i "anonymous"` finds a match; `grep -i` finds **no** match for `real account`,
      `upgrade`, `promote`, `you should` or `anonymous account`
- [ ] No sentence in the block mentions a display name
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
