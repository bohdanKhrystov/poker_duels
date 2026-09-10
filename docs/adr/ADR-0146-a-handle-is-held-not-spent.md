# ADR-0146 — A handle is held, not spent, and signing up owes no irreversibility sentence

- **Status:** Accepted
- **Date:** 2026-09-10
- **Resolves:** `DEC-157` — **the product owner's** — does *give this profile a password* owe an
  irreversibility sentence of its own? Registered 2026-09-09 by
  [`ADR-0143`](ADR-0143-irreversibility-is-said-last-and-never-coloured.md) §4, which found it by
  measuring rather than by being asked and deliberately declined to close it. **Answered: no. The
  act takes nothing from the player, and a handle is held, not spent.** The register's second half —
  *is a handle spent in the sense [`ADR-0130`](ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md)
  §2 gives the word, or merely held for as long as the credential row exists?* — is §3: **held.**
  The two words name opposite states of the same unique index, and the difference is not a nuance;
  it is the whole answer.
- **Where the answer came from:** **derived from the vision; the human did not state this call.**
  Most of the work is done by merged ADRs and by measurement — `ADR-0030`, `ADR-0031` §1,
  `ADR-0039` and `ADR-0130` §2 between them already settle what signing up costs, which is nothing —
  and the vision breaks the one tie those leave: whether a permanent-but-costless act should get a
  sentence anyway, to be safe. The licensing sentence is [`docs/vision.md`](../vision.md)'s
  *Positioning* — *"The reference points are **Lichess** and **Chess.com**, not PokerStars. **Dark,
  quiet, fast, minimal.**"* — read as saying that a quiet screen does not warn about a cost it does
  not impose. The boundary is [`docs/workflow.md`](../workflow.md)'s row assigning *"what a player
  sees, and what they are told"* to this role: nothing in *What it is* or *What it is not* moves in
  either direction, and there is no money, no roadmap milestone, no new kind of thing and no risk
  outside the software. **No string is added and none is removed**, so `ADR-0091` §3's minting is
  not engaged either.
- **Supersedes nothing.** No sentence of any merged ADR is deleted, replaced or narrowed.
- **Corrects one word, in a clause that decided nothing.** `ADR-0143` §Consequences 5 reads
  *"give this profile a password **spends** a handle and says nothing"*. §3 finds that word wrong on
  the facts: a handle is claimed and **held**, never spent. It is a characterisation inside the
  bullet that registers `DEC-157`, in an ADR whose §4 says in as many words that it *"does not
  close"* the question — so correcting it supersedes no decision, and the register's own second half
  is what asked for the correction.
- **Applies, and amends nothing:** `ADR-0143` §2's three obligations, §4's table and §1's refusal of
  a danger register; [`ADR-0031`](ADR-0031-an-optional-verified-recovery-email.md) §1's
  ***"Nothing here changes a handle. No endpoint updates `credential.identifier`; the row is written
  once, at sign-up. That is not foreclosed … but it is not built, and no story should assume it"***,
  its ***"a handle is never shown to anybody"*** and its ***"The handle appears in no response body
  and in no `ServerMessage`, ever"*** with the reset-mail exception `ADR-0082` implements;
  [`ADR-0030`](ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md), whose title is the fact
  this rests on; [`ADR-0039`](ADR-0039-v01-offers-no-account-deletion.md)'s refusal of deletion in
  v0.1; `ADR-0130` §2 and its derivation from `ADR-0067` §1;
  [`ADR-0125`](ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md) §3's
  three claims about an anonymous profile; `ADR-0027` §6 and `ADR-0029` §5, which are why this
  product will not discuss which handles exist.
- **Constrains:** `web-client/src/account/account-text.ts` and `SignUpForm.tsx`, which gain no
  sentence and no second press; `design/screens/account.html`, which owes **one margin sentence and
  no change to any frame** (§5). **No wire change, no `PROTOCOL_VERSION` move, no server file, no
  engine file, no migration, no new string, no new token, no new control.**
- **Registers nothing.** One adjacent question is named in §6 and deliberately left unregistered.

## Context

### What the act actually does, measured on `develop` at `cee03146`

