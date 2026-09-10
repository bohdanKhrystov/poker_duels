import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactElement,
  type ReactNode,
} from "react";
import type { ProfileStripState } from "./profile-strip";
import type { SetNameOutcome } from "./set-name";

interface ProfileContextValue {
  readonly state: ProfileStripState | null;
  readonly reportNameWrite: (outcome: SetNameOutcome) => void;
  readonly refresh: () => void;
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
  const live = useRef(true);

  // Armed on every mount and disarmed on every unmount. React's StrictMode
  // mounts, unmounts and remounts a tree in development: a ref that was only
  // ever set to false would stay false after the remount, and the answer to a
  // read that landed afterwards would be dropped on the floor — the lobby
  // showed no profile, no name ask and no coin balance under `npm run dev`.
  useEffect(() => {
    live.current = true;
    return (): void => {
      live.current = false;
    };
  }, []);

  useEffect(() => {
    void read().then((answer) => {
      if (live.current) setState(answer);
    });
  }, [read, live]);

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

  // Re-reads the profile from the server and adopts the answer (`ADR-0132` §6).
  // Guards against unmounting between the call and the answer with the same `live`
  // flag the mount effect uses. Any answer — profile, no-profile, or unavailable —
  // is adopted wholesale; the client never composes a status code with a held
  // profile or keeps a stale profile because the fresh answer was worse.
  const refresh = useCallback((): void => {
    void read().then((answer) => {
      if (live.current) setState(answer);
    });
  }, [read, live]);

  const contextValue = useMemo<ProfileContextValue>(
    () => ({ state, reportNameWrite, refresh }),
    [state, reportNameWrite, refresh],
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

/**
 * Re-reads the profile from the server and adopts the answer, the only way
 * the held profile is replaced other than the mount read and `reportNameWrite`.
 *
 * Called by code that knows the server has changed the caller's own profile
 * — at this time, the sole case is `signUp`'s `signed-up` outcome (`ADR-0132` §6).
 *
 * A no-op where no provider is above — mirrors `useReportNameWrite`'s contract
 * for the same case.
 */
export function useRefreshProfile(): () => void {
  const ctx = useContext(ProfileContext);
  return ctx?.refresh ?? ((): void => {});
}
