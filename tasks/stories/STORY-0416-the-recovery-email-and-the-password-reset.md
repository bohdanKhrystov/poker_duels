---
id: STORY-0416
title: The recovery email, verified, and the password reset
type: story
status: ready
parent: EPIC-04
module: poker-server
labels: [server, auth, http, security, mail]
depends_on: [STORY-0405]
---

## Goal

A player may attach one email address to their account, prove it, and use it to reset a forgotten
password — with a single-use one-hour token, and every one of their sessions killed when the password
changes.

## Why

`ADR-0027` gave a player a password and no way back if they forget it, which under `ADR-0029`'s
permanence and `ADR-0012`'s device binding means a profile that is unreachable forever.
[`ADR-0031`](../../docs/adr/ADR-0031-an-optional-verified-recovery-email.md) answers `DEC-027` with
an **optional, verified-only** address in its own table.

> **This story is not in `EPIC-04`'s original table**, which was written before `ADR-0031` was
> accepted. The work is the ADR's, not an invention: rather than swelling `STORY-0403` and
> `STORY-0404` past what one story can hold, it is placed here with its client half in `STORY-0417`.

## Design notes

- **Three tables in one new migration** (`ADR-0031` §2–§4): `recovery_email` keyed by `player_id`
  with `verified_at NOT NULL` and a unique index on `lower(address COLLATE "und-x-icu")`;
  `email_verification` with `UNIQUE (player_id)` and **no** unique constraint on `address`;
  `password_reset` with `UNIQUE (player_id)`. No `ON DELETE` clause anywhere — `DEC-029` is
  unanswered and a cascade would answer part of it silently.
- **The row's existence is the proof.** `recovery_email` cannot represent an unverified address, so
  no query anywhere carries an *is it verified* branch.
- **Attaching costs the current password**, even inside a valid session: a session token is a bearer
  credential in web storage, and without this an unattended browser converts into permanent ownership
  of the account.
- **`forgot-password` answers `202` in every case** — unknown address, pending-but-unverified,
  verified and mailed, over budget, no sender configured — with the response written *before* any
  mail work and delivery on a detached coroutine, so latency does not vary with whether an address
  matched.
- **The reset token is single-use by construction**: consumption is one `DELETE … RETURNING
  player_id` inside the transaction that writes the new password. No `used_at` flag, no read-then-
  write window.
- **A successful reset deletes every `auth_session` row for that player**, in the same transaction —
  the usual reason to reset is that somebody else has the password. The reset issues no session and
  returns no token.
- **The token travels in a URL fragment**, `<baseUrl>/reset#token=…`, and the endpoint accepts it
  **only in a request body**, never as a query parameter. `baseUrl` is one configured value and is
  never derived from a request header.
- **`RecoveryMailer` has exactly two functions**, named for the only two permitted mails, and a test
  asserts that structurally. There is no `send(to, subject, body)` anywhere. No log line records an
  address; `EmailAddress` redacts itself in `toString()`.
- **`ProfileResponse` gains `hasRecoveryEmail: Boolean` and nothing more** — the client can say
  *recovery is on* and can never display the address.
- **Expired `email_verification` rows are deleted on the existing sweep** (`ADR-0025`), never a
  second ticker. Expiry is enforced at read time regardless.
- The mail transport itself is `EPIC-07`'s; a build with no sender configured is a valid state here
  and the endpoints behave identically from outside.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-041601](../tasks/TASK-041601-three-tables-that-cannot-become-a-mailing-list.md) | Three tables that cannot become a mailing list | **ready** |
