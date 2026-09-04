import type {
  ActedForAbsent,
  DuelOutcome,
  GameEvent,
  LegalActions,
  PlayerAllIn,
  PlayerBet,
  PlayerCalled,
  PlayerChecked,
  PlayerFolded,
  PlayerRaised,
  PlayerView,
  ProtocolError,
  Rejection,
  SeatPresence,
  ServerMessage,
  Street,
  StreetDealt,
} from "../protocol";

/**
 * `ADR-0109` §1's six acts, and no other event: a fold, a check, a call, a bet, a raise or an
 * all-in. An `ActEvent` is exactly one of these; a blind post, a deal, a presence change and a
 * rejection are not acts.
 */
export type ActEvent =
  | PlayerFolded
  | PlayerChecked
  | PlayerCalled
  | PlayerBet
  | PlayerRaised
  | PlayerAllIn;

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
   *
   * Rarely holds both seats: `ADR-0044` §4 answers the second offer with the new
   * hand's `Snapshot` directly, never a restated `RematchOffered`, so no frame ever
   * states "both sides now want it." The card's *"it begins"* frame between
   * accepting and that `Snapshot` (`TASK-121102`) is therefore `RematchControl`'s
   * own local state, not a field here — only the accepting seat's own click can
   * know it, and only for as long as that component stays mounted waiting on the
   * `Snapshot` that ends it.
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
   * How many `OpponentPresence` frames the server has sent. Client bookkeeping in the class of
   * `rejectionCount`, and the same job: something that always changes. Two separate disconnects
   * in one duel can carry the same `presence`, so the value alone cannot tell a second window
   * from a re-render — only a count can.
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
  /**
   * The most recent action the server took for an absent seat, or `null` if it has taken
   * none — or if the absence that produced it has ended. The whole frame, kept rather than
   * picked apart: `(handNumber, actionSequence)` identifies the decision point uniquely, so a
   * screen that ever wants to attach the mark to an event can do it by coordinate rather than
   * by the order the frames arrived in (`ADR-0028` §4). `ADR-0046` §4 asks for the most recent
   * one and no log; `ADR-0075` fixes how long it lives.
   */
  readonly serverAction: ActedForAbsent | null;
  /**
   * The most recent of the hand's six act events (`ADR-0109` §1) — the whole event kept rather
   * than picked apart, in `serverAction`'s register, so a screen reads the seat and the `to` off
   * the server's own event rather than off something a client worked out — or `null` before the
   * hand on screen has seen one.
   *
   * A resume rebuilds nothing here: `PlayerView` carries no last-act field (`ADR-0109`
   * §Consequences), and repairing it is a `PROTOCOL_VERSION` bump this epic does not spend.
   */
  readonly lastAct: ActEvent | null;
  /**
   * The `StreetDealt` entries the most recent `Events` frame carried, not yet claimed by a
   * `Snapshot`. Scratch space for exactly one thing: `ADR-0102` §2 lays out a hand-completing
   * snapshot's steps from "the `Events` frame that immediately preceded" it, and every
   * `Snapshot` — hand-completing or not — claims and clears this, so a later `Snapshot` sent with
   * no `Events` in front of it (a resume, `ADR-0102` §5) never inherits an older hand's cards.
   */
  readonly pendingStreetDealt: readonly StreetDealt[];
  /**
   * The steps a hand's ending is being painted as, or `null` when nothing is being paced
   * (`ADR-0102` §§1-2). While this is non-null, every arriving frame is queued onto it — FIFO,
   * `reveal.queued` — rather than applied, and released once `reveal.steps` runs out.
   */
  readonly reveal: Reveal | null;
  /**
   * The turn clock the server most recently stated, anchored with this client's own monotonic
   * reading of the instant the frame *arrived* — never the instant it is painted, and never a
   * host wall clock that a correction could stretch or collapse (`ADR-0113` §6). A frame that
   * arrives while a `Reveal` stands is queued with that same reading (`Reveal.queued`), so a
   * clock released only once the runout finishes still anchors to when it showed up, not to
   * when the queue let it through. `null` before the server has sent one, and again whenever a
   * `RoomJoined` names a different room (`ADR-0104`).
   */
  readonly turnClock: TurnClockState | null;
}

export interface PendingTurn {
  readonly handNumber: number;
  readonly actionSequence: number;
  readonly legalActions: LegalActions;
}

/**
 * A hand's ending, mid-paint (`ADR-0102` §2). `steps` is never empty while a `Reveal` stands:
 * its last entry is always the whole snapshot, unlagged, so `ADR-0095` §4's banner gets its own
 * step even when this delivery dealt no street at all (a fold, or a hand run to the river).
 */