**One row is written and nothing else moves.** `POST /api/auth/sign-up` creates a `credential` row —
`kind = 'password'`, `identifier` the folded handle, `secret_hash` the Argon2id string — under
`V4__credential_and_auth_session.sql:17`'s `CONSTRAINT credential_kind_identifier_unique UNIQUE
(kind, identifier)`. `ADR-0030`'s title is the rest of it: *a claim adds a credential and moves
nothing*. The profile keeps its id, its duel coins, its duels and its display name, and the screen
already says so — `ANONYMOUS_WAY_OUT`, *"The form below gives this profile a password and keeps
every duel coin and every duel, because nothing moves."*

**Nothing removes that row.** `DELETE FROM credential` occurs **0** times in `poker-server/`; the
only write to an existing credential is `PostgresPasswordResets.kt:223`'s `UPDATE credential SET
secret_hash = ? WHERE player_id = ? AND kind = 'password'`. `ADR-0039` refuses account deletion in
v0.1, and V4's own header comment records the missing `ON DELETE` clause as deliberate. So the act
cannot be undone, in the plain sense that no path in the product reverses it.

**Nothing changes the handle either — and that is explicitly not a decision.** `ADR-0031` §1: *"No
endpoint updates `credential.identifier`; the row is written once, at sign-up. **That is not
foreclosed** — an `UPDATE` under the same unique constraint would do it — but it is not built, and
no story should assume it."* The fixity of a handle is an unbuilt mechanism, not a rule the product
has adopted. Contrast the display name, whose permanence `ADR-0029` §4 made *machinery* — a trigger
whose stated reason was that the invariant *"is defined by the impossibility of that `UPDATE`"* —
and which `ADR-0130` §1 then had to supersede with a migration.

### A handle is not a display name, and the difference is structural

`ADR-0130` §2's derivation is entirely about visibility: *"the display name is a leaderboard row's
only identity (`ADR-0067` §1), and history is a live join (`ADR-0039`), so a reissued string would
print a second player onto the first player's finished duels — on the opponent's own history screen,
retroactively."*

None of that transfers. `ADR-0031` §1: *"a handle is **never shown to anybody**"*, and
*"The handle appears in no response body and in no `ServerMessage`, ever"* — the one exception being
the password-reset mail, which goes only to a proven mailbox (`ADR-0082`). Measured: `ProfileResponse`
carries **seven** fields — `playerId`, `coinBalance`, `displayName`, `displayNameRemoved`,
`deviceRouteLive`, `hasRecoveryEmail`, `hasPassword` — and no handle. A handle that came free and
was handed to somebody else would print a second player onto **no** row, because a handle is printed
on none.

### What is in tension

**Something permanent does happen, and the form says nothing about it.** That is the report, and it
is not imagined: a player types a string into a field labelled `Handle`, and under today's product
that string is the one they will type at every sign-in on every browser, forever, and no other
player will ever have it. `STORY-0411`'s entitlement, quoted by `ADR-0052` and by `ADR-0130` §5, is
general: *a player is entitled to know that at the moment they can still avoid it.* One sentence
would discharge it.

**But the sentence would assert something the product has declined to assert.** The only honest
permanence sentence — *you cannot change this later* — is a claim `ADR-0031` §1 explicitly leaves
open in both directions. Writing it into shipped copy would decide handle permanence in a string
rather than in an ADR, which is precisely the failure `DEC-157` was registered to avoid.

**And the product has just finished paying to delete exactly such a sentence.** `PERMANENCE_LINE` —
*"A name is chosen once. You cannot change it later, and it can be taken away."* — shipped from
`ADR-0119` §3 until `ADR-0130` §5 retired it; `ADR-0130`'s own Context called it *"a lie with a date
on it"*, and its Consequences priced the removal as *"real work … not a line removed"*. Measured
today: **0** occurrences of `PERMANENCE_LINE` in `web-client/src`. Since `ADR-0142` §5 a shipped
string is also a register entry paired to a card, so the price of the next one is higher than the
last.

**The act is a gain, and the screen is already built to say so.** `ADR-0125` §3 spends three
sentences on the anonymous profile — what it is, what that costs, and *"the way out with what it
keeps"* — and `ADR-0036`'s standing rule is that an account is *offered, never required*. A warning
placed at the end of that is the product arguing with itself on one panel.

**The evidence is one ADR's reading of three string constants.** `ADR-0143` §4 measured
`SIGN_UP_LABEL`, `HANDLE_LABEL` and `PASSWORD_LABEL` and was right about all three. The screen
carries a fourth sentence the row did not count — `ANONYMOUS_WAY_OUT`, immediately above the form.

### The deadline

The register names it: *before the sign-up form's words are next opened, or before the first player
who is not the author holds an account.* Both clocks are real, and neither is a reason to answer a
particular way.

What is a reason is the asymmetry. Adding a sentence later costs one export and one card line and
nothing is lost. Removing a shipped one is what `ADR-0130` §5 has just cost: a golden string, its
tests, a card line and — since `ADR-0142` §5 — a register entry, all deleted, plus the player-facing
embarrassment of a promise withdrawn. With no players yet, the cheap direction is not to ship it,
and that is stated as a reason rather than dressed up as a principle.

## Decision

### 1. *Give this profile a password* owes no irreversibility sentence

**No sentence about permanence, loss or irreversibility is added to the sign-up form, to
`account-text.ts`, or to the account screen on behalf of this act.** `SignUpForm` takes none of
`ADR-0143` §2's three obligations and gets **no second press**. `SIGN_UP_LABEL`, `HANDLE_LABEL` and
`PASSWORD_LABEL` are unchanged.

**And no copy in this product asserts that a handle is permanent, or that it is now unavailable to
anybody else.** That holds on every surface, now and later — a sentence saying either would be
deciding, in a string, a question `ADR-0031` §1 left open and `ADR-0029` §5 refused.

### 2. `ADR-0143` §2 is triggered by a cost, not by a row nothing deletes

`ADR-0143` §2 is **applied, not narrowed**: every sentence of it stands, and this section writes down
the test its §4 table left implicit.

**An act owes §2's obligations when a player who performs it ends up with less than they had and
cannot get it back.** Not when the row it writes cannot be deleted. Under `ADR-0039` almost nothing
in this product can be deleted — a `duel_result`, a `name_registry` entry, an `auth_session`, a
`player` — and a rule that fired on permanence alone would put a warning under every control on
every screen, which is how the three sentences the product already ships would stop being read.

Measured against the three acts that do conform: revoking loses a route, signing out can lose a duel
and hands the browser a different profile, renaming retires a string the player will never have
again. Signing up loses none of these. The obligation is discharged by there being nothing to
discharge — not waived.

### 3. A handle is held, not spent

`ADR-0130` §2's word describes a string that has **left** its owner: *"Nobody else may take it,
ever"*, and *"The player who gave it up may not take it back either."* A spent string sits in
`name_registry` attached to nobody.

**A handle has left nobody.** It sits in `credential.identifier` naming exactly the player who chose
it, and it goes on doing that for as long as the row exists, which under `ADR-0039` is indefinitely.
The two words describe opposite states of the same unique index: a spent name is one nobody may use,
a held handle is one its owner alone uses. Nothing in the product converts the second into the
first, and this ADR builds nothing that would.

**What is true, and is not the player's cost:** while that row lives, no other player may claim the
string. That loss lands on a stranger, not on the person pressing the button, and it is the one
thing this product has deliberately refused to discuss — `ADR-0029` §5 refuses an availability
endpoint, and the sign-up `409` conflates *the handle is taken* with *this player already holds a
password* on `ADR-0027` §6's anti-enumeration reasoning, which `HANDLE_UNAVAILABLE` renders as one
sentence for both. **No third party is owed a sentence on somebody else's form**, and a screen that
started giving them one would be a screen that had started answering *which handles are free*.

### 4. Every irreversible act in the product today, with the fourth row answered

| Act | Says what it costs, before the act | Last before the control | Second press |
| --- | --- | --- | --- |
| Set or change a display name | yes — `ADR-0130` §5 obligation 2, unconditional | yes | **refused** by `ADR-0130` §1, §7 |
| Stop this device signing in | yes — `REVOKE_PERMANENT`, unconditional | yes — `RevokeControl.tsx` | yes |
| Sign out | yes — `signOutWarning`, conditional and truthfully so | yes — `SignOutControl.tsx` | yes |
| Give this profile a password | **nothing to say — the act costs the player nothing** | n/a | **not owed** |

The first three rows are unchanged and **nothing about them moves**. `ADR-0143` §4's table is
completed rather than corrected: its fourth row measured a silence, and this ADR answers what the
silence is.

**What the screen already says, measured.** In the state `design/screens/account.html`'s
*No password yet* frame draws — and it is the state almost every real player is in —
`ANONYMOUS_WAY_OUT` is the last line before the panel, with only the two fields between it and the
button. So even read as a cost claim, the screen already satisfies `ADR-0143` §2.2 in the state that
matters, and the claim it makes is *nothing moves*.

**And the one state where it does not.** `AccountScreen.tsx:106` gates the form on
`!signedIn`, while `:133` gates the three anonymous sentences on `!hasPassword`, so a browser that is
not signed in but whose player holds a password renders the form with none of them above it. That
state is reachable — sign-up issues no session token — and in it the form can only ever answer the
`409` that `HANDLE_UNAVAILABLE` renders. **Whether a form that cannot succeed should render at all
is not this decision's**, and §6 says so rather than folding it in.

### 5. What the account card owes: one margin sentence, and no change to any frame

`design/screens/account.html`'s *No password yet* frame is correct as drawn. It owes `ADR-0126` §4's
shape and nothing more: **one line in that frame's existing margin note recording that the panel
carries no permanence or irreversibility sentence and none is owed, under this ADR.** No frame
changes, no new `<p class="line">`, no string. The planner cuts the ticket; this ADR writes none.

The reason is the same one `ADR-0143` §5 gave: so that the silence is graded at the pane as a
decision rather than re-litigated as an oversight, and so the next reader of this card inherits the
answer instead of re-raising the question. **No gate is bought**, and the honest note is that a
margin sentence is not one: nothing fails if the line never lands.

### 6. What this decides nothing about

- **Whether a handle can ever be changed.** `ADR-0031` §1 leaves it open in both directions —
  *"not foreclosed … but it is not built"* — and this ADR leaves it exactly there. Answering *yes*
  would be a mechanism nobody has asked for; answering *no* is the commitment §1 declines to make.
- **Whether the account screen should ever show a player their own handle.** A different question
  with a real hazard behind it (§Consequences 2), and answering it means a wire field and a reversal
  of `ADR-0031` §1's *no response body, ever*. **Deliberately not registered**: nobody has asked, and
  a register row nothing actions is a deferral rather than a decision.
- **Whether the sign-up form should render in the one state where it can only refuse.** Measured in
  §4, adjacent to `DEC-090`'s family of questions about what the account screen offers when, and not
  answered here.
- **The words of the anonymous block.** `ADR-0125` §3's three claims, unchanged in their words and
  their trigger.
- **Anything about the recovery address**, the sign-in screen, or `DEC-090` and `DEC-091`.
- **`ADR-0143` §1.** Its refusal of a danger register is applied, not reopened; §1 here adds no
  treatment because it adds nothing at all.

### 7. What would reverse this

1. **A merged ADR deciding that a handle is permanent** — in machinery or in policy. §2's test fires
   the same day, the sentence becomes owed, and this ADR is superseded rather than qualified. That is
   the one thing that flips this, and it is a decision, not a drift.
2. **One player asking to change their handle, or asking what theirs is.** Needs no instrument: no
   mechanism in the product serves either request, so they have to ask a person, and one of them is
   the observation. It is also the only signal that separates *nothing was owed* from *nobody
   noticed*.
3. **The human**, in one sentence, at any time.

**What is not evidence:** another agent reading `ADR-0143` §4's table and reporting that the fourth
row is emptier than the other three. That is the report this ADR answers; repeating it is not a
second observation.

## Consequences

**What it buys.** The gap `ADR-0143` shipped knowingly — *"the account screen ships knowingly
inconsistent with its own new rule"* — is closed, and closed by finding the rule does not bind rather
than by adding a sentence, so `ADR-0143` §2 keeps meaning what it says. The next act that looks
irreversible gets a test (*does the player end up with less?*) instead of a re-derivation at Haiku
temperature. `SIGN_UP_LABEL`, `HANDLE_LABEL` and `PASSWORD_LABEL` stop being a standing finding, so
`TASK-140910`–`TASK-140912` and every future card transcribe them without inventing a caution
between them. And the product does not ship a fourth permanence promise one document after paying to
delete its third.

**What it costs.**

1. **A mistyped handle is still forever, and nothing says so while it can still be avoided.**
   `ADR-0029`'s own sharpest recorded cost — *"A typo is forever… a player named `Bobb` who meant
   `Bob`, with no recourse, ever"* — is what `ADR-0130` removed for display names, and it is still
   true of handles. This ADR declines to spend a sentence on it. It is small because a handle is
   shown to nobody and real because the player types it at every sign-in on every new browser, and
   `STORY-0411`'s entitlement is knowingly not extended here.
2. **A player with no recovery address who forgets their handle has no path back.** Measured:
   `ProfileResponse` carries no handle, and the reset mail is the only place one ever appears
   (`ADR-0031` §1, `ADR-0082`) — and a recovery address is optional by `ADR-0031`'s own decision.
   Nothing at sign-up tells a player that the string they are typing is the one thing the product
   will never show them again. That hazard is left exactly where `ADR-0031` §Consequences left it,
   and §6 names the fix this ADR refuses to smuggle in.
3. **The answer is conditional on a decision nobody has made, and nothing detects the condition
   changing.** If a future ADR fixes the handle, the sentence becomes owed — but no gate, test or
   trigger notices, and only a reader of §7 will. The product will be one merge away from being
   silently wrong, in a direction no CI job can see.
4. **The gap is closed by ruling rather than by shipping, so it still looks like a gap.** A reader
   comparing the four acts on one screen still sees one form saying less than the other three, and
   now has to read an ADR to learn why. §5's margin sentence is the whole remedy and it is not a
   gate — `ADR-0126` §Consequences 5's price (*"a refusal that leaves nothing behind"*) is paid a
   third time, knowingly.

**What it forecloses.** A warning sentence on the sign-up form, a second press on it, and any copy
anywhere asserting that a handle is permanent or that it is now unavailable to others — until §7's
trigger or the human's sentence. **Nothing irreversible is spent**: reversing this costs one exported
string and one line of a card note. No wire field, no migration, no promise withdrawn, nothing
un-said. With no players yet and the whole of the evidence being one ADR's measurement of three
string constants, being the cheapest answer to reverse is a substantial part of why it is the answer.

## Alternatives considered

**1. Say it — one sentence on the form telling the player the handle is theirs for good.** The
strongest case in the set, and the one the register was written expecting. The act genuinely cannot
be undone; `STORY-0411`'s entitlement is stated generally and has been honoured three times on this
very screen; the sentence costs one export and no colour, no token and no minting; and a first-timer
choosing a login string has no other way to learn that the choice is not casual. **Rejected on
three counts, in order of weight.** The claim is not the product's to make: `ADR-0031` §1 says the
handle's fixity is *"not foreclosed … but it is not built"*, so *you cannot change this later* would
decide handle permanence in shipped copy rather than in an ADR — the exact move `DEC-157` exists to
prevent. The product deleted the identical sentence for names one document ago, at a price
`ADR-0130` recorded as *"real work … not a line removed"*, and `ADR-0142` §5 has since made a shipped
string a register entry too, so the next withdrawal costs more than the last. And the obligation it
would discharge is not owed, because §2's test is a cost and this act has none.

**2. Say the true, weaker thing — *nobody else can use this handle*.** Its case is that it dodges the
first objection entirely: it is unambiguously true today, it makes no claim about the future, and it
would sit last before the control exactly as `ADR-0143` §2.2 wants. **Rejected because it states a
cost that lands on somebody who is not reading it.** It is information about a stranger, not a
warning to the player, so it discharges no entitlement — and it is the one subject this product has
refused: `ADR-0029` §5 refuses an availability endpoint, and the sign-up `409` deliberately conflates
*the handle is taken* with *this player already holds a password* on `ADR-0027` §6's anti-enumeration
reasoning. A screen that tells a player their handle is now unavailable to others has started
answering which handles are available.

**3. Give the form the second press, matching `RevokeControl` and `SignOutControl`.** Its case is
better here than it was for the name form: this is the product's own shipped grammar, it needs no
colour, no token and no minting session, and unlike `ADR-0143`'s alternative 2 it is genuinely
**available** — no merged ADR refuses a confirmation on this form, so §2.3's *"wherever no merged ADR
has refused one"* is satisfied. **Rejected because a second press guards a loss, and there is none.**
It would put friction on an act whose entire content is a gain, on a panel where `ADR-0125` §3
already spends three sentences persuading the player it is safe (*"the way out with what it keeps"*),
and `ADR-0143` §2.3 is explicit that a press *"is the only place weight is permitted to cost a player
anything"* — spending that on an act with no downside is how the press stops meaning anything on the
two controls where it does.

**4. Say nothing at sign-up, but show the player their own handle on the account screen afterwards.**
A real case, and the one that addresses the sharpest hazard in this whole area: it removes the
forgotten-handle problem without a single word of warning, and it is a fact rather than a caution, so
it fits *dark, quiet, minimal* better than any sentence would. **Rejected as a different question,
not as a bad idea.** It is a wire field — `ProfileResponse` carries no handle — and it reverses
`ADR-0031` §1's *"The handle appears in no response body and in no `ServerMessage`, ever"*, a merged
refusal with an anti-enumeration reason behind it. `DEC-157` asks whether a sentence is owed before
the act, and answering it with a feature after the act would be settling two decisions because they
looked related. §6 names it and deliberately does not register it.

**5. Escalate to the human, because this is the door to an account.** Its case: the roadmap puts
*Persistent profile* at v0.2, an account is the one thing in this product a player can be stuck with,
and a rule about what they are told when they acquire one reads like a commitment rather than an
application. **Rejected because the boundary does not run there.** `docs/workflow.md` gives *"what a
player sees, and what they are told"* to this role; no milestone moves, no money is involved, no new
kind of thing appears, and no risk lands outside the software. Decisively, **§1 adds nothing and
removes nothing** — the branch that would have needed the human is alternative 4's wire field and
reversal of a merged refusal, and that is refused here partly for exactly that reason. Same reading
`ADR-0143` §Alternatives 5 made on the neighbouring question a day ago.
