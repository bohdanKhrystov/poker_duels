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
import { readTypedAmount } from "./typed-amount";

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
        <>
          <Waiting />
          <Notice
            entryRefusal={null}
            rejection={props.rejection}
            refusal={props.refusal}
          />
        </>
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
          rejection={props.rejection}
          refusal={props.refusal}
          send={props.send}
        />
      )}
    </section>
  );
}

/**
 * Your turn. One button per action the server allowed, in the order it sent
 * them, and a sizing row of named presets plus a typed total only when a bet
 * or a raise is on offer (`ADR-0101`, `ADR-0111`).
 */
function Live(props: {
  turn: PendingTurn;
  potIncludingStreet: number;
  committedThisStreet: number;
  rejection: Rejection | null;
  refusal: ProtocolError | null;
  send: (message: ClientMessage) => void;
}): ReactElement {
  const actions = props.turn.legalActions;
  const floor = amountFloor(actions);
  // The only amount state, and it is a string: what the player has typed, or
  // the server's own minimum before they have touched anything. `reading` and
  // `dialled` below derive everything sendable from this one field — nothing
  // numeric is stored beside it (`ADR-0111` §§1, 4).
  const [entry, setEntry] = useState(floor === null ? "" : formatChips(floor));
  const [entryRefusal, setEntryRefusal] = useState<string | null>(null);
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

  // A reading, never a rule (`typed-amount.ts`): the same entry compared
  // against this turn's own bounds. `null` only when there is no amount on
  // offer at all, so nothing here is ever asked to read against a bound the
  // server did not send.
  const reading =
    floor === null ? null : readTypedAmount(entry, floor, actions.allInTo);
  const dialled =
    reading !== null && reading.kind === "amount" ? reading.to : null;

  return (
    <>
      <div
        aria-label="amount"
        className="flex min-h-7 items-center gap-3"
        role="group"
      >
        {chips.map((chip) => (
          <button
            className="shrink-0 rounded-medium border border-hairline px-3 py-2 font-mono text-small leading-tight text-text disabled:border-hairline disabled:text-text-faint"
            disabled={sent}
            key={chip.label}
            onClick={() => {
              setEntry(formatChips(chip.amount));
              setEntryRefusal(null);
            }}
            type="button"
          >
            {chip.label}
          </button>
        ))}
        {floor !== null && (
          <input
            aria-label="the total"
            className="min-w-0 ml-auto w-[7ch] rounded-medium border border-hairline bg-transparent px-3 py-2 text-right font-mono leading-tight text-text tabular-nums disabled:border-hairline disabled:text-text-faint"
            disabled={sent}
            inputMode="numeric"
            onChange={(event) => {
              setEntry(event.target.value);
              setEntryRefusal(null);
            }}
            type="text"
            value={entry}
          />
        )}
      </div>
      <div aria-label="actions" className="flex gap-3" role="group">
        {actions.allowed.map((type) => {
          const text = actionText(type, actions, dialled ?? 0);
          // The button prints the proposal a press would send, or nothing —
          // never a different amount (`ADR-0111` §7, `ADR-0100` §2). Only Bet
          // and Raise carry a typed total, so only they can be silenced by a
          // refused one.
          const printed =
            (type === "BET" || type === "RAISE") && dialled === null
              ? null
              : text.amount;
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
                if ((type === "BET" || type === "RAISE") && dialled === null) {
                  // No frame, no sent-lock, no rewrite (`ADR-0111` §1): the
                  // entry the player typed stands exactly as it is, and the
                  // refusal is only ever said, never enforced.
                  if (reading !== null && reading.kind === "refused") {
                    setEntryRefusal(reading.sentence);
                  }
                  return;
                }
                setSent(true);
                props.send(actFrame(props.turn, type, dialled ?? 0));
              }}
              type="button"
            >
              {text.verb}
              {printed !== null && (
                <>
                  {" "}
                  <span className="font-mono tabular-nums">
                    {formatChips(printed)}
                  </span>
                </>
              )}
            </button>
          );
        })}
      </div>
      <Notice
        entryRefusal={entryRefusal}
        rejection={props.rejection}
        refusal={props.refusal}
      />
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
 * would accept — `floor` through `allInTo` — never rewritten into range and
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
 * The line the server's last word about this seat's action goes on — or, when
 * the player's own typed entry was refused before it was ever sent, the local
 * word about that instead. It is reserved whether or not there is anything to
 * say, so saying something moves nothing.
 */
function Notice(props: {
  entryRefusal: string | null;
  rejection: Rejection | null;
  refusal: ProtocolError | null;
}): ReactElement {
  return (
    <p className="min-h-[calc(var(--pd-fs-small)*var(--pd-lh-body))] text-center text-small text-text-muted">
      {noticeText(props.entryRefusal, props.rejection, props.refusal)}
    </p>
  );
}

/**
 * A refusal of what the player just typed is said before anything is ever
 * sent, so it wins first. Failing that, a rejection is the server's answer to
 * the action itself, so it wins next.
 */
function noticeText(
  entryRefusal: string | null,
  rejection: Rejection | null,
  refusal: ProtocolError | null,
): string {
  if (entryRefusal !== null) return entryRefusal;
  if (rejection !== null) return rejectionText(rejection);
  if (refusal !== null) return refusalText();
  return "";
}

/**
 * A refused frame, said plainly and once. No `ProtocolError` gets its own copy here — the one
 * that used to, the paused-duel refusal, no longer exists (`TASK-130805`); a future error that
 * wants its own line reopens this function rather than reusing an unused parameter for it.
 */
function refusalText(): string {
  return "The server did not apply that action.";
}
