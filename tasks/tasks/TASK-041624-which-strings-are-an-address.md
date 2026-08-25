---
schema: 2
id: TASK-041624
title: Which strings are an address
type: task
status: ready
parent: STORY-0416
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, auth, blocked]
depends_on: [TASK-041623]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.auth.EmailAddressSyntaxTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Unblocked

**`DEC-071` — the product owner's — is answered and merged**, so this section is history rather than
a gate. The `blocked` label in the front matter is a historical marker and this ticket's `status:` is
not `blocked`.

[`ADR-0078`](../../docs/adr/ADR-0078-the-mail-is-the-only-real-check-on-an-address.md) — *the mail is
the only real check on an address, so the syntax rule refuses almost nothing.* §1 states the rule,
§4 confirms that nothing is canonicalised, §5 fixes the answer at `400` with an empty body, and **§6
supplies both fixture tables**, which this ticket previously left empty by design. They are
transcribed below verbatim; that transcription is the whole of what changed here.

The ADR errs **permissive** and says so. In practice only a bare handle and an empty field ever
produce a `400`; `a@b`, `bob@localhost`, `bob@gmail.con`, `admin@example.com` and
`"john smith"@example.com` are all addresses as far as this function is concerned, and the mail that
never arrives is this system's real deliverability check (§3).

## Goal

