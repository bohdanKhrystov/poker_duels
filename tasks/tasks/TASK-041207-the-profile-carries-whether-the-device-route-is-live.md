---
schema: 2
id: TASK-041207
title: The profile carries whether the device route is still live
type: task
status: ready
parent: STORY-0412
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 4
atomic:
  - web-client `npm run typecheck` — a required field on `PlayerProfile` fails every construction site at once
  - web-client `npm run test` — `profile-fixture.test.ts` enumerates both fixtures' keys and fails on the field it does not name
labels: [client, profile, auth]
depends_on: [TASK-041206]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'builds a profile carrying every field PlayerProfile declares'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'builds bodies carrying every field the wire declares, opponent id included'
  - cd web-client && npm run check
  - cd web-client && npm run build
---

## Goal

`PlayerProfile` carries `deviceRouteLive`, the one server-sent fact the account screen reads to say
whether this device still signs in (`ADR-0050` §4).

## Files

Four, and the count was **probed, not remembered** (`ADR-0069`, `ADR-0070`): the field was stubbed
onto `PlayerProfile` alone and the client gate set from `.github/workflows/build.yml` — `npm ci`,
`npm run check`, `npm run build` — was run until it exited 0. The first red run named three files;
the fourth was behind the format and test gates and is exactly the prefix `ADR-0070` warns about.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `web-client/src/profile/profile.ts` | modify | The field itself, and `profileFromBody`'s returned object — `tsc` reports `TS2741` on the constructor in this same file |
| `web-client/src/profile/profile-fixture.ts` | modify | `aProfile` fails `TS2322` and `meBody` must carry what the wire carries; both are in this one file |
| `web-client/src/profile/set-name-provider.test.tsx` | modify | Builds a `PlayerProfile` object literal by hand and fails `TS2741`. Nothing else in this ticket touches set-name |
| `web-client/src/profile/profile-fixture.test.ts` | modify | Two tests enumerate `Object.keys(aProfile())` and `Object.keys(meBody())` against sorted literal lists, and fail once the field exists. **Invisible in the first probe run**, because `tsc` and `prettier` both fail before `vitest` runs |

Read, and do not edit: `docs/protocol.md` *Profile endpoint*;
[`ADR-0049`](../../docs/adr/ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md) §5;
[`ADR-0050`](../../docs/adr/ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md) §3, §4.

## Scope

- `PlayerProfile` gains `readonly deviceRouteLive: boolean;`, declared after `displayNameRemoved`, in
  the wire's own order. KDoc: `true` exactly when this player holds a live device binding; `false`
  covers **revoked** and **never bound**, two cases the field deliberately does not distinguish —
  `ADR-0049` §5's uniform answer, one layer up.
- `profileFromBody` requires it: `typeof body.deviceRouteLive === "boolean"`, and the parsed object
  carries it. No default, for the reason the two neighbours give — an absent field on the wire must
  not become `false`, which would tell a bound player their device does not sign in.
- `aProfile` and `meBody` each gain the field. The value is `true`, which is the state of every
  player who has not revoked, and the two fixtures agree.
- `profile-fixture.test.ts`'s two key lists gain `"deviceRouteLive"` in sorted position. **Nothing
  else in that file changes**, no assertion is weakened, and no test is removed.
- `set-name-provider.test.tsx`'s literal gains the field and **nothing else** — that test is about a
  provider handing down a stable reference and this ticket has no opinion about it.

## Out of scope

- **`hasRecoveryEmail`.** It is on the same response and belongs to `STORY-0417`; adding it here
  would put a field on screen that nothing reads. **A refusal, not an omission** — a criterion greps
  for it.
- **Asserting the parse.** `TASK-041208` writes the tests that prove `profileFromBody` reads both
  values and refuses a body without the field. They belong in `profile.test.ts`, and **no gate names
  that file** — the probe reached green without it — so under `ADR-0068` it cannot be a fifth
  `atomic:` row. This is `TASK-041641`'s precedent applied deliberately.
- Rendering the field anywhere. `TASK-041217`.
- Any server change. `ProfileResponse` already carries `deviceRouteLive`; this is the client catching
  up.

## Tests

No new test file. The gate is two existing tests in `web-client/src/profile/profile-fixture.test.ts`,
which change from failing to passing:

| Test | Proves |
| --- | --- |
| `builds a profile carrying every field PlayerProfile declares` | `Object.keys(aProfile()).sort()` equals the five names including `"deviceRouteLive"`. Reddens the day a fixture stops carrying a field the type declares |
| `builds bodies carrying every field the wire declares, opponent id included` | The same for `meBody()`, which is what every parse test is built from |

## Acceptance criteria

- [ ] `the profile fixtures > builds a profile carrying every field PlayerProfile declares` passes
      with `"deviceRouteLive"` in the list
- [ ] `the profile fixtures > builds bodies carrying every field the wire declares, opponent id
      included` passes with `"deviceRouteLive"` in the list
- [ ] `npm run typecheck` exits 0 with `deviceRouteLive` declared **without** `?` and **without** a
      default
- [ ] `grep -c 'hasRecoveryEmail' web-client/src/profile/profile.ts` returns `0`
- [ ] `git diff --stat web-client/src/profile/set-name-provider.test.tsx` shows exactly one line added
      and none removed
- [ ] `npm run check` reports the same test count as `develop` — this ticket adds no test
- [ ] `npm run build` exits 0
- [ ] No file outside the four listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Give the field a default in `aProfile` and remove it from `profile-fixture.test.ts`'s first list.
   **`builds a profile carrying every field PlayerProfile declares` reddens alone.** Revert.
2. Make `profileFromBody` fill `deviceRouteLive: true` rather than reading it from the body.
   **Nothing reddens** — the whole gate set exits 0. Run this one and record it: it is the reason
   `TASK-041208` exists, and it is a client asserting a server fact, which is the defect this field
   was added to prevent. Revert.
3. Declare the field `deviceRouteLive?: boolean`.
   **`npm run typecheck` passes and both fixture tests still pass**, because an optional field is
   still enumerable once the fixtures set it. What reddens is nothing at all — record that the `?`
   is caught by the criterion above and by review, not by a test.
4. Remove the field from `meBody` only.
   **`builds bodies carrying every field the wire declares…` reddens alone**, and `npm run
   typecheck` stays green — `meBody` returns `Record<string, unknown>`, so the compiler has no
   opinion. That asymmetry is why both fixtures are named in this ticket and both tests in the gate.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