export interface Reveal {
  readonly steps: readonly RevealStep[];
  readonly queued: readonly QueuedMessage[];
}

/** One paint of a hand's ending: the board `ADR-0102` §2 says to show, and the street to name it. */
export interface RevealStep {
  readonly board: readonly string[];
  readonly street: Street;
}

/**
 * A frame `ADR-0102` §1 queued behind a standing `Reveal`, paired with the arrival reading
 * `applyServerMessage` took for it — so folding it back through the reducer once the reveal
 * drains (`advanceReveal`) anchors any `TurnClock` inside it to when it showed up, not to when
 * the queue let it through (`ADR-0113` §6).
 */
interface QueuedMessage {
  readonly message: ServerMessage;
  readonly arrivedAt: number;
}

/**
 * The turn clock the store is holding: two absolute instants on the same monotonic clock the
 * frame's arrival was read from (`ADR-0113` §6), derived once and never recomputed, so nothing
 * here needs a tick to stay correct.
 */
export interface TurnClockState {
  readonly seat: number;
  readonly handNumber: number;
  readonly actionSequence: number;
  /** The instant the turn's own allowance runs out. */
  readonly turnEndsAt: number;
  /** The instant the seat's bank would also be spent, were the turn allowance exhausted. */
  readonly expiresAt: number;
  /** Both seats' bank balances, in seat order, exactly as the frame stated them. */
  readonly bankRemainingMillis: readonly number[];
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
    presenceCount: 0,
    rivalReturned: false,
    serverAction: null,
    lastAct: null,
    pendingStreetDealt: [],
    reveal: null,
    turnClock: null,
  };
}

/**
 * One tick of a hand's ending (`ADR-0102` §4): drops the step now standing and, once none are
 * left, folds every frame `ADR-0102` §1 queued behind it back through this same reducer, in
 * arrival order — so a queued `Snapshot` that itself ends a hand lays out its own `Reveal`
 * exactly as a live one would, with nothing dropped and nothing reordered. The store calls this
 * once per step; a step of `0` calls it synchronously until `reveal` is `null` (`ADR-0102` §4).
 */
export function advanceReveal(state: DuelState): DuelState {
  if (state.reveal === null) return state;
  if (state.reveal.steps.length > 1) {
    return {
      ...state,
      reveal: { ...state.reveal, steps: state.reveal.steps.slice(1) },
    };
  }
  let next: DuelState = { ...state, reveal: null };
  for (const queued of state.reveal.queued) {
    next = applyServerMessage(next, queued.message, queued.arrivedAt);
  }
  return next;
}