`emailAddressOrNull` accepts a string that holds an `@` which is neither its first nor its last code
point, holds no ASCII control character, and is at most 254 code points long — returning it
**unchanged** — and answers `null` for everything else.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/EmailAddressSyntax.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/auth/EmailAddressSyntaxTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/LoginHandle.kt` — `loginHandleOrNull` is the
house shape for a canonicalise-or-refuse function, including how it counts **code points** with
`String.codePointCount(0, length)` rather than `String.length`, and where its bounds live as named
`private const val`s;
`poker-server/src/main/kotlin/duels/poker/server/auth/EmailAddress.kt` — a `@JvmInline value class`
with a `value` property and a redacting `toString()`. It stays as it is: `TASK-041603`'s *"no `init`,
no `require`, no regex"* holds, because the type is constructed from stored rows too and a throwing
constructor would turn a refusal into a `500` (`ADR-0078` §4);
`docs/adr/ADR-0078-the-mail-is-the-only-real-check-on-an-address.md` §1, §4 and §6.

## Scope

- One `public fun emailAddressOrNull(raw: String): EmailAddress?` in `duels.poker.server.auth`,
  accepting exactly when **all five** of `ADR-0078` §1's clauses hold:
  1. `raw` contains at least one `@` (`U+0040`);
  2. its first code point is not `@`;
  3. its last code point is not `@`;
  4. it contains no ASCII control character — nothing in `U+0000`–`U+001F`, and not `U+007F`;
  5. it is at most **254 code points** long.

  There is no separate minimum: clauses 1–3 make three code points the shortest thing that can pass,
  and `a@b` is that string.
- **254 is counted in code points**, `raw.codePointCount(0, raw.length)`, never `String.length` —
  the unit `ADR-0029` §2 and `ADR-0048` §1 already fixed, and the one `loginHandleOrNull` uses. The
  ceiling lives in a named `private const val`; nothing else in the file is a bare number.
- On acceptance the function returns `EmailAddress(raw)` with `raw` **unmodified** — no `trim`, no
  `lowercase`, no `Normalizer`. `ADR-0031` §2 stores what the player typed because that is what must
  be delivered to, and the only fold that exists is the database index's (`ADR-0078` §4).
- The KDoc cites `ADR-0078` §1 by number, states the five clauses in prose, and carries **one
  sentence saying clause 4 is a security clause and not a syntax clause**: no `addr-spec` holds an
  ASCII control character in any position, so it denies no mailbox, and it is there because a line
  terminator inside an address is the one thing this predicate could hand `EPIC-07`'s unwritten
  transport that would harm somebody who is not a player of this game. Without that sentence a later
  reader tidies the clause away as arbitrary.
- The file holds one top-level function and its constants and **no top-level class**, so ktlint's
  filename rule does not apply to it.

## Out of scope

Read this list as the tests it does **not** produce. `ADR-0078` §3 is a list of refusals the rule
declines to make, and a refusal produces no `Tests` row; each line below therefore says what gates
it, or says plainly that nothing does.

- **DNS, MX, an SMTP probe or any third-party validation** — §3. **Gated by nothing executable.**
  There is no test that a socket was not opened; what a reviewer checks instead is that this file
  imports nothing beyond `kotlin`/`java.lang` and takes no parameter but `raw`. The acceptance
  criteria say so in those terms rather than pretending a test covers it.
- **A domain that does not exist or is misspelled** — `bob@gmail.con` is accepted (§3). This is the
  typo that actually costs a player their recovery and the rule does not catch it. Ungated by
  construction: there is nothing to assert.
- **A blocklist, role addresses, disposable mailboxes** — all accepted (§3). Gated in one direction
  only, by `postmaster@duels.test` in the held-out table below; nothing asserts that a *list* was not
  introduced.
- **Plus-address stripping** — refused by §3, gated by `bob+duels@example.com` in the accepted table,
  which asserts the tag survives byte for byte.
- **Folding, trimming and normalising** — refused by §4, gated by `anAcceptedAddressIsNotCanonicalised`
  and by `Bob@Example.com` in the accepted table.
- **A space inside an address** — accepted, and deliberately so (§3, RFC 5321's quoted local part).
  Gated by `"john smith"@example.com` in the accepted table. A tab is a different matter and is
  refused under clause 4, which is the held-out table's job.
- **Punycode, transliteration or any unicode conversion** — refused by §3, gated by `рома@пример.рф`.
- The endpoint that calls this, its `400`, and its `401`/`202` — `TASK-041625`.
- A second rule at the client — `STORY-0417`. The server's answer is authoritative either way.
- Applying the predicate anywhere else. §2 puts it at `POST /api/auth/recovery-email` and nowhere
  else: `forgot-password` keeps its unconditional `202`, `verify-email` takes a token, and **no
  stored address is ever re-judged**. That last property is what makes a future tightening free, and
  it is the one this ticket must not spend.

## Tests

`EmailAddressSyntaxTest`. The first two tables are `ADR-0078` §6 transcribed exactly — seven strings
each, no additions and no omissions. **Every string is written as a literal in the test**, and no
fixture may be computed from the implementation's constants: a test that says
`"a".repeat(MAX_ADDRESS_CODE_POINTS - 8)` moves with an off-by-one instead of catching it.

| Test | Proves |
| --- | --- |
| `everyAcceptedFormIsAccepted` | Each of §6's seven accepted strings returns a non-null `EmailAddress` whose `value` is the input **unchanged**. `a@b` and `bob@localhost` are the two the naive `.+@.+\..+` refuses |
| `everyRefusedFormIsRefused` | Each of §6's seven refused strings returns `null`. `bob\u0000@example.com` and the 255-code-point string are the two the naive regex accepts |
| `anAcceptedAddressIsNotCanonicalised` | `emailAddressOrNull("Bob@Example.com")?.value` is `"Bob@Example.com"` — no trim, no fold, no normalisation |
| `theRuleIsAppliedToStringsInNoFixtureTable` | Eight strings that appear in **neither** §6 table and in neither table above are judged correctly. This is the only test in the file that an implementation hard-coded to the fixtures fails; see `## Proof` steps 5 and 6 |

**Accepted** — `ADR-0078` §6, as Kotlin literals:

| Literal | Why §6 lists it |
| --- | --- |
| `"a@b"` | The shortest string that passes. **The naive regex refuses it and a mail system does not** |
| `"bob@localhost"` | No dot in the domain. Also refused by the naive regex |
| `"Bob@Example.com"` | Case survives — the fold is the index's |
| `"bob+duels@example.com"` | Plus-addressing, tag intact |
| `"\"john smith\"@example.com"` | A quoted local part containing a space — why a space is not refused |
| `"рома@пример.рф"` | Non-ASCII on both sides, unconverted |
| `"a".repeat(246) + "@ex.test"` | 254 code points: the ceiling, inclusive |

