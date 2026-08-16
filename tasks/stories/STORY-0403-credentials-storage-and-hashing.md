---
id: STORY-0403
title: Credentials — the schema, the hash, and a port that returns none
type: story
status: backlog
parent: EPIC-04
module: poker-server
labels: [server, auth, schema, security]
depends_on: [STORY-0401]
---

## Goal

A password can be stored and proved: the `credential` and `auth_session` tables exist, Argon2id
hashes a presented secret into a PHC string, and the port that verifies one **returns a `PlayerId`
or nothing** — no function anywhere in the codebase returns a hash.

## Why

Everything from sign-up to recovery stands on this, and it is the one story where getting the shape
wrong is expensive later: a hash that leaks into a response body or a log line is a defect that no
amount of endpoint care repairs. Building the storage and the verification alone — with no endpoint
yet — is what lets that be asserted structurally before anything calls it.

## Design notes

- **The schema is [`ADR-0027`](../../docs/adr/ADR-0027-the-session-outranks-the-device-id.md) §1 and
  §2, transcribed**: `credential (id, player_id, kind, identifier, secret_hash, created_at)` with
  `UNIQUE (kind, identifier)`, and `auth_session (token_hash, player_id, issued_at, expires_at)`
  with `auth_session_player_id_idx`. One new migration file, taking the next free `V<n>` —
  `STORY-0401` has already taken `V3`, so this is `V4` unless something else landed first.
- **No `ON DELETE` clause anywhere**, deliberately:
  [`ADR-0039`](../../docs/adr/ADR-0039-v01-offers-no-account-deletion.md) says v0.1 offers no
  deletion, and the epic's rule is that the schema must not foreclose one. A cascade would answer
  that question silently.
- **Argon2id via Bouncy Castle** (`org.bouncycastle:bcprov-jdk18on`, a new entry in
  `gradle/libs.versions.toml`), `m = 19456 KiB, t = 2, p = 1`, a 16-byte `SecureRandom` salt, a
  32-byte output, stored as the PHC string `$argon2id$v=19$m=19456,t=2,p=1$<salt>$<hash>`. Bouncy
  Castle emits no PHC, so encoding **and parsing** it are ours and are tested against published
  vectors — a parser that silently accepts the wrong parameters is a downgrade attack in a helper
  function.
- **`kind` carries `"password"` and nothing else**
  ([`ADR-0041`](../../docs/adr/ADR-0041-a-handle-and-a-password-are-the-only-credential.md)). The
  column is not narrowed or constrained — the door stays open and unfinished.
- **The handle is stored already folded**: ASCII-lowercased, 3–32 characters of `[a-z0-9._-]`, the
  first of `[a-z0-9]`. The rule lives in the write path, not in a `CHECK`. A handle is never shown to
  anybody, so only its canonical form needs to exist — the deliberate contrast with `ADR-0029`'s
  display name, which is shown and therefore stores what the player typed.
- **Nothing returns a hash.** The port in `duels.poker.server.auth` exposes
  `verify(kind, identifier, presented): PlayerId?` and functions that write. This is structural over
  the public API and a test asserts it there, not by inspection.
- **Value classes redact themselves**: `SessionToken` and the presented secret have a `toString()`
  that returns a fixed string, so leaking one into a log line takes intent rather than a careless
  template.
- **Verification runs on `Dispatchers.IO.limitedParallelism(4)`**, bounding peak Argon2 memory at
  roughly 4 × 19 MiB. A memory-hard hash with unbounded concurrency is a self-service denial of
  service.
- No endpoint, no route and no wire change in this story. `PROTOCOL_VERSION` does not move here.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0403` when it comes up.* | — |

## Acceptance criteria

- [ ] The migration applies on top of the existing chain, `V1`–`V3` byte-unchanged, and both tables
      exist with their constraints — asserted against the database container.
- [ ] A password hashed and then verified against the same PHC string succeeds; the same password
      hashed twice produces two different strings (the salt varies), and both verify.
- [ ] A wrong password fails verification, and an unknown identifier answers exactly as a wrong
      password does.
- [ ] The PHC encoder round-trips through the parser, and the parser refuses a string whose
      parameters or version differ from what was written.
- [ ] Two credentials with the same `kind` and `identifier` are refused by the database.
- [ ] No public function in `duels.poker.server.auth` or `duels.poker.server.db` returns a hash,
      asserted by reflecting over the API rather than by reading it.
- [ ] `SessionToken.toString()` and the presented-secret type's `toString()` return a redaction, and
      a test proves the secret is not in the output.

## Out of scope

- Every endpoint — `STORY-0404` (sign-up) and `STORY-0405` (sign-in, session, socket).
- The recovery email, the verification token and the password reset — `STORY-0416`; its three tables
  are their own migration.
- Rate limiting and the enumeration defences, which belong with the endpoints that need them.
- The device-binding revocation column or row, whatever shape it takes — `STORY-0406`, and `DEC-041`
  first.