| [TASK-041602](../tasks/TASK-041602-two-strangers-may-both-claim-one-address-and-nothing-cascades.md) | Two strangers may both claim one address, and nothing cascades | backlog |
| [TASK-041603](../tasks/TASK-041603-an-address-that-redacts-itself.md) | An address that redacts itself | backlog |
| [TASK-041604](../tasks/TASK-041604-two-tokens-minted-the-way-a-session-token-is.md) | Two tokens, minted the way a session token is | backlog |
| [TASK-041605](../tasks/TASK-041605-one-digest-for-both-recovery-tokens.md) | One digest for both recovery tokens | backlog |
| [TASK-041606](../tasks/TASK-041606-a-port-that-can-send-exactly-two-mails.md) | A port that can send exactly two mails | backlog |
| [TASK-041607](../tasks/TASK-041607-the-port-where-a-pending-address-and-a-proven-one-both-live.md) | The port where a pending address and a proven one both live | backlog |
| [TASK-041608](../tasks/TASK-041608-a-second-claim-replaces-the-first-in-one-transaction.md) | A second claim replaces the first, in one transaction | backlog |
| [TASK-041609](../tasks/TASK-041609-the-first-to-verify-takes-the-address.md) | The first to verify takes the address | backlog |
| [TASK-041610](../tasks/TASK-041610-a-pending-address-answers-exactly-as-an-unknown-one.md) | A pending address answers exactly as an unknown one | backlog |
| [TASK-041611](../tasks/TASK-041611-erasing-an-address-is-one-statement-and-so-is-forgetting-a-stale-one.md) | Erasing an address is one statement, and so is forgetting a stale one | backlog |
| [TASK-041612](../tasks/TASK-041612-the-existing-ticker-forgets-unproven-addresses-too.md) | The existing ticker forgets unproven addresses too | backlog |
| [TASK-041613](../tasks/TASK-041613-one-live-reset-token-and-a-quarter-hour-of-silence.md) | One live reset token, and a quarter hour of silence | backlog |
| [TASK-041614](../tasks/TASK-041614-one-statement-spends-the-token-and-the-same-transaction-ends-every-session.md) | One statement spends the token, and the same transaction ends every session | backlog |
| [TASK-041615](../tasks/TASK-041615-a-session-holder-proves-the-password-they-already-have.md) | A session holder proves the password they already have | backlog |
| [TASK-041616](../tasks/TASK-041616-the-profile-says-recovery-is-on-and-never-what-the-address-is.md) | The profile says recovery is on, and never what the address is — **`atomic:`, 6 files** | backlog |
| [TASK-041617](../tasks/TASK-041617-five-endpoints-and-a-field-written-down.md) | Five endpoints and a field, written down | backlog |
| [TASK-041618](../tasks/TASK-041618-a-token-from-the-mailbox-proves-the-address.md) | A token from the mailbox proves the address | backlog |
| [TASK-041619](../tasks/TASK-041619-three-ways-to-fail-verification-and-one-answer-for-all-of-them.md) | Three ways to fail verification, and one answer for all of them | backlog |
| [TASK-041620](../tasks/TASK-041620-a-reset-takes-a-token-in-a-body-and-never-in-a-url.md) | A reset takes a token in a body, and never in a URL | backlog |
| [TASK-041621](../tasks/TASK-041621-two-submissions-of-one-link-and-only-one-of-them-works.md) | Two submissions of one link, and only one of them works | backlog |
| [TASK-041622](../tasks/TASK-041622-a-reset-signs-you-out-everywhere-including-here.md) | A reset signs you out everywhere, including here | backlog |
| [TASK-041623](../tasks/TASK-041623-taking-the-address-back-costs-the-password.md) | Taking the address back costs the password | backlog |
| [TASK-041624](../tasks/TASK-041624-which-strings-are-an-address.md) | Which strings are an address | **blocked — `DEC-071`** |
| [TASK-041625](../tasks/TASK-041625-attaching-an-address-costs-the-current-password.md) | Attaching an address costs the current password | **blocked — `DEC-071`, `DEC-072`** |
| [TASK-041626](../tasks/TASK-041626-four-different-things-happen-and-the-caller-reads-the-same-answer.md) | Four different things happen, and the caller reads the same answer | **blocked — `DEC-072`** |
| [TASK-041627](../tasks/TASK-041627-a-sender-that-sends-nothing.md) | A sender that sends nothing | backlog |
| [TASK-041628](../tasks/TASK-041628-two-budgets-that-say-nothing-when-they-refuse.md) | Two budgets that say nothing when they refuse | backlog |
| [TASK-041629](../tasks/TASK-041629-a-good-token-and-a-password-the-policy-refuses.md) | A good token, and a password the policy refuses | backlog |
| [TASK-041630](../tasks/TASK-041630-a-decorator-that-detaches-over-the-same-port.md) | A decorator that detaches, over the same port | backlog |
| [TASK-041631](../tasks/TASK-041631-a-failed-send-stays-inside-the-scope-and-names-a-class.md) | A failed send stays inside the scope, and its log line names a class | backlog |
| [TASK-041632](../tasks/TASK-041632-the-origin-every-recovery-link-is-built-from-is-configuration.md) | The origin every recovery link is built from is configuration | backlog |
| [TASK-041633](../tasks/TASK-041633-one-function-builds-both-recovery-links-and-no-header-reaches-it.md) | One function builds both recovery links, and no header reaches it | backlog |
| [TASK-041634](../tasks/TASK-041634-a-build-with-no-sender-is-a-valid-build.md) | A build with no sender is a valid build | backlog |
| [TASK-041635](../tasks/TASK-041635-the-fold-the-address-index-depends-on-written-down-in-the-catalog.md) | The fold the address index depends on, written down in the catalog | backlog |