**Refused** — `ADR-0078` §6, as Kotlin literals:

| Literal | Why §6 lists it |
| --- | --- |
| `"bob"` | No `@` — the handle typed into the address field. The one refusal a player can act on |
| `""` | No `@`. The submitted-empty form, and the likeliest way a refusal actually arrives |
| `"@example.com"` | Begins with `@`; nothing to deliver to |
| `"bob@"` | Ends with `@`; no domain |
| `"bob\u0000@example.com"` | An ASCII control character. **The naive regex accepts it** — `.` matches `U+0000` |
| `"bob@example.com\r\nBcc: someone@else.test"` | A line terminator handed to a transport: the clause-4 case that matters |
| `"a".repeat(247) + "@ex.test"` | 255 code points, one past the ceiling. **The naive regex accepts it** |

**Held out** — in `ADR-0078` §6 nowhere, and in neither table above. Assert as pairs of string and
expected outcome:

| Literal | Expected | What it kills |
| --- | --- | --- |
| `"postmaster@duels.test"` | accepted | An implementation that accepts only the strings in the accepted table. Also the one place a role address is asserted to be fine (§3) |
| `"postmaster.duels.test"` | refused | An implementation that refuses only the strings in the refused table. Clause 1, on unseen input |
| `"@postmaster.duels.test"` | refused | Clause 2, on unseen input |
| `"postmaster@duels.test@"` | refused | Clause 3, on unseen input |
| `"postmaster\u0009@duels.test"` | refused | Clause 4 with a **tab**, which reads as ordinary whitespace and is not: §3's accepted space is `U+0020` and this is `U+0009` |
| `"b".repeat(243) + "@duels.test"` | accepted | 254 code points on a string in no table — the ceiling, inclusive |
| `"b".repeat(244) + "@duels.test"` | refused | 255 code points on a string in no table — one past it |
| `"😀".repeat(124) + "@duels.test"` | accepted | **135 code points but 259 UTF-16 units.** The only fixture anywhere that tells `codePointCount` from `String.length`; every other string in this file is BMP, so an implementation measuring `raw.length` passes all fourteen §6 entries |

Two of those are written as **escapes in the Kotlin source, never as the character itself**: the tab
entry, because a raw tab inside a string literal is invisible in a diff and ktlint's indentation rule
has an opinion about it; and the two control-character entries in the refused table, for the same
reason. The astral entry is `U+1F600`, and the emoji literal and its surrogate-pair escape compile to
the same two-`Char` string — either is fine, and whichever is chosen gets a comment naming the code
point, because *why is there an emoji in this test* is the first question a reader has.

## Acceptance criteria

- [ ] `EmailAddressSyntaxTest.everyAcceptedFormIsAccepted` passes
- [ ] `EmailAddressSyntaxTest.everyRefusedFormIsRefused` passes
- [ ] `EmailAddressSyntaxTest.anAcceptedAddressIsNotCanonicalised` passes
- [ ] `EmailAddressSyntaxTest.theRuleIsAppliedToStringsInNoFixtureTable` passes
- [ ] The accepted table holds **exactly** the seven literals listed above and the refused table
      **exactly** the seven listed above — `ADR-0078` §6 with nothing added and nothing dropped
- [ ] The held-out table holds **exactly** the eight literals listed above, and **no string in it
      appears in `ADR-0078` §6 or in either of the other two tables in this file**
- [ ] Every fixture is a literal: no fixture references `MAX_ADDRESS_CODE_POINTS` or any other
      constant declared in `EmailAddressSyntax.kt`
- [ ] `emailAddressOrNull` returns the input string unchanged on acceptance — the file contains no
      `trim`, no `lowercase`, no `uppercase` and no `Normalizer`
- [ ] The length check reads `codePointCount`; the string `.length` appears in `EmailAddressSyntax.kt`
      only as the second argument to it
- [ ] `EmailAddressSyntax.kt` takes no parameter but `raw`, imports nothing outside `kotlin` and
      `java.lang`, and therefore opens no socket and reads no configuration
