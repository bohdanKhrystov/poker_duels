import {
  createContext,
  useContext,
  type ReactElement,
  type ReactNode,
} from "react";
import type { SetNameOutcome } from "./set-name";

const SetNameContext = createContext<
  ((name: string) => Promise<SetNameOutcome>) | null
>(null);

/**
 * Makes a `setName` function available to components below in the tree.
 *
 * The function must be a stable reference (a module-scope constant, not an inline
 * arrow) — this provider hands it down as-is, and a reference that changes
 * on every render would appear to change to consumers as well.
 */
export function SetNameProvider(props: {
  setName: (name: string) => Promise<SetNameOutcome>;
  children: ReactNode;
}): ReactElement {
  const { setName, children } = props;

  return (
    <SetNameContext.Provider value={setName}>
      {children}
    </SetNameContext.Provider>
  );
}

/** The `setName` function, or `null` where no provider is above. */
export function useSetName():
  ((name: string) => Promise<SetNameOutcome>) | null {
  return useContext(SetNameContext);
}
