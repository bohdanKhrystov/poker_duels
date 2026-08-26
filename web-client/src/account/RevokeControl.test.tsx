import { act, fireEvent, render } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import {
  CANCEL,
  DEVICE_ROUTE_REVOKED,
  REVOKE_LABEL,
  REVOKE_ONLY_WAY_BACK,
  REVOKE_OTHER_SESSIONS,
  REVOKE_PERMANENT,
} from "./account-text";
import { RevokeControl } from "./RevokeControl";
import type { RevokeOutcome } from "./revoke-device";

/** A double that always resolves to the same outcome, and records every call. */
function revokeReturning(outcome: RevokeOutcome) {
  return vi.fn(async (): Promise<RevokeOutcome> => outcome);
}

describe("stopping this device signing in", () => {
  it("offers nothing to a browser that is not signed in", () => {
    const revoke = revokeReturning({ kind: "revoked" });
    const { container } = render(
      <RevokeControl deviceRouteLive={true} signedIn={false} revoke={revoke} />,
    );

    expect(container.firstChild).toBeNull();
  });

  it("offers nothing once the device route is already gone", () => {
    const revoke = revokeReturning({ kind: "revoked" });
    const { container } = render(
      <RevokeControl deviceRouteLive={false} signedIn={true} revoke={revoke} />,
    );

    expect(container.firstChild).toBeNull();
  });

  it("offers the control where both routes are live", () => {
    const revoke = revokeReturning({ kind: "revoked" });
    const { getByRole, queryByText } = render(
      <RevokeControl deviceRouteLive={true} signedIn={true} revoke={revoke} />,
    );

    expect(getByRole("button", { name: REVOKE_LABEL })).not.toBeNull();
    expect(queryByText(REVOKE_PERMANENT)).toBeNull();
  });

  it("states all three facts before it acts, and acts on nothing until it is confirmed", () => {
    const revoke = revokeReturning({ kind: "revoked" });
    const { getByRole, queryByText } = render(
      <RevokeControl deviceRouteLive={true} signedIn={true} revoke={revoke} />,
    );

    fireEvent.click(getByRole("button", { name: REVOKE_LABEL }));

    expect(queryByText(REVOKE_PERMANENT)).not.toBeNull();
    expect(queryByText(REVOKE_OTHER_SESSIONS)).not.toBeNull();
    expect(queryByText(REVOKE_ONLY_WAY_BACK)).not.toBeNull();
    expect(revoke).toHaveBeenCalledTimes(0);
  });

  it("calls once, and only from the confirming control", async () => {
    const confirmed = revokeReturning({ kind: "revoked" });
    const confirmRun = render(
      <RevokeControl
        deviceRouteLive={true}
        signedIn={true}
        revoke={confirmed}
      />,
    );
    fireEvent.click(confirmRun.getByRole("button", { name: REVOKE_LABEL }));
    await act(async () => {
      fireEvent.click(confirmRun.getByRole("button", { name: REVOKE_LABEL }));
    });

    expect(confirmed).toHaveBeenCalledTimes(1);
    confirmRun.unmount();

    const cancelled = revokeReturning({ kind: "revoked" });
    const cancelRun = render(
      <RevokeControl
        deviceRouteLive={true}
        signedIn={true}
        revoke={cancelled}
      />,
    );
    fireEvent.click(cancelRun.getByRole("button", { name: REVOKE_LABEL }));
    fireEvent.click(cancelRun.getByRole("button", { name: CANCEL }));

    expect(cancelled).toHaveBeenCalledTimes(0);
    expect(
      cancelRun.queryByRole("button", { name: REVOKE_LABEL }),
    ).not.toBeNull();
  });

  it("offers nothing more once the device route has been stopped", async () => {
    const revoke = revokeReturning({ kind: "revoked" });
    const { getByRole, queryByRole, queryByText } = render(
      <RevokeControl deviceRouteLive={true} signedIn={true} revoke={revoke} />,
    );
    fireEvent.click(getByRole("button", { name: REVOKE_LABEL }));
    await act(async () => {
      fireEvent.click(getByRole("button", { name: REVOKE_LABEL }));
    });

    expect(queryByRole("button", { name: REVOKE_LABEL })).toBeNull();
    expect(queryByText(DEVICE_ROUTE_REVOKED)).not.toBeNull();
  });

  it("says so and stays offered when the server refuses", async () => {
    const refusedNoCredential = revokeReturning({ kind: "no-credential" });
    const noCredentialRun = render(
      <RevokeControl
        deviceRouteLive={true}
        signedIn={true}
        revoke={refusedNoCredential}
      />,
    );
    fireEvent.click(
      noCredentialRun.getByRole("button", { name: REVOKE_LABEL }),
    );
    await act(async () => {
      fireEvent.click(
        noCredentialRun.getByRole("button", { name: REVOKE_LABEL }),
      );
    });
    expect(
      noCredentialRun.queryByRole("button", { name: REVOKE_LABEL }),
    ).not.toBeNull();
    const noCredentialMessage = noCredentialRun.getByRole("status").textContent;
    noCredentialRun.unmount();

    const refusedFailed = revokeReturning({ kind: "failed" });
    const failedRun = render(
      <RevokeControl
        deviceRouteLive={true}
        signedIn={true}
        revoke={refusedFailed}
      />,
    );
    fireEvent.click(failedRun.getByRole("button", { name: REVOKE_LABEL }));
    await act(async () => {
      fireEvent.click(failedRun.getByRole("button", { name: REVOKE_LABEL }));
    });
    expect(
      failedRun.queryByRole("button", { name: REVOKE_LABEL }),
    ).not.toBeNull();
    const failedMessage = failedRun.getByRole("status").textContent;

    expect(noCredentialMessage).not.toBe(failedMessage);
  });
});
