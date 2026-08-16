---
id: STORY-0416
title: The recovery email, verified, and the password reset
type: story
status: backlog
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
| — | *Not yet split. Run `/plan-story STORY-0416` once `STORY-0405` has merged.* | — |

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
