import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactElement,
  type ReactNode,
} from "react";
import type { ProfileStripState } from "./profile-strip";
import type { SetNameOutcome } from "./set-name";

interface ProfileContextValue {
  readonly state: ProfileStripState | null;
  readonly reportNameWrite: (outcome: SetNameOutcome) => void;
}

const ProfileContext = createContext<ProfileContextValue | null>(null);

/**
 * Runs `read` once above the tree and puts its answer within reach of the lobby.
 *
 * `read` must be a stable reference (a module-scope constant, not an inline
 * arrow) — it is the effect's only dependency, and a reference that changes
 * on every render would re-run the read on every render with it.
 */
export function ProfileProvider(props: {
  read: () => Promise<ProfileStripState>;
  children: ReactNode;
}): ReactElement {
  const { read, children } = props;
  const [state, setState] = useState<ProfileStripState | null>(null);

  useEffect(() => {
    let live = true;
    void read().then((answer) => {
      if (live) setState(answer);
    });
    return (): void => {
      live = false;
    };
  }, [read]);

  // `SetNameOutcome`'s "named" case already carries the profile the server
  // returned, never the string the player typed (`ADR-0029` §5) — adopted
  // here directly, with no second round trip. Recent duels are untouched by
  // a name write, so the held list survives; any other outcome kind is a
  // refusal and leaves the held profile exactly as it was.
  const reportNameWrite = useCallback((outcome: SetNameOutcome): void => {
    if (outcome.kind !== "named") return;
    setState((current) => ({
      kind: "profile",
      profile: outcome.profile,
      duels: current?.kind === "profile" ? current.duels : [],
    }));
  }, []);

  const contextValue = useMemo<ProfileContextValue>(
    () => ({ state, reportNameWrite }),
    [state, reportNameWrite],
  );

  return (
    <ProfileContext.Provider value={contextValue}>
      {children}
    </ProfileContext.Provider>
  );
}

/** The strip's answer, or `null` before it lands and where no provider is above. */
export function useProfileStrip(): ProfileStripState | null {
  return useContext(ProfileContext)?.state ?? null;
}

/**
 * The function to call with the outcome of a name write, so the profile the
 * provider holds catches up on the same render instead of waiting for the
 * next boot.
 *
 * A no-op where no provider is above — mirrors `useProfileStrip`'s answer
 * for the same case.
 */
export function useReportNameWrite(): (outcome: SetNameOutcome) => void {
  const ctx = useContext(ProfileContext);
  return ctx?.reportNameWrite ?? ((): void => {});
}
