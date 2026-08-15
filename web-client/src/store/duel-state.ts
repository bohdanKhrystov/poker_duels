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
      };
    case "Snapshot":
      return {
        ...state,
        view: message.view,
        pendingTurn: null,
        rejection: null,
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
      return {
        ...state,
        outcome: message.outcome,
        pendingTurn: null,
        rejection: null,
      };
    case "Failure":
      return { ...state, refusal: message.error };
    default:
      return state;
  }
}