**The table is in id order; `depends_on` is the sequence.** They stopped coinciding when `ADR-0077`
and `ADR-0078` merged and their answers were folded back in:

`…041623 → 041624 → 041627 → 041625 → 041626 → 041630 → 041631 → 041632 → 041633 → 041634 →
041628 → 041629`, with `041635` hanging off the merged `041602`.

`TASK-041627` was **re-cut into six**, which `ADR-0077` §Consequences called for by name: *"Three
implementation files, `ServerConfig`, `ServerComponents`, `Application.kt` and its tests exceed what
one ticket carries; that is the planner's to re-cut."* `MAX_FILES_TOUCHED` is 3 and no merged gate
forbids splitting any of the seams, so no piece of it is `atomic:` — `RecoveryLinks` takes an origin
string and compiles without `ServerConfig.baseUrl`; the decorator compiles without the wiring.
**One edge moved**: `TASK-041627` now ships `NoRecoveryMailer` alone and runs *before*
`TASK-041625`, because `TASK-041626`'s fourth case binds that object and `TASK-041625` asserts
through the same seam. An inline no-op written in a route test would be a copy of the seam rather
than the seam, and the property both tickets assert would then be about a fixture.

`TASK-041635` is `TASK-041601`'s parked follow-up, unparked. That ticket's Notes made it conditional
— *"if the answer admits non-ASCII, this becomes a ticket"* — and `ADR-0078` §1 admits it.

## Open decisions

Split on 2026-08-25 into 29 tickets, of which `TASK-041601` was the one startable ticket and six
were `blocked`. Four decisions were raised and none was answered inside a ticket. `ADR-0077` and
`ADR-0078` then merged and were folded back in, taking the story to **35 tickets** — six from
re-cutting `TASK-041627`, one from unparking `TASK-041601`'s conditional follow-up. **All four are
now answered and this story carries no open decision.**

