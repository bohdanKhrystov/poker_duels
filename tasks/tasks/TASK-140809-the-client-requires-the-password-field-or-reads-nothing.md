---
schema: 2
id: TASK-140809
title: The client requires the password field, or reads nothing
type: task
status: done
parent: STORY-1408
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 7
atomic:
  - web-client `npm run typecheck` — a required field on `PlayerProfile` fails every construction site at once, `TS2741`/`TS2322`
  - web-client `npm run test` — `profile-fixture.test.ts` enumerates both fixtures' keys against sorted literal lists and fails on the field it does not name
  - web-client `npm run test` — `account-server.test.ts` asserts four whole `GET /api/me` bodies, so the stub server's two bodies and the four expectations move together
labels: [client, profile, account]
depends_on: [TASK-140808]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a body with no hasPassword is not a profile'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'carries the password flag both ways'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'builds a profile carrying every field PlayerProfile declares'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'builds bodies carrying every field the wire declares, opponent id included'
  - awk 'index($0, "hasPassword") { n++ } END { exit (n < 3) }' web-client/src/profile/profile.ts
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`PlayerProfile` carries `hasPassword`, and `profileFromBody` **requires** it — so a `GET /api/me`
body without the field makes the read `unavailable`, never a profile whose `hasPassword` is `false`.
That is the one line of code that keeps `ADR-0125` §4's *not told at all* case reachable
(`ADR-0132` §4).

## Files

Seven — six **probed not remembered** (`ADR-0069`, `ADR-0070`): `readonly hasPassword: boolean` was added to
`PlayerProfile` alone and the client gate set from `.github/workflows/build.yml` — `npm ci`,
`npm run check`, `npm run build` — was run until it exited `0`. The first red run named three files;
the next three sat behind the typecheck and lint stages and are exactly the prefix `ADR-0070` warns
about. The seventh is named by no gate at all, and its row says why it is here anyway.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `web-client/src/profile/profile.ts` | modify | The field, the parser's type guard and the returned object — `tsc` reports `TS2741` on the constructor in this same file |
| `web-client/src/profile/profile-fixture.ts` | modify | `aProfile` fails `TS2322`, and `meBody` must carry what the wire carries; both are in this one file |
| `web-client/src/profile/set-name-provider.test.tsx` | modify | Builds a `PlayerProfile` object literal by hand and fails `TS2741`. Nothing else in this ticket touches set-name |
| `web-client/src/profile/profile-fixture.test.ts` | modify | Two cases compare `Object.keys(...).sort()` against sorted literal lists and fail once the field exists. Note the sort: `hasPassword` sorts **before** `hasRecoveryEmail` |
| `web-client/src/e2e/account-server.ts` | modify | The stub server builds two `GET /api/me` bodies by hand; without the field every arc's profile read answers `unavailable` |
| `web-client/src/e2e/account-server.test.ts` | modify | Five expectations assert a whole profile body object, and four of its cases fail |
| `web-client/src/profile/profile.test.ts` | modify | The only file that tests `profileFromBody`. **No gate names it** — it builds every body through `meBody` and passed the probe untouched — and `ADR-0132` §4's requirement is unproven without the two cases below |

Read, and do not edit:
[`ADR-0132`](../../docs/adr/ADR-0132-the-profile-says-whether-it-holds-a-password.md) §4 and §8;
`docs/protocol.md` *Profile endpoint* — the field's contract, landed by `TASK-140808`.

## Scope

- **`profile.ts`:** add `readonly hasPassword: boolean` **last** on `PlayerProfile`, with a KDoc line
  saying what it means and that it is the server's answer rather than anything derived. Add
  `typeof (body as Record<string, unknown>).hasPassword === "boolean"` to `profileFromBody`'s guard,
  beside the three booleans already there, and carry it into the returned object. Update the function
  KDoc's list of required fields.
- **`profile-fixture.ts`:** `aProfile` and `meBody` both gain `hasPassword: false` before the
  `...overrides` spread, so a test can bend it. Correct `meBody`'s KDoc, which enumerates the fields
  it carries and is already one field out of date.
- **`account-server.ts`:** both hand-built profile bodies gain `hasPassword: false`. The stub does
  **not** learn to remember a password — see *Out of scope*.
- **`account-server.test.ts`:** add the field to the five body expectations; no assertion is weakened
  and no case is deleted.
- **`profile-fixture.test.ts`:** add `"hasPassword"` to both sorted key lists, **before**
  `"hasRecoveryEmail"`.
- **`set-name-provider.test.tsx`:** add `hasPassword: false` to the hand-built profile literal.
- Add the two new parser cases below to `profile.test.ts` — the seventh row of the table. No gate
  named it (it builds every body through `meBody`, so it compiled and passed untouched through the
  probe); it is in the ticket because `ADR-0132` §4's whole requirement — *a missing field reads
  `unavailable`, never `false`* — is unproven without them, and `profile.test.ts` is the only file in
  the client that tests `profileFromBody`.

## Out of scope

- **Teaching `account-server.ts` to remember a password.** After this ticket the stub answers
  `hasPassword: false` for every player, including one that has just signed up. An arc that signs up
  and then reads `true` would prove `TASK-140814` end to end and is worth a later ticket; it is not
  ticketed yet and is named in `STORY-1408`'s *Out of scope*.
- **Rendering anything.** `AccountScreen` does not read the field until `TASK-140812`.
- **Re-keying `showPasswordRoute`, `showSignUp`, `showSignInDoor` or `showAttach`** — `ADR-0132` §5.
- **`protocol.gen.ts`.** `ADR-0132` §7: this is a hand-written plain-HTTP DTO and the generator has no
  subject here.

## Tests

`profile.test.ts`

| Test | Proves |
| --- | --- |
| `a body with no hasPassword is not a profile` | `profileFromBody` answers `null` for a body carrying the other six fields and omitting this one, and answers `null` again for one carrying `hasPassword: "yes"`. Two inputs, because a guard that only checked presence would pass the second |
| `carries the password flag both ways` | `profileFromBody(meBody({ hasPassword: true }))` answers a profile whose `hasPassword` is `true`, and `meBody({ hasPassword: false })` answers `false`. Both, because one fixture default cannot tell a copy from a constant |

## Acceptance criteria

- [ ] `a body with no hasPassword is not a profile` passes, with both of its inputs
- [ ] `carries the password flag both ways` passes, with both of its inputs
- [ ] `builds a profile carrying every field PlayerProfile declares` and
      `builds bodies carrying every field the wire declares, opponent id included` both pass
- [ ] `readProfile` over a body missing the field answers `{ kind: "unavailable" }` — this follows
      from `profileFromBody` answering `null`, and `profile.test.ts` already covers that path for the
      other required fields
- [ ] `npm run check` and `npm run build` exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
