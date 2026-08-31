import { useState, type ReactElement } from "react";
import type {
  ActionType,
  ClientMessage,
  LegalActions,
  ProtocolError,
  Rejection,
} from "../protocol";
import type { PendingTurn } from "../store/duel-state";
import { actionText } from "./action-text";
import { formatChips } from "./chips";
import { actFrame } from "./act-frame";
import { rejectionText } from "./rejection-text";

/**
 * The action bar: the one place a player asserts anything.
 *
 * It offers exactly the actions the server named in `YourTurn` and no others —
 * it hides none it thinks bad, adds none it thinks legal, and works out no
 * amount the server did not send. Nothing it does is optimistic: a click sends
 * one `Act` and the bar goes quiet until the server's next frame moves the
 * store on.
 */
export function ActionBar(props: {
  turn: PendingTurn | null;
  /**
   * `view.pot` plus every seat's `committedThisStreet` (`ADR-0100` §6,
   * `ADR-0101` §1) — the pot the sizing row's presets are fractions of. A
   * function of what `Lobby.tsx` hands the bar, never of the store itself.
   */
  potIncludingStreet: number;
  /**
   * The acting seat's own street commitment, or `0` when there is no
   * pending turn (`ADR-0101` §1, §7). Kept apart from `potIncludingStreet`
   * because a re-raise is the one frame that needs it: the call still
   * *costs* `callTo` minus this, not `callTo` itself.
   */
  committedThisStreet: number;
  rejection: Rejection | null;
  refusal: ProtocolError | null;
  /**
   * Client bookkeeping the store keeps, not a game fact: the count of
   * refusals, whose only job is to change.
   */
  rejectionCount?: number;
  send: (message: ClientMessage) => void;
}): ReactElement {
  const { turn } = props;
  return (
    <section
      aria-label="your move"
      className="mx-auto flex w-full max-w-[460px] flex-col gap-3 rounded-medium border border-hairline bg-surface p-4"
    >
      {turn === null ? (
        <Waiting />
      ) : (
        // Keyed by the turn's identity and the refusal count, so a new decision
        // point *or* a rejected attempt at the same one mounts a fresh bar: the
        // amount returns to the server's minimum and the sent lock lifts, by
        // construction rather than by an effect that clears them.
        <Live
          key={`${turn.handNumber}:${turn.actionSequence}:${props.rejectionCount ?? 0}`}
          turn={turn}
          potIncludingStreet={props.potIncludingStreet}
          committedThisStreet={props.committedThisStreet}
          send={props.send}
        />
      )}
      <Notice rejection={props.rejection} refusal={props.refusal} />
    </section>
  );
}

/**
 * Your turn. One button per action the server allowed, in the order it sent
 * them, and a sizing row of named presets only when a bet or a raise is on
 * offer (`ADR-0101`).
 */
function Live(props: {
  turn: PendingTurn;
  potIncludingStreet: number;
  committedThisStreet: number;
  send: (message: ClientMessage) => void;
}): ReactElement {
  const actions = props.turn.legalActions;
  const floor = amountFloor(actions);
  // 0 when no amount is on offer: it reaches no frame, because only Bet and
  // Raise carry a total, and neither is allowed here.
  const [to, setTo] = useState(floor ?? 0);
  const [sent, setSent] = useState(false);
  const filled = filledAction(actions.allowed);
  const chips =
    floor === null
      ? []
      : sizingChips(
          actions,
          floor,
          props.potIncludingStreet,
          props.committedThisStreet,
        );

  return (
    <>
      <div
        aria-label="amount"
        className="flex min-h-7 items-center gap-3"
        role="group"
      >
        {chips.map((chip) => (
          <button
            className="rounded-medium border border-hairline px-3 py-2 font-mono text-small leading-tight text-text disabled:border-hairline disabled:text-text-faint"
            disabled={sent}
            key={chip.label}
            onClick={() => setTo(chip.amount)}
            type="button"
          >
            {chip.label}
          </button>
        ))}
      </div>
      <div aria-label="actions" className="flex gap-3" role="group">
        {actions.allowed.map((type) => {
          const text = actionText(type, actions, to);
          return (
            <button
              className={`flex-1 rounded-medium border px-3 py-4 leading-tight font-medium ${
                type === filled
                  ? "border-transparent bg-accent-fill text-on-accent"
                  : "border-hairline text-text"
              } disabled:border-hairline disabled:bg-transparent disabled:text-text-faint`}
              disabled={sent}
              key={type}
              onClick={() => {
                setSent(true);
                props.send(actFrame(props.turn, type, to));
              }}
              type="button"
            >
              {text.verb}
              {text.amount !== null && (
                <>
                  {" "}
                  <span className="font-mono tabular-nums">
                    {formatChips(text.amount)}
                  </span>
                </>
              )}
            </button>
          );
        })}
      </div>
    </>
  );
}

