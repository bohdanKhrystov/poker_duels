import { useState, type ReactElement } from "react";
import type { ProtocolError } from "../protocol";
import { useDuelState, useForgetRoom, useSend } from "../store/duel-provider";
import { useScreen } from "../routing/use-screen";
import { useProfileStrip } from "../profile/profile-provider";
import { useHistory, useLadder } from "../main";
import { ProfileStrip } from "../profile/ProfileStrip";
import { NameSurface } from "../profile/NameSurface";
import { useSetName } from "../profile/set-name-provider";
import { ActionBar } from "../table/ActionBar";
import { DuelResult } from "../result/DuelResult";
import { RematchControl } from "../result/RematchControl";
import { DuelTable } from "../table/DuelTable";
import { HistoryScreen } from "../history/HistoryScreen";
import { HISTORY_HEADING } from "../history/history-text";
import { LadderScreen } from "../ladder/LadderScreen";
import { LADDER_HEADING } from "../ladder/ladder-text";
import { normalizeRoomCode, roomLink } from "./room-link";
import { PresenceNotice } from "../table/PresenceNotice";
import { absentActionText } from "../table/absent-action-text";

/** The first screen: open a duel room, or join one by the code on the invite. */
export function Lobby(): ReactElement {
  const state = useDuelState();
  const send = useSend();
  const forgetRoom = useForgetRoom();
  const profile = useProfileStrip();
  const setName = useSetName();
  const read = useHistory();
  const readLadder = useLadder();
  const [typedCode, setTypedCode] = useState("");
  const { screen, open, leave } = useScreen();
  const code = normalizeRoomCode(typedCode);

  // The duel is over. This comes first because the reducer clears nothing a
  // frame established: `view` and `roomCode` both outlive the duel, so a result
  // branch placed after either is a branch that never runs.
  if (state.outcome !== null) {
    return (
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
        onLeave={forgetRoom}
      />
    );
  }

  // The first Snapshot is how the host learns the guest arrived: seating the
  // guest starts the duel, and there is no "opponent joined" frame to wait for.
  if (state.view !== null) {
    return (
      <div className="mx-auto flex max-w-[560px] flex-col gap-5">
        <DuelTable view={state.view} rivalPresence={state.rivalPresence} />
        <PresenceNotice
          key={state.presenceCount}
          presence={state.rivalPresence}
          returned={state.rivalReturned}
          graceRemainingMillis={state.graceRemainingMillis}
        />
        {state.serverAction !== null && (
          <p className="min-h-[calc(var(--pd-fs-small)*var(--pd-lh-body))] text-center text-small text-text-muted">
            {absentActionText(state.serverAction, state.mySeat)}
          </p>
        )}
        <ActionBar
          turn={state.pendingTurn}
          rejection={state.rejection}
          refusal={state.refusal}
          rejectionCount={state.rejectionCount}
          send={send}
        />
      </div>
    );
  }

  if (state.roomCode !== null) {
    return <WaitingForRival code={state.roomCode} onLeave={forgetRoom} />;
  }

  // A player is not in a duel (view is null and roomCode is null).
  // They can reach the history screen from here.
  if (screen === "duels" && read !== null) {
    return (
      <section className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4">
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
      <section className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4">
        <LadderScreen read={readLadder} />
        <button type="button" onClick={leave}>
          Back
        </button>
      </section>
    );
  }

  return (
    <section>
      {state.refusal !== null && <p>{refusalMessage(state.refusal)}</p>}
      <button type="button" onClick={() => send({ type: "CreateRoom" })}>
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
          value={typedCode}
          onChange={(event) => setTypedCode(event.target.value)}
        />
        <button type="submit">Join the duel</button>
      </form>
      {profile !== null && <ProfileStrip state={profile} />}
      {profile !== null && profile.kind === "profile" && setName !== null && (
        <NameSurface profile={profile.profile} setName={setName} />
      )}
      <button type="button" onClick={() => open("duels")}>
        {HISTORY_HEADING}
      </button>
      <button type="button" onClick={() => open("leaderboard")}>
        {LADDER_HEADING}
      </button>
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
 * The invite is selectable text before it is anything else: the one interaction
 * this product depends on cannot need a working clipboard.
 */
function WaitingForRival(props: {
  code: string;
  onLeave: () => void;
}): ReactElement {
  const link = roomLink(window.location.origin, props.code);
  return (
    <section>
      <h2>Waiting for your rival</h2>
      <p>{props.code}</p>
      <label htmlFor="invite-link">Invite link</label>
      <input
        autoFocus
        id="invite-link"
        readOnly
        value={link}
        onFocus={(event) => event.currentTarget.select()}
      />
      <CopyLink link={link} />
      <a
        className="rounded-medium border border-hairline px-5 py-4 leading-tight font-medium text-text"
        href="/"
        onClick={props.onLeave}
      >
        Back to the lobby
      </a>
      {/* prettier-ignore */}
      <p className="text-small text-text-muted">The room stays open. That link still works for your rival, and it brings you back.</p>
    </section>
  );
}

/** Absent where the clipboard API is: the box above is always the fallback. */
function CopyLink(props: { link: string }): ReactElement | null {
  const [outcome, setOutcome] = useState<"none" | "copied" | "refused">("none");
  if (!navigator.clipboard) {
    return null;
  }
  return (
    <>
      <button
        type="button"
        onClick={() => {
          void navigator.clipboard.writeText(props.link).then(
            () => setOutcome("copied"),
            () => setOutcome("refused"),
          );
        }}
      >
        Copy the link
      </button>
      {outcome === "copied" && <p>Link copied.</p>}
      {outcome === "refused" && <p>Copy it from the box above.</p>}
    </>
  );
}
