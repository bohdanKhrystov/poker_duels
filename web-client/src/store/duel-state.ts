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
      };
    case "Snapshot":
      return { ...state, view: message.view, pendingTurn: null };
    case "Rejected":
      return { ...state, pendingTurn: null, rejection: message.rejection };
    case "Events":
      return { ...state, narration: [...state.narration, ...message.events] };
    case "DuelFinished":
      return { ...state, outcome: message.outcome, pendingTurn: null };
    case "Failure":
      return { ...state, refusal: message.error };
    default:
      return state;
  }
}
