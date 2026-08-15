import type { ReactElement } from "react";
import type { ClientMessage } from "../protocol";
import type { PendingTurn } from "../store/duel-state";

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
  send: (message: ClientMessage) => void;
}): ReactElement {
  const { turn } = props;
  return (
    <section
      aria-label="your move"
      className="mx-auto flex w-full max-w-[460px] flex-col gap-3 rounded-medium border border-hairline bg-surface p-4"
    >
      {turn === null && <Waiting />}
    </section>
  );
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
