---
schema: 2
id: TASK-120601
title: A claimed profile is never offered the claim form again
type: task
status: backlog
parent: STORY-1206
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [qa, bug, medium]
depends_on: []
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF "a successful claim leaves this browser holding a session"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF "a claim the server refuses leaves no session behind"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF "a claim whose follow-up sign-in fails is still a claim"
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A browser that has just given its profile a password holds a session afterwards, so every later
load of the account screen states *"Your password signs in to this account."* instead of offering
the claim form to a profile that already has one.

## The defect

Round 2 of `/qa-cycle regression`, 2026-08-29, commit `c7b35f4b`. Reported against `04-02`, one
click beyond that case's own check.

**A profile that has a password is shown the unclaimed-profile form on every reload, indefinitely.**
The screen omits `Your password signs in to this account.` and renders *Handle*, *Password* and
*Give this profile a password* — the same screen an unclaimed profile gets. Submitting it again is
correctly refused (`That handle is taken, or this profile already has a password.`) and the
`credential` row is never touched, so nothing is lost or corrupted: the screen states a falsehood
about the account and nothing else.

**The mechanism.** `web-client/src/main.tsx:182` computes
`signedIn = readSessionToken(localStorage ?? nullStorage) !== null`, and
`web-client/src/account/AccountScreen.tsx` decides both statements from it:

    const showPasswordRoute = signedIn && profile !== null && profile.kind === "profile";
    const showSignUp = !signedIn && profile !== null && profile.kind === "profile" && signUp !== undefined;

The comment above them derives `showPasswordRoute` correctly — `POST /api/auth/sign-in` is the only
endpoint that ever issues a session token, so a browser holding one is a browser whose player has a
password. **That derivation runs one way only, and `showSignUp` uses its converse.** A browser with
no session is not a browser whose player has no password.

`docs/protocol.md`'s sign-up row already says what closes the gap on the reported path:

> `201 Created` — One `credential` row now points at the profile this request resolved to;
> **no session is issued** and the client signs in afterwards.

**The client does not sign in afterwards.** `web-client/src/account/sign-up.ts` returns
`{ kind: "signed-up" }` and stops, `SignUpForm` swaps itself for `This profile now has a password.`,
and that sentence is component state a reload throws away. The browser holds `pd.deviceId` and
nothing else, so `signedIn` is false forever.

## The reproduction, by hand (`ADR-0089` §4)

Run at commit `c7b35f4b` on the round's live stack, on **two** browser profiles, with the driver,
which is a player's hands (`ADR-0089` §3):

    node scripts/qa/drive.mjs 9233 open              # a plain navigation to /
    node scripts/qa/drive.mjs 9233 click "Account"
    node scripts/qa/drive.mjs 9233 text 1200

renders *Handle / Password / Give this profile a password* and no password-route line, for the
player behind device `MjF3MdDWR_cDpsfbEQAlIw`, whose `credential` row
(`84e8e12a-2123-4388-bcbd-5a138a630c67`, `password`, `winnerplayer`, secret present) was read from
the database in the same minute. The same three commands on port 9232
(`UEMJWw0n0DezVTe_L0McoQ`, `loserplayer`) render the same screen.

**The control is visible, not a driver artifact.** `TASK-120505` made that distinction matter, so it
was checked rather than assumed:

    [{"t":"Give this profile a password","hidden":false,"off":"shown","rect":312}, ...]

**And the browser really holds no session**: `localStorage` on that profile is
`{"pd.deviceId":"MjF3MdDWR_cDpsfbEQAlIw"}` — one key, no token. It reproduces; it is a product
defect.

## Why `medium` and not `high`

`qa` reported `medium`. **Severity unchanged**, and the reasoning is written out because the count
this feeds decided the round's verdict.

- No vision promise is broken. Hole cards stay secret, the winner is right, the coins are right,
  rematch works — the four properties `EPIC-12`'s severity table names, plus every other property in
  `docs/vision.md`, are untouched by this screen.
- **Nothing is lost and nothing is corrupted.** The credential exists, the server refuses the
  re-claim on its own authority, and the false display cannot cause a state change.
