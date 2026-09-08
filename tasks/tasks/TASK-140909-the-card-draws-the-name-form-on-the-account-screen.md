---
schema: 2
id: TASK-140909
title: The card draws the name form on the account screen
type: task
status: done
parent: STORY-1409
module: design
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [design, account, client]
depends_on: [TASK-140908]
verify:
  - sh design/check-drift.sh
  - sh -c 'test $(grep -c "NAME-FORM:" design/screens/account.html) -eq 3'
  - sh -c 'test $(grep -c "<h2>" design/screens/account.html) -eq 5'
  - sh -c '! grep -qiE "chosen once|cannot change it later|permanent and cannot be changed" design/screens/account.html'
  - sh -c '! grep -qiE "suggest" design/screens/account.html'
  - sh -c 'grep -q "NAME-FORM: changeable:" design/screens/account.html'
  - sh -c 'grep -q "NAME-FORM: spent:" design/screens/account.html'
  - sh -c 'grep -q "NAME-FORM: throttled:" design/screens/account.html'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/screens/account.html` draws the name form in both of its states — a profile with no name
yet, and a profile changing the name it holds — and carries, as three one-line markers the next two
tickets read back out, the exact words for `ADR-0130` §5's two obligations and `ADR-0134` §5's
`throttled` sentence.

## Files

| File | Action |
| --- | --- |
| `design/screens/account.html` | modify |

Read, and do not edit:
[`ADR-0130`](../../docs/adr/ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md) §5
and §6;
[`ADR-0134`](../../docs/adr/ADR-0134-a-rename-spends-before-it-replaces.md) §5;
[`ADR-0091`](../../docs/adr/ADR-0091-a-screen-is-drawn-before-it-is-built.md) §2 — the card owns the
words, the heading and the layout;
`web-client/src/profile/NameSurface.tsx` — the shape the frames are drawn against.

## Scope

- **Two new frames**, beside the three that ship, in the existing `.frame` shape: *No name yet* and
  *Changing a name*. Both show the field, the button and the two obligations **above** the field;
  the second additionally shows the name the player holds now.
- **A third addition to an existing frame is not made.** The removal notice (`ADR-0052` §1) is
  already drawn where a name is set and travels with the surface unchanged (`ADR-0130` §6); note
  that in the frame's `.note`, do not redraw it.
- **Three one-line markers**, in HTML comments, so `TASK-140910` and `TASK-140911` can transcribe
  without a human copying by eye:
  - `<!-- NAME-FORM: changeable: … -->` — `ADR-0130` §5's obligation 1, *it can be changed later*
  - `<!-- NAME-FORM: spent: … -->` — obligation 2, *the name you give up is gone for good; you
    cannot take it back and nobody else can take it either*
  - `<!-- NAME-FORM: throttled: … -->` — `ADR-0134` §5's refusal sentence for a `429`, which is a
    statement about **requests** and must not suggest the name was rejected
- **The words are this ticket's decision**, within those three obligations. Nothing below chooses a
  word: `TASK-140911` copies the first two character for character and `TASK-140910` copies the
  third.

## Out of scope

- **`PERMANENCE_LINE`'s sentence in any form.** `ADR-0130` §5 removes it; a `verify` command refuses
  *chosen once*, *cannot change it later* and *permanent and cannot be changed* anywhere on this
  card. The bare word *permanent* is **not** refused: line 126 already uses it correctly, about
  permanent ownership of an account through a recovery address, and a blanket ban would fail on a
  sentence this story has no business touching.
- **A suggestion, a *changes remaining* counter, a confirmation press or a quota.** `ADR-0130` §1,
  §5 and §7; `ADR-0134` §6.
- **Any TypeScript.** `TASK-140911` and `TASK-140912` build against this card.
- **A *formerly known as* line, a list of previous names, or any mark on a changed row.**
  `ADR-0130` §3.
- **Changing the heading.** It stays `Account` (`ADR-0125` §3).

## Tests

A design card has no unit tests; its gates are `design/check-drift.sh` and the `verify` greps above.

| Gate | Proves |
| --- | --- |
| `check-drift.sh` | every `--pd-*` the new frames name is declared in the canonical sheet at the canonical value, and every inlined symbol still matches its source |
| three `NAME-FORM:` markers | the words exist in a machine-readable form the next two tickets can read; exactly three, so a fourth obligation invented here fails |
| five `<h2>` | three shipped frames plus exactly two new ones — a third new frame, or one absorbed into an existing one, fails |
| the negative greps | `PERMANENCE_LINE`'s promise and any suggestion are absent from the card |

## What would still pass if the coder got it wrong

- **A card that drew the frames but wrote no markers** passes `check-drift.sh` and every visual
  reading. The marker count is the only gate, and it is an **equality** at three rather than a
  presence check, so both a missing marker and an invented fourth obligation fail.
- **Words that satisfied neither obligation** — a marker reading `spent: TBD` — would pass a
  presence grep. That is what review is for on this ticket, and it is why `review: standard` rather
  than `light`: the gates can prove the markers are there and cannot prove they say the right thing.
- **A card that kept the permanence sentence and added the two obligations beside it** would draw a
  screen that contradicts itself. The negative greps are what catch it, and they are three separate
  phrases rather than one, because the sentence could be reworded and still make the promise.

## Acceptance criteria

- [ ] `design/screens/account.html` has exactly five `<h2>` frames, the last two being the name form
      with no name yet and the name form changing a name
- [ ] Exactly three `NAME-FORM:` markers exist, one each for `changeable`, `spent` and `throttled`
- [ ] The strings `chosen once`, `cannot change it later` and `permanent and cannot be changed`
      appear nowhere in the file, case-insensitively — and the existing sentence at line 126 about
      permanent ownership of an account is left alone
- [ ] The string `suggest` appears nowhere in the file, case-insensitively
- [ ] `sh design/check-drift.sh` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
