---
schema: 2
id: TASK-041627
title: A build with no sender is a valid build
type: task
status: backlog
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, config, mail, wiring, blocked]
depends_on: [TASK-041626]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.mail.NoSenderConfiguredTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.config.ServerConfigTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Blocked

**`DEC-072` — the architect's**, in full. This ticket *is* the decision's implementation: what the
wiring holds when no sender is configured, where `baseUrl` lives and what an unconfigured one does,
what the detached delivery is a child of, what a failed send does, and what a test can await. None
of it is derivable and all of it shapes `TASK-041625` and `TASK-041626`.

**Not the human's and not about money.** Choosing an SMTP relay or a provider API — and therefore a
bill — is `ADR-0031` §7's explicit deferral to `EPIC-07`, and it is the human's. This ticket builds
the seam that lets the server ship, be tested and behave identically from the outside **with no
sender at all**. If answering `DEC-072` seems to require naming a provider, the decision has drifted
into the human's territory and should stop rather than pick one.

## Goal

The server runs, and all five recovery endpoints behave exactly as specified, with no mail sender
configured — which is the state every developer machine and every CI run is in.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/mail/…` | create |
| `poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/mail/NoSenderConfiguredTest.kt` | create |

The first row's exact path and file name are `DEC-072`'s to fix; `ADR-0031` §7 places the mailer
implementation in `duels.poker.server.mail`. `ServerComponents.kt` is **not** listed: it takes a new
field with zero propagation — probed, see *Note* — but whether it needs one at all depends on the
answer, and a fourth row would need a gate.

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryMailer.kt`;
`poker-server/src/main/kotlin/duels/poker/server/ServerComponents.kt`;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §4's `baseUrl` clause and §7;
the ADR answering `DEC-072`.

## Scope

- Whatever `DEC-072` names for the no-sender state, in `duels.poker.server.mail`.
- `ServerConfig` gains `baseUrl` with a key, an env name and a default, in the shape every other
  field uses. **It is never derived from a request header** — not `Host`, not `X-Forwarded-Host`
  (§4). Building a reset link from a header is how reset links get rewritten to point at somebody
  else's domain, and it reads as harmless in a diff.
- Whatever the answer says about the detached delivery's scope, its failure behaviour and its
  observability.

## Out of scope

- SMTP, any provider SDK, any new Gradle dependency, and any credential. `EPIC-07`, and the
  human's.
- A startup log line or health check for a configured sender. `ADR-0031`'s Consequences hand both
  to `EPIC-07` explicitly.
- Retrying a failed send **unless `DEC-072` says so**. It is a decision, not a default.
- Any change to the five routes. They are written against the port and must not branch on whether a
  sender exists — that is the property this ticket exists to make true.

## Tests

`NoSenderConfiguredTest`

| Test | Proves |
| --- | --- |
| `theServerStartsWithNoSenderConfigured` | `duelServer` boots and `GET /api/me` answers, with no mail configuration present |
| `everyRecoveryEndpointAnswersIdenticallyWithNoSender` | For each of the five endpoints, the `(status, body, header names)` triple with no sender configured equals the triple with a recording sender configured, for one representative request each. **Two configurations, one answer** — a single-configuration test cannot tell them apart |
| `nothingIsSentWhenNoSenderIsConfigured` | With no sender, a `forgot-password` for a verified address mints a `password_reset` row and sends nothing. The row proves the path ran; the absence proves it stopped at the seam |
| `theResetLinkComesFromConfigurationAndNotFromAHeader` | With a recording sender and `baseUrl` configured to a known value, a `forgot-password` carrying `Host: evil.test` and `X-Forwarded-Host: evil.test` produces a link built from the configured value, and the recorded mail contains no occurrence of `evil.test` |
| `anUnconfiguredBaseUrlBehavesAsTheAnswerSays` | Whatever `DEC-072` decides — a default, a refusal to start, or a state in which nothing is sent — asserted directly. Named now so the answer cannot land without one |

## Acceptance criteria

- [ ] `DEC-072` is answered by a merged ADR before this leaves `blocked`
- [ ] All five `NoSenderConfiguredTest` tests pass
- [ ] `ServerConfigTest` passes, extended with `baseUrl`'s env-then-file-then-default precedence
      like every other field
- [ ] `everyRecoveryEndpointAnswersIdenticallyWithNoSender` covers **all five** endpoints and
      compares two configurations against each other
- [ ] `theResetLinkComesFromConfigurationAndNotFromAHeader` sends **both** `Host` and
      `X-Forwarded-Host` and asserts neither appears in the recorded mail
- [ ] No file under `poker-server/src/main` reads `Host` or `X-Forwarded-Host`
- [ ] No new dependency appears in `poker-server/build.gradle.kts`
- [ ] No route branches on whether a sender is configured
- [ ] Every command in `verify:` exits 0

## Proof

1. Build the link from `call.request.headers["Host"] ?: config.baseUrl`.
   **`theResetLinkComesFromConfigurationAndNotFromAHeader` reddens alone**, on the `evil.test`
   assertion. Every status assertion in the file still passes, because the wire is unchanged and
   only the mailbox differs — which is why this is a test about a *recorded mail* rather than a
   response. Revert.
2. Have a route answer `503` when no sender is configured.
   **`everyRecoveryEndpointAnswersIdenticallyWithNoSender` reddens** on that endpoint's triple pair,
   **and `theServerStartsWithNoSenderConfigured` stays green.** This is the failure `ADR-0031` §5
   forbids and the one a reasonable engineer adds for observability; the two-configuration
   comparison is the only thing that catches it. Revert.
3. Make the no-sender state throw from `sendVerification`.
   **`nothingIsSentWhenNoSenderIsConfigured` reddens** if the throw escapes to the response — or
   **nothing reddens** if the detached seam swallows it. Run it and record which: the answer tells
   you exactly what `DEC-072` decided about failure, and a silent swallow is a property that must
   be written down rather than discovered.
4. Remove `baseUrl` from `ServerConfig` and hard-code a literal.
   **`ServerConfigTest` reddens** on the precedence assertions, and
   `theResetLinkComesFromConfigurationAndNotFromAHeader` reddens if its configured value differs
   from the literal — make sure it does. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Note

**`ServerComponents` was probed and takes a new required field with zero propagation.** Adding one
and running `./gradlew check -PrequireDocker=true` to completion, plus `npm ci && npm run check &&
npm run build` in `web-client`, exits 0 with no other file edited: every construction site in the
repository goes through the `serverComponents(...)` factory. So if `DEC-072`'s answer needs a field
there, it is a fourth *Files* row and `files_touched: 4` with no `atomic:` — not a reason to split.
