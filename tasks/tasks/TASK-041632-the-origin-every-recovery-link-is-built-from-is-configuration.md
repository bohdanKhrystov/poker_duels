---
schema: 2
id: TASK-041632
title: The origin every recovery link is built from is configuration
type: task
status: backlog
parent: STORY-0416
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, config, mail, security]
depends_on: [TASK-041631]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.config.ServerConfigTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`ServerConfig` carries `baseUrl`, with the same key/env/default shape every other field uses, and a
present-but-malformed value stops the server starting instead of filling a mailbox with links
nobody can click.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/config/ServerConfigTest.kt` | modify |

Both are `modify`, and the test file is in the budget because this ticket's field is only real once
its precedence is asserted the way every other field's is. No existing test in either file changes.

Read, and do not edit:
`docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md` §6;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §4.

## Scope

- `ServerConfig` gains `val baseUrl: String`, **with a default**, alongside the existing defaulted
  fields and for the identical recorded reason: several tests construct `ServerConfig(…)` field by
  field, and an undefaulted field would drag every one of them into this ticket. Add the same style
  of comment those fields carry.
- `BASE_URL_KEY = "server.baseUrl"`, `BASE_URL_ENV = "BASE_URL"`,
  `DEFAULT_BASE_URL = "http://localhost:5173"` — `ADR-0077` §6's three values **verbatim**. The
  default is the Vite dev origin because the link addresses a *client* screen, not this server's
  port.
- `from` resolves it env-then-file-then-default like every other field, and **refuses a
  present-but-malformed value** with `requireNotNull`/`require` and a message naming the key, in the
  shape `rejectsAPortThatIsNotANumber` already covers for `port`. Malformed means: not an absolute
  `http` or `https` origin, or carrying a trailing slash.
- **Absent is not malformed.** `ADR-0077` §6 keeps the default deliberately: a build with no sender
  builds no link, so requiring an origin in the state where nothing reads one would cost every
  developer machine and every CI run a value that is never used.

## Out of scope

- **Requiring a `baseUrl` when a sender *is* configured.** `ADR-0077` §6 makes the pairing rule a
  fact about the sender and sends it to `EPIC-07` beside the startup log line and health check that
  `ADR-0031`'s Consequences already assigned there. This ticket therefore ships a state — configured
  sender, defaulted origin — that mails dead links, and `ADR-0077` §Consequences accepts it by name.
  **Say so in the PR**; it is an accepted cost, not an oversight.
- **Building a URL.** `RecoveryLinks` is `TASK-041633`, and nothing in `main` reads this field until
  then. An unread `val` on a public data class is expected here and is not a detekt finding to
  suppress.
- **Reading `Host` or `X-Forwarded-Host` anywhere.** `ADR-0031` §4 forbids it and `TASK-041633`
  gates it over the source tree; this ticket's contribution is that the value has somewhere else to
  come from. **Say in the PR that no header read was added**, since nothing here fails if one is.
- The two budget pairs, which also land in this file — `TASK-041628`.
- The *path* after this origin, which `ADR-0081` has since fixed as `#/reset/<token>` and
  `#/verify/<token>`. It changes no part of this field: `baseUrl` is still an absolute origin with
  **no trailing slash**, and the `/` before the `#` is written by `RecoveryLinks` (`ADR-0081` §1,
  §8, which names this ticket as untouched).

## Tests

`ServerConfigTest`, four new methods in the shape the existing per-field groups use.

| Test | Proves |
| --- | --- |
| `readsTheBaseUrlFromTheConfig` | A `server.baseUrl` in the config is what `from` returns |
| `theEnvironmentVariableOverridesTheBaseUrl` | With **both** a config value and `BASE_URL` set to a *different* origin, the environment wins. Two different values, or the test cannot tell an override from a coincidence |
| `fallsBackToTheDefaultBaseUrl` | With neither set, `from` returns `DEFAULT_BASE_URL`, asserted against the literal `"http://localhost:5173"` and not against the constant — a constant compared to itself is a tautology |
| `rejectsABaseUrlThatIsNotAnOrigin` | Four values each refused, asserted one by one so the failure names which: `"localhost:5173"` (no scheme), `"ftp://x.test"` (wrong scheme), `"http://x.test/"` (trailing slash) and `""` (empty). The last is the one an unset environment variable most often looks like |

## Acceptance criteria

- [ ] `ServerConfigTest.readsTheBaseUrlFromTheConfig` passes
- [ ] `ServerConfigTest.theEnvironmentVariableOverridesTheBaseUrl` passes
- [ ] `ServerConfigTest.fallsBackToTheDefaultBaseUrl` passes
- [ ] `ServerConfigTest.rejectsABaseUrlThatIsNotAnOrigin` passes
- [ ] `theEnvironmentVariableOverridesTheBaseUrl` sets **two different** origins and asserts the
      environment's
- [ ] `fallsBackToTheDefaultBaseUrl` asserts the literal `"http://localhost:5173"`, not
      `ServerConfig.DEFAULT_BASE_URL`
- [ ] `rejectsABaseUrlThatIsNotAnOrigin` covers all **four** listed values, each in its own
      assertion
- [ ] `baseUrl` is declared with a default, and no existing `ServerConfig(…)` construction site in
      the repository is edited
- [ ] Every pre-existing `ServerConfigTest` test passes unchanged
- [ ] Every command in `verify:` exits 0

## Proof

1. Remove the `require` and accept any string.
   **`rejectsABaseUrlThatIsNotAnOrigin` reddens alone**, on all four values. Revert.
2. Keep the `require` but drop the trailing-slash clause.
   **`rejectsABaseUrlThatIsNotAnOrigin` reddens on `"http://x.test/"` only**, and the other three
   still pass. Run it: a scheme check alone looks like a complete validation, and this is the clause
   that turns `<baseUrl>/#/reset/…` into `<baseUrl>//#/reset/…` — a double slash before the fragment,
   which no host and no test in this repository ever sees, because a fragment reaches neither.
   Revert.
3. Read the environment before the config but return the config's value anyway.
   **`theEnvironmentVariableOverridesTheBaseUrl` reddens alone** — and only because that test sets
   two *different* origins. Set them to the same string first and watch it pass under the mutation;
   that is the reason the criterion above says two.
4. Change `DEFAULT_BASE_URL` to `"http://localhost:8080"`, this server's own port — the plausible
   "surely it points at us" edit.
   **`fallsBackToTheDefaultBaseUrl` reddens alone**, and only because it asserts the literal. Had it
   asserted `ServerConfig.DEFAULT_BASE_URL`, **nothing would redden**; run that variant too and
   record it, because a constant compared to itself is exactly how this test rots.
5. Give `baseUrl` no default value.
   **The module stops compiling**, at every `ServerConfig(…)` construction site that names its
   fields — the outcome `ADR-0077` §6 predicted and the reason the field is defaulted. Record the
   count of failing sites in the PR; it is the number of files this ticket would otherwise have
   touched.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
