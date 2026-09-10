import {
  createContext,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactElement,
  type ReactNode,
} from "react";

/**
 * `false` is `ADR-0135` §6's rule, written where it cannot be forgotten: "a
 * client that has not been told reads `false`." A consumer with no provider
 * above it keeps its profile, and so does one whose read has not returned.
 */
const DeviceStandingContext = createContext<boolean>(false);

/**
 * Runs `read` once above the tree and puts its answer within reach of the
 * lobby: whether the pair of credentials on the request that produced this
 * session, not the player, hands a new profile on sign-out (`ADR-0135`
 * §Consequences). It is a second provider rather than a field on
 * `ProfileStripState` precisely because that state's all-or-nothing contract
 * would blank the front door's strip on an unavailable read.
 *
 * `read` must be a stable reference (a module-scope constant, not an inline
 * arrow) — it is the effect's only dependency, and a reference that changes
 * on every render would re-run the read on every render with it.
 */
export function DeviceStandingProvider(props: {
  read: () => Promise<boolean>;
  children: ReactNode;
}): ReactElement {
  const { read, children } = props;
  const [handsANewProfile, setHandsANewProfile] = useState<boolean>(false);
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
      if (live.current) setHandsANewProfile(answer);
    });
  }, [read, live]);

  return (
    <DeviceStandingContext.Provider value={handsANewProfile}>
      {children}
    </DeviceStandingContext.Provider>
  );
}

/**
 * Whether the pair of credentials that produced this session hands a new
 * profile on sign-out: `false` before the read has answered, and `false`
 * where no provider is above, mirroring `useProfileStrip`'s answer for the
 * same case.
 */
export function useSignOutHandsANewProfile(): boolean {
  return useContext(DeviceStandingContext);
}
