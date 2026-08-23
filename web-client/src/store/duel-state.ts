import type {
  DuelOutcome,
  GameEvent,
  LegalActions,
  PlayerView,
  ProtocolError,
  Rejection,
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
    default:
      return state;
  }
}