- **There is a workaround, and the product offers it on the same screen.** *Sign in* is one control
  below the form; `ADR-0083` §2 lands a successful sign-in on `#/account`, which then states the
  password route.

It is a real defect with a workaround, which is `medium`. It is **not** filed to the backlog to make
a number fall: it would be `medium` under any `B(N-1)`, and `EPIC-12` §Termination counts it in
neither direction, since `B(N)` counts `blocker` and `high` only.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/sign-up.ts` | modify |
| `web-client/src/account/sign-up.test.ts` | modify |
| `web-client/src/account/sign-in.ts` | read |

## Scope

- **After `201`, sign the browser in** with the handle and password already in hand, per
  `docs/protocol.md`'s own sentence, so the token `signedIn` reads lands in storage.
- **The claim's outcome never depends on the follow-up.** A `201` is `{ kind: "signed-up" }` whether
  or not the sign-in that follows it succeeds: the credential was created, and a client that reported
  otherwise would tell the player their claim failed when it did not.
- **A refused claim sends no follow-up at all.** No second request on `400`, `409`, `422`, `401`,
  `429` or an unknown status — `ADR-0056` §3 forbids the client retrying by itself, and a sign-in
  after a refusal is a request about a credential that was never created.

## Out of scope

- **`AccountScreen.tsx`.** Its two guards are the mechanism, and they are correct for every browser
  that holds a session. Changing them is the other half below, which needs a fact this client cannot
  currently obtain. Do not edit this file.
- **The other half of the defect, which this ticket does not repair and does not pretend to.** A
  browser that signed out — or whose session expired — still cannot tell a claimed profile from an
  unclaimed one, and still gets the form. The round-2 report reproduced exactly that on player A
  through the product's own *Sign out* control. Repairing it needs the account screen to read
  whether a credential exists, and **no endpoint carries that fact**: `GET /api/me` returns
  `deviceRouteLive` and `hasRecoveryEmail` and nothing about a password.
- **Adding a field to `ProfileResponse` to close it.** `ADR-0050` §4 says in as many words:
  *"It needs no new server fact: `deviceRouteLive` is the whole of what the screen reads"*, and
  *"no `ProfileResponse` field"*. **That sentence is now known to be wrong, and overturning it is a
  decision, not a repair.** Whoever picks that half up registers a `DEC` and routes it to the
  `architect` agent before writing code (`CLAUDE.md` rule 5). Not yet ticketed; it waits on that
  answer.
- **`web-client/src/result/account-offer.ts`.** It carries the same conflation — its doc comment
  says *"they do not already hold a credential"* over a field named `signedIn` — so a claimed player
  who never signed in is offered an account after a win. It is the same root cause, it is bounded by
  `pd.accountOfferSettled` (`ADR-0086`), and this ticket's repair removes the reported path to it.
  Not a second defect and not a second ticket.

## Tests

`sign-up.test.ts`

| Test | Proves |
| --- | --- |
| `a successful claim leaves this browser holding a session` | a `201` is followed by one `POST /api/auth/sign-in`, and the token it answers is in storage afterwards |
| `a claim the server refuses leaves no session behind` | **at least two** refusal statuses — `409` and `422` — each send exactly one request and leave storage without a token |
| `a claim whose follow-up sign-in fails is still a claim` | a `201` whose follow-up answers `401` or `429` still returns `{ kind: "signed-up" }`, and storage holds no token |

## Acceptance criteria

- [ ] `sign-up.test.ts > a successful claim leaves this browser holding a session` passes
- [ ] `sign-up.test.ts > a claim the server refuses leaves no session behind` passes, over two
      different refusal statuses rather than one
- [ ] `sign-up.test.ts > a claim whose follow-up sign-in fails is still a claim` passes
- [ ] Reverting `sign-up.ts` alone reddens all three; the reviewer runs this rather than reading it,
      because a test that passes against the pre-fix module gates nothing
- [ ] **By hand, on a live stack** — the browser-level half no jsdom test reaches: claim an
      unclaimed profile, navigate to `/`, open *Account*, and read
      `Your password signs in to this account.` with no *Give this profile a password* control on
      the screen
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
