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
  rejection: Rejection | null;
  refusal: ProtocolError | null;
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
        // Keyed by the turn's identity, so a new decision point mounts a fresh
        // bar: the amount returns to the server's minimum and the sent lock
        // lifts, by construction rather than by an effect that clears them.
        <Live
          key={`${turn.handNumber}:${turn.actionSequence}`}
          turn={turn}
          send={props.send}
        />
      )}
      <Notice rejection={props.rejection} refusal={props.refusal} />
    </section>
  );
}

/**
 * Your turn. One button per action the server allowed, in the order it sent
 * them, and an amount control only when a bet or a raise is on offer.
 */
function Live(props: {
  turn: PendingTurn;
  send: (message: ClientMessage) => void;
}): ReactElement {
  const actions = props.turn.legalActions;
  const floor = amountFloor(actions);
  // 0 when no amount is on offer: it reaches no frame, because only Bet and
  // Raise carry a total, and neither is allowed here.
  const [to, setTo] = useState(floor ?? 0);
  const [sent, setSent] = useState(false);
  const filled = filledAction(actions.allowed);

  return (
    <>
      <div className="flex min-h-7 items-center gap-3">
        {floor !== null && (
          <>
            <input
              aria-label={
                actions.allowed.includes("RAISE") ? "raise to" : "bet to"
              }
              className="flex-1"
              max={actions.allInTo}
              min={floor}
              onChange={(event) => setTo(Number(event.target.value))}
              step={1}
              type="range"
              value={to}
              disabled={sent}
            />
            <span className="font-mono tabular-nums">{formatChips(to)}</span>
          </>
        )}
      </div>
      <div className="flex gap-3">
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
