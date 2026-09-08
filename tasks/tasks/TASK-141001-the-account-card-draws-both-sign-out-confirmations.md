---
schema: 2
id: TASK-141001
title: The account card draws both sign-out confirmations
type: task
status: backlog
parent: STORY-1410
module: design
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [design, account]
depends_on: []
verify:
  - ./design/check-drift.sh
  - ./design/check-frame-cards.sh
  - awk 'index($0,"<h2>Signing out — this browser becomes a new profile</h2>"){n++} END{exit (n!=1)}' design/screens/account.html
  - awk 'index($0,"<h2>Signing out — back to the profile this browser owns</h2>"){n++} END{exit (n!=1)}' design/screens/account.html
  - awk 'index($0,"<h2>"){f=0} index($0,"<h2>Signing out — this browser becomes a new profile</h2>"){f=1} f && index($0,"class=\"line\""){n++} END{exit (n!=1)}' design/screens/account.html
  - awk 'index($0,"<h2>"){f=0} index($0,"<h2>Signing out — back to the profile this browser owns</h2>"){f=1} f && index($0,"class=\"line\""){n++} END{exit (n!=1)}' design/screens/account.html
  - awk 'index($0,"<h2>"){f=0} index($0,"<h2>Signing out — back to the profile this browser owns</h2>"){f=1} f && index($0,"Signing out leaves any duel room this browser is in, and a duel left this way can be lost. This browser goes back to the profile it had before."){n++} END{exit (n!=1)}' design/screens/account.html
  - awk 'index($0,"<h2>"){f=0} index($0,"<h2>Signing out — this browser becomes a new profile</h2>"){f=1} f && index($0,"Signing out leaves any duel room this browser is in, and a duel left this way can be lost."){n++} END{exit (n!=1)}' design/screens/account.html
  - awk 'index($0,"<h2>"){f=0} index($0,"<h2>Signing out — this browser becomes a new profile</h2>"){f=1} f && index($0,"goes back to the profile it had before"){n++} END{exit (n!=0)}' design/screens/account.html
  - awk 'index($0,"<h2>"){f=0} index($0,"<h2>Signing out — this browser becomes a new profile</h2>"){f=1} f && tolower($0) ~ /are you sure/ {n++} END{exit (n!=0)}' design/screens/account.html
  - awk 'index($0,"<h2>"){f=0} index($0,"<h2>Signing out — this browser becomes a new profile</h2>"){f=1} f && index($0,">Sign out<"){n++} END{exit (n!=1)}' design/screens/account.html
  - awk 'index($0,"<h2>"){f=0} index($0,"<h2>Signing out — this browser becomes a new profile</h2>"){f=1} f && index($0,">Cancel<"){n++} END{exit (n!=1)}' design/screens/account.html
  - awk 'index($0,"<h2>"){f=0} index($0,"<h2>Signing out — back to the profile this browser owns</h2>"){f=1} f && index($0,">Sign out<"){n++} END{exit (n!=1)}' design/screens/account.html
  - awk 'index($0,"<h2>"){f=0} index($0,"<h2>Signing out — back to the profile this browser owns</h2>"){f=1} f && index($0,">Cancel<"){n++} END{exit (n!=1)}' design/screens/account.html
  - awk 'index($0,"<h2>"){n++} END{exit (n!=5)}' design/screens/account.html
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/screens/account.html` draws the sign-out **confirmation step** — which it has never
drawn — in **both** of the states `ADR-0131` §1 gives it, so the words the client will carry are
decided at the card and not in a TypeScript module.

## Files

| File | Action |
| --- | --- |
| `design/screens/account.html` | modify |

Read, do not edit: `docs/adr/ADR-0131-signing-out-of-your-own-account-hands-the-browser-a-new-profile.md`
§5, `docs/adr/ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md` §3,
`web-client/src/account/SignOutControl.tsx` (the step this frame draws),
`web-client/src/account/account-text.ts` (`SIGN_OUT_WARNING`, `SIGN_OUT_LABEL`, `CANCEL`).

## Scope

Two new `<div class="frame">` blocks at the end of the existing `<div class="frames">`, drawn in
`SignOutControl`'s own confirming shape — one `<p class="line">`, then a `Sign out` button, then a
`Cancel` button, both `class="btn ghost"` like the `Sign out` control the card already draws:

- **`<h2>Signing out — back to the profile this browser owns</h2>`.** The shipped sentence, whole
  and unchanged: `Signing out leaves any duel room this browser is in, and a duel left this way can
  be lost. This browser goes back to the profile it had before.` `ADR-0131` §5 says this case *"is
  already exactly right and stays"*, so this frame writes down what ships rather than inventing
  anything.
- **`<h2>Signing out — this browser becomes a new profile</h2>`.** The same first sentence —
  `Signing out leaves any duel room this browser is in, and a duel left this way can be lost.` —
  followed by **new words of the card's own** discharging `ADR-0131` §5's **two obligations**, in
  order:
  1. that this browser will be a **new, empty profile** afterwards, and not the one it is now;
  2. that the profile being left **keeps its duel coins and its duels**, and is reached again by
     signing in with the password.

  Inside `ADR-0125` §3's **three refusals**, re-applied by `ADR-0131` §5: it **states, it does not
  urge** — no *are you sure*, no red, no count of what is at stake, no plea to attach a recovery
  address first; it is **not a warning about loss**, because nothing is lost — the coin sentence is
  there so that a number going to zero on the next screen is not a surprise, not to frighten anyone
  out of the press; and the heading of the screen stays `Account`.
- **Each `<p class="line">` in these two frames is written on a single source line**, however long.
  The other frames on this card wrap theirs, which is why `ADR-0142` §Context had to normalise
  whitespace to read them; this ticket's gates are plain `index()` matches and need the sentence
  whole on one line. Say so in the frames' `<p class="note">` so the next editor does not re-wrap
  them.
- A `<p class="note">` on each frame, naming `ADR-0131` §5 and `ADR-0125` §3, saying which of the
  two rows of `ADR-0131` §1 the frame draws and that `signedIn` decides only whether a sign-out is
  offered at all — never which of the two it is (`ADR-0135` §7).

## Out of scope

- **Any TypeScript file.** The transcription is `TASK-141008`'s, and it is a separate ticket
  because `ADR-0091` §2 puts the card first.
- **`ADR-0142`'s marker deletion.** `account.html`'s three `<!-- ANON-BLOCK: … -->` comments are
  deleted by *that* ADR's implementing ticket, which does not exist at `c6a41e6d`. Leave all three
  exactly where they are; this ticket adds none.
- **A new frame for the offered (unconfirmed) state.** The card already draws the `Sign out` button
  in its `Signed in` frame; these two frames draw the step after it.
- **The revoke confirmation.** `RevokeControl` is `TASK-041220`'s and is still not placed on this
  screen; its three sentences already exist in `account-text.ts` and are not touched.

## Tests

No test class: the gates are the `verify:` block, `awk` over the card plus the two merged design
gates.

| Gate | Proves |
| --- | --- |
| the two `<h2>` anchor gates | each new heading appears **exactly once**, so every frame-scoped gate below has exactly one span to read |
| `<h2>` total is **5** | measured: `account.html` carries **3** frames at `c6a41e6d`; two are added and no more |
| one `class="line"` per new frame | one paragraph each, exactly as `SignOutControl` renders one `<p>` — a card that split the abandoning text across two paragraphs would describe a component this client does not have |
| the returning frame carries the whole shipped `SIGN_OUT_WARNING` value | the sentence `ADR-0131` §5 says *stays* is written down verbatim, on one line, ready for `TASK-141008` to compare against |
| the abandoning frame carries the shipped **first** sentence | leaving a duel room is true on both branches, so the branch is the second half and not the whole string |
| the abandoning frame does **not** carry `goes back to the profile it had before` | the sentence that is false in this case is not in it — the exact defect `ADR-0131` §Consequences names |
| the abandoning frame contains no `are you sure`, in any case | `ADR-0125` §3's first refusal, in the one form a gate can see |
| one `>Sign out<` and one `>Cancel<` in each new frame | the confirming step draws the two controls `SignOutControl` draws, and the frames are confirmations rather than a second offer |
| `check-drift.sh`, `check-frame-cards.sh` | no bare suit glyph, no undeclared or mis-valued `--pd-` name, and `duel-table-states.html`'s frames are untouched |

## What would still pass if the coder were wrong

- **Writing both sentences into the returning frame and leaving the abandoning one empty** passes
  the `<h2>` gates and fails the abandoning frame's `class="line"` count and its first-sentence
  gate.
- **Copying the whole shipped string into the abandoning frame** — the tempting shortcut, because
  it is right there — passes every positive gate and fails the negative one, which is why that gate
  is written as an absence with an expected count of **0** rather than as prose in the ticket.
- **Wrapping the new paragraph across source lines**, the way the other three frames wrap theirs,
  makes every `index()` gate report absent. That failure is loud, and the note the frame carries is
  what tells the next editor why.
- **The three refusals themselves cannot be gated**, and this ticket does not pretend otherwise:
  *no red*, *no count of what is at stake* and *no plea about recovery* are the reviewer's, against
  `ADR-0131` §5 and `ADR-0125` §3. That is why this ticket is `review: standard` and not `light`.

## Acceptance criteria

- [ ] Both `<h2>` anchor gates exit 0, and the `<h2>` total gate (`n != 5`) exits 0
- [ ] Both `class="line"` count gates exit 0
- [ ] The returning frame's whole-sentence gate exits 0
- [ ] The abandoning frame's first-sentence gate exits 0
- [ ] The abandoning frame's two absence gates (`goes back to the profile it had before`,
      `are you sure`) exit 0
- [ ] All four button gates exit 0
- [ ] `./design/check-drift.sh` and `./design/check-frame-cards.sh` exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
