import { useEffect, useState, type ReactElement } from "react";
import type { ProtocolError } from "../protocol";
import { useDuelState, useForgetRoom, useSend } from "../store/duel-provider";
import { useScreen } from "../routing/use-screen";
import { tokenFromHash } from "../routing/screen";
import { useProfileStrip } from "../profile/profile-provider";
import {
  useHistory,
  useLadder,
  useSignedIn,
  offerSettledHere,
  settleOfferHere,
} from "../main";
import { ProfileStrip } from "../profile/ProfileStrip";
import { NameSurface } from "../profile/NameSurface";
import { useSetName } from "../profile/set-name-provider";
import { ActionBar } from "../table/ActionBar";
import { CoinMark } from "../result/CoinMark";
import { DuelResult } from "../result/DuelResult";
import { RematchControl } from "../result/RematchControl";
import { AccountOffer } from "../result/AccountOffer";
import { offerAccount } from "../result/account-offer";
import { verdictOf } from "../result/outcome-text";
import { DuelTable } from "../table/DuelTable";
import { HistoryScreen } from "../history/HistoryScreen";
import { HISTORY_HEADING } from "../history/history-text";
import { LadderScreen } from "../ladder/LadderScreen";
import { LADDER_HEADING } from "../ladder/ladder-text";
import { AccountScreen } from "../account/AccountScreen";
import { ACCOUNT_HEADING, SIGN_IN_HEADING } from "../account/account-text";
import { useAccount, type AccountCalls } from "../account/account-provider";
import { SignInForm } from "../account/SignInForm";
import { ForgotPasswordForm } from "../account/ForgotPasswordForm";
import { FORGOT_PASSWORD_LABEL } from "../account/recovery-text";
import { VerifyScreen } from "../account/VerifyScreen";
import { ResetScreen } from "../account/ResetScreen";
import { normalizeRoomCode } from "./room-link";
import { PresenceNotice } from "../table/PresenceNotice";
import { absentActionText } from "../table/absent-action-text";
import { WaitingTable } from "../table/WaitingTable";