`DEC-074` was **answered on 2026-08-25** by
[`ADR-0080`](../../docs/adr/ADR-0080-the-password-is-judged-before-the-token-is-touched.md) — *the
password is judged before the token is touched, so a refusal costs no link.* **`ADR-0031` §5's
precondition gives way; §4 stands byte-unchanged**, which is the whole point: the clause with the
concurrency reason behind it is the one that survives. `POST /api/auth/reset-password` runs three
steps and no others — decode ⇒ `400`; `passwordIsLongEnough` **and**
`passwordIsWithinTheWorkBound` ⇒ `422`, **with no connection taken and no statement executed**; then
`consume` ⇒ `204` / `400`. So a `422` never touches `password_reset`: same `token_hash`, same
`issued_at`, same `expires_at`, and the same link works on the next submission while it lives.
Because `issued_at` is untouched, §5's fifteen-minute suppression still sees a live token, so a
player who presses *email me a link* again after a refusal still gets §5's complete no-op and the
link they are about to use survives. The order is `ADR-0048` §2's own rule — the maximum runs
*"before Argon2 runs and before the identifier is looked up"*, and here the token **is** the
identifier — and the minimum joins it because §2 makes reset one of the two endpoints it applies at.
The disclosure the register worried about runs the other way: the branch is chosen entirely by the
caller's own password, so the `422` is byte-identical for a live token, an expired one and a string
the caller invented, and `400`-versus-`422` says nothing about the row. **Costs**: a `422` no longer
proves the link is alive, so `STORY-0417`'s form must be able to move from *password refused* to
*link expired* without having contradicted itself; a stranger with no token can make the endpoint
answer `422`, which holds only while the policy stays a published pure function — the day a breach
corpus or a row-reading rule joins it, this endpoint must be budgeted or the rule moved behind the
lookup. **`TASK-041620` is unchanged and needs no re-cut**, since the step lands in front of
`consume`; the one constraint on it is a fixture one — every request in `ResetPasswordRouteTest`,
including the two expecting `400`, must carry a `newPassword` of 8–128 code points, or
`TASK-041629`'s *"passes unchanged"* criterion is unsatisfiable. **`TASK-041629` gains the check
between decode and `consume` and loses one named test**:
`aBadTokenStillAnswersFourHundredNotFourHundredAndTwentyTwo` asserts the order this reverses — a
fabricated token with a 7-code-point password now answers `422` — and what it defended is sharper
asserted the other way, that the `422` for a fabricated token and for a live one are
indistinguishable; `aRefusedPasswordLeavesTheTokenAsTheDecisionSays` resolves to a second request
with the same token and an acceptable password answering `204`. Its losing branch is a pure function
of the request body, so it needs no clock, no latch and no second connection — the one hazard is a
fixture pinning `java.time.Clock` (`ADR-0062` §5) far from the database's `now()`, which mints
tokens already expired. `TASK-041617` transcribes the corrected sentence rather than §5's
parenthetical. `PasswordResets.consume(token, secret): Boolean` is unchanged and needs no third
value, exactly as `TASK-041614` already assumes. **Unblocks `TASK-041629`.** Raises no `DEC`, and
nothing is the product owner's or the human's.

`DEC-073` was **answered on 2026-08-25** by
[`ADR-0079`](../../docs/adr/ADR-0079-five-to-attach-ten-to-forget-and-the-attach-budget-is-the-only-mail-cap.md)
— *five to attach, ten to forget, and the attach budget is the only cap on the mail it causes.*
**`POST /api/auth/forgot-password` admits 10 attempts per remote address per rolling 60 000 ms;
`POST /api/auth/recovery-email` admits 5 per 60 000 ms.** Four `ServerConfig` values
(`auth.forgotPasswordMaxAttempts` / `AUTH_FORGOT_PASSWORD_MAX_ATTEMPTS`,
`auth.forgotPasswordWindowMillis` / `AUTH_FORGOT_PASSWORD_WINDOW_MILLIS`,
`auth.recoveryEmailMaxAttempts` / `AUTH_RECOVERY_EMAIL_MAX_ATTEMPTS`,
`auth.recoveryEmailWindowMillis` / `AUTH_RECOVERY_EMAIL_WINDOW_MILLIS`), a `forgotPasswordLimits()`
and a `recoveryEmailLimits()`, and a separate `AttemptBudget` instance per endpoint. The numbers
differ because the endpoints do: **`ADR-0031` §5's fifteen-minute rule covers the reset path only**
— `TASK-041613` builds it inside `PasswordResets.issue`, while `TASK-041607` fixes `claimPending` as
returning `Unit` and `TASK-041608` writes unconditionally, so a verification mail follows **every**
successful attach. On `forgot-password` the address budget therefore adds nothing against bombing one
victim and is set where it cannot bite a player who has lost their password and is being told mail is
on its way; on `recovery-email` it is the only cap on mail to a caller-chosen recipient and a second
door to the current-password guess `ADR-0074` priced at ten a minute, so five keeps sign-in the
cheaper door. **An over-budget attempt still counts**, on both — one rule for every limiter here, and
the rule that actually stops a sprayer who is answered `202` and has no reason to pace. **Placement,
which the ticket asked for explicitly:** `recovery-email` admits after the `401`, the decode and
`ADR-0078`'s syntax `400` and **before** the Argon2 verify; `forgot-password` admits **after the
`202` is written**, so `TASK-041626`'s write-first timing defence is untouched by a `Mutex`. Neither
calls `refund`; neither can answer `429`; the key is §5's `origin.remoteAddress` and no
`X-Forwarded-*` until `EPIC-07`. **Unblocks `TASK-041628`.** Its seven tests are unchanged by the
answer — the numbers are config the test sets low, and the window rolls on the injected
`ServerClock`, so nothing waits in real time. Raises no `DEC`, and leaves one residual for the
planner: **the attach path has no per-account resend suppression**, which on the best reading of §5
is a defect against `TASK-041607`, `TASK-041608` and `TASK-041625` rather than a new question, due
before `EPIC-07` configures a sender.

