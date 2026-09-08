---
schema: 2
id: TASK-140812
title: The account screen names the anonymous profile
type: task
status: backlog
parent: STORY-1408
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, account]
depends_on: [TASK-140811]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names the profile anonymous when the profile it holds has no password'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says nothing about the state to a profile that holds a password'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says nothing about the state where it was told nothing'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says the same words to a named player and a nameless one'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'never reads the session token for the state'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'carries exactly one heading'
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`AccountScreen` renders `ADR-0125` §3's block when, and only when, the profile is in hand and the
server said it holds no password — `profile.kind === "profile" && !profile.profile.hasPassword` —
and consults `signedIn` for it never (`ADR-0132` §4).

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/AccountScreen.tsx` | modify |
| `web-client/src/account/AccountScreen.test.tsx` | modify |

Read, and do not edit:
`web-client/src/account/account-text.ts` — the three constants landed by `TASK-140811`;
`web-client/src/profile/profile.ts` — `PlayerProfile.hasPassword` and `ProfileRead`;
[`ADR-0125`](../../docs/adr/ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md)
§3, §4 and §6;
[`ADR-0132`](../../docs/adr/ADR-0132-the-profile-says-whether-it-holds-a-password.md) §4 and §5;
`design/screens/account.html` — where the three lines sit in the frame's order.

## Scope

- Add one derived value beside the screen's existing ones, in the same shape and with the same kind
  of comment:
  `const anonymous = profile !== null && profile.kind === "profile" && !profile.profile.hasPassword;`
  and render the three sentences as `<p className="text-small">` lines when it is true, positioned as
  the card draws them — after the route sentences, before `SignUpForm`.
- The comment beside it says the one thing a later reader must not undo: this is the **told** fact,
  negated, and it is not `!signedIn`. A browser holding no token is not a browser whose player holds
  no password (`ADR-0125` §4, and `TASK-120601` found that exact defect on this screen once already).
- **Nothing else is re-keyed.** `showPasswordRoute` stays `signedIn && …`, and `showSignUp`,
  `showAttach`, `showSignInDoor` and `SignOutControl` keep the predicates they have — `ADR-0132` §5
  and `ADR-0125` §4 both say so in as many words.
- **The block adds no heading.** `carries exactly one heading` asserts the screen has exactly one, and
  it must still pass. No `<h2>`, `<h3>`, `role="heading"`, badge, pill or status label — `ADR-0125`
  §3's *not a tier*.
- **The block contains no `@`.** `renders no address, because it is given none and asks for none`
  asserts `container.textContent` never matches `/@/`, and it must still pass.

## Watch: `aProfile()` now defaults to `hasPassword: false`

Every existing case in `AccountScreen.test.tsx` that hands the screen `aProfile()` will start
rendering the block. **No existing assertion may be weakened, deleted or edited to accommodate it** —
they are all `getByText`/`queryByText` on other strings, plus the two absolute assertions named above,
and every one of them still holds. If one does not, that is a finding to report rather than a test to
change.

## Out of scope

- Re-reading the profile after a sign-up — `TASK-140813` and `TASK-140814`. Until they land, a browser
  that signs up here keeps the profile it read at mount and the block stays on screen beside
  `SIGNED_UP` until the tab next boots. That is the state `ADR-0132` §6 exists to fix, and fixing it
  needs the provider, which is not this ticket's file.
- Anything on the front door, the result screen or the strip.
- `DEC-090` — whether *Attach a recovery address* should show on a profile with no password. Open, the
  product owner's, and `showAttach` is unchanged here.
- Choosing or editing a word. The strings come from `account-text.ts`, which took them from the card.

## Tests

`AccountScreen.test.tsx`

| Test | Proves |
| --- | --- |
| `names the profile anonymous when the profile it holds has no password` | Given `aProfile({ hasPassword: false })`, all three sentences are on screen |
| `says nothing about the state to a profile that holds a password` | Given `aProfile({ hasPassword: true })`, none of the three is on screen — and the screen still renders, so this is a withheld block and not an empty tree |
| `says nothing about the state where it was told nothing` | Three inputs in one case — `profile={null}` (still loading), `{ kind: "no-profile" }` and `{ kind: "unavailable" }` — and none of the three sentences appears under any of them. One input could not tell a rule from a coincidence |
| `says the same words to a named player and a nameless one` | Two renders at `hasPassword: false`, one with `displayName: "Bob"` and one with `displayName: null`; the three sentences are present and **identical** in both, and neither render puts the name inside the block. `ADR-0125` §6 after `ADR-0119`: named and anonymous is an ordinary state |
| `never reads the session token for the state` | Two renders that cross the two facts: `signedIn: true` with `hasPassword: false` **shows** the block, and `signedIn: false` with `hasPassword: true` **hides** it. A screen deriving the state from the token fails both halves, and either half alone would pass a screen that derived it |
| `carries exactly one heading` | Unchanged, and it must still pass — the block adds no heading |

## Acceptance criteria

- [ ] `names the profile anonymous when the profile it holds has no password` passes
- [ ] `says nothing about the state to a profile that holds a password` passes
- [ ] `says nothing about the state where it was told nothing` passes, with all three of its inputs
- [ ] `says the same words to a named player and a nameless one` passes, with both of its renders
- [ ] `never reads the session token for the state` passes, with both of its crossed renders
- [ ] `carries exactly one heading` and
      `renders no address, because it is given none and asks for none` both still pass, unedited
- [ ] No existing case in `AccountScreen.test.tsx` has an assertion removed or weakened
- [ ] `npm run check` and `npm run build` exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