/** The first screen: open a duel room, or join one by the code on the invite. */
export function Lobby(): ReactElement {
  const state = useDuelState();
  const send = useSend();
  const forgetRoom = useForgetRoom();
  const profile = useProfileStrip();
  const setName = useSetName();
  const read = useHistory();
  const readLadder = useLadder();
  const signedIn = useSignedIn();
  const account = useAccount();
  // The initialiser, not a call: this runs once per mount, so a dismissal
  // (below) takes the offer off screen without waiting for a reload.
  const [offerSettled, setOfferSettled] = useState(offerSettledHere);
  const [typedCode, setTypedCode] = useState("");
  const { screen, open, leave, clearToken } = useScreen();
  // A lazy initialiser, so this runs once per mount rather than on every
  // render (ADR-0081 §5): the effect below replaces the fragment this reads
  // from, so a re-derivation after that replace would find nothing left to
  // read, and no type checker catches a second read. Shared by the verify
  // and reset branches below — the fragment has one second segment, so only
  // one of the two mailed screens can ever be showing, and one read serves
  // both (TASK-041719).
  const [mailedToken] = useState(() => tokenFromHash(window.location.hash));
  const code = normalizeRoomCode(typedCode);

  // ADR-0081 §5: the mailed secret leaves the address once the verify or
  // reset branch below is live, in an effect and not during render —
  // writing to history during render is a side effect in a render path, and
  // React may run it twice. Guarded on a token still being held, so a bare
  // "#/verify" or "#/reset" — already the correct address — never calls
  // replaceState for nothing.
  // Declared, and so run, before the effect below: ADR-0076 §3 makes the
  // store outrank the address, and a frame that seats this tab while a
  // mailed link is still open must have the last word on the two, not this
  // one. The call below is a fresh closure every render (`useScreen()`
  // memoises none of its returned functions), but its behaviour is already a
  // pure function of `screen`, which is a dependency here, so the array
  // deliberately omits it rather than re-running this effect on every render
  // for no semantic reason.
  useEffect(() => {
    if ((screen === "verify" || screen === "reset") && mailedToken !== null) {
      clearToken();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [screen, mailedToken]);

  // ADR-0076 §3: when the two disagree there is one authority, and it is the
  // store. A frame that seats a store-owned screen (outcome, view or
  // roomCode) overrules whatever the address named, so the fragment is
  // replaced back to "/" — never pushed, since the player did not navigate.
  // This runs in an effect, not during render: writing to history during
  // render is a side effect in a render path, and React may run it twice.
  // Guarded to screens other than "first" so a player already at "/" never
  // has replaceState called on every frame that arrives.
  const seatedByAFrame =
    state.outcome !== null || state.view !== null || state.roomCode !== null;
  useEffect(() => {
    if (seatedByAFrame && screen !== "first") {
      leave();
    }
  }, [seatedByAFrame, screen, leave]);

  // The duel is over. This comes first because the reducer clears nothing a
  // frame established: `view` and `roomCode` both outlive the duel, so a result
  // branch placed after either is a branch that never runs.
  if (state.outcome !== null) {
    return (
      <section className="p-6">
        <DuelResult
          outcome={state.outcome}
          mySeat={state.mySeat}
          rematch={
            <RematchControl
              offers={state.rematchOffers}
              mySeat={state.mySeat}
              refusal={state.refusal}
              onOffer={() => send({ type: "OfferRematch" })}
            />
          }
          offer={
            offerAccount({
              verdict: verdictOf(state.outcome, state.mySeat),
              signedIn,
              settled: offerSettled,
            }) ? (
              // The two handlers deliberately differ (`ADR-0086` §6). Taking the
              // offer settles it and stops there: the anchor still loads the
              // account screen, and that page load is what replaces this tree.
              // Dismissing settles it *and* hides it, because nothing else
              // will — there is no page load coming to do it instead.
              <AccountOffer
                onAccept={settleOfferHere}
                onDismiss={() => {
                  settleOfferHere();
                  setOfferSettled(true);
                }}
              />
            ) : undefined
          }
          onLeave={forgetRoom}
        />
      </section>
    );
  }

  // The first Snapshot is how the host learns the guest arrived: seating the
  // guest starts the duel, and there is no "opponent joined" frame to wait for.
  if (state.view !== null) {
    // ADR-0101 §7: every term the bar's two extra props need is already on
    // the wire and already in `view`/`pendingTurn` — no wire change, no
    // `PROTOCOL_VERSION` move, no new store field.
    const { view, pendingTurn } = state;
    const potIncludingStreet = view.seats.reduce(
      (sum, seat) => sum + seat.committedThisStreet,
      view.pot,
    );
    // 0 when there is no pending turn: the sizing row is not shown then, and
    // no amount reaches a frame either way.
    const committedThisStreet =
      pendingTurn === null
        ? 0
        : (view.seats.find(
            (seat) => seat.index === pendingTurn.legalActions.seat,
          )?.committedThisStreet ?? 0);

    return (
      // ADR-0103 §5: one column, not two — this is now the only element that
      // carries the container-query context, the `560px` cap and the height
      // budget; `DuelTable` renders no wrapper of its own, so its centre
      // block's `flex-1` claims slack against this column's full height,
      // action bar included, rather than against a nested column with
      // nothing to grow into.
      // `min-h-[100dvh]` is the floor `ADR-0103` §5 says is still owed: on
      // its own it does not shrink content past the fold (`flex-grow` only
      // distributes slack that already exists), but once the give order
      // below brings the column's own content under budget, this is what
      // keeps the laptop shape's fit a property instead of an accident.
      // `--wgap` is `ADR-0103` §3.1's first give — the column's outer
      // padding and the gaps between its blocks tighten before anything
      // else does, continuously with the column's own width and never with
      // a breakpoint. `ADR-0106` §4: the ramp steps down one token at the
      // phone width — measured, the floor is inert there, so it is the ramp
      // that moves and not the floor.
      <div className="[container-type:inline-size] mx-auto flex min-h-[100dvh] max-w-[560px] flex-col gap-[var(--wgap)] p-[var(--wgap)] [--wgap:clamp(4px,calc((100cqi-340px)/12.5),16px)]">
        <DuelTable
          view={view}
          rivalPresence={state.rivalPresence}
          narration={state.narration}
          revealStep={state.reveal?.steps[0] ?? null}
          lastAct={state.lastAct}
        />
        <PresenceNotice
          key={state.presenceCount}
          presence={state.rivalPresence}
          returned={state.rivalReturned}
        />
        {state.serverAction !== null && (
          <p className="min-h-[calc(var(--pd-fs-small)*var(--pd-lh-body))] text-center text-small text-text-muted">
            {absentActionText(state.serverAction, state.mySeat)}
          </p>
        )}
        <ActionBar
          turn={pendingTurn}
          potIncludingStreet={potIncludingStreet}
          committedThisStreet={committedThisStreet}
          rejection={state.rejection}
          refusal={state.refusal}
          rejectionCount={state.rejectionCount}
          send={send}
        />
      </div>
    );
  }

  if (state.roomCode !== null) {
    return <WaitingTable code={state.roomCode} onLeave={forgetRoom} />;
  }

  // A player is not in a duel (view is null and roomCode is null).
  // They can reach the history screen from here.
  if (screen === "duels" && read !== null) {
    return (
      <section className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 p-6">
        <HistoryScreen read={read} />
        <button type="button" onClick={leave}>
          Back
        </button>
      </section>
    );
  }

  // They can reach the ladder screen from here too. The way back is rendered
  // here, by the swap, and not by LadderScreen itself (ADR-0060): LadderScreen
  // knows nothing about navigation, so its own affordance is assertable with
  // no transport at all.
  if (screen === "leaderboard" && readLadder !== null) {
    return (
      <section className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 p-6">
        <LadderScreen read={readLadder} />
        <button type="button" onClick={leave}>
          Back
        </button>
      </section>
    );
  }

  // They can reach the account screen from here too, the same shape as the
  // record's and the ladder's: the way back is rendered here, by the swap,
  // and AccountScreen itself knows nothing about navigation (ADR-0060 §4).
  if (screen === "account") {
    return (
      <section className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 p-6">
        <AccountScreen
          profile={profile}
          signedIn={signedIn}
          signUp={account !== null ? account.signUp : undefined}
          signOut={account !== null ? account.signOut : undefined}
          attachRecoveryEmail={
            account !== null ? account.attachRecoveryEmail : undefined
          }
          onSignIn={() => open("sign-in")}
        />
        <button type="button" onClick={leave}>
          Back
        </button>
      </section>
    );
  }

  // The sign-in screen, reached only from the account screen's one door
  // (ADR-0060 §2's crowding argument keeps it off the first screen). The way
  // back is rendered here, by the swap, and SignInForm itself knows nothing
  // about navigation (ADR-0060 §4). ADR-0083 §4: the address is refused to
  // nobody, so this branch reads only the address — never signedIn, and
  // never main.tsx's own module-scope token read. `account` is null only
  // where no AccountProvider sits above this tree; the branch falls through
  // to the first screen in that case, the same fallback `duels` and
  // `leaderboard` already take when their own read is unavailable.
  if (screen === "sign-in" && account !== null) {
    return (
      <section className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 p-6">
        <h2 className="text-small">{SIGN_IN_HEADING}</h2>
        <SignInScreenBody
          signIn={account.signIn}
          forgotPassword={account.forgotPassword}
        />
        <button type="button" onClick={leave}>
          Back
        </button>
      </section>
    );
  }

  // The verification screen a mailed link opens onto, reached only by its
  // own fragment — never from a button anywhere in this file (`ADR-0081`
  // §3). The way back is rendered here, by the swap, and VerifyScreen itself
  // knows nothing about navigation (`ADR-0060` §4). `account` is null only
  // where no AccountProvider sits above this tree; the branch falls through
  // to the first screen in that case, the same fallback `sign-in` already
  // takes above.
  if (screen === "verify" && account !== null) {
    return (
      <section className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 p-6">
        <VerifyScreen token={mailedToken} verify={account.verifyEmail} />
        <button type="button" onClick={leave}>
          Back
        </button>
      </section>
    );
  }

  // The reset screen a mailed link opens onto, this branch's mirror and
  // reached the same way: only by its own fragment, never from a button
  // anywhere in this file (`ADR-0081` §3), and sharing the one token read
  // above rather than deriving a second. The way back is rendered here, by
  // the swap, and ResetScreen itself knows nothing about navigation
  // (`ADR-0060` §4). `account` is null only where no AccountProvider sits
  // above this tree; the branch falls through to the first screen in that
  // case, the same fallback the verify branch above already takes.
  // `onDone` sends the player on to sign-in with an in-page navigation, not
  // the reload `signIn` uses (`ADR-0083` §5's reload carries a new identity;
  // a reset carries none — no session was issued, the store is untouched,
  // and this browser's session was deleted server-side along with every
  // other one, not replaced here).
  if (screen === "reset" && account !== null) {
    return (
      <section className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 p-6">
        <ResetScreen
          token={mailedToken}
          reset={account.resetPassword}
          onDone={() => open("sign-in")}
        />
        <button type="button" onClick={leave}>
          Back
        </button>
      </section>
    );
  }

  return (
    <section className="p-6">
      {/* ADR-0098 §1: the coin-and-two-tone lockup, card-drawn only on the
          front door's pre-create branch. `aria-label` pins the accessible
          name to "Poker Duels" — the card's markup has no text node between
          the two spans, so a screen reader's own concatenation reads
          "PokerDuels" without it (ADR-0098's settlement, measured 2026-08-31). */}
      <h1
        aria-label="Poker Duels"
        className="inline-flex items-center gap-4 text-display font-bold"
      >
        <CoinMark />
        <span>Poker</span>
        <span className="font-medium text-text-muted">Duels</span>
      </h1>
      {state.refusal !== null && <p>{refusalMessage(state.refusal)}</p>}
      <button
        type="button"
        className="rounded-medium border border-transparent bg-accent-fill px-5 py-4 leading-tight font-medium text-on-accent"
        onClick={() => send({ type: "CreateRoom" })}
      >
        Create a duel room
      </button>
      <form
        onSubmit={(event) => {
          event.preventDefault();
          // An empty code would spend one of the ten failed joins ADR-0022
          // budgets this player every minute, and tell them nothing.
          if (code === "") return;
          send({ type: "JoinRoom", code });
        }}
      >
        <label htmlFor="room-code">Room code</label>
        <input
          id="room-code"
          className="rounded-medium border border-hairline bg-surface px-5 py-4 text-text"
          value={typedCode}
          onChange={(event) => setTypedCode(event.target.value)}
        />
        <button
          type="submit"
          className="rounded-medium border border-hairline px-5 py-4 leading-tight font-medium text-text"
        >
          Join the duel
        </button>
      </form>
      {profile !== null && <ProfileStrip state={profile} />}
      {profile !== null && profile.kind === "profile" && setName !== null && (
        <NameSurface profile={profile.profile} setName={setName} />
      )}
      {/* TASK-121303: the three doors were adjacent inline-level buttons with
          no text node between them (JSX elides it) and no layout on the
          bare section around them, so they abutted with no space at any
          zoom. A flex column blockifies each button and gives it a gap —
          separation only. DEC-094 (open) still owns whether a door should
          wear the client's control dress; nothing here answers it. */}
      <div className="flex flex-col gap-2">
        <button type="button" onClick={() => open("duels")}>
          {HISTORY_HEADING}
        </button>
        <button type="button" onClick={() => open("leaderboard")}>
          {LADDER_HEADING}
        </button>
        {/* ADR-0036: the door is offered whatever the profile read answered —
            nothing here gates on having an account, the same rule the record's
            and the ladder's doors already carry. */}
        <button type="button" onClick={() => open("account")}>
          {ACCOUNT_HEADING}
        </button>
      </div>
    </section>
  );
}

/**
 * The client shows the refusal and stops. Retrying on the player's behalf would
 * spend the ten failed joins a minute `ADR-0022` budgets them.
 */
function refusalMessage(error: ProtocolError): string {
  switch (error) {
    case "UNKNOWN_ROOM":
      return "No duel room has that code.";
    case "ROOM_FULL":
      return "That duel room already has a rival in it.";
    default:
      return "The server refused that.";
  }
}

/**
 * The sign-in screen's own mode: the sign-in form, or the recovery form in its place
 * (`ADR-0087`). Held here rather than in `Lobby` (`ADR-0087` §7) so that leaving the sign-in
 * screen — the only way back besides `CANCEL` once the form is open — drops the mode with no
 * handler and no effect: mounted fresh, this component always starts closed.
 */
function SignInScreenBody(props: {
  readonly signIn: AccountCalls["signIn"];
  readonly forgotPassword: AccountCalls["forgotPassword"];
}): ReactElement {
  const [askingForALink, setAskingForALink] = useState(false);
  if (askingForALink) {
    return (
      <ForgotPasswordForm
        forgotPassword={props.forgotPassword}
        onCancel={() => setAskingForALink(false)}
      />
    );
  }
  return (
    <>
      <SignInForm signIn={props.signIn} />
      <button
        type="button"
        className="cursor-pointer text-small text-accent"
        onClick={() => setAskingForALink(true)}
      >
        {FORGOT_PASSWORD_LABEL}
      </button>
    </>
  );
}