- [ ] `EmailAddress.kt` is unchanged
- [ ] Every command in `verify:` exits 0

## Proof

1. Replace the body with `return EmailAddress(raw)` — accept everything.
   **`everyRefusedFormIsRefused` reddens at `"bob"` and `theRuleIsAppliedToStringsInNoFixtureTable`
   reddens at `"postmaster.duels.test"`** — predict **two**. Revert.
2. Replace the body with `return null` — refuse everything. **`everyAcceptedFormIsAccepted`,
   `anAcceptedAddressIsNotCanonicalised` and `theRuleIsAppliedToStringsInNoFixtureTable` all
   redden**; `everyRefusedFormIsRefused` passes — predict **three**. That pair of steps is why both
   §6 tables have to be non-empty: one list alone cannot tell a rule from a constant. Revert.
3. Return `EmailAddress(raw.lowercase())` on acceptance. **`anAcceptedAddressIsNotCanonicalised` and
   `everyAcceptedFormIsAccepted` redden** — the latter at `"Bob@Example.com"`, the only entry with an
   uppercase letter — and `theRuleIsAppliedToStringsInNoFixtureTable` **stays green**, since every
   accepted held-out string is already lower case. Predict exactly **two**, and that the third stays
   green; a coder who predicts three has not read the tables. Revert.
4. Replace the implementation with
   `if (raw.matches(Regex(".+@.+\\..+"))) EmailAddress(raw) else null` — the first regex anyone
   writes. **Three redden**: `everyAcceptedFormIsAccepted` at `"a@b"` (no dot in the domain),
   `everyRefusedFormIsRefused` at `"bob\u0000@example.com"` (`.` matches `U+0000`), and
   `theRuleIsAppliedToStringsInNoFixtureTable` at `"postmaster\u0009@duels.test"`. Now delete the
   four bolded §6 entries — `a@b`, `bob@localhost`, `bob\u0000@example.com` and the 255-code-point
   string — and run the regex again: **only the held-out test still reddens**. That is what those
   four entries are for, and it is why the criteria above pin the tables to §6 exactly. Restore.
5. **The mutation this ticket exists to survive.** Replace the implementation with a lookup:
   `return if (raw in setOf(/* the seven accepted literals */)) EmailAddress(raw) else null`.
   **`everyAcceptedFormIsAccepted`, `everyRefusedFormIsRefused` and
   `anAcceptedAddressIsNotCanonicalised` all pass. `theRuleIsAppliedToStringsInNoFixtureTable` reddens
   alone**, at `"postmaster@duels.test"`. A table-driven test over the ADR's own fixtures cannot tell
   the rule from a list of the fixtures, and the held-out table is the only thing in this file that
   can. Revert.
6. The mirror: `return if (raw in setOf(/* the seven refused literals */)) null else EmailAddress(raw)`.
   The same three pass and **`theRuleIsAppliedToStringsInNoFixtureTable` reddens alone**, this time at
   `"postmaster.duels.test"`. Both directions, because one held-out string only kills one of the two
   lookups — which is why the held-out table needs an accepted entry *and* a refused one. Revert.
7. Change the ceiling test to `> 255`. **`everyRefusedFormIsRefused` reddens at the 255-code-point
   entry and `theRuleIsAppliedToStringsInNoFixtureTable` reddens at `"b".repeat(244) + "@duels.test"`**
   — two. Now change it to `>= 254` instead: **`everyAcceptedFormIsAccepted` reddens at the
   254-code-point entry and the held-out test at `"b".repeat(243) + "@duels.test"`** — two again.
   Both directions of the off-by-one are caught, and only because the fixtures are literals: a test
   that computed `246` from the constant would move with the mutation and redden at neither. Revert.
8. Replace `raw.codePointCount(0, raw.length)` with `raw.length`.
   **`theRuleIsAppliedToStringsInNoFixtureTable` reddens alone**, at
   `"😀".repeat(124) + "@duels.test"` — 135 code points, 259 UTF-16 units, so the mutant
   refuses an address the rule accepts. Every other fixture in this file is BMP and cannot see the
   difference, which is the entire reason that one entry is in the table. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
