---
schema: 2
id: TASK-140907
title: The document answers 429 and no longer answers 403
type: task
status: done
parent: STORY-1409
module: docs
estimate: XS
tier: haiku
review: standard
files_touched: 3
labels: [docs, protocol, server, account]
depends_on: [TASK-140906]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.HttpEndpointDocumentationTest' -PrequireDocker=true
  - grep -q 'tests="45" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.http.HttpEndpointDocumentationTest.xml
  - sh -c 'awk "/^### Set display name/{f=1} /^### Revoke this device/{f=0} f" docs/protocol.md | grep -qF "429 Too Many Requests"'
  - sh -c '! awk "/^### Set display name/{f=1} /^### Revoke this device/{f=0} f" docs/protocol.md | grep -qF "403"'
  - sh -c '! awk "/^### Set display name/{f=1} /^### Revoke this device/{f=0} f" docs/protocol.md | grep -qF "permanent"'
  - sh -c '! grep -qF "permanent" poker-server/src/main/kotlin/duels/poker/server/http/DisplayName.kt'
  - sh -c 'awk "/^### Profile endpoint/{f=1} /^### Set display name/{f=0} f" docs/protocol.md | grep -qF "renaming"'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`docs/protocol.md` says what the server now does: `PUT /api/me/name` answers
`200 | 400 | 401 | 409 | 429`, `displayNameRemoved` reads `false` for a player who replaced a name
by renaming, and the two sentences that gave *permanence* as a rule's reason give the reason that is
actually true — a string is spent forever once written.

## Files

Three.

| File | Action |
| --- | --- |
| `docs/protocol.md` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/HttpEndpointDocumentationTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/http/DisplayName.kt` | modify |

Read, and do not edit:
[`ADR-0134`](../../docs/adr/ADR-0134-a-rename-spends-before-it-replaces.md) §4 and §5;
[`ADR-0130`](../../docs/adr/ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md) §1.

## Scope

- **The `403 Forbidden` row leaves the *Set display name* response table**, and a
  `429 Too Many Requests` row arrives: empty body, no `Retry-After` header, and *Retryable?* reading
  that the client may try again shortly — the refusal is about requests, not about the name, and a
  `429` invalidates nothing the player typed (`ADR-0134` §5).
- **The `200 OK` row's *Retryable?* cell stops saying the client already owns the name and cannot
  change it.** Setting a name is now something a player may do again.
- **Canonicalisation rule 4's doubled-space clause keeps the rule and changes its reason**: names
  that render identically but are stored differently are *spent forever once written*, so a
  doubled-space variant burns a second string that renders the same. The rule does not change.
- **`DisplayName.kt`'s KDoc** loses *"and is then permanent"* for the same reason, in the same
  words. No behaviour, no character rule, no test of that file changes.
- **The `displayNameRemoved` row in the *Profile endpoint* section gains a fifth case**:
  *holds a name, replaced one by renaming → `false`* (`ADR-0134` §4). A reader who now knows a
  rename writes `retired_from` would otherwise conclude the field is broken.
- **`HttpEndpointDocumentationTest.theDocumentDescribesTheSetNameEndpoint`** lists
  `"400", "401", "409", "429"`, and gains an assertion that the section contains **no** `403`. A new
  test, `theSetNameSectionRefusesToPromisePermanence`, asserts the section does not contain the word
  `permanent`.

## Out of scope

- **Any behaviour.** Every gate here reads the document or a comment.
- **The `/api/me` field list itself.** `ADR-0134` §4 changes no field; only the `displayNameRemoved`
  row's prose gains a case.
- **`ADR-0053`'s own index entry.** `TASK-140908` carries it, beside the comment in
  `PostgresProfileReads` it belongs with.
- **`docs/operations.md`.** `ADR-0134` §8: nothing an operator does changes.

## Tests

`HttpEndpointDocumentationTest` — 44 tests on `develop` at `develop` today — the ticket's 43 predates `TASK-140808`, which added one case, 44 after.

| Test | Proves |
| --- | --- |
| `theDocumentDescribesTheSetNameEndpoint` | *(modified)* the *Set display name* section documents `400`, `401`, `409` and `429`, and contains no `403` |
| `theSetNameSectionRefusesToPromisePermanence` | *(new)* the word `permanent` appears nowhere in that section |
| `theSetNameSectionIsStillWhereItWas` | *(unchanged, must stay green)* the section still names its path and its `409` row |

## What would still pass if the coder got it wrong

- **The four-status loop is a *contains* check, so leaving the `403` row in place passes it.** That
  is why the negative assertion exists, and why a `verify` command greps the section independently:
  a positive-only gate over a document can never notice a row that should have gone.
- **A `429` mentioned in the prose but absent from the table** satisfies `contains("429")`. The
  `verify` command therefore greps for the literal `429 Too Many Requests`, which is the table
  cell's own text and not a sentence anyone would write in passing.
- **A `permanent` left in the canonicalisation rule** passes every merged test in the file; the new
  test is the only gate, and it is scoped to the section rather than the whole document, because
  the sign-in and device sections legitimately use the word about revocation.
- **The `displayNameRemoved` fifth case** is gated by a grep for `renaming` inside the *Profile
  endpoint* section — a word that appears nowhere in that section today, measured — because a
  reflection test over the DTO cannot see prose.

## Acceptance criteria

- [ ] `HttpEndpointDocumentationTest.theDocumentDescribesTheSetNameEndpoint` passes and asserts the
      section contains no `403`
- [ ] `HttpEndpointDocumentationTest.theSetNameSectionRefusesToPromisePermanence` passes
- [ ] `HttpEndpointDocumentationTest` reports exactly 44 tests, `failures="0" errors="0"` — 43
      measured on `develop` at `1c3c7fd9` plus the one new case
- [ ] The *Set display name* section contains `429 Too Many Requests`, no `403` and no `permanent`
- [ ] The *Profile endpoint* section's `displayNameRemoved` row names the renaming case
- [ ] `permanent` appears nowhere in `DisplayName.kt`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