export function applyServerMessage(
  state: DuelState,
  message: ServerMessage,
  // ADR-0113 §6: read here, as a default, so every call site that does not name its own
  // instant — every one of them but the socket seam in duel-store.ts and a queue's own
  // replay above — still anchors to when this call happened rather than to nothing at all.
  arrivedAt: number = performance.now(),
): DuelState {
  if (state.reveal !== null) {
    // ADR-0102 §1: every frame that arrives while a step stands is queued, in arrival order,
    // and applied only once the last step has stood — FIFO, OpponentPresence included, so a
    // screen mid-reveal never jumps ahead of the server or drops what it sent. The reading
    // taken above travels with it, so a TurnClock inside still anchors to this instant and not
    // to whenever the queue is drained (ADR-0113 §6).
    return {
      ...state,
      reveal: {
        ...state.reveal,
        queued: [...state.reveal.queued, { message, arrivedAt }],
      },
    };
  }
  switch (message.type) {
    case "RoomJoined": {
      // ADR-0104 §4: a RoomJoined naming a room other than the one the store already holds means
      // the connection has moved on the same socket (ADR-0104 §5's third window), so what the old
      // room left behind — presence, narration, the rest — is discarded before this room's own
      // fields are applied. The monotone counters carry over rather than reset: they exist to be
      // strictly-increasing change tokens, and resetting them could make two different states
      // compare equal. Naming the room the store already holds (a resume, ADR-0028 §5) clears
      // nothing beyond the refusal this case has always cleared.
      const base =
        state.roomCode !== null && state.roomCode !== message.code
          ? {
              ...initialState(),
              rejectionCount: state.rejectionCount,
              presenceCount: state.presenceCount,
            }
          : state;
      return {
        ...base,
        mySeat: message.seat,
        roomCode: message.code,
        refusal: null,
      };
    }
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
        // Claimed either way (ADR-0102 §2): a mid-hand Snapshot has no use for it, and a
        // hand-completing one has just spent it below.
        pendingStreetDealt: [],
        reveal:
          message.view.street === "COMPLETE"
            ? layOutReveal(message.view, state.pendingStreetDealt)
            : null,
      };
    case "Rejected":
      // A rejection reports on an attempt, not on state (ADR-0043): `pendingTurn` and `view` stay
      // untouched, and this never reads which `Rejection` variant arrived.
      return {
        ...state,
        rejection: message.rejection,
        rejectionCount: state.rejectionCount + 1,
      };
    case "Events": {
      // ADR-0109 §1 & §3: one ordered walk over the frame's own events — a HandStarted clears
      // the running value, an act sets it — so the frame's result is whichever came last in the
      // order the server sent them. Not events.at(-1), not a reverse scan: a frame with the act
      // last keeps it, a frame with the HandStarted last clears it, and this is the one pass that
      // gets both right.
      let lastAct = state.lastAct;
      for (const event of message.events) {
        if (event.type === "HandStarted") {
          lastAct = null;
        } else if (isAct(event)) {
          lastAct = event;
        }
      }
      return {
        ...state,
        narration: [...state.narration, ...message.events],
        pendingStreetDealt: message.events.filter(isStreetDealt),
        lastAct,
      };
    }
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
        // ADR-0075 §2: a boundary guard, not a statement about absence. At DuelFinished the mark
        // renders nowhere either way; this is what stops one surviving into a rematch, since a
        // Snapshot clears `outcome` and brings the table back (ADR-0044 §4).
        serverAction: null,
        // ADR-0109 §3: the duel's end retires the mark with the table, the same boundary guard
        // as serverAction just above — so nothing survives into a rematch, since a Snapshot
        // clears `outcome` and brings the table back (ADR-0044 §4).
        lastAct: null,
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
        presenceCount: state.presenceCount + 1,
        rivalReturned:
          message.presence === "PRESENT" &&
          (state.rivalPresence === "AWAY" || state.rivalPresence === "ABSENT"),
        // ADR-0075 §2: the mark lives as long as the absence that produced it. Cleared on the
        // frame, not on a transition — unlike `rivalReturned`, this needs no memory of what the
        // client held before, because it is about whether the server is still acting for that
        // seat and not about whether a return happened.
        serverAction:
          message.presence === "PRESENT" ? null : state.serverAction,
      };
    case "ActedForAbsent":
      return { ...state, serverAction: message };
    case "TurnClock": {
      // ADR-0113 §6: turnEndsAt and expiresAt are absolute instants derived once, right here,
      // from the reading taken on arrival — never recomputed later, so nothing downstream needs
      // its own clock to read them correctly.
      const turnEndsAt = arrivedAt + message.turnRemainingMillis;
      return {
        ...state,
        turnClock: {
          seat: message.seat,
          handNumber: message.handNumber,
          actionSequence: message.actionSequence,
          turnEndsAt,
          expiresAt: turnEndsAt + message.bankRemainingMillis[message.seat],
          bankRemainingMillis: message.bankRemainingMillis,
        },
      };
    }
    default:
      return state;
  }
}

function isStreetDealt(event: GameEvent): event is StreetDealt {
  return event.type === "StreetDealt";
}

/** `ADR-0109` §1's six-event membership test, `isStreetDealt`'s idiom applied to acts. */
function isAct(event: GameEvent): event is ActEvent {
  return (
    event.type === "PlayerFolded" ||
    event.type === "PlayerChecked" ||
    event.type === "PlayerCalled" ||
    event.type === "PlayerBet" ||
    event.type === "PlayerRaised" ||
    event.type === "PlayerAllIn"
  );
}

/**
 * `ADR-0102` §2's steps for a hand-completing snapshot: one per `StreetDealt` the immediately
 * preceding `Events` frame carried, in the order the server sent them, then one final step for
 * the whole snapshot. Every board is a prefix of `view.board.cards`, at a length that is the
 * snapshot's own length minus the cards the *later* steps carry — reached backward from a total
 * the server just sent, never built forward from an assumed-empty board, which is why no previous
 * view is needed and a hand that opens all-in walks the same steps with no special case.
 */
function layOutReveal(
  view: PlayerView,
  streetDealt: readonly StreetDealt[],
): Reveal {
  const boardLength = view.board.cards.length;
  const steps: RevealStep[] = new Array(streetDealt.length + 1);
  let cardsAfter = 0;
  for (let i = streetDealt.length - 1; i >= 0; i--) {
    const event = streetDealt[i];
    steps[i] = {
      board: view.board.cards.slice(0, boardLength - cardsAfter),
      street: event.street,
    };
    cardsAfter += event.cards.length;
  }
  steps[streetDealt.length] = { board: view.board.cards, street: view.street };
  return { steps, queued: [] };
}