`DEC-071` was **answered on 2026-08-25** by
[`ADR-0078`](../../docs/adr/ADR-0078-the-mail-is-the-only-real-check-on-an-address.md) — *the mail is
the only real check on an address, so the syntax rule refuses almost nothing.* An address is accepted
when it holds at least one `@`, its first code point is not `@`, its last code point is not `@`, it
holds no ASCII control character, and it is at most **254 code points** long. There is no separate
minimum: `a@b` is the shortest string that passes, and `bob` is the refusal that earns the `400`. The
asymmetry was settled on `ADR-0031`'s own text — §3 requires verification and §2 notes that a stored
address has *"by construction, received mail at it"*, so the deliverability check already exists and
a syntax rule can only report earlier and only be wrong in the direction that loses an account. The
control-character clause is a **security clause, not a syntax clause**, and is labelled so: no
`addr-spec` holds one, and a line terminator is what would harm somebody outside this game once
`EPIC-07` has a transport. **The predicate runs only where an address enters** —
`POST /api/auth/recovery-email` — so `forgot-password` keeps its unconditional `202`, no stored
address is ever re-judged, and a later tightening costs nothing. `emailAddressOrNull` returns the
input **unchanged**: no trim, no fold, no `Normalizer`, confirming `TASK-041624`'s scope and
`TASK-041603`'s *no `init`, no `require`, no regex*. Deliverability, DNS and MX, domain spelling,
disposable-address lists, plus-address stripping, unicode conversion, a dot in the domain, quoting,
and any whitespace that is not a control character are all **deliberately unchecked**. The refusal is
`400` with an **empty body**, identical to a failed decode; the client says one sentence naming no
mailbox, no domain and no other account; and **a `202` may not be rendered as recovery being on**,
since §3 leaves `hasRecoveryEmail` false. Both fixture tables are in the ADR's §6, each holding the
entry that distinguishes the answer from `.+@.+\..+`, which is what `TASK-041624`'s Proof step 4
predicts. The cost being chosen: **this endpoint's only feedback now fires almost never**, so a
pasted `Bob Smith <bob@example.com>`, a trailing space and a dead domain are all answered `202` and
silence — and the honesty of that silence is `STORY-0417`'s to carry. `TASK-041624` and
`TASK-041625` are unblocked as far as this decision goes. Nothing in it was the human's, and nothing
is sent to any address under any answer.

