import { render, cleanup } from "@testing-library/react";
import { StrictMode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { VerifyScreen } from "./VerifyScreen";
import type { VerifyEmailOutcome } from "./verify-email";
import {
  VERIFY_HEADING,
  VERIFY_DONE,
  VERIFY_LINK_DEAD,
  VERIFY_ADDRESS_TAKEN,
  VERIFY_NO_LINK,
} from "./recovery-text";

const TOKEN = "zqx-verify-token-zqx";

afterEach(cleanup);

/** A `verify` double that answers every call, however many, with the same fixed outcome. */
function resolvedVerify(
  outcome: VerifyEmailOutcome,
): (token: string) => Promise<VerifyEmailOutcome> {
  return vi.fn(async (): Promise<VerifyEmailOutcome> => outcome);
}

describe("VerifyScreen", () => {
  it("sends the token it was handed, once, at mount", async () => {
    const spy = resolvedVerify({ kind: "verified" });

    const { findByText } = render(<VerifyScreen token={TOKEN} verify={spy} />);

    expect(spy).toHaveBeenCalledTimes(1);
    expect(spy).toHaveBeenCalledWith(TOKEN);
    expect(await findByText(VERIFY_DONE)).toBeDefined();

    cleanup();

    // main.tsx renders under React.StrictMode, which mounts a component's effects
    // twice in development. A second call would spend a token that is single-use
    // by construction, so the guard has to survive the double mount, not just a
    // single one.
    const strictSpy = resolvedVerify({ kind: "verified" });

    const strict = render(
      <StrictMode>
        <VerifyScreen token={TOKEN} verify={strictSpy} />
      </StrictMode>,
    );

    expect(await strict.findByText(VERIFY_DONE)).toBeDefined();
    expect(strictSpy).toHaveBeenCalledTimes(1);
  });

  it("renders one sentence per answer, and each is its own", async () => {
    // verified
    {
      const { findByText, queryByText, unmount } = render(
        <VerifyScreen
          token={TOKEN}
          verify={resolvedVerify({ kind: "verified" })}
        />,
      );
      expect(await findByText(VERIFY_DONE)).toBeDefined();
      expect(queryByText(VERIFY_LINK_DEAD)).toBeNull();
      expect(queryByText(VERIFY_ADDRESS_TAKEN)).toBeNull();
      unmount();
    }

    // link-dead
    {
      const { findByText, queryByText, unmount } = render(
        <VerifyScreen
          token={TOKEN}
          verify={resolvedVerify({ kind: "link-dead" })}
        />,
      );
      expect(await findByText(VERIFY_LINK_DEAD)).toBeDefined();
      expect(queryByText(VERIFY_DONE)).toBeNull();
      expect(queryByText(VERIFY_ADDRESS_TAKEN)).toBeNull();
      unmount();
    }

    // address-taken
    {
      const { findByText, queryByText, unmount } = render(
        <VerifyScreen
          token={TOKEN}
          verify={resolvedVerify({ kind: "address-taken" })}
        />,
      );
      expect(await findByText(VERIFY_ADDRESS_TAKEN)).toBeDefined();
      expect(queryByText(VERIFY_DONE)).toBeNull();
      expect(queryByText(VERIFY_LINK_DEAD)).toBeNull();
      unmount();
    }

    // failed — shares its sentence with link-dead. Asserted here, not assumed:
    // the same VERIFY_LINK_DEAD constant that link-dead showed above is what
    // this outcome shows too.
    {
      const { findByText, queryByText, unmount } = render(
        <VerifyScreen
          token={TOKEN}
          verify={resolvedVerify({ kind: "failed" })}
        />,
      );
      expect(await findByText(VERIFY_LINK_DEAD)).toBeDefined();
      expect(queryByText(VERIFY_DONE)).toBeNull();
      expect(queryByText(VERIFY_ADDRESS_TAKEN)).toBeNull();
      unmount();
    }
  });

  it("sends nothing at all with no token, and says so without calling it an error", async () => {
    const spy = resolvedVerify({ kind: "verified" });

    const { findByText, queryByText } = render(
      <VerifyScreen token={null} verify={spy} />,
    );

    // The count is the assertion: a screen that called with "" and got a 400
    // back would still land on VERIFY_LINK_DEAD and a presence-only check
    // would not notice the call it should never have made.
    expect(spy).toHaveBeenCalledTimes(0);
    expect(await findByText(VERIFY_NO_LINK)).toBeDefined();
    expect(queryByText(VERIFY_LINK_DEAD)).toBeNull();
  });

  it("shows the heading before any answer, and shows no outcome while it waits", () => {
    const neverSettles = vi.fn(() => new Promise<VerifyEmailOutcome>(() => {}));

    const { getByText, queryByText } = render(
      <VerifyScreen token={TOKEN} verify={neverSettles} />,
    );

    expect(getByText(VERIFY_HEADING)).toBeDefined();
    expect(queryByText(VERIFY_DONE)).toBeNull();
    expect(queryByText(VERIFY_LINK_DEAD)).toBeNull();
    expect(queryByText(VERIFY_ADDRESS_TAKEN)).toBeNull();
  });

  it("puts the token on no part of the screen", async () => {
    const { container, findByText } = render(
      <VerifyScreen
        token={TOKEN}
        verify={resolvedVerify({ kind: "verified" })}
      />,
    );

    // The presence half runs first.
    expect(await findByText(VERIFY_DONE)).toBeDefined();

    expect(container.textContent).not.toContain(TOKEN);
    // innerHTML catches an attribute (title, aria-label, a hidden input's
    // value) that textContent cannot see.
    expect(container.innerHTML).not.toContain(TOKEN);
  });

  it("asks again for nothing when the answer changes", async () => {
    const spy = resolvedVerify({ kind: "link-dead" });

    const { findByText, rerender } = render(
      <VerifyScreen token={TOKEN} verify={spy} />,
    );
    expect(await findByText(VERIFY_LINK_DEAD)).toBeDefined();
    expect(spy).toHaveBeenCalledTimes(1);

    rerender(<VerifyScreen token={TOKEN} verify={spy} />);

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it("never renders two sentences at once", async () => {
    const outcomes: readonly VerifyEmailOutcome[] = [
      { kind: "verified" },
      { kind: "link-dead" },
      { kind: "address-taken" },
      { kind: "failed" },
    ];
    const sentenceFor: Record<VerifyEmailOutcome["kind"], string> = {
      verified: VERIFY_DONE,
      "link-dead": VERIFY_LINK_DEAD,
      "address-taken": VERIFY_ADDRESS_TAKEN,
      failed: VERIFY_LINK_DEAD,
    };
    const outcomeSentences = [
      VERIFY_DONE,
      VERIFY_LINK_DEAD,
      VERIFY_ADDRESS_TAKEN,
      VERIFY_NO_LINK,
    ];

    for (const outcome of outcomes) {
      const { container, getByText, findByText, unmount } = render(
        <VerifyScreen token={TOKEN} verify={resolvedVerify(outcome)} />,
      );

      await findByText(sentenceFor[outcome.kind]);
      expect(getByText(VERIFY_HEADING)).toBeDefined();

      const presentCount = outcomeSentences.filter((sentence) =>
        (container.textContent ?? "").includes(sentence),
      ).length;
      expect(presentCount).toBe(1);

      unmount();
    }
  });
});
