---
schema: 2
id: TASK-041633
title: One function builds both recovery links, and no header reaches it
type: task
status: backlog
parent: STORY-0416
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, mail, security]
depends_on: [TASK-041632]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.mail.RecoveryLinksTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

Every URL this system will ever mail is built in one function, from configuration, and nothing under
`poker-server/src/main` reads a `Host` header.

## Why now, for a class nothing calls yet

`ADR-0077`'s Context is explicit that this is the one part of the mail seam that is cheaper today
than later: *"every link this system will ever mail is built in one function. Written that way today
it costs nothing; retrofitted after a transport composes its own strings, it is a hunt through mail
templates for the one that concatenated a header."* Nothing constructs `RecoveryLinks` in `main`
until `EPIC-07` has a transport, and `ADR-0077` §Consequences names that outright — *"a builder for
links nobody delivers"*. It is tested directly, which is why it needs no caller to be gated.

`DEC-075` is open and may move the **path** — `/reset` as a segment versus a fragment route — under
`ADR-0076` §4. It blocks nothing today because no sender is configured and no link is delivered to
anybody, and `ADR-0077` §6 put both links here precisely so that answering it is a one-function
change. This ticket transcribes `ADR-0031` §4 as written and does not anticipate the answer.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/mail/RecoveryLinks.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/mail/RecoveryLinksTest.kt` | create |

Read, and do not edit:
`poker-server/src/test/kotlin/duels/poker/server/http/SignUpSecrecyTest.kt` — the repository-root
walk and the *name every file before opening any of them* idiom the source sweep copies;
`poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt` — `BASE_URL_KEY` and
`DEFAULT_BASE_URL`, for the KDoc only;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §4;
`docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md` §6.

## Scope

- `public class RecoveryLinks(baseUrl: String)` in `duels.poker.server.mail`, with
  `verification(token)` and `reset(token)`. It takes the **origin string**, not a `ServerConfig`:
  the class has no reason to know where the value came from, and taking the config would put a
  config type in the one place `EPIC-07`'s transport has to reach.
- `reset` returns `"$baseUrl/reset#token=$token"` — `ADR-0031` §4's form, character for character.
  `verification` returns `"$baseUrl/verify#token=$token"`, §4's shape applied to the mail §4 did not
  spell out: a **fragment**, never a query string, for §4's reasons unchanged — a fragment is not
  sent to the server, so it stays out of access logs, referrers and proxy records.
- **The expected strings in the test are written as literals**, not assembled from the same template
  the production code uses. A test that concatenates `"$base/reset#token=$t"` restates the encoder
  and passes whatever the encoder does.
- A source sweep asserting that **no file under `poker-server/src/main/kotlin` contains the string
  `X-Forwarded-Host`, or reads a `Host` header** (`ADR-0077` §6's last bullet: *asserted over the
  source tree rather than inferred*). Walk the tree rather than naming files — the point is that a
  *future* file cannot do it either — and assert the walk found a non-zero number of `.kt` files
  before judging any of them.

## Out of scope

- **Answering `DEC-075`.** If the reviewer believes `/reset` should already be a fragment route,
  that is the decision landing, not this ticket widening. Leave the ticket and say so.
- Any mail body, subject or wording. `ADR-0031` defers the copy to `STORY-0412`, and this class
  returns a URL and nothing else.
- Constructing `RecoveryLinks` anywhere in `main`. There is no caller until `EPIC-07`; if detekt
  reports the class as unused, that is a finding to raise, not a reason to wire it in early.
- Percent-encoding the token. Both token types are URL-safe by construction (`TASK-041604`), and an
  encoder added here would silently double-encode once a transport exists. **Gated below**, since a
  refusal to encode produces no assertion by itself.
- Reading `Host` in `web-client`, which is a different tree and a different rule.

## Tests

`RecoveryLinksTest`, `internal`. No database, no Ktor, no coroutines.

| Test | Proves |
| --- | --- |
| `aResetLinkIsTheConfiguredOriginAndAFragment` | `RecoveryLinks("https://duels.test").reset(token)` equals the literal `"https://duels.test/reset#token=abc123"`. The whole string, asserted as one literal |
| `aVerificationLinkIsTheConfiguredOriginAndAFragment` | The same for `verification`, equal to `"https://duels.test/verify#token=abc123"`. **Both members** — one template copied wrong is invisible to a test that checks the other |
| `twoOriginsProduceTwoLinks` | The same token through `RecoveryLinks("https://a.test")` and `RecoveryLinks("https://b.test")` yields two different strings, each containing its own origin. One origin cannot tell a configured value from a hard-coded constant |
| `neitherLinkCarriesTheTokenInAQueryString` | Neither result contains `?`, and in each the first `#` precedes the token. A `?token=` form sends the token to the server and into its logs, which is the exact failure §4 chose the fragment to avoid |
| `theTokenIsPassedThroughUnchanged` | A token containing `-` and `_` — the URL-safe alphabet's two non-alphanumerics — appears in both links byte for byte, unencoded |
| `noMainSourceFileReadsAHostHeader` | Walking `poker-server/src/main/kotlin`: at least one `.kt` file is found, asserted first; and none of them contains `X-Forwarded-Host` or a `Host` header read |

## Acceptance criteria

- [ ] All six `RecoveryLinksTest` tests pass
- [ ] The two link assertions compare against **string literals**; the file contains no expression
      that rebuilds a link from `baseUrl` and a token
- [ ] `twoOriginsProduceTwoLinks` uses **two** origins
- [ ] `noMainSourceFileReadsAHostHeader` asserts the file count is non-zero **before** examining any
      content, and names the resolved directory in its failure message
- [ ] `RecoveryLinks.kt` contains no `URLEncoder`, no `encodeURLParameter` and no `?`
- [ ] `RecoveryLinks.kt` takes a `String`, not a `ServerConfig`
- [ ] Every command in `verify:` exits 0

## Proof

1. Change `reset` to `"$baseUrl/reset?token=$token"`.
   **`aResetLinkIsTheConfiguredOriginAndAFragment` and `neitherLinkCarriesTheTokenInAQueryString`
   both redden**, and `aVerificationLinkIsTheConfiguredOriginAndAFragment` stays green — which is
   what makes the second member's own test worth having. Revert.
2. Hard-code the origin: `"https://duels.test/reset#token=$token"`, ignoring `baseUrl`.
   **`twoOriginsProduceTwoLinks` reddens alone**, while both literal assertions **pass**, because
   their fixture origin is the hard-coded one. Run this: it is the mutation that proves the two
   literal tests gate the *shape* and not the *source* of the origin, and it is exactly the shape
   that a single-fixture test can never catch. Revert.
3. Rewrite the two literal assertions as `assertEquals("$origin/reset#token=$token", links.reset(token))`.
   **Nothing reddens under mutation 1.** Do not commit this; run it once and record it. It is the
   tidier-looking test and it asserts that the encoder equals itself.
4. Add `URLEncoder.encode(token, UTF_8)` inside both members.
   **`theTokenIsPassedThroughUnchanged` reddens alone**, on the `-`/`_` token — and stays green for a
   purely alphanumeric one. Check the fixture actually contains both characters before trusting this
   result. Revert.
5. Add `call.request.headers["X-Forwarded-Host"]` to any file under `poker-server/src/main/kotlin`.
   **`noMainSourceFileReadsAHostHeader` reddens**, naming that file. Then point the walk at a
   directory that does not exist: **the count assertion fires** rather than the sweep passing over
   zero files. Run both; the second is the way this test rots into a tautology, and it is
   `SignUpSecrecyTest`'s recorded reason for naming its files before opening them.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
