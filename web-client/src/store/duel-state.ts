import type {
  DuelOutcome,
  GameEvent,
  LegalActions,
  PlayerView,
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
  };
}

export function applyServerMessage(
  state: DuelState,
  message: ServerMessage,
): DuelState {
  switch (message.type) {
    case "RoomJoined":
      return { ...state, mySeat: message.seat, roomCode: message.code };
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
    default:
      return state;
  }
}
