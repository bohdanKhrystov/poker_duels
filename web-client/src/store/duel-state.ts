import type {
  DuelOutcome,
  GameEvent,
  LegalActions,
  PlayerView,
  ProtocolError,
  Rejection,
  SeatPresence,
  ServerMessage,
} from "../protocol";

export interface DuelState {
  readonly mySeat: number | null;
  readonly roomCode: string | null;
  readonly view: PlayerView | null;
  readonly pendingTurn: PendingTurn | null;
  readonly narration: readonly GameEvent[];
  readonly rejection: Rejection | null;
  /**
   * How many actions the server has refused since the duel began. Client bookkeeping, not a game
   * fact: the store is the only layer that sees frames as events rather than state, so it alone
   * can turn "a rejection happened" into a value a component can key off. It never resets, and
   * the reducer never reads it back.
   */
  readonly rejectionCount: number;
  readonly outcome: DuelOutcome | null;
  readonly refusal: ProtocolError | null;
  /**
   * The seats whose rematch offers stand, in the order the server stated them.
   * Client bookkeeping the store accumulates across frames, in the same class as
   * `rejectionCount` — no single frame carries it (`ADR-0044`, Consequences).
   */
  readonly rematchOffers: readonly number[];
  /**
   * The rival's presence, as the last `OpponentPresence` stated it, or `null` before the
   * server has stated one. Presence is state, not an event (`ADR-0028` §2): nothing but
   * another `OpponentPresence` moves it, and a `Snapshot` in particular does not — the duel
   * goes on being played while a seat is absent.
   */
  readonly rivalPresence: SeatPresence | null;
  /**
   * How much of the grace window was left at the instant the server built the frame, or
   * `null` whenever the presence is not `AWAY`. A duration, never a deadline: the two sides
   * share no epoch (`ADR-0028` §2). The reducer never reads it back and never counts it down.
   */
  readonly graceRemainingMillis: number | null;
  /**
   * How many `OpponentPresence` frames the server has sent. Client bookkeeping in the class of
   * `rejectionCount`, and the same job: something that always changes. Two grace windows in one
   * duel carry the same `graceRemainingMillis`, so the value cannot tell a second window from
   * a re-render — only a count can.
   */
  readonly presenceCount: number;
  /**
   * Whether the rival came back from an absence **this client saw**. Client bookkeeping the
   * store accumulates across frames, in the class of `rejectionCount`: no frame carries it.
   *
   * `ADR-0046` §2: a resuming client is always sent its rival's current presence, `PRESENT`
   * included, so the frame alone cannot tell a return from a status quo. Telling a player who
   * reloaded the page that their rival returned from an absence that never happened is the one
   * way this copy can state a falsehood.
   */
  readonly rivalReturned: boolean;
}

export interface PendingTurn {
  readonly handNumber: number;
  readonly actionSequence: number;
  readonly legalActions: LegalActions;
}

export function initialState(): DuelState {
  return {
    mySeat: null,
    roomCode: null,
    view: null,
    pendingTurn: null,
    narration: [],
    rejection: null,
    rejectionCount: 0,
    outcome: null,
    refusal: null,
    rematchOffers: [],
    rivalPresence: null,
    graceRemainingMillis: null,
    presenceCount: 0,
    rivalReturned: false,
  };
}

export function applyServerMessage(
  state: DuelState,
  message: ServerMessage,
): DuelState {
  switch (message.type) {
    case "RoomJoined":
      return {
        ...state,
        mySeat: message.seat,
        roomCode: message.code,
        refusal: null,
      };
    case "YourTurn":
      return {
        ...state,
        pendingTurn: {
          handNumber: message.handNumber,
          actionSequence: message.actionSequence,
          legalActions: message.legalActions,
        },
        rejection: null,
        refusal: null,
      };
    case "Snapshot":
      // ADR-0044 §4: there is no started frame, and after a `DuelFinished` a `Snapshot` can only
      // mean a new duel has begun in the same room, because `resumeFrames` gives a finished duel
      // `finishedFrames` alone. Clearing the result and the offers that produced it is what
      // unblocks the table's return: `Lobby.tsx` tests `state.outcome` before `state.view`.
      return {
        ...state,
        view: message.view,
        pendingTurn: null,
        rejection: null,
        refusal: null,
        outcome: null,
        rematchOffers: [],
        // ADR-0046 §2: `Your rival is back.` clears on the next Snapshot and on nothing else —
        // never on a timer, never on a fade. The presence itself is not cleared here: hands go on
        // being dealt while a seat is ABSENT, and a Snapshot that wiped it would put the table
        // back to normal under a rival who is not there.
        rivalReturned: false,
      };
    case "Rejected":
      // A rejection reports on an attempt, not on state (ADR-0043): `pendingTurn` and `view` stay
      // untouched, and this never reads which `Rejection` variant arrived.
      return {
        ...state,
        rejection: message.rejection,
        rejectionCount: state.rejectionCount + 1,
      };
    case "Events":
      return { ...state, narration: [...state.narration, ...message.events] };
    case "DuelFinished":
      // ADR-0044 §5: the server restates a standing offer after a returning socket's
      // DuelFinished and never before, precisely because this frame is where the client
      // enters its result screen. Clearing here is the client half of that commitment —
      // without it, the ordering the server took on buys nothing.
      return {
        ...state,
        outcome: message.outcome,
        pendingTurn: null,
        rejection: null,
        refusal: null,
        rematchOffers: [],
      };
    case "Failure":
      // ADR-0044 §6 documents REMATCH_UNAVAILABLE as transient: nothing was recorded and the
      // same offer may be sent again, so there is no state to enter and no screen to change.
      if (message.error === "REMATCH_UNAVAILABLE") return state;
      return { ...state, refusal: message.error };
    case "RematchOffered":
      // ADR-0044 §3: a repeat offer is answered with the same frame, not an error. Returning
      // the state unchanged is what keeps the store from notifying anybody about nothing.
      if (state.rematchOffers.includes(message.seat)) return state;
      return {
        ...state,
        rematchOffers: [...state.rematchOffers, message.seat],
      };
    case "OpponentPresence":
      return {
        ...state,
        rivalPresence: message.presence,
        graceRemainingMillis: message.graceRemainingMillis,
        presenceCount: state.presenceCount + 1,
        rivalReturned:
          message.presence === "PRESENT" &&
          (state.rivalPresence === "AWAY" || state.rivalPresence === "ABSENT"),
      };
    default:
      return state;
  }
}
