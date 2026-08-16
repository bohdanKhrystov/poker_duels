import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactElement,
  type ReactNode,
} from "react";
import type { ProfileStripState } from "./profile-strip";

const ProfileContext = createContext<ProfileStripState | null>(null);

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

  return (
    <ProfileContext.Provider value={state}>{children}</ProfileContext.Provider>
  );
}

/** The strip's answer, or `null` before it lands and where no provider is above. */
export function useProfileStrip(): ProfileStripState | null {
  return useContext(ProfileContext);
}