/**
 * The total the amount control starts at: the server's own minimum for whichever
 * of a bet or a raise it allowed, or `null` when it allowed neither.
 */
function amountFloor(actions: LegalActions): number | null {
  if (actions.allowed.includes("RAISE")) return actions.minRaiseTo;
  if (actions.allowed.includes("BET")) return actions.minBetTo;
  return null;
}

/** One named preset on the sizing row, and the street total pressing it sets. */
interface SizingChip {
  readonly label: string;
  readonly amount: number;
}

/**
 * The card's five sizing presets (`design/screens/duel-table.html`), each
 * computing the street total its own label names (`ADR-0101` §§1–2):
 *
 * ```
 * toCall = actions.callTo − committedThisStreet
 * base   = potIncludingStreet + toCall
 * ```
 *
 * A preset is offered only when the amount it computes is one the server
 * would accept — `floor` through `allInTo` — never clamped into range and
 * never rendered dead (`ADR-0101` §3). `min` and `all-in` always satisfy
 * that by construction, because the engine caps both into range itself
 * (`minRaiseTo`/`minBetTo ≤ allInTo`); one rule offers all five.
 */
function sizingChips(
  actions: LegalActions,
  floor: number,
  potIncludingStreet: number,
  committedThisStreet: number,
): readonly SizingChip[] {
  const toCall = actions.callTo - committedThisStreet;
  const base = potIncludingStreet + toCall;
  const presets: readonly SizingChip[] = [
    { label: "min", amount: floor },
    { label: "⅓", amount: actions.callTo + Math.floor(base / 3) },
    { label: "½", amount: actions.callTo + Math.floor(base / 2) },
    { label: "pot", amount: actions.callTo + base },
    { label: "all-in", amount: actions.allInTo },
  ];
  return presets.filter(
    (chip) => chip.amount >= floor && chip.amount <= actions.allInTo,
  );
}

/**
 * The one filled button — the design's aggressive line. The last button the
 * server named carries it when neither a bet nor a raise is on offer, which is
 * the only case where the aggressive line is the all-in.
 */
function filledAction(allowed: readonly ActionType[]): ActionType | undefined {
  if (allowed.includes("RAISE")) return "RAISE";
  if (allowed.includes("BET")) return "BET";
  return allowed[allowed.length - 1];
}

/**
 * Not your turn: the sizing row's height is reserved and the actions row says
 * who is being waited on, so the bar is the same height in every state and
 * nothing below it moves when a turn opens.
 */
function Waiting(): ReactElement {
  return (
    <>
      <div className="min-h-7" />
      <p className="py-4 text-center leading-tight text-text-muted">
        {"Waiting for your rival…"}
      </p>
    </>
  );
}

/**
 * The line the server's last word about this seat's action goes on. It is
 * reserved whether or not there is anything to say, so saying something moves
 * nothing.
 */
function Notice(props: {
  rejection: Rejection | null;
  refusal: ProtocolError | null;
}): ReactElement {
  return (
    <p className="min-h-[calc(var(--pd-fs-small)*var(--pd-lh-body))] text-center text-small text-text-muted">
      {noticeText(props.rejection, props.refusal)}
    </p>
  );
}

/** A rejection is the server's answer to the action itself, so it wins. */
function noticeText(
  rejection: Rejection | null,
  refusal: ProtocolError | null,
): string {
  if (rejection !== null) return rejectionText(rejection);
  if (refusal !== null) return refusalText(refusal);
  return "";
}

/**
 * A refused frame, said plainly and once. `DUEL_PAUSED` means the action was
 * not applied, so the client says so and sends nothing again — retrying is how
 * a client turns one refusal into two.
 */
function refusalText(error: ProtocolError): string {
  return error === "DUEL_PAUSED"
    ? "The duel is paused. That action was not applied."
    : "The server did not apply that action.";
}