`DEC-072` was **answered on 2026-08-25** by
[`ADR-0077`](../../docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md)
— *no sender is an implementation, detachment is a decorator, and a test binds neither.* The wiring
holds `NoRecoveryMailer`, a `public object` with two empty bodies, so no route branches on
configuration; detachment is `DetachedRecoveryMailer(delegate, scope, log)` over the same port, on a
**supervisor child of the application's job** built in `duelServer`, so shutdown cancels every
in-flight send and the server may exit with mail pending. A failed send is logged once by member name
and exception **class name** — no message, no stack trace, no `player_id`, no success line — and
**nothing above the port is retried and no row is compensated**, which costs a player whose reset
mail is lost the fifteen minutes §5 already suppresses. `baseUrl` is a `ServerConfig` field where
absent is the default and malformed refuses to start, and `RecoveryLinks` is the only place either
URL is built. **What a test can await: the test binds an undecorated recording double**, so the send
is an ordinary suspend call in the handler and both *a mail was sent, with this link* and *no mail was
sent* are list comparisons with no join, no channel and no timeout — absence is what forced the
shape, since no await proves a negative. §5's `202`-before-the-send ordering stays a **review
criterion** and is gated by nothing, as `TASK-041626`'s Proof step 3 predicted. `TASK-041627` was
**bigger than its Files table** and has been re-cut into six, above. The ADR raises `DEC-075` — whether the
mailed link survives a static host with no rewrite rule, given `ADR-0076` §4 — which blocked nothing
here, because no sender is configured and no link is delivered to anybody. It was **answered on
2026-08-25** by
[`ADR-0081`](../../docs/adr/ADR-0081-a-mailed-link-is-a-fragment-route-and-the-token-is-the-segment-behind-the-slug.md):
both links become fragment routes — `"$baseUrl/#/reset/$token"` and `"$baseUrl/#/verify/$token"` —
so **`TASK-041633` changes in two string literals and its `DEC-075` note, and nothing else in this
story moves.** `TASK-041632` and `TASK-041620` are unchanged; the token is still read from a body and
still never reaches a server in a URL.

**Nothing here is the human's, and nothing here is about money.** `ADR-0031` §7 already defers the
transport — SMTP relay or provider API, and therefore any bill — to `EPIC-07`, and every ticket in
this story sends nothing under every answer to `DEC-072`. Nothing in this story touches the vision's
*What it is* or *What it is not*: the human already chose *optional email, recovery only*, and
`ADR-0031` records that choice verbatim. `DEC-071` is the product owner's rather than the human's
because it applies that choice rather than changing it — the same test `DEC-043` (*what may a
password be*) passed.

**Twenty-three tickets are unblocked**, and they are the whole schema, both token types, the digest,
the mailer port and its structural test, both storage ports, the sweep, `hasRecoveryEmail`,
`docs/protocol.md`, and three of the five endpoints. The two blocked endpoints are the two that
send mail, plus the one refusal that needs a rule.

**One deliberate divergence from `ADR-0031`, recorded rather than resolved silently.** §6.2's port
snippet writes `handle: LoginHandle`; no such type exists, because `loginHandleOrNull` returns
`String?` and `Credentials` takes `identifier: String`. `TASK-041606` uses `String` and says so.
The mechanism §6.2 specifies — two members, named for the only two permitted mails — is transcribed
exactly, so nothing the ADR relies on moves.

**One thing this story needs that `ADR-0031` does not mention.** Attaching and detaching both cost
the current password, and `Credentials` has no way to check one for a caller identified by session:
`verify` is keyed by identifier. `TASK-041615` adds `verifyCurrent(playerId, kind, presented)`
rather than putting a player→identifier reverse lookup in the codebase, which would make
`credential.identifier` readable.

## Acceptance criteria

- [ ] An address is stored only after its token is presented; before that, the account reports
      `hasRecoveryEmail: false` and `forgot-password` behaves as for an unknown address.
- [ ] Attaching with a wrong current password answers `403` and stores nothing.
- [ ] Two players may hold a pending claim on one address; the first to verify takes it and the
      second's verification answers `409`.
- [ ] A reset token works once: the second use answers `400`, asserted with two concurrent uses as
      well as two sequential ones.
- [ ] A token past its hour is refused, proven by moving the injected clock — no test sleeps.
- [ ] A successful reset deletes every session for that player and issues none.
- [ ] `forgot-password` answers `202` for an unknown address, a pending address, a verified address
      and an over-budget caller — all four asserted, and the response body is identical.
- [ ] A second mail within fifteen minutes is not sent, and the outstanding token still works.
- [ ] `RecoveryMailer` declares exactly two members, asserted over the API; no address appears in any
      response body or log line.

## Out of scope

- The screens — `STORY-0417`.
- Changing a handle, which no endpoint does (`ADR-0031` §1).
- Any mail that is not one of the two — the port's shape is what forbids it.
- The SMTP or provider transport — `EPIC-07`.
